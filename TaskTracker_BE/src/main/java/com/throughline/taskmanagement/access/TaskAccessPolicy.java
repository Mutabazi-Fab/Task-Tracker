package com.throughline.taskmanagement.access;

import com.throughline.taskmanagement.enums.AccessResourceType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.repository.AccessGrantRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may open a task page and act on it. A person may if they are:
 *  - an Executive or Super Admin (everything);
 *  - the person who assigned it, or a Director of the department it belongs to;
 *  - assigned to it, on the team it is assigned to, or on the chain of a task that is theirs
 *    (the same rule the task lists use, see TaskRepository.VISIBLE_TO_PERSON_OR_ANCESTOR);
 *  - given an access grant for it (see AccessGrantService).
 * Anything under a task you can open (its subtasks) can be opened too.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAccessPolicy {

    private final TaskRepository taskRepository;
    private final PersonRepository personRepository;
    private final AccessGrantRepository accessGrantRepository;

    public boolean canView(Long taskId, Long viewerId) {
        Person viewer = personRepository.findById(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (Role.isAtLeastExecutive(viewer.getRole())) {
            return true;
        }
        for (Task node = task; node != null; node = node.getParentTask()) {
            if (grantsAccess(viewer, node)) {
                return true;
            }
        }
        return false;
    }

    /** Does this task, or anything above it in its chain, belong to that department? Backs a Director's
     *  right to share a task from their own department. */
    public boolean belongsToDepartment(Long taskId, Long departmentId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        for (Task node = task; node != null; node = node.getParentTask()) {
            Long nodeDepartment = departmentIdOf(node);
            if (nodeDepartment != null && nodeDepartment.equals(departmentId)) {
                return true;
            }
        }
        return false;
    }

    public void requireCanView(Long taskId, Long viewerId) {
        if (!canView(taskId, viewerId)) {
            throw new ForbiddenActionException(
                    "This task isn't shared with you. Ask a Super Admin or the CEO to share it with you.");
        }
    }

    private boolean grantsAccess(Person viewer, Task node) {
        if (node.getAssignedBy() != null && node.getAssignedBy().getId().equals(viewer.getId())) {
            return true;
        }
        if (taskRepository.isVisibleToPerson(node.getId(), viewer.getId())) {
            return true;
        }
        if (accessGrantRepository.existsByGranteeIdAndResourceTypeAndResourceIdAndRevokedAtIsNull(
                viewer.getId(), AccessResourceType.TASK, node.getId())) {
            return true;
        }
        Long departmentId = departmentIdOf(node);
        return viewer.getRole() == Role.DIRECTOR && viewer.getDepartment() != null
                && departmentId != null && departmentId.equals(viewer.getDepartment().getId());
    }

    /** A task's department, derived from whoever it is assigned to (exactly one of the three is set). */
    private Long departmentIdOf(Task task) {
        if (task.getAssignedDepartment() != null) {
            return task.getAssignedDepartment().getId();
        }
        if (task.getAssignedTeam() != null && task.getAssignedTeam().getDepartment() != null) {
            return task.getAssignedTeam().getDepartment().getId();
        }
        if (task.getAssignedPerson() != null && task.getAssignedPerson().getDepartment() != null) {
            return task.getAssignedPerson().getDepartment().getId();
        }
        return null;
    }
}
