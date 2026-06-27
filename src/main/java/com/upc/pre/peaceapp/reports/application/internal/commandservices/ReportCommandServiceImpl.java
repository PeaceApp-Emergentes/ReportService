package com.upc.pre.peaceapp.reports.application.internal.commandservices;

import com.upc.pre.peaceapp.reports.domain.events.ReportApprovedEvent;
import com.upc.pre.peaceapp.reports.domain.events.ReportCreatedEvent;
import com.upc.pre.peaceapp.reports.domain.events.ReportDeletedEvent;
import com.upc.pre.peaceapp.reports.domain.events.ReportRejectedEvent;
import com.upc.pre.peaceapp.reports.domain.model.aggregates.Report;
import com.upc.pre.peaceapp.reports.domain.model.commands.*;
import com.upc.pre.peaceapp.reports.domain.model.valueobjects.ReportState;
import com.upc.pre.peaceapp.reports.domain.services.ReportCommandService;
import com.upc.pre.peaceapp.reports.infrastructure.external.messaging.ReportEventPublisher;
import com.upc.pre.peaceapp.reports.infrastructure.persistence.jpa.ReportRepository;
import com.upc.pre.peaceapp.reports.application.internal.outboundservices.ExternalAlertService;
import com.upc.pre.peaceapp.reports.application.internal.outboundservices.ExternalUserService;
import com.upc.pre.peaceapp.reports.application.internal.services.DistrictResolverService;
import com.upc.pre.peaceapp.reports.interfaces.ws.EmergencyWebSocketHandler;
import com.upc.pre.peaceapp.reports.interfaces.ws.resources.EmergencyResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class ReportCommandServiceImpl implements ReportCommandService {

    private final ReportRepository reportRepository;
    private final ExternalUserService userService;
    private final ExternalAlertService alertService;
    private final ReportEventPublisher reportEventPublisher;
    private final EmergencyWebSocketHandler emergencyWebSocketHandler;
    private final DistrictResolverService districtResolverService;

    public ReportCommandServiceImpl(ReportRepository reportRepository,
                                    ExternalUserService userService,
                                    ExternalAlertService alertService,
                                    ReportEventPublisher reportEventPublisher,
                                    EmergencyWebSocketHandler emergencyWebSocketHandler,
                                    DistrictResolverService districtResolverService) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.alertService = alertService;
        this.reportEventPublisher = reportEventPublisher;
        this.emergencyWebSocketHandler = emergencyWebSocketHandler;
        this.districtResolverService = districtResolverService;
    }

    // ----------------------------------------------------
    // CREATE
    // ----------------------------------------------------
    @Override
    public Optional<Report> handle(CreateReportCommand command) {
        log.info("Creating report for user ID: {}", command.userId());

        if (!userService.existsById(command.userId())) {
            throw new IllegalArgumentException("User not found");
        }

        String district = districtResolverService.resolve(command.latitude(), command.longitude())
                .orElseGet(() -> {
                    String requestedDistrict = districtResolverService.canonicalizeDistrict(command.district());
                    return requestedDistrict != null ? requestedDistrict : DistrictResolverService.OUT_OF_COVERAGE;
                });

        // Un reporte de emergencia (incluido el SOS) requiere una municipalidad con cobertura en la zona.
        if (Boolean.TRUE.equals(command.isEmergency()) && !userService.hasCoverage(district)) {
            log.warn("Emergency report rejected: no municipality coverage for district '{}'", district);
            throw new IllegalArgumentException("No hay una municipalidad con cobertura en tu zona. No se pudo enviar la alerta de emergencia.");
        }

        var report = new Report(
                command.title(),
                command.description(),
                command.location(),
                district,
                command.type(),
                command.userId(),
                command.imageUrl(),
                command.videoUrl(),
                command.audioUrl(),
                command.latitude(),
                command.longitude()
        );

        report.setState(ReportState.PENDING);
        report.setEmergency(command.isEmergency());

        var savedReport = reportRepository.save(report);
        log.info("Report created with ID: {}", savedReport.getId());

        reportEventPublisher.publishReportCreated(
                new ReportCreatedEvent(
                        savedReport.getId(),
                        savedReport.getUserId(),
                        savedReport.getTitle(),
                        LocalDateTime.now().toString()
                )
        );

        // Notify municipality dashboards in real time as soon as the report enters
        // the district queue. Emergency reports also create a persistent alert.
        if (Boolean.TRUE.equals(savedReport.getEmergency())) {
            alertService.createEmergencyAlert(savedReport);
        }
        emergencyWebSocketHandler.broadcast(EmergencyResource.fromReport(savedReport));

        return Optional.of(savedReport);
    }

    // ----------------------------------------------------
    // DELETE
    // ----------------------------------------------------
    @Transactional
    @Override
    public void handle(DeleteReportByIdCommand command) {
        log.info("Deleting report with ID: {}", command.id());

        var report = reportRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        reportRepository.deleteById(report.getId());
        log.info("Report deleted successfully: {}", report.getId());

        reportEventPublisher.publishReportDeleted(
                new ReportDeletedEvent(
                        report.getId(),
                        report.getUserId(),
                        "Report deleted successfully",
                        LocalDateTime.now().toString()
                )
        );
    }

    // ----------------------------------------------------
    // MARK IN REVIEW
    // ----------------------------------------------------
    @Transactional
    @Override
    public void handle(MarkReportInReviewCommand command) {
        log.info("Marking report ID {} as IN_REVIEW", command.reportId());

        var report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.markInReview();
        reportRepository.save(report);
        alertService.deleteAlertsByReportId(report.getId());

        log.info("Report {} marked as IN_REVIEW", report.getId());
    }
    @Transactional
    @Override
    public void handle(ApproveReportCommand command) {
        log.info("Approving report ID {}", command.reportId());

        var report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.approve();
        reportRepository.save(report);

        log.info("Report {} approved", report.getId());

        reportEventPublisher.publishReportApproved(
                new ReportApprovedEvent(
                        report.getId(),
                        report.getUserId(),
                        report.getLatitude(),
                        report.getLongitude(),
                        LocalDateTime.now().toString()
                )
        );
    }
    @Transactional
    @Override
    public void handle(AttendReportCommand command) {
        log.info("Marking report ID {} as ATTENDED", command.reportId());

        var report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        report.attend();
        reportRepository.save(report);

        log.info("Report {} attended", report.getId());
    }
    @Transactional
    @Override
    public void handle(RejectReportCommand command) {
        log.info("Rejecting report ID {}", command.reportId());

        var report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        // ⭐ AQUÍ se actualiza bien el modelo
        report.reject(command.reason());

        var saved = reportRepository.save(report);

        log.info("Report {} rejected with reason: {}", saved.getId(), saved.getRejectionReason());

        reportEventPublisher.publishReportRejected(
                new ReportRejectedEvent(
                        saved.getId(),
                        saved.getUserId(),
                        saved.getRejectionReason(),   // ⭐ consistente con lo guardado
                        LocalDateTime.now().toString()
                )
        );
    }


    // SET EMERGENCY FLAG (municipality override)
    @Transactional
    @Override
    public void handle(SetReportEmergencyCommand command) {
        log.info("Setting emergency={} for report ID {}", command.emergency(), command.reportId());

        var report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (report.getState() == ReportState.REJECTED || report.getState() == ReportState.ATTENDED) {
            throw new IllegalStateException("Rejected or attended reports cannot be marked as emergency");
        }

        report.setEmergency(command.emergency());
        var saved = reportRepository.save(report);

        log.info("Report {} emergency flag updated to {}", saved.getId(), command.emergency());

        // US36: when a report is (re)marked as emergency, also push it live to the
        // municipality dashboards. Unmarking does not broadcast.
        if (Boolean.TRUE.equals(command.emergency())) {
            emergencyWebSocketHandler.broadcast(EmergencyResource.fromReport(saved));
        }
    }

}
