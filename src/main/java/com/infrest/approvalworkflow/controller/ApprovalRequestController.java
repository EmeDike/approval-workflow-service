package com.infrest.approvalworkflow.controller;

import com.infrest.approvalworkflow.dto.ApprovalActionDto;
import com.infrest.approvalworkflow.dto.ApprovalRequestResponseDto;
import com.infrest.approvalworkflow.dto.CreateRequestDto;
import com.infrest.approvalworkflow.entity.RequestStatus;
import com.infrest.approvalworkflow.mapper.ApprovalRequestMapper;
import com.infrest.approvalworkflow.service.ApprovalRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final ApprovalRequestMapper mapper;

    @PostMapping
    public ResponseEntity<ApprovalRequestResponseDto> create(@Valid @RequestBody CreateRequestDto dto) {
        var created = approvalRequestService.create(dto);
        var body = mapper.toDto(created);
        return ResponseEntity.created(URI.create("/api/approval-requests/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequestResponseDto> getById(@PathVariable UUID id) {
        var found = approvalRequestService.getById(id);
        return ResponseEntity.ok(mapper.toDto(found));
    }

    @GetMapping
    public ResponseEntity<Page<ApprovalRequestResponseDto>> search(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String department,
            Pageable pageable
    ) {
        Page<ApprovalRequestResponseDto> page = approvalRequestService
                .search(status, department, pageable)
                .map(mapper::toDto);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalActionDto action
    ) {
        var updated = approvalRequestService.approve(id, action);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequestResponseDto> reject(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalActionDto action
    ) {
        var updated = approvalRequestService.reject(id, action);
        return ResponseEntity.ok(mapper.toDto(updated));
    }
}
