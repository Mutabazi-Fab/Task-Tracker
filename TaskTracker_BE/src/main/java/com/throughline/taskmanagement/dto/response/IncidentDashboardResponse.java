package com.throughline.taskmanagement.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Backs the Incident Management dashboard. Reproduces every KPI the source Excel's
 *  Dashboard sheet computed (total/open-monitoring/critical-high/overdue tiles, gross/
 *  recovery/net loss sums, regulator-notifiable count, status and severity breakdowns) plus
 *  additions the underlying data already supports but the Excel never surfaced: a category
 *  breakdown, a monthly trend, and an SLA-compliance view (closedLateCount/
 *  slaComplianceRate) using the new closed-late flag the source formulas never had. */
public record IncidentDashboardResponse(
    long totalIncidents,
    long openOrMonitoringCount,
    long criticalOrHighCount,
    long overdueActionsCount,
    BigDecimal grossLossTotal,
    BigDecimal recoveryTotal,
    BigDecimal netLossTotal,
    long regulatorNotifiableCount,
    List<IncidentCountResponse> statusBreakdown,
    List<IncidentCountResponse> severityBreakdown,
    List<IncidentCountResponse> categoryBreakdown,
    List<IncidentMonthlyCountResponse> monthlyTrend,
    long closedCount,
    long closedLateCount,
    // Percentage (0-100) of closed incidents that closed on or before their target date.
    // 100.0 when nothing has closed yet — no evidence of a breach, not "perfect compliance".
    double slaComplianceRate
) {}
