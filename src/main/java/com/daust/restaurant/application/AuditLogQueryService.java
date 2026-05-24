package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC20 — View Audit Log.
 *
 * <p>Read-only viewer over {@link AuditLogRepository}. Resolves the acting user's username for
 * display; falls back to the raw UUID when the user has been deleted (NFR-AUD-2: the audit log
 * survives user deletion, so we never drop entries whose user is gone).
 *
 * <p>Per NFR-AUD-1 / NFR-AUD-3 this service is strictly query-side — it neither writes audit
 * entries nor mutates anything else, hence {@code readOnly = true} at the class level.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogQueryService(
            AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public List<AuditLogView> recentEntries(int limit) {
        return toViews(auditLogRepository.findRecent(limit));
    }

    public List<AuditLogView> entriesBetween(LocalDateTime from, LocalDateTime to) {
        return toViews(auditLogRepository.findByTimestampBetween(from, to));
    }

    public List<AuditLogView> entriesByEventType(String eventType) {
        return toViews(auditLogRepository.findByEventType(eventType));
    }

    private List<AuditLogView> toViews(List<AuditLogEntry> entries) {
        Map<UserId, String> nameCache = new HashMap<>();
        return entries.stream().map(e -> toView(e, nameCache)).toList();
    }

    private AuditLogView toView(AuditLogEntry e, Map<UserId, String> nameCache) {
        String username = nameCache.computeIfAbsent(e.getUserId(), this::resolveUsername);
        return new AuditLogView(
                e.getTimestamp(),
                username,
                e.getUserRoleAtTime(),
                e.getEventType(),
                e.getAffectedEntityType(),
                e.getAffectedEntityId(),
                e.getBeforeValue(),
                e.getAfterValue());
    }

    private String resolveUsername(UserId userId) {
        return userRepository.findById(userId).map(User::getUsername).orElse(userId.value().toString());
    }
}
