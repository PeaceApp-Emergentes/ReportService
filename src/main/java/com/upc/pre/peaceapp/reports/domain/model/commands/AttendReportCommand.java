package com.upc.pre.peaceapp.reports.domain.model.commands;

public record AttendReportCommand(Long reportId) {
    public AttendReportCommand {
        if (reportId == null || reportId <= 0) {
            throw new IllegalArgumentException("Report ID must be valid");
        }
    }
}
