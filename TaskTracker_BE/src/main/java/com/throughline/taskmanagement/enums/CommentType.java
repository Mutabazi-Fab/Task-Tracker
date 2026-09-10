package com.throughline.taskmanagement.enums;

/** What a TaskComment actually is — kept as an explicit field rather than inferred from
 *  whether a percentage was supplied, so the two concepts (a percentage update vs. a
 *  plain discussion message) never get muddled: PROGRESS always carries a real percentage
 *  reading of the task at that moment and counts toward its trend/timeline chart; DISCUSSION
 *  never does either — it's a message in the open Q&A thread (see TaskComment.parentComment
 *  for its reply-threading), fully separate from the progress log. */
public enum CommentType {
    PROGRESS,
    DISCUSSION
}
