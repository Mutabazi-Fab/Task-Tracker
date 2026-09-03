package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.AccountStatusChange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatusChangeRepository extends JpaRepository<AccountStatusChange, Long> {
    Page<AccountStatusChange> findAllByOrderByTimestampDesc(Pageable pageable);
}
