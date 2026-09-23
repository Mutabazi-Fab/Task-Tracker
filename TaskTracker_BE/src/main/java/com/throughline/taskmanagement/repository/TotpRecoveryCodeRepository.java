package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.TotpRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TotpRecoveryCodeRepository extends JpaRepository<TotpRecoveryCode, Long> {

    /** Small list per person (a handful at most) — checked one by one against a submitted
     *  code with the PasswordEncoder, same shape as checking a password, since codeHash is
     *  salted and can't be looked up by an equality match on the plain code. */
    List<TotpRecoveryCode> findByPersonIdAndUsedAtIsNull(Long personId);

    void deleteByPersonId(Long personId);
}
