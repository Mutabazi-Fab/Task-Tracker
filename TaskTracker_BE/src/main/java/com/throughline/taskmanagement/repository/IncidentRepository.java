package com.throughline.taskmanagement.repository;


import com.throughline.taskmanagement.enums.IncidentCategory;
import com.throughline.taskmanagement.enums.IncidentSeverity;
import com.throughline.taskmanagement.enums.IncidentStatus;
import com.throughline.taskmanagement.enums.RegulatorNotifiableStatus;
import com.throughline.taskmanagement.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByIncidentCode(String incidentCode);

    /** Every filter is optional — a null parameter matches everything, so the dashboard's plain "all
     *  incidents" list and its filtered views share one query instead of a combinatorial explosion of
     *  derived-name methods. qPattern is the fully-formed, already-lowercased "%...%" pattern (see
     *  IncidentServiceImpl.getAllIncidents) — NOT built here with CONCAT: binding a null :q through
     *  LOWER(CONCAT('%', :q, '%')) leaves Postgres unable to infer that parameter's type, and it silently
     *  resolves to bytea, which then fails with "function lower(bytea) does not exist" the moment a real
     *  string is compared against it. */
    @Query("SELECT i FROM Incident i WHERE "
            + "(:status IS NULL OR i.status = :status) "
            + "AND (:severity IS NULL OR i.severity = :severity) "
            + "AND (:category IS NULL OR i.category = :category) "
            + "AND (:businessUnit IS NULL OR i.businessUnit = :businessUnit OR i.businessUnit = :legacyBusinessUnit) "
            + "AND (:from IS NULL OR i.dateOccurred >= :from) "
            + "AND (:to IS NULL OR i.dateOccurred <= :to) "
            + "AND (:qPattern IS NULL OR LOWER(i.title) LIKE :qPattern OR LOWER(i.incidentCode) LIKE :qPattern)")
    Page<Incident> search(
            @Param("status") IncidentStatus status,
            @Param("severity") IncidentSeverity severity,
            @Param("category") IncidentCategory category,
            @Param("businessUnit") String businessUnit,
            @Param("legacyBusinessUnit") String legacyBusinessUnit,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("qPattern") String qPattern,
            Pageable pageable);

    // Dashboard KPI tiles — see IncidentServiceImpl.getDashboard.
    long countByStatusIn(List<IncidentStatus> statuses);

    long countBySeverityIn(List<IncidentSeverity> severities);

    long countByRegulatorNotifiable(RegulatorNotifiableStatus status);

    // Backs incidentCode generation (INC-{year}-{seq}) — see IncidentServiceImpl.generateIncidentCode.
    long countByIncidentCodeStartingWith(String prefix);
}
