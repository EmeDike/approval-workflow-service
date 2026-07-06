package com.infrest.approvalworkflow.repository;

import com.infrest.approvalworkflow.entity.ApprovalRequest;
import com.infrest.approvalworkflow.entity.ApprovalStep;
import com.infrest.approvalworkflow.entity.RequestStatus;
import com.infrest.approvalworkflow.entity.StepStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApprovalRequestRepositoryTest {

    @Autowired
    private ApprovalRequestRepository repository;

    private ApprovalRequest engineeringRequest;
    private ApprovalRequest hrRequest;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        engineeringRequest = buildRequest("Engineering", RequestStatus.PENDING);
        hrRequest = buildRequest("HR", RequestStatus.APPROVED);

        repository.save(engineeringRequest);
        repository.save(hrRequest);
    }

    private ApprovalRequest buildRequest(String department, RequestStatus status) {
        ApprovalRequest request = ApprovalRequest.builder()
                .title("Request for " + department)
                .department(department)
                .requesterName("Tester")
                .status(status)
                .currentLevel(status == RequestStatus.PENDING ? 1 : null)
                .build();

        ApprovalStep step = ApprovalStep.builder()
                .levelNumber(1)
                .approverName("Approver")
                .status(StepStatus.PENDING)
                .build();

        request.addStep(step);
        return request;
    }

    @Test
    void findWithStepsById_shouldReturnRequestWithStepsLoaded() {
        Optional<ApprovalRequest> found = repository.findWithStepsById(engineeringRequest.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getApprovalSteps()).hasSize(1);
        assertThat(found.get().getApprovalSteps().get(0).getApproverName()).isEqualTo("Approver");
    }

    @Test
    void findWithStepsById_whenIdDoesNotExist_shouldReturnEmpty() {
        Optional<ApprovalRequest> found = repository.findWithStepsById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void search_withNoFilters_shouldReturnAllRequests() {
        Pageable pageable = PageRequest.of(0, 10);

        var result = repository.search(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void search_filteredByDepartment_shouldReturnOnlyMatching() {
        Pageable pageable = PageRequest.of(0, 10);

        var result = repository.search(null, "Engineering", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getDepartment()).isEqualTo("Engineering");
    }

    @Test
    void search_filteredByStatus_shouldReturnOnlyMatching() {
        Pageable pageable = PageRequest.of(0, 10);

        var result = repository.search(RequestStatus.APPROVED, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void search_filteredByStatusAndDepartment_shouldReturnOnlyMatchingBoth() {
        Pageable pageable = PageRequest.of(0, 10);

        var result = repository.search(RequestStatus.PENDING, "HR", pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}