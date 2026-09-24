package com.throughline.taskmanagement.dto.response;

/** One bar/slice of a dashboard breakdown chart — label is the enum's name (e.g. "CRITICAL",
 *  "ICT_SYSTEMS") so the frontend controls its own display formatting. */
public record IncidentCountResponse(String label, long count) {}
