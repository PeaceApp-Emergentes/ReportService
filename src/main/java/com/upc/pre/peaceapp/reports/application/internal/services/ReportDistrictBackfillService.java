package com.upc.pre.peaceapp.reports.application.internal.services;

import com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.ReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ReportDistrictBackfillService {

    private final ReportRepository reportRepository;
    private final DistrictResolverService districtResolverService;

    public ReportDistrictBackfillService(ReportRepository reportRepository,
                                         DistrictResolverService districtResolverService) {
        this.reportRepository = reportRepository;
        this.districtResolverService = districtResolverService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillMissingDistricts() {
        var reports = reportRepository.findAll().stream()
                .filter(report -> report.getDistrict() == null || report.getDistrict().isBlank())
                .toList();

        if (reports.isEmpty()) {
            return;
        }

        reports.forEach(report -> {
            String district = districtResolverService.resolve(report.getLatitude(), report.getLongitude())
                    .orElse(DistrictResolverService.OUT_OF_COVERAGE);
            report.setDistrict(district);
        });

        reportRepository.saveAll(reports);
        log.info("Backfilled district for {} existing report(s)", reports.size());
    }
}
