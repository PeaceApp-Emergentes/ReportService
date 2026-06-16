package com.upc.pre.peaceapp.reports.infrastructure.external.clients;

import com.upc.pre.peaceapp.reports.infrastructure.external.dto.CreateAlertDto;
import com.upc.pre.peaceapp.reports.infrastructure.external.fallbacks.AlertServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "alert-service",
        fallback = AlertServiceClientFallback.class
)
public interface AlertServiceClient {

    @PostMapping("/api/v1/alerts")
    void createAlert(@RequestBody CreateAlertDto alert);

    @DeleteMapping("/api/v1/alerts/report/{reportId}")
    void deleteAlertsByReportId(@PathVariable("reportId") Long reportId);
}
