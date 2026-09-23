package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.PasswordResetRequestStatus;
import com.throughline.taskmanagement.model.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    /** At most one row can match this at a time by construction — see PasswordResetRequest's
     *  class comment. Used both to reject a duplicate submission and to find the row a
     *  Super Admin is resolving (fulfilling or dismissing). */
    Optional<PasswordResetRequest> findByPersonIdAndStatus(Long personId, PasswordResetRequestStatus status);

    /** Batch form of the above — backs PersonServiceImpl.getAllPeople's "show a pending
     *  request badge" without an N+1 query per row on the page. */
    List<PasswordResetRequest> findByPersonIdInAndStatus(List<Long> personIds, PasswordResetRequestStatus status);
}
