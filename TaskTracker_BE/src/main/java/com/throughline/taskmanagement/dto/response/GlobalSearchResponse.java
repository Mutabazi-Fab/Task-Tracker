package com.throughline.taskmanagement.dto.response;

import java.util.List;

public record GlobalSearchResponse(
    List<PersonResponse> people,
    List<TaskListResponse> tasks
) {}
