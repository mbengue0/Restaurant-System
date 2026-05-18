package com.daust.restaurant.presentation;

import com.daust.restaurant.application.SeatCustomersService;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TableController {

    private final TableRepository tableRepository;
    private final SeatCustomersService seatCustomersService;
    private final CurrentUserHelper currentUser;

    public TableController(
            TableRepository tableRepository,
            SeatCustomersService seatCustomersService,
            CurrentUserHelper currentUser) {
        this.tableRepository = tableRepository;
        this.seatCustomersService = seatCustomersService;
        this.currentUser = currentUser;
    }

    @GetMapping("/tables")
    public String list(Model model) {
        List<Table> tables = tableRepository.findAll().stream()
                .filter(Table::isActive)
                .sorted(Comparator.comparing(t -> t.getId().value()))
                .toList();
        model.addAttribute("tables", tables);
        return "tables/list";
    }

    @PostMapping("/tables/{id}/seat")
    public String seat(@PathVariable UUID id, Authentication authentication) {
        seatCustomersService.seatTable(TableId.of(id), currentUser.currentUserId(authentication));
        return "redirect:/tables";
    }
}
