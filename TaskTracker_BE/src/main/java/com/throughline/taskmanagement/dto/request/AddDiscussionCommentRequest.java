package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body for POST /tasks/{id}/discussion-comments — a plain Q&A message, fully open (any
 * authenticated person may post on any task, same as the progress log always has been).
 * Never touches percentage/status. parentCommentId is optional: omitted for a new
 * top-level comment, or the id of the top-level comment being replied to — replying to a
 * reply attaches to that reply's own parent instead of nesting further (see
 * TaskServiceImpl.addDiscussionComment).
 */
public record AddDiscussionCommentRequest(
    @NotNull Long authorId,
    @NotBlank String body,
    Long parentCommentId
) {}
