package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.ExtensionRequestStatus;
import com.throughline.taskmanagement.model.TaskDeadlineExtensionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDeadlineExtensionRequestRepository extends JpaRepository<TaskDeadlineExtensionRequest, Long> {
    Page<TaskDeadlineExtensionRequest> findByTaskIdOrderByRequestedAtDesc(Long taskId, Pageable pageable);

    /** Every still-undecided request, across every task — the decider each one actually
     *  resolves to isn't a stored column (see TaskServiceImpl.resolveDeadlineDecider, which
     *  walks the parent chain), so filtering "mine" happens in the service layer after this
     *  fetch, not here. Pending requests are a small, naturally self-draining set (each one
     *  leaves this list the moment it's decided), so an unpaginated list is deliberate —
     *  same reasoning as the notification bell, not a scale concern. */
    List<TaskDeadlineExtensionRequest> findByStatusOrderByRequestedAtDesc(ExtensionRequestStatus status);
}
