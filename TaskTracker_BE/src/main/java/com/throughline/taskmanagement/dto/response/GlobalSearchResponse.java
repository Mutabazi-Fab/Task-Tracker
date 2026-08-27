package com.throughline.taskmanagement.dto.response;

import java.util.List;

public record GlobalSearchResponse(
    List<PersonSearchResultResponse> people,
    List<TaskListResponse> tasks
) {}
