package com.infrest.approvalworkflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infrest.approvalworkflow.dto.ApprovalActionDto;
import com.infrest.approvalworkflow.dto.ApprovalRequestResponseDto;
import com.infrest.approvalworkflow.dto.CreateRequestDto;
import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.RequestStatus;
import com.infrest.approvalworkflow.exception.InvalidWorkflowStateException;
import com.infrest.approvalworkflow.exception.ResourceNotFoundException;
import com.infrest.approvalworkflow.mapper.ApprovalRequestMapper;
import com.infrest.approvalworkflow.service.ApprovalRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApprovalRequestController.class)
class ApprovalRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalRequestService approvalRequestService;

    @MockBean
    private ApprovalRequestMapper mapper;

    @Test
    void create_withValidPayload_shouldReturn201() throws Exception {
        UUID id = UUID.randomUUID();
        CreateRequestDto dto = new CreateRequestDto(
                "New laptop", "desc", "Engineering", "Ada",
                List.of(new CreateRequestDto.ApproverDto("Grace"))
        );

        ApprovalRequestResponseDto responseDto = new ApprovalRequestResponseDto(
                id, "New laptop", "desc", "Engineering", "Ada",
                RequestStatus.PENDING, 1, 0L, Instant.now(), Instant.now(), List.of()
        );

        when(approvalRequestService.create(any())).thenReturn(new ApprovalRequest());
        when(mapper.toDto(any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/approval-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/approval-requests/" + id))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void create_withMissingTitle_shouldReturn400() throws Exception {
        CreateRequestDto dto = new CreateRequestDto(
                "", "desc", "Engineering", "Ada",
                List.of(new CreateRequestDto.ApproverDto("Grace"))
        );

        mockMvc.perform(post("/api/approval-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void getById_whenNotFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(approvalRequestService.getById(eq(id)))
                .thenThrow(new ResourceNotFoundException("Approval request not found: " + id));

        mockMvc.perform(get("/api/approval-requests/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void approve_byWrongActor_shouldReturn409() throws Exception {
        UUID id = UUID.randomUUID();
        ApprovalActionDto action = new ApprovalActionDto("WrongPerson", "comment");

        when(approvalRequestService.approve(eq(id), any()))
                .thenThrow(new InvalidWorkflowStateException("Level 1 must be actioned by 'Grace', not 'WrongPerson'"));

        mockMvc.perform(post("/api/approval-requests/{id}/approve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void approve_withMissingActorName_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        ApprovalActionDto action = new ApprovalActionDto("", "comment");

        mockMvc.perform(post("/api/approval-requests/{id}/approve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isBadRequest());
    }
}