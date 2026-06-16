package com.upc.pre.peaceapp.reports.interfaces.rest.transform;

import com.upc.pre.peaceapp.reports.domain.model.commands.CreateReportCommand;
import com.upc.pre.peaceapp.reports.interfaces.rest.resources.CreateReportResource;
import org.springframework.stereotype.Component;

@Component
public class CreateReportCommandFromResourceAssembler {

    public CreateReportCommand toCommand(CreateReportResource resource) {
        return new CreateReportCommand(
                resource.title(),
                resource.description(),
                resource.location(),
                resource.district(),
                resource.type(),
                resource.userId(),
                resource.imageUrl(),
                resource.videoUrl(),
                resource.audioUrl(),
                resource.latitude(),
                resource.longitude(),
                Boolean.TRUE.equals(resource.isEmergency())
        );
    }
}
