package com.throughline.taskmanagement.enums;

/** The "Incident Category" dropdown from the Excel register — a Basel-II-style operational
 *  risk event-type taxonomy, kept exactly as the source list (11 values). */
public enum IncidentCategory {
    INTERNAL_FRAUD,
    EXTERNAL_FRAUD,
    EMPLOYMENT_PRACTICES_SAFETY,
    CLIENTS_PRODUCTS_BUSINESS_PRACTICES,
    PHYSICAL_ASSET_DAMAGE,
    ICT_SYSTEMS,
    CYBERSECURITY,
    PROCESS_EXECUTION,
    BUSINESS_DISRUPTION_BCM,
    COMPLIANCE_LEGAL,
    THIRD_PARTY_OUTSOURCING
}
