package com.throughline.taskmanagement.enums;

/** Derived from Likelihood x Impact (both 1-5) into a 1-25 Inherent Score, then banded into
 *  this severity — see IncidentServiceImpl.computeSeverity. Never set directly by a
 *  request; always recomputed server-side from likelihood/impact on every create/update.
 *  Standardized on MODERATE for the 6-10 band: the source Excel's formula literally said
 *  "Medium" there, which conflicted with its own canonical dropdown/guidance sheet, both of
 *  which say "Moderate" — this fixes that inconsistency rather than reproducing it. */
public enum IncidentSeverity {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}
