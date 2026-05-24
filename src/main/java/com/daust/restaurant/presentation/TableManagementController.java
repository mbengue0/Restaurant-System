package com.daust.restaurant.presentation;

import com.daust.restaurant.application.TableHasActiveOrdersException;
import com.daust.restaurant.application.TableManagementService;
import com.daust.restaurant.application.TableNotFoundException;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * UC02 — Manage Tables. Admin-only routes; {@code /admin/**} is already gated by
 * {@code hasRole("ADMIN")} in {@link com.daust.restaurant.infrastructure.security.SecurityConfig}.
 */
@Controller
public class TableManagementController {

    private final TableManagementService tableManagementService;
    private final CurrentUserHelper currentUser;

    public TableManagementController(
            TableManagementService tableManagementService, CurrentUserHelper currentUser) {
        this.tableManagementService = tableManagementService;
        this.currentUser = currentUser;
    }

    @GetMapping("/admin/tables")
    public String list(Model model) {
        List<Table> tables = new ArrayList<>(tableManagementService.listTables());
        tables.sort(
                Comparator.comparing(Table::isActive).reversed()
                        .thenComparingInt(Table::getCapacity)
                        .thenComparing(t -> t.getId().value()));
        model.addAttribute("tables", tables);
        return "admin/tables/list";
    }

    @GetMapping("/admin/tables/new")
    public String createForm() {
        return "admin/tables/new";
    }

    @PostMapping("/admin/tables")
    public String create(
            @RequestParam("capacity") int capacity,
            Authentication authentication,
            RedirectAttributes flash) {
        try {
            Table created =
                    tableManagementService.createTable(capacity, currentUser.currentUserId(authentication));
            flash.addFlashAttribute(
                    "message",
                    "Table created (capacity " + created.getCapacity() + ").");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/tables/new";
        }
        return "redirect:/admin/tables";
    }

    @PostMapping("/admin/tables/{id}/deactivate")
    public String deactivate(
            @PathVariable("id") UUID id,
            Authentication authentication,
            RedirectAttributes flash) {
        try {
            tableManagementService.deactivateTable(
                    TableId.of(id), currentUser.currentUserId(authentication));
            flash.addFlashAttribute("message", "Table deactivated.");
        } catch (TableHasActiveOrdersException | TableNotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tables";
    }
}
