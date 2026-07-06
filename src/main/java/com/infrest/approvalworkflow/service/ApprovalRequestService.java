package com.infrest.approvalworkflow.service;

import com.infrest.approvalworkflow.dto.ApprovalActionDto;
import com.infrest.approvalworkflow.dto.CreateRequestDto;
import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ApprovalRequestService {

    ApprovalRequest create(CreateRequestDto dto);

    ApprovalRequest getById(UUID id);

    Page<ApprovalRequest> search(RequestStatus status, String department, Pageable pageable);

    ApprovalRequest approve(UUID requestId, ApprovalActionDto action);

    ApprovalRequest reject(UUID requestId, ApprovalActionDto action);
}
