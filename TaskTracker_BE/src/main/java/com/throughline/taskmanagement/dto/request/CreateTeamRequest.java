package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** The Director creates the team, picks its initial roster, and designates one of those
 *  members as Team Leader, all in one request. leaderId must be one of memberIds. */
public record CreateTeamRequest(
    @NotBlank String name,
    @NotNull Long createdById,
    @NotNull Long leaderId,
    @NotEmpty List<Long> memberIds
) {}
