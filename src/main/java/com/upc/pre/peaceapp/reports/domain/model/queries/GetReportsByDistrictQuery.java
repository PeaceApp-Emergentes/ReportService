package com.upc.pre.peaceapp.reports.domain.model.queries;

public record GetReportsByDistrictQuery(String district) {
    public GetReportsByDistrictQuery {
        if (district == null || district.isBlank()) {
            throw new IllegalArgumentException("district cannot be null or empty");
        }
    }
}
