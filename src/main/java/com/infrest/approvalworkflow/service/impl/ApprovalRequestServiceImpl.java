package com.infrest.approvalworkflow.service.impl;

import com.infrest.approvalworkflow.dto.ApprovalActionDto;
import com.infrest.approvalworkflow.dto.CreateRequestDto;
import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.ApprovalStep;
import com.infrest.approvalworkflow.entity.RequestStatus;
import com.infrest.approvalworkflow.entity.StepStatus;
import com.infrest.approvalworkflow.exception.InvalidWorkflowStateException;
import com.infrest.approvalworkflow.exception.ResourceNotFoundException;
import com.infrest.approvalworkflow.repository.ApprovalRequestRepository;
import com.infrest.approvalworkflow.service.ApprovalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalRequestServiceImpl implements ApprovalRequestService {

    private final ApprovalRequestRepository requestRepository;

    @Override
    public ApprovalRequest create(CreateRequestDto dto) {
        ApprovalRequest request = ApprovalRequest.builder()
                .title(dto.title())
                .description(dto.description())
                .department(dto.department())
                .requesterName(dto.requesterName())
                .status(RequestStatus.PENDING)
                .currentLevel(1)
                .build();

        List<CreateRequestDto.ApproverDto> approvers = dto.approvers();
        for (int i = 0; i < approvers.size(); i++) {
            ApprovalStep step = ApprovalStep.builder()
                    .levelNumber(i + 1)
                    .approverName(approvers.get(i).approverName())
                    .status(StepStatus.PENDING)
                    .build();
            request.addStep(step);
        }

        return requestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequest getById(UUID id) {
        return requestRepository.findWithStepsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequest> search(RequestStatus status, String department, Pageable pageable) {
        return requestRepository.search(status, department, pageable);
    }

    @Override
    public ApprovalRequest approve(UUID requestId, ApprovalActionDto action) {
        return actOnCurrentLevel(requestId, action, StepStatus.APPROVED);
    }

    @Override
    public ApprovalRequest reject(UUID requestId, ApprovalActionDto action) {
        return actOnCurrentLevel(requestId, action, StepStatus.REJECTED);
    }

    private ApprovalRequest actOnCurrentLevel(UUID requestId, ApprovalActionDto action, StepStatus outcome) {
        ApprovalRequest request = requestRepository.findWithStepsById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + requestId));

        if (request.getStatus() == RequestStatus.APPROVED || request.getStatus() == RequestStatus.REJECTED) {
            throw new InvalidWorkflowStateException(
                    "Request " + requestId + " is already in a terminal state: " + request.getStatus());
        }

        ApprovalStep currentStep = request.getApprovalSteps().stream()
                .filter(step -> step.getLevelNumber().equals(request.getCurrentLevel()))
                .min(Comparator.comparing(ApprovalStep::getLevelNumber))
                .orElseThrow(() -> new InvalidWorkflowStateException(
                        "Request " + requestId + " has no pending level configured - data is inconsistent"));

        if (currentStep.getStatus() != StepStatus.PENDING) {
            throw new InvalidWorkflowStateException(
                    "Level " + currentStep.getLevelNumber() + " has already been actioned");
        }

        if (!currentStep.getApproverName().equalsIgnoreCase(action.actorName())) {
            throw new InvalidWorkflowStateException(
                    "Level " + currentStep.getLevelNumber() + " must be actioned by '"
                            + currentStep.getApproverName() + "', not '" + action.actorName() + "'");
        }

        currentStep.setStatus(outcome);
        currentStep.setComments(action.comments());
        currentStep.setActedAt(Instant.now());

        if (outcome == StepStatus.REJECTED) {
            request.setStatus(RequestStatus.REJECTED);
            request.setCurrentLevel(null);
            return request;
        }

        boolean isLastLevel = request.getApprovalSteps().stream()
                .mapToInt(ApprovalStep::getLevelNumber)
                .max()
                .orElse(currentStep.getLevelNumber()) == currentStep.getLevelNumber();

        if (isLastLevel) {
            request.setStatus(RequestStatus.APPROVED);
            request.setCurrentLevel(null);
        } else {
            request.setStatus(RequestStatus.IN_PROGRESS);
            request.setCurrentLevel(currentStep.getLevelNumber() + 1);
        }

        return request;
    }
}
