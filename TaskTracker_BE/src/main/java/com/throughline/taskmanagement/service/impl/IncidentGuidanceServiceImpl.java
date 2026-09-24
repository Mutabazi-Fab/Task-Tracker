package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreateGuidanceNoteRequest;
import com.throughline.taskmanagement.dto.request.UpdateGuidanceNoteRequest;
import com.throughline.taskmanagement.dto.response.GuidanceNoteResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.IncidentGuidanceNote;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.IncidentGuidanceNoteRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.IncidentGuidanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentGuidanceServiceImpl implements IncidentGuidanceService {

    private final IncidentGuidanceNoteRepository guidanceNoteRepository;
    private final PersonRepository personRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GuidanceNoteResponse> getNotes() {
        return guidanceNoteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public GuidanceNoteResponse createNote(CreateGuidanceNoteRequest request) {
        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));
        if (!Role.isAtLeastDirector(createdBy.getRole())) {
            throw new ForbiddenActionException("Only a Director, Executive, or Super Admin can add a guidance note.");
        }

        IncidentGuidanceNote note = new IncidentGuidanceNote();
        note.setTitle(request.title().trim());
        note.setBody(request.body().trim());
        note.setCreatedBy(createdBy);

        return toResponse(guidanceNoteRepository.save(note));
    }

    @Override
    public GuidanceNoteResponse updateNote(Long id, UpdateGuidanceNoteRequest request) {
        Person actor = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        if (!Role.isAtLeastDirector(actor.getRole())) {
            throw new ForbiddenActionException("Only a Director, Executive, or Super Admin can edit a guidance note.");
        }

        IncidentGuidanceNote note = guidanceNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guidance note not found"));
        note.setTitle(request.title().trim());
        note.setBody(request.body().trim());
        note.setUpdatedBy(actor);

        return toResponse(guidanceNoteRepository.save(note));
    }

    @Override
    public void deleteNote(Long id, Long actorId) {
        Person actor = personRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("actorId not found"));
        if (!Role.isAtLeastDirector(actor.getRole())) {
            throw new ForbiddenActionException("Only a Director, Executive, or Super Admin can remove a guidance note.");
        }

        IncidentGuidanceNote note = guidanceNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guidance note not found"));
        guidanceNoteRepository.delete(note);
    }

    private GuidanceNoteResponse toResponse(IncidentGuidanceNote note) {
        return new GuidanceNoteResponse(
                note.getId(),
                note.getTitle(),
                note.getBody(),
                note.getCreatedBy().getFullName(),
                note.getCreatedAt(),
                note.getUpdatedBy() != null ? note.getUpdatedBy().getFullName() : null,
                note.getUpdatedAt()
        );
    }
}
