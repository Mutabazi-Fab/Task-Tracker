package com.throughline.taskmanagement.dto.response;

public record StatusMixResponse(
    String status,
    long count,
    double percentageShare
) {}
