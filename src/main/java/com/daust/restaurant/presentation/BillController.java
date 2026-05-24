package com.daust.restaurant.presentation;

import com.daust.restaurant.application.BillNotFoundException;
import com.daust.restaurant.application.GenerateBillService;
import com.daust.restaurant.application.OrderNotFoundException;
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
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BillController {

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final ConfigurationRepository configurationRepository;
    private final MenuItemRepository menuItemRepository;
    private final GenerateBillService generateBillService;
    private final CurrentUserHelper currentUser;

    public BillController(
            OrderRepository orderRepository,
            BillRepository billRepository,
            ConfigurationRepository configurationRepository,
            MenuItemRepository menuItemRepository,
            GenerateBillService generateBillService,
            CurrentUserHelper currentUser) {
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.configurationRepository = configurationRepository;
        this.menuItemRepository = menuItemRepository;
        this.generateBillService = generateBillService;
        this.currentUser = currentUser;
    }

    public record PreviewLine(String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    @GetMapping("/bills/new")
    public String preview(@RequestParam("orderId") UUID orderId, Model model) {
        Order order = orderRepository
                .findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        if (order.getState() != OrderState.SERVED) {
            throw new IllegalStateException(
                    "Bill can only be previewed for an Order in state SERVED; was " + order.getState());
        }
        Configuration config = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration not initialized"));

        List<PreviewLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            String name = menuItemRepository
                    .findById(item.getMenuItemId())
                    .map(MenuItem::getName)
                    .orElse("(unknown)");
            BigDecimal line = item.lineTotal();
            lines.add(new PreviewLine(name, item.getQuantity(), item.getRecordedUnitPrice(), line));
            subtotal = subtotal.add(line);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = config.getTaxRate().multiply(subtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal service = config.getServiceChargeRate().multiply(subtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cover = config.getCoverChargeAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).add(service).add(cover);

        model.addAttribute("orderId", orderId);
        model.addAttribute("order", order);
        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("tax", tax);
        model.addAttribute("service", service);
        model.addAttribute("cover", cover);
        model.addAttribute("total", total);
        model.addAttribute("splitEnabled", config.isSplitMergePolicyEnabled());
        return "bills/preview";
    }

    @PostMapping("/bills")
    public String generate(@RequestParam("orderId") UUID orderId, Authentication authentication) {
        BillId billId = generateBillService.generateBill(
                OrderId.of(orderId), currentUser.currentUserId(authentication));
        return "redirect:/bills/" + billId.value();
    }

    @GetMapping("/bills/{id}")
    public String view(@PathVariable UUID id, Model model) {
        Bill bill = billRepository
                .findById(BillId.of(id))
                .orElseThrow(() -> new BillNotFoundException("Bill not found: " + id));
        model.addAttribute("bill", bill);
        return "bills/view";
    }
}
