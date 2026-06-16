package com.upc.pre.peaceapp.reports.infrastructure.external.dto;

public record CreateAlertDto(
        String location,
        String district,
        String type,
        String description,
        Long userId,
        String imageUrl,
        Long reportId
) {
}
