package com.daust.restaurant.presentation;

import com.daust.restaurant.application.AuditLogQueryService;
import com.daust.restaurant.application.AuditLogView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * UC20 — View Audit Log. Read-only viewer for ADMIN (route is gated by
 * {@code /admin/**} → {@code hasRole("ADMIN")} in SecurityConfig).
 *
 * <p>Three filters are accepted as optional query params, applied in priority order:
 * date-range > event type > recent. Default view is the most recent 100 entries.
 */
@Controller
public class AuditLogController {

    private static final int DEFAULT_LIMIT = 100;

    /** Known event types emitted across the Application layer. */
    private static final List<String> EVENT_TYPES = List.of(
            "USER_CREATED",
            "USER_UPDATED",
            "USER_DEACTIVATED",
            "USER_PASSWORD_RESET",
            "CONFIG_UPDATED",
            "CATEGORY_CREATED",
            "CATEGORY_UPDATED",
            "CATEGORY_DEACTIVATED",
            "MENU_ITEM_CREATED",
            "MENU_ITEM_UPDATED",
            "MENU_ITEM_PRICE_CHANGED",
            "MENU_ITEM_DEACTIVATED",
            "TABLE_SEATED",
            "TABLE_RELEASED",
            "ORDER_STARTED",
            "ORDER_ITEM_ADDED",
            "ORDER_ITEM_REMOVED",
            "ORDER_SUBMITTED",
            "ORDER_PREPARATION_STARTED",
            "ORDER_READY",
            "ORDER_SERVED",
            "ORDER_CANCELLED",
            "ORDER_COMPLETED",
            "BILL_GENERATED",
            "PAYMENT_RECORDED");

    private final AuditLogQueryService auditLogQueryService;

    public AuditLogController(AuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping("/admin/audit")
    public String list(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "from", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        List<AuditLogView> entries;
        if (from != null && to != null) {
            entries = auditLogQueryService.entriesBetween(
                    from.atStartOfDay(), to.atTime(LocalTime.MAX));
        } else if (eventType != null && !eventType.isBlank()) {
            entries = auditLogQueryService.entriesByEventType(eventType);
        } else {
            entries = auditLogQueryService.recentEntries(DEFAULT_LIMIT);
        }

        model.addAttribute("entries", entries);
        model.addAttribute("eventTypes", EVENT_TYPES);
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("defaultLimit", DEFAULT_LIMIT);
        return "admin/audit/list";
    }
}
