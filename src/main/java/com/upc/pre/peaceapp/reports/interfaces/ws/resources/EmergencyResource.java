package com.upc.pre.peaceapp.reports.interfaces.ws.resources;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;

/**
 * Lightweight payload broadcast over the WebSocket channel /ws/alerts when a
 * report flagged as emergency is created or toggled. The field names are chosen
 * to be consumable directly by the municipality web dashboard, which expects an
 * alert-like object exposing id / reportId / description / location.
 */
public record EmergencyResource(
        Long id,
        Long reportId,
        Long userId,
        String title,
        String description,
        String location,
        String district,
        String latitude,
        String longitude,
        String type,
        String state,
        boolean isEmergency,
        String createdAt
) {
    public static EmergencyResource fromReport(Report report) {
        return new EmergencyResource(
                report.getId(),
                report.getId(),
                report.getUserId(),
                report.getTitle(),
                report.getDescription(),
                report.getLocation(),
                report.getDistrict(),
                report.getLatitude(),
                report.getLongitude(),
                report.getType() != null ? report.getType().name() : null,
                report.getState() != null ? report.getState().name() : null,
                Boolean.TRUE.equals(report.getEmergency()),
                report.getCreatedAt() != null ? report.getCreatedAt().toString() : null
        );
    }
}
