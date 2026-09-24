package com.throughline.taskmanagement.dto.response;

/** One point of the dashboard's monthly trend line. month is "YYYY-MM". */
public record IncidentMonthlyCountResponse(String month, long count) {}
