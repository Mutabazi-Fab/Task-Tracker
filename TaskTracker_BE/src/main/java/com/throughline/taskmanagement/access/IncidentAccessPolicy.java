package com.throughline.taskmanagement.access;

import com.throughline.taskmanagement.enums.AccessResourceType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.model.Incident;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.AccessGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Who may see (and therefore act on) an incident. An incident belongs to the department named in its
 * Business Unit:
 *  - Executive and Super Admin see every incident;
 *  - a Director sees the incidents of their own department;
 *  - whoever reported an incident, or is its Action Owner, always sees it;
 *  - anyone else needs an access grant for that specific incident (see AccessGrantService).
 * Ordinary members of a department do not see its incidents unless one of the above applies to them.
 */
@Component
@RequiredArgsConstructor
public class IncidentAccessPolicy {

    private final AccessGrantRepository accessGrantRepository;

    public boolean seesAll(Person viewer) {
        return Role.isAtLeastExecutive(viewer.getRole());
    }

    public boolean canView(Person viewer, Incident incident) {
        return visibleTo(viewer).test(incident);
    }

    public void requireCanView(Person viewer, Incident incident) {
        if (!canView(viewer, incident)) {
            throw new ForbiddenActionException(
                    "This incident belongs to another department. Ask a Super Admin or the CEO to share it with you.");
        }
    }

    /** Loads the viewer's granted incident ids once, so a whole list can be filtered without a query per row. */
    public Predicate<Incident> visibleTo(Person viewer) {
        if (seesAll(viewer)) {
            return incident -> true;
        }
        Set<Long> granted = accessGrantRepository.findActiveResourceIds(viewer.getId(), AccessResourceType.INCIDENT);
        return incident -> isReporterOrOwner(viewer, incident)
                || isDirectorOfUnit(viewer, incident.getBusinessUnit())
                || granted.contains(incident.getId());
    }

    /** Ids of the incidents shared with the viewer. Never empty, because a SQL IN () with no values is an
     *  error - a placeholder that matches nothing stands in when there are none. */
    public List<Long> grantedIncidentIds(Person viewer) {
        Set<Long> ids = accessGrantRepository.findActiveResourceIds(viewer.getId(), AccessResourceType.INCIDENT);
        return ids.isEmpty() ? List.of(-1L) : List.copyOf(ids);
    }

    /** Business Unit spellings that mean the viewer's own department: its name, and the old enum-constant
     *  spelling that incidents recorded before Business Unit became department-driven still carry. Matches
     *  nothing for someone who isn't a Director with a department. */
    public List<String> ownUnitNames(Person viewer) {
        if (viewer.getRole() == Role.DIRECTOR && viewer.getDepartment() != null) {
            String name = viewer.getDepartment().getName();
            return List.of(name, legacyKey(name));
        }
        return List.of("");
    }

    public static boolean unitMatchesDepartment(String businessUnit, String departmentName) {
        if (businessUnit == null || departmentName == null) {
            return false;
        }
        return businessUnit.equalsIgnoreCase(departmentName) || businessUnit.equals(legacyKey(departmentName));
    }

    public static String legacyKey(String departmentName) {
        return departmentName.trim().toUpperCase().replace(' ', '_');
    }

    private boolean isReporterOrOwner(Person viewer, Incident incident) {
        return incident.getReportedBy().getId().equals(viewer.getId())
                || (incident.getActionOwner() != null && incident.getActionOwner().getId().equals(viewer.getId()));
    }

    private boolean isDirectorOfUnit(Person viewer, String businessUnit) {
        return viewer.getRole() == Role.DIRECTOR && viewer.getDepartment() != null
                && unitMatchesDepartment(businessUnit, viewer.getDepartment().getName());
    }
}
