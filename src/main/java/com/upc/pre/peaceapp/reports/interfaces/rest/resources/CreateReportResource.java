package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for creating a new report")
public record CreateReportResource(

        @NotBlank
        @Schema(description = "Report title", example = "Robo en la avenida principal")
        String title,

        @Schema(description = "Detailed description of the report", example = "Incidente cerca del parque.")
        String description,

        @NotBlank
        @Schema(description = "Location name or address of the report", example = "Av. Primavera 123, Lima")
        String location,

        @Schema(description = "District resolved from the report coordinates", example = "San Borja")
        String district,

        @NotNull
        @Schema(description = "Type of report", example = "ROBBERY")
        ReportType type,

        @NotNull
        @Schema(description = "User ID who created the report", example = "101")
        Long userId,

        @Schema(description = "Image URL associated with the report", example = "https://example.com/images/report1.jpg")
        String imageUrl,

        @Schema(description = "Video URL associated with the report", example = "https://example.com/videos/report1.mp4")
        String videoUrl,

        @Schema(description = "Audio URL associated with the report", example = "https://example.com/audio/report1.m4a")
        String audioUrl,

        @NotBlank
        @Schema(description = "Latitude coordinate of the report", example = "-12.046374")
        String latitude,

        @NotBlank
        @Schema(description = "Longitude coordinate of the report", example = "-77.042793")
        String longitude,

        @Schema(description = "Whether the citizen marked this report as an emergency", example = "false")
        Boolean isEmergency
) {}
