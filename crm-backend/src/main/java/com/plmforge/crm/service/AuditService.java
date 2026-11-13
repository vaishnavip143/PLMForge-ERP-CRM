package com.plmforge.crm.service;

import com.plmforge.crm.dto.response.AuditLogResponse;
import com.plmforge.crm.entity.AuditLog;
import com.plmforge.crm.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(Long ticketId, String ticketNumber, String action, String performedBy, String details) {
        AuditLog entry = AuditLog.builder()
                .ticketId(ticketId)
                .ticketNumber(ticketNumber)
                .action(action)
                .performedBy(truncate(performedBy, 100))
                .details(details)
                .build();

        auditLogRepository.save(entry);
        log.info("[AUDIT] action={}, ticket={}, by={}, details={}",
                action, ticketNumber != null ? ticketNumber : ticketId, performedBy, details);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getTicketAuditTrail(Long ticketId) {
        return auditLogRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "SYSTEM";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
