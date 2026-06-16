package com.upc.pre.peaceapp.reports.domain.model.commands;

/**
 * Command used by the municipality to flag or unflag a report as an emergency.
 */
public record SetReportEmergencyCommand(Long reportId, boolean emergency) {
    public SetReportEmergencyCommand {
        if (reportId == null || reportId <= 0)
            throw new IllegalArgumentException("reportId must be greater than 0");
    }
}
