package com.infrest.approvalworkflow.service;

import com.infrest.approvalworkflow.dto.ApprovalActionDto;
import com.infrest.approvalworkflow.dto.CreateRequestDto;
import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.ApprovalStep;
import com.infrest.approvalworkflow.entity.RequestStatus;
import com.infrest.approvalworkflow.entity.StepStatus;
import com.infrest.approvalworkflow.exception.InvalidWorkflowStateException;
import com.infrest.approvalworkflow.exception.ResourceNotFoundException;
import com.infrest.approvalworkflow.repository.ApprovalRequestRepository;
import com.infrest.approvalworkflow.service.impl.ApprovalRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalRequestServiceImplTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @InjectMocks
    private ApprovalRequestServiceImpl service;

    private UUID requestId;
    private ApprovalRequest request;
    private ApprovalStep step1;
    private ApprovalStep step2;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();

        step1 = ApprovalStep.builder()
                .id(UUID.randomUUID())
                .levelNumber(1)
                .approverName("Grace")
                .status(StepStatus.PENDING)
                .build();

        step2 = ApprovalStep.builder()
                .id(UUID.randomUUID())
                .levelNumber(2)
                .approverName("Linus")
                .status(StepStatus.PENDING)
                .build();

        request = ApprovalRequest.builder()
                .id(requestId)
                .title("New laptop")
                .department("Engineering")
                .requesterName("Ada")
                .status(RequestStatus.PENDING)
                .currentLevel(1)
                .approvalSteps(List.of(step1, step2))
                .build();
    }

    @Test
    void create_shouldBuildRequestWithStepsInOrder() {
        CreateRequestDto dto = new CreateRequestDto(
                "New laptop", "desc", "Engineering", "Ada",
                List.of(new CreateRequestDto.ApproverDto("Grace"), new CreateRequestDto.ApproverDto("Linus"))
        );

        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ApprovalRequest result = service.create(dto);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getCurrentLevel()).isEqualTo(1);
        assertThat(result.getApprovalSteps()).hasSize(2);
        assertThat(result.getApprovalSteps().get(0).getApproverName()).isEqualTo("Grace");
        assertThat(result.getApprovalSteps().get(1).getApproverName()).isEqualTo("Linus");
    }

    @Test
    void approve_atNonFinalLevel_shouldAdvanceToNextLevel() {
        when(requestRepository.findWithStepsById(requestId)).thenReturn(Optional.of(request));

        ApprovalRequest result = service.approve(requestId, new ApprovalActionDto("Grace", "ok"));

        assertThat(result.getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThat(result.getCurrentLevel()).isEqualTo(2);
        assertThat(step1.getStatus()).isEqualTo(StepStatus.APPROVED);
    }

    @Test
    void approve_atFinalLevel_shouldMarkRequestApproved() {
        request.setCurrentLevel(2);
        when(requestRepository.findWithStepsById(requestId)).thenReturn(Optional.of(request));

        ApprovalRequest result = service.approve(requestId, new ApprovalActionDto("Linus", "ok"));

        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(result.getCurrentLevel()).isNull();
    }

    @Test
    void reject_shouldMarkRequestRejectedRegardlessOfLevel() {
        when(requestRepository.findWithStepsById(requestId)).thenReturn(Optional.of(request));

        ApprovalRequest result = service.reject(requestId, new ApprovalActionDto("Grace", "not needed"));

        assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.getCurrentLevel()).isNull();
        assertThat(step1.getStatus()).isEqualTo(StepStatus.REJECTED);
    }

    @Test
    void approve_byWrongActor_shouldThrow() {
        when(requestRepository.findWithStepsById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(requestId, new ApprovalActionDto("Linus", "ok")))
                .isInstanceOf(InvalidWorkflowStateException.class)
                .hasMessageContaining("must be actioned by");
    }

    @Test
    void approve_onAlreadyTerminalRequest_shouldThrow() {
        request.setStatus(RequestStatus.APPROVED);
        when(requestRepository.findWithStepsById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(requestId, new ApprovalActionDto("Grace", "ok")))
                .isInstanceOf(InvalidWorkflowStateException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    void approve_onAlreadyActionedLevel_shouldThrow() {
        step1.setStatus(StepStatus.APPROVED);
        when(requestRepository.findWithStepsById(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(requestId, new ApprovalActionDto("Grace", "ok")))
                .isInstanceOf(InvalidWorkflowStateException.class)
                .hasMessageContaining("already been actioned");
    }

    @Test
    void getById_whenNotFound_shouldThrowResourceNotFound() {
        UUID missingId = UUID.randomUUID();
        when(requestRepository.findWithStepsById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}