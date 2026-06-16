package com.upc.pre.peaceapp.reports.application.internal.outboundservices;

import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.infrastructure.external.clients.AlertServiceClient;
import com.upc.pre.peaceapp.reports.infrastructure.external.dto.CreateAlertDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExternalAlertService {

    private final AlertServiceClient alertServiceClient;

    public ExternalAlertService(AlertServiceClient alertServiceClient) {
        this.alertServiceClient = alertServiceClient;
    }

    public void createEmergencyAlert(Report report) {
        var description = "[EMERGENCY] Reporte prioritario: %s. %s"
                .formatted(report.getTitle(), report.getDescription());

        var alert = new CreateAlertDto(
                report.getLocation(),
                report.getDistrict(),
                "EMERGENCY",
                description,
                report.getUserId(),
                report.getImageUrl(),
                report.getId()
        );

        try {
            alertServiceClient.createAlert(alert);
            log.info("Emergency alert created for report ID {}", report.getId());
        } catch (Exception exception) {
            log.error("Could not create emergency alert for report ID {}: {}", report.getId(), exception.getMessage());
        }
    }

    public void deleteAlertsByReportId(Long reportId) {
        try {
            alertServiceClient.deleteAlertsByReportId(reportId);
            log.info("Alerts deleted for report ID {}", reportId);
        } catch (Exception exception) {
            log.error("Could not delete alerts for report ID {}: {}", reportId, exception.getMessage());
        }
    }
}
