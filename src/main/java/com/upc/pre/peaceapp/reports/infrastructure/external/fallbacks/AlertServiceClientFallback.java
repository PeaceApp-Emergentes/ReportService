package com.upc.pre.peaceapp.reports.infrastructure.external.fallbacks;

import com.upc.pre.peaceapp.reports.infrastructure.external.clients.AlertServiceClient;
import com.upc.pre.peaceapp.reports.infrastructure.external.dto.CreateAlertDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlertServiceClientFallback implements AlertServiceClient {

    @Override
    public void createAlert(CreateAlertDto alert) {
        log.warn("Fallback: could not create emergency alert for report ID {}", alert.reportId());
    }

    @Override
    public void deleteAlertsByReportId(Long reportId) {
        log.warn("Fallback: could not delete alerts for report ID {}", reportId);
    }
}
