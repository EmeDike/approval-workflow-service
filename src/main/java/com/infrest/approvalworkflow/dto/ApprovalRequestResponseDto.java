package com.infrest.approvalworkflow.dto;

import com.infrest.approvalworkflow.entity.RequestStatus;
import com.infrest.approvalworkflow.entity.StepStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalRequestResponseDto(
        UUID id,
        String title,
        String description,
        String department,
        String requesterName,
        RequestStatus status,
        Integer currentLevel,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<StepDto> approvalSteps
) {
    public record StepDto(
            UUID id,
            Integer levelNumber,
            String approverName,
            StepStatus status,
            String comments,
            Instant actedAt
    ) {}
}
