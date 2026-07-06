package com.infrest.approvalworkflow.mapper;

import com.infrest.approvalworkflow.dto.ApprovalRequestResponseDto;
import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.ApprovalStep;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ApprovalRequestMapper {

    public ApprovalRequestResponseDto toDto(ApprovalRequest entity) {
        List<ApprovalRequestResponseDto.StepDto> steps = entity.getApprovalSteps().stream()
                .sorted(Comparator.comparing(ApprovalStep::getLevelNumber))
                .map(this::toStepDto)
                .toList();

        return new ApprovalRequestResponseDto(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDepartment(),
                entity.getRequesterName(),
                entity.getStatus(),
                entity.getCurrentLevel(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                steps
        );
    }

    private ApprovalRequestResponseDto.StepDto toStepDto(ApprovalStep step) {
        return new ApprovalRequestResponseDto.StepDto(
                step.getId(),
                step.getLevelNumber(),
                step.getApproverName(),
                step.getStatus(),
                step.getComments(),
                step.getActedAt()
        );
    }
}
