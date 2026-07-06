package com.infrest.approvalworkflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRequestDto(

        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotBlank(message = "department is required")
        @Size(max = 100, message = "department must be at most 100 characters")
        String department,

        @NotBlank(message = "requesterName is required")
        @Size(max = 150, message = "requesterName must be at most 150 characters")
        String requesterName,

        @NotEmpty(message = "at least one approver is required")
        @Valid
        List<ApproverDto> approvers
) {
    public record ApproverDto(
            @NotBlank(message = "approverName is required")
            @Size(max = 150, message = "approverName must be at most 150 characters")
            String approverName
    ) {}
}
