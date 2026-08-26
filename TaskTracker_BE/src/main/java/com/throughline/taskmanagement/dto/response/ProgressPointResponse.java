package com.throughline.taskmanagement.dto.response;

import java.time.LocalDate;

public record ProgressPointResponse(
    LocalDate date,
    Double averagePercentage
) {}
