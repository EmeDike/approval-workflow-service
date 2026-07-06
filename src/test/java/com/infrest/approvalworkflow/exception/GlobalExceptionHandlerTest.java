package com.infrest.approvalworkflow.exception;

import com.infrest.approvalworkflow.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/approval-requests/123");
    }

    @Test
    void handleNotFound_shouldReturn404WithMessageAndPath() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Approval request not found: 123");

        ResponseEntity<ErrorResponseDto> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("Approval request not found: 123");
        assertThat(body.path()).isEqualTo("/api/approval-requests/123");
        assertThat(body.fieldErrors()).isNull();
        assertThat(body.timestamp()).isNotNull();
    }

    @Test
    void handleInvalidState_shouldReturn409WithMessage() {
        InvalidWorkflowStateException ex =
                new InvalidWorkflowStateException("Level 1 must be actioned by 'Grace', not 'Linus'");

        ResponseEntity<ErrorResponseDto> response = handler.handleInvalidState(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.error()).isEqualTo("Conflict");
        assertThat(body.message()).isEqualTo("Level 1 must be actioned by 'Grace', not 'Linus'");
        assertThat(body.path()).isEqualTo("/api/approval-requests/123");
    }

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() {
        FieldError titleError = new FieldError("createRequestDto", "title", "title is required");
        FieldError deptError = new FieldError("createRequestDto", "department", "department is required");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(titleError, deptError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponseDto> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Validation failed for one or more fields");
        assertThat(body.fieldErrors()).hasSize(2);
        assertThat(body.fieldErrors().get(0).field()).isEqualTo("title");
        assertThat(body.fieldErrors().get(0).message()).isEqualTo("title is required");
        assertThat(body.fieldErrors().get(1).field()).isEqualTo("department");
        assertThat(body.fieldErrors().get(1).message()).isEqualTo("department is required");
    }

    @Test
    void handleValidation_withNoFieldErrors_shouldReturnEmptyList() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponseDto> response = handler.handleValidation(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void handleOptimisticLock_shouldReturn409WithGenericRetryMessage() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("ApprovalRequest", "123");

        ResponseEntity<ErrorResponseDto> response = handler.handleOptimisticLock(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.message())
                .isEqualTo("The record was modified by another request. Please retry with the latest version.");
        assertThat(body.path()).isEqualTo("/api/approval-requests/123");
    }

    @Test
    void handleGeneric_shouldReturn500WithoutLeakingExceptionDetails() {
        RuntimeException ex = new RuntimeException("some internal NullPointerException stack trace detail");

        ResponseEntity<ErrorResponseDto> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        assertThat(body.message()).doesNotContain("NullPointerException");
        assertThat(body.path()).isEqualTo("/api/approval-requests/123");
    }
}