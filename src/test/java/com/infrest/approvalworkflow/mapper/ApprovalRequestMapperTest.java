package com.infrest.approvalworkflow.mapper;

import com.infrest.approvalworkflow.dto.ApprovalRequestResponseDto;
import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.ApprovalStep;
import com.infrest.approvalworkflow.entity.RequestStatus;
import com.infrest.approvalworkflow.entity.StepStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalRequestMapperTest {

    private final ApprovalRequestMapper mapper = new ApprovalRequestMapper();

    @Test
    void toDto_shouldMapAllTopLevelFields() {
        ApprovalRequest entity = ApprovalRequest.builder()
                .id(UUID.randomUUID())
                .title("New laptop")
                .description("Engineering needs a new laptop")
                .department("Engineering")
                .requesterName("Ada")
                .status(RequestStatus.IN_PROGRESS)
                .currentLevel(2)
                .version(3L)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .approvalSteps(List.of())
                .build();

        ApprovalRequestResponseDto dto = mapper.toDto(entity);

        assertThat(dto.id()).isEqualTo(entity.getId());
        assertThat(dto.title()).isEqualTo("New laptop");
        assertThat(dto.description()).isEqualTo("Engineering needs a new laptop");
        assertThat(dto.department()).isEqualTo("Engineering");
        assertThat(dto.requesterName()).isEqualTo("Ada");
        assertThat(dto.status()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThat(dto.currentLevel()).isEqualTo(2);
        assertThat(dto.version()).isEqualTo(3L);
        assertThat(dto.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(dto.updatedAt()).isEqualTo(entity.getUpdatedAt());
    }

    @Test
    void toDto_shouldMapStepsSortedByLevelNumberRegardlessOfInputOrder() {
        ApprovalStep level2 = ApprovalStep.builder()
                .id(UUID.randomUUID())
                .levelNumber(2)
                .approverName("Linus")
                .status(StepStatus.PENDING)
                .build();

        ApprovalStep level1 = ApprovalStep.builder()
                .id(UUID.randomUUID())
                .levelNumber(1)
                .approverName("Grace")
                .status(StepStatus.APPROVED)
                .comments("looks good")
                .actedAt(Instant.parse("2026-01-01T12:00:00Z"))
                .build();

        // Steps deliberately added out of order to verify the mapper re-sorts them.
        ApprovalRequest entity = ApprovalRequest.builder()
                .id(UUID.randomUUID())
                .title("Conference budget")
                .department("Marketing")
                .requesterName("Bob")
                .status(RequestStatus.IN_PROGRESS)
                .currentLevel(2)
                .approvalSteps(List.of(level2, level1))
                .build();

        ApprovalRequestResponseDto dto = mapper.toDto(entity);

        assertThat(dto.approvalSteps()).hasSize(2);
        assertThat(dto.approvalSteps().get(0).levelNumber()).isEqualTo(1);
        assertThat(dto.approvalSteps().get(0).approverName()).isEqualTo("Grace");
        assertThat(dto.approvalSteps().get(0).status()).isEqualTo(StepStatus.APPROVED);
        assertThat(dto.approvalSteps().get(0).comments()).isEqualTo("looks good");
        assertThat(dto.approvalSteps().get(1).levelNumber()).isEqualTo(2);
        assertThat(dto.approvalSteps().get(1).approverName()).isEqualTo("Linus");
    }

    @Test
    void toDto_withNoSteps_shouldReturnEmptyStepList() {
        ApprovalRequest entity = ApprovalRequest.builder()
                .id(UUID.randomUUID())
                .title("Empty request")
                .department("Ops")
                .requesterName("Carla")
                .status(RequestStatus.PENDING)
                .currentLevel(1)
                .approvalSteps(List.of())
                .build();

        ApprovalRequestResponseDto dto = mapper.toDto(entity);

        assertThat(dto.approvalSteps()).isEmpty();
    }
}