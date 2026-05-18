package com.daust.restaurant.presentation;

import com.daust.restaurant.application.OrderLifecycleService;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class KitchenController {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderLifecycleService lifecycleService;
    private final CurrentUserHelper currentUser;

    public KitchenController(
            OrderRepository orderRepository,
            MenuItemRepository menuItemRepository,
            OrderLifecycleService lifecycleService,
            CurrentUserHelper currentUser) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.lifecycleService = lifecycleService;
        this.currentUser = currentUser;
    }

    @GetMapping("/kitchen")
    public String list(Model model) {
        List<Order> orders = new ArrayList<>();
        orderRepository.findByState(OrderState.PLACED).stream()
                .filter(Order::isVisibleToKitchen)
                .forEach(orders::add);
        orders.addAll(orderRepository.findByState(OrderState.IN_PREPARATION));
        orders.addAll(orderRepository.findByState(OrderState.READY));
        orders.sort(Comparator.comparing(Order::getPlacedAt));

        Map<MenuItemId, String> nameLookup = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                nameLookup.computeIfAbsent(
                        item.getMenuItemId(),
                        id -> menuItemRepository
                                .findById(id)
                                .map(MenuItem::getName)
                                .orElse("(unknown)"));
            }
        }
        model.addAttribute("orders", orders);
        model.addAttribute("itemNames", nameLookup);
        return "kitchen/list";
    }

    @PostMapping("/kitchen/{orderId}/start")
    public String start(@PathVariable UUID orderId, Authentication authentication) {
        lifecycleService.startPreparation(
                OrderId.of(orderId), currentUser.currentUserId(authentication));
        return "redirect:/kitchen";
    }

    @PostMapping("/kitchen/{orderId}/ready")
    public String ready(@PathVariable UUID orderId, Authentication authentication) {
        lifecycleService.markReady(
                OrderId.of(orderId), currentUser.currentUserId(authentication));
        return "redirect:/kitchen";
    }
}
