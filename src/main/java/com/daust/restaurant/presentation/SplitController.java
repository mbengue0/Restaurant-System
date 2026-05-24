package com.daust.restaurant.presentation;

import com.daust.restaurant.application.BillAlreadyGeneratedException;
import com.daust.restaurant.application.InvalidSplitException;
import com.daust.restaurant.application.OrderNotFoundException;
import com.daust.restaurant.application.SplitMergeDisabledException;
import com.daust.restaurant.application.SplitOrderService;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.OrderItemId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * UC15 — Split Order. Manager + Admin. The HTTP path is gated by a dedicated rule in
 * {@link com.daust.restaurant.infrastructure.security.SecurityConfig} placed BEFORE the broad
 * {@code /orders/**} rule (which otherwise allows waiters in).
 */
@Controller
public class SplitController {

    public record SplitLine(UUID id, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public record CreatedBill(UUID id, String billNumber, BigDecimal total) {}

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final ConfigurationRepository configurationRepository;
    private final MenuItemRepository menuItemRepository;
    private final SplitOrderService splitOrderService;
    private final CurrentUserHelper currentUser;

    public SplitController(
            OrderRepository orderRepository,
            BillRepository billRepository,
            ConfigurationRepository configurationRepository,
            MenuItemRepository menuItemRepository,
            SplitOrderService splitOrderService,
            CurrentUserHelper currentUser) {
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.configurationRepository = configurationRepository;
        this.menuItemRepository = menuItemRepository;
        this.splitOrderService = splitOrderService;
        this.currentUser = currentUser;
    }

    @GetMapping("/orders/{id}/split")
    public String splitForm(
            @PathVariable UUID id,
            @RequestParam(value = "groups", defaultValue = "2") int groupCount,
            Model model,
            RedirectAttributes flash) {
        Order order = orderRepository
                .findById(OrderId.of(id))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

        Configuration config = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration not initialized"));
        if (!config.isSplitMergePolicyEnabled()) {
            flash.addFlashAttribute("error", "Split/merge is disabled in Configuration (BR6).");
            return "redirect:/bills/new?orderId=" + id;
        }
        if (order.getState() != OrderState.SERVED) {
            flash.addFlashAttribute(
                    "error", "Order must be SERVED to split; was " + order.getState() + ".");
            return "redirect:/dashboard";
        }
        if (!billRepository.findByOrderId(order.getId()).isEmpty()) {
            flash.addFlashAttribute("error", "Bill already exists for this order — cannot split.");
            return "redirect:/dashboard";
        }

        int clampedGroups = Math.max(2, Math.min(4, groupCount));

        List<SplitLine> lines = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            String name = menuItemRepository
                    .findById(item.getMenuItemId())
                    .map(MenuItem::getName)
                    .orElse("(unknown)");
            lines.add(new SplitLine(
                    item.getId().value(),
                    name,
                    item.getQuantity(),
                    item.getRecordedUnitPrice(),
                    item.lineTotal()));
        }

        model.addAttribute("orderId", id);
        model.addAttribute("lines", lines);
        model.addAttribute("groupCount", clampedGroups);
        model.addAttribute("groupRange", buildRange(1, clampedGroups));
        return "orders/split";
    }

    @PostMapping("/orders/{id}/split")
    public String submit(
            @PathVariable UUID id,
            @RequestParam Map<String, String> form,
            Authentication authentication,
            RedirectAttributes flash) {

        Order order = orderRepository
                .findById(OrderId.of(id))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

        int groupCount;
        try {
            groupCount = Integer.parseInt(form.getOrDefault("groupCount", "2"));
        } catch (NumberFormatException e) {
            groupCount = 2;
        }
        groupCount = Math.max(2, Math.min(4, groupCount));

        Map<Integer, List<OrderItemId>> groupsByIndex = new LinkedHashMap<>();
        for (int i = 1; i <= groupCount; i++) {
            groupsByIndex.put(i, new ArrayList<>());
        }
        for (OrderItem item : order.getItems()) {
            String key = "assign-" + item.getId().value();
            String assigned = form.get(key);
            int groupIndex = parseGroupIndex(assigned, groupCount);
            groupsByIndex.get(groupIndex).add(item.getId());
        }
        List<List<OrderItemId>> groups = new ArrayList<>(groupsByIndex.values());
        groups.removeIf(List::isEmpty);

        try {
            List<BillId> billIds = splitOrderService.splitOrder(
                    order.getId(), groups, currentUser.currentUserId(authentication));
            flash.addFlashAttribute("billIds", billIds.stream().map(b -> b.value()).toList());
            return "redirect:/orders/" + id + "/split/result";
        } catch (SplitMergeDisabledException | BillAlreadyGeneratedException | InvalidSplitException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/orders/" + id + "/split?groups=" + groupCount;
        }
    }

    @GetMapping("/orders/{id}/split/result")
    public String result(@PathVariable UUID id, Model model) {
        List<Bill> bills = billRepository.findByOrderId(OrderId.of(id));
        List<CreatedBill> view = new ArrayList<>(bills.size());
        for (Bill bill : bills) {
            view.add(new CreatedBill(bill.getId().value(), bill.getBillNumber(), bill.getTotal()));
        }
        model.addAttribute("orderId", id);
        model.addAttribute("bills", view);
        return "bills/split_result";
    }

    private static int parseGroupIndex(String raw, int max) {
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            int n = Integer.parseInt(raw);
            if (n < 1 || n > max) {
                return 1;
            }
            return n;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static List<Integer> buildRange(int from, int toInclusive) {
        List<Integer> range = new ArrayList<>(toInclusive - from + 1);
        for (int i = from; i <= toInclusive; i++) {
            range.add(i);
        }
        return range;
    }
}
