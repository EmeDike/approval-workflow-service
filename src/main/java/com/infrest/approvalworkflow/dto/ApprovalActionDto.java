package com.infrest.approvalworkflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalActionDto(

        @NotBlank(message = "actorName is required")
        @Size(max = 150, message = "actorName must be at most 150 characters")
        String actorName,

        @Size(max = 1000, message = "comments must be at most 1000 characters")
        String comments
) {}
