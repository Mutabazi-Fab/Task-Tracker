package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.access.IncidentAccessPolicy;
import com.throughline.taskmanagement.access.TaskAccessPolicy;
import com.throughline.taskmanagement.dto.request.GrantAccessRequest;
import com.throughline.taskmanagement.dto.response.AccessGrantResponse;
import com.throughline.taskmanagement.enums.AccessResourceType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.AccessGrant;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.AccessGrantRepository;
import com.throughline.taskmanagement.repository.IncidentRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.service.AccessGrantService;
import com.throughline.taskmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AccessGrantServiceImpl implements AccessGrantService {

    private final AccessGrantRepository accessGrantRepository;
    private final PersonRepository personRepository;
    private final TaskRepository taskRepository;
    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;
    private final TaskAccessPolicy taskAccessPolicy;

    /** The item's code and title, e.g. "TSK-0012 — Deploy the SIEM" — also what proves the item exists. */
    private record Resource(String code, String title) {
        String label() {
            return code + " — " + title;
        }
    }

    @Override
    public AccessGrantResponse grant(GrantAccessRequest request) {
        Person actor = requireCanManage(request.grantedById(), request.resourceType(), request.resourceId());
        if (request.reason() == null || request.reason().isBlank()) {
            throw new InvalidAssignmentException("A reason is required when sharing access.");
        }
        Person grantee = personRepository.findById(request.granteeId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        if (Role.isAtLeastExecutive(grantee.getRole())) {
            throw new InvalidAssignmentException(grantee.getFullName() + " can already see everything.");
        }
        Resource resource = resolve(request.resourceType(), request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("That item no longer exists."));

        Optional<AccessGrant> existing = accessGrantRepository
                .findByGranteeIdAndResourceTypeAndResourceIdAndRevokedAtIsNull(
                        grantee.getId(), request.resourceType(), request.resourceId());
        if (existing.isPresent()) {
            return toResponse(existing.get(), resource);
        }

        AccessGrant grant = new AccessGrant();
        grant.setGrantee(grantee);
        grant.setResourceType(request.resourceType());
        grant.setResourceId(request.resourceId());
        grant.setGrantedBy(actor);
        grant.setReason(request.reason().trim());
        AccessGrant saved = accessGrantRepository.save(grant);

        notificationService.notifyAccessGranted(grantee, actor, request.resourceType(), request.resourceId(), resource.label());
        return toResponse(saved, resource);
    }

    @Override
    public void revoke(Long grantId, Long actorId) {
        AccessGrant grant = accessGrantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Access grant not found"));
        Person actor = requireCanManage(actorId, grant.getResourceType(), grant.getResourceId());
        if (grant.getRevokedAt() != null) {
            return;
        }
        grant.setRevokedAt(LocalDateTime.now());
        grant.setRevokedBy(actor);
        accessGrantRepository.save(grant);

        String label = resolve(grant.getResourceType(), grant.getResourceId()).map(Resource::label).orElse("an item");
        notificationService.notifyAccessRevoked(grant.getGrantee(), actor, label);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessGrantResponse> listForResource(AccessResourceType type, Long resourceId, Long actorId) {
        requireCanManage(actorId, type, resourceId);
        Optional<Resource> resource = resolve(type, resourceId);
        List<AccessGrantResponse> result = new ArrayList<>();
        for (AccessGrant grant : accessGrantRepository
                .findByResourceTypeAndResourceIdAndRevokedAtIsNullOrderByGrantedAtDesc(type, resourceId)) {
            result.add(toResponse(grant, resource.orElse(null)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessGrantResponse> listSharedWith(Long personId) {
        List<AccessGrantResponse> result = new ArrayList<>();
        for (AccessGrant grant : accessGrantRepository.findByGranteeIdAndRevokedAtIsNullOrderByGrantedAtDesc(personId)) {
            resolve(grant.getResourceType(), grant.getResourceId())
                    .ifPresent(resource -> result.add(toResponse(grant, resource)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveGrant(Long personId, AccessResourceType type, Long resourceId) {
        return accessGrantRepository.existsByGranteeIdAndResourceTypeAndResourceIdAndRevokedAtIsNull(personId, type, resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManage(Long actorId, AccessResourceType type, Long resourceId) {
        Person actor = personRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        if (Role.isAtLeastExecutive(actor.getRole())) {
            return true;
        }
        if (actor.getRole() != Role.DIRECTOR || actor.getDepartment() == null) {
            return false;
        }
        return switch (type) {
            case TASK -> taskAccessPolicy.belongsToDepartment(resourceId, actor.getDepartment().getId());
            case INCIDENT -> incidentRepository.findById(resourceId)
                    .map(i -> IncidentAccessPolicy.unitMatchesDepartment(i.getBusinessUnit(), actor.getDepartment().getName()))
                    .orElse(false);
            case RISK -> false;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public String sharedByName(Long personId, AccessResourceType type, Long resourceId) {
        return accessGrantRepository.findByGranteeIdAndResourceTypeAndResourceIdAndRevokedAtIsNull(personId, type, resourceId)
                .map(grant -> grant.getGrantedBy().getFullName())
                .orElse(null);
    }

    /** An Executive/Super Admin may share or remove access to anything; a Director only for a task or
     *  incident that belongs to their own department. */
    private Person requireCanManage(Long personId, AccessResourceType type, Long resourceId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        if (!canManage(personId, type, resourceId)) {
            throw new ForbiddenActionException(
                    "Only an Executive, a Super Admin, or the Director of the department it belongs to can share or remove access.");
        }
        return person;
    }

    private Optional<Resource> resolve(AccessResourceType type, Long id) {
        return switch (type) {
            case TASK -> taskRepository.findById(id).map(t -> new Resource(t.getTaskCode(), t.getTitle()));
            case INCIDENT -> incidentRepository.findById(id).map(i -> new Resource(i.getIncidentCode(), i.getTitle()));
            case RISK -> throw new InvalidAssignmentException("Risks aren't available yet.");
        };
    }

    private AccessGrantResponse toResponse(AccessGrant grant, Resource resource) {
        Person grantee = grant.getGrantee();
        return new AccessGrantResponse(
                grant.getId(),
                grantee.getId(),
                grantee.getFullName(),
                grantee.getJobTitle(),
                grant.getResourceType(),
                grant.getResourceId(),
                resource != null ? resource.code() : null,
                resource != null ? resource.title() : null,
                grant.getGrantedBy().getFullName(),
                grant.getReason(),
                grant.getGrantedAt()
        );
    }
}
