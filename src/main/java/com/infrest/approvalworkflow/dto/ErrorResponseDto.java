package com.infrest.approvalworkflow.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDto> fieldErrors
) {
    public record FieldErrorDto(
            String field,
            String message
    ) {}

    public static ErrorResponseDto of(int status, String error, String message, String path) {
        return new ErrorResponseDto(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponseDto ofValidation(int status, String error, String message, String path, List<FieldErrorDto> fieldErrors) {
        return new ErrorResponseDto(Instant.now(), status, error, message, path, fieldErrors);
    }
}
