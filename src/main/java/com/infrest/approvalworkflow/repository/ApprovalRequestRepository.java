package com.infrest.approvalworkflow.repository;

import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    @EntityGraph(attributePaths = "approvalSteps")
    Optional<ApprovalRequest> findWithStepsById(UUID id);

    @Query("SELECT r FROM ApprovalRequest r WHERE (:status IS NULL OR r.status = :status) AND (:department IS NULL OR r.department = :department)")
    Page<ApprovalRequest> search(
            @Param("status") RequestStatus status,
            @Param("department") String department,
            Pageable pageable
    );
}
