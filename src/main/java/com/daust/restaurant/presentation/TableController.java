package com.daust.restaurant.presentation;

import com.daust.restaurant.application.SeatCustomersService;
import com.daust.restaurant.application.TableHasActiveOrdersException;
import com.daust.restaurant.application.TableNotFoundException;
import com.daust.restaurant.application.TableNotOccupiedException;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TableController {

    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final SeatCustomersService seatCustomersService;
    private final CurrentUserHelper currentUser;

    public TableController(
            TableRepository tableRepository,
            OrderRepository orderRepository,
            SeatCustomersService seatCustomersService,
            CurrentUserHelper currentUser) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.seatCustomersService = seatCustomersService;
        this.currentUser = currentUser;
    }

    @GetMapping("/tables")
    public String list(Model model) {
        List<Table> tables = tableRepository.findAll().stream()
                .filter(Table::isActive)
                .sorted(Comparator.comparing(t -> t.getId().value()))
                .toList();
        // An OCCUPIED table is safe to release iff no active orders still point at it.
        // Show the "Free Table" button only for those — the service still re-checks the guard.
        Set<UUID> releasable = new HashSet<>();
        for (Table t : tables) {
            if (t.getStatus() == TableStatus.OCCUPIED
                    && orderRepository.findActiveByTableId(t.getId()).isEmpty()) {
                releasable.add(t.getId().value());
            }
        }
        model.addAttribute("tables", tables);
        model.addAttribute("releasable", releasable);
        return "tables/list";
    }

    @PostMapping("/tables/{id}/seat")
    public String seat(@PathVariable UUID id, Authentication authentication) {
        seatCustomersService.seatTable(TableId.of(id), currentUser.currentUserId(authentication));
        return "redirect:/tables";
    }

    @PostMapping("/tables/{id}/release")
    public String release(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes flash) {
        try {
            seatCustomersService.releaseTable(
                    TableId.of(id), currentUser.currentUserId(authentication));
            flash.addFlashAttribute("message", "Table freed.");
        } catch (TableHasActiveOrdersException
                | TableNotOccupiedException
                | TableNotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tables";
    }
}
