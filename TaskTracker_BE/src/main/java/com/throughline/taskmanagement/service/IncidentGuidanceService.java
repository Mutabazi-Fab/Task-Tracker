package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.CreateGuidanceNoteRequest;
import com.throughline.taskmanagement.dto.request.UpdateGuidanceNoteRequest;
import com.throughline.taskmanagement.dto.response.GuidanceNoteResponse;

import java.util.List;

/** The editable half of the Incident Guidance page — free-text notes a Director/Executive/ Super Admin
 *  can add on top of the fixed reference material (severity table, likelihood/impact scale,
 *  minimum-completion checklist) carried over verbatim from the source Excel's "Lists & Guidance"
 *  sheet. */
public interface IncidentGuidanceService {

    List<GuidanceNoteResponse> getNotes();

    /** Director/Executive/Super Admin only (Role.isAtLeastDirector), enforced here.
     *  createdById is the caller's real, JWT-resolved identity. */
    GuidanceNoteResponse createNote(CreateGuidanceNoteRequest request);

    /** Same tier as creating one. changedById is the caller's real identity. */
    GuidanceNoteResponse updateNote(Long id, UpdateGuidanceNoteRequest request);

    /** Same tier as creating one. actorId is the caller's real identity — re-checked here
     *  even though it doesn't need to match the note's own creator, since any Director-or-
     *  above may curate this shared reference material, not just whoever wrote a given note. */
    void deleteNote(Long id, Long actorId);
}
