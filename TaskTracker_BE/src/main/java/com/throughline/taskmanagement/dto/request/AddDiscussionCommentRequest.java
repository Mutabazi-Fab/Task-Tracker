package com.throughline.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body for POST /tasks/{id}/discussion-comments — a plain Q&A message, fully open (any authenticated
 *  person may post on any task, same as the progress log always has been). */
public record AddDiscussionCommentRequest(
    @NotNull Long authorId,
    @NotBlank String body,
    Long parentCommentId
) {}
