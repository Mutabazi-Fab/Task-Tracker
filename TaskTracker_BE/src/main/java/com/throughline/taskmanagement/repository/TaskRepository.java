package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByTaskCode(String taskCode);
    
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
    
    List<Task> findByAssignedPersonId(Long personId);
    
    List<Task> findByAssignedTeamId(Long teamId);
    
    List<Task> findByAssignedById(Long personId);
    
    long countByStatus(TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Task> search(@Param("q") String q);
    
    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN t.reassignments r " +
           "LEFT JOIN t.comments c " +
           "WHERE t.assignedPerson.id = :personId " +
           "OR t.assignedTeam.id IN (SELECT p.team.id FROM Person p WHERE p.id = :personId) " +
           "OR r.fromPerson.id = :personId " +
           "OR c.author.id = :personId")
    List<Task> findTasksConnectedToPerson(@Param("personId") Long personId);
    
    @Query("SELECT COALESCE(AVG(t.progressPercentage), 0.0) FROM Task t WHERE t.assignedPerson.id = :personId")
    Double getAverageProgressByAssignedPersonId(@Param("personId") Long personId);
    
    @Query("SELECT COALESCE(AVG(t.progressPercentage), 0.0) FROM Task t WHERE t.assignedTeam.id = :teamId")
    Double getAverageProgressByAssignedTeamId(@Param("teamId") Long teamId);
    
    @EntityGraph(attributePaths = {"comments", "reassignments"})
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findWithDetailsById(@Param("id") Long id);
    
    @EntityGraph(attributePaths = {"comments", "reassignments"})
    @Query("SELECT t FROM Task t WHERE t.taskCode = :taskCode")
    Optional<Task> findWithDetailsByTaskCode(@Param("taskCode") String taskCode);

    // Drives taskCode generation off the highest existing "TSK-NNNN" suffix rather than row
    // count, so deleting a task never causes the next generated code to collide with a
    // surviving one (COUNT(*)-based generation shrinks on delete; MAX(suffix) does not).
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(task_code FROM 5) AS INTEGER)), 0) FROM tasks", nativeQuery = true)
    int findMaxTaskCodeSequence();
}
