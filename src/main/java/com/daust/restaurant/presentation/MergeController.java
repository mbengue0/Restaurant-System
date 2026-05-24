package com.daust.restaurant.presentation;

import com.daust.restaurant.application.BillAlreadyGeneratedException;
import com.daust.restaurant.application.InvalidMergeException;
import com.daust.restaurant.application.MergeOrdersService;
import com.daust.restaurant.application.SplitMergeDisabledException;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * UC16 — Merge Orders. Manager + Admin. The path {@code /orders/merge} is gated by a dedicated
 * rule in {@link com.daust.restaurant.infrastructure.security.SecurityConfig} placed BEFORE the
 * broad {@code /orders/**} rule (which would otherwise let waiters in).
 */
@Controller
public class MergeController {

    public record EligibleOrder(
            UUID id, String tableLabel, int itemCount, BigDecimal subtotal) {}

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final ConfigurationRepository configurationRepository;
    private final TableRepository tableRepository;
    private final MergeOrdersService mergeOrdersService;
    private final CurrentUserHelper currentUser;

    public MergeController(
            OrderRepository orderRepository,
            BillRepository billRepository,
            ConfigurationRepository configurationRepository,
            TableRepository tableRepository,
            MergeOrdersService mergeOrdersService,
            CurrentUserHelper currentUser) {
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.configurationRepository = configurationRepository;
        this.tableRepository = tableRepository;
        this.mergeOrdersService = mergeOrdersService;
        this.currentUser = currentUser;
    }

    @GetMapping("/orders/merge")
    public String mergeForm(Model model, RedirectAttributes flash) {
        Configuration config = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration not initialized"));
        if (!config.isSplitMergePolicyEnabled()) {
            flash.addFlashAttribute("error", "Split/merge is disabled in Configuration (BR6).");
            return "redirect:/dashboard";
        }

        List<Order> served = orderRepository.findByState(OrderState.SERVED);
        Map<TableId, Table> tableCache = new HashMap<>();
        List<EligibleOrder> eligible = new ArrayList<>();
        for (Order order : served) {
            if (!billRepository.findByOrderId(order.getId()).isEmpty()) {
                continue;
            }
            Table table = tableCache.computeIfAbsent(
                    order.getTableId(),
                    id -> tableRepository.findById(id).orElse(null));
            String label = table != null ? "Table cap " + table.getCapacity() : "(unknown)";
            BigDecimal subtotal = order.getItems().stream()
                    .map(i -> i.lineTotal())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            eligible.add(new EligibleOrder(
                    order.getId().value(),
                    label,
                    order.getItems().size(),
                    subtotal));
        }

        model.addAttribute("eligible", eligible);
        return "orders/merge";
    }

    @PostMapping("/orders/merge")
    public String submit(
            @RequestParam(value = "orderIds", required = false) List<UUID> orderIds,
            Authentication authentication,
            RedirectAttributes flash) {
        if (orderIds == null || orderIds.size() < 2) {
            flash.addFlashAttribute("error", "Select at least 2 orders to merge.");
            return "redirect:/orders/merge";
        }
        try {
            List<OrderId> ids = orderIds.stream().map(OrderId::of).toList();
            BillId billId = mergeOrdersService.mergeOrders(
                    ids, currentUser.currentUserId(authentication));
            return "redirect:/bills/" + billId.value();
        } catch (SplitMergeDisabledException
                | BillAlreadyGeneratedException
                | InvalidMergeException
                | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/orders/merge";
        }
    }
}
