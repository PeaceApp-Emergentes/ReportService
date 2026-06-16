package com.upc.pre.peaceapp.reports.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for the municipality to flag/unflag a report as emergency")
public record UpdateEmergencyResource(

        @Schema(description = "New emergency value for the report", example = "true")
        Boolean isEmergency
) {}
