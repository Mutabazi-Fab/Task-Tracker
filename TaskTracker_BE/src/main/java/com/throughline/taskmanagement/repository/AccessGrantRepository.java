package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.AccessResourceType;
import com.throughline.taskmanagement.model.AccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {

    boolean existsByGranteeIdAndResourceTypeAndResourceIdAndRevokedAtIsNull(
            Long granteeId, AccessResourceType type, Long resourceId);

    Optional<AccessGrant> findByGranteeIdAndResourceTypeAndResourceIdAndRevokedAtIsNull(
            Long granteeId, AccessResourceType type, Long resourceId);

    List<AccessGrant> findByResourceTypeAndResourceIdAndRevokedAtIsNullOrderByGrantedAtDesc(
            AccessResourceType type, Long resourceId);

    List<AccessGrant> findByGranteeIdAndRevokedAtIsNullOrderByGrantedAtDesc(Long granteeId);

    @Query("SELECT g.resourceId FROM AccessGrant g WHERE g.grantee.id = :granteeId "
            + "AND g.resourceType = :type AND g.revokedAt IS NULL")
    Set<Long> findActiveResourceIds(@Param("granteeId") Long granteeId, @Param("type") AccessResourceType type);
}
