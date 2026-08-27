package com.throughline.taskmanagement.dto.response;

import java.util.List;

/** A search-result entry for a person: their profile plus a per-team stats breakdown —
 *  not one blended number across every team they belong to. */
public record PersonSearchResultResponse(
    PersonResponse person,
    List<PersonTeamStatisticsResponse> teamBreakdown
) {}
