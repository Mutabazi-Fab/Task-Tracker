package com.throughline.taskmanagement.enums;

/** Derived from Likelihood x Impact (both 1-5) into a 1-25 Inherent Score, then banded into this
 *  severity — see IncidentServiceImpl.computeSeverity. */
public enum IncidentSeverity {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}
