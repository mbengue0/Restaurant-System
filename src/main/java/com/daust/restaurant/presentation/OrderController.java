package com.daust.restaurant.presentation;

import com.daust.restaurant.application.OrderLifecycleService;
import com.daust.restaurant.application.OrderNotFoundException;
import com.daust.restaurant.application.PlaceOrderService;
import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.OrderItemId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

@Controller
public class OrderController {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final TableRepository tableRepository;
    private final PlaceOrderService placeOrderService;
    private final OrderLifecycleService lifecycleService;
    private final CurrentUserHelper currentUser;

    public OrderController(
            OrderRepository orderRepository,
            MenuItemRepository menuItemRepository,
            CategoryRepository categoryRepository,
            TableRepository tableRepository,
            PlaceOrderService placeOrderService,
            OrderLifecycleService lifecycleService,
            CurrentUserHelper currentUser) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.tableRepository = tableRepository;
        this.placeOrderService = placeOrderService;
        this.lifecycleService = lifecycleService;
        this.currentUser = currentUser;
    }

    @GetMapping("/orders/active")
    public String active(Model model) {
        List<Order> orders = new ArrayList<>();
        orders.addAll(orderRepository.findByState(OrderState.PLACED));
        orders.addAll(orderRepository.findByState(OrderState.IN_PREPARATION));
        orders.addAll(orderRepository.findByState(OrderState.READY));
        orders.addAll(orderRepository.findByState(OrderState.SERVED));
        orders.sort(Comparator.comparing(Order::getPlacedAt));

        Map<TableId, Table> tables = new HashMap<>();
        for (Order order : orders) {
            tables.computeIfAbsent(
                    order.getTableId(),
                    id -> tableRepository.findById(id).orElse(null));
        }
        model.addAttribute("orders", orders);
        model.addAttribute("tables", tables);
        return "orders/active";
    }

    @GetMapping("/orders/new")
    public String newOrder(@RequestParam("tableId") UUID tableId, Model model) {
        model.addAttribute("tableId", tableId);
        model.addAttribute("menuByCategory", menuByCategory());
        return "orders/new";
    }

    @PostMapping("/orders")
    public String start(@RequestParam("tableId") UUID tableId, Authentication authentication) {
        OrderId orderId = placeOrderService.startOrder(
                TableId.of(tableId), currentUser.currentUserId(authentication));
        return "redirect:/orders/" + orderId.value() + "/edit";
    }

    @GetMapping("/orders/{id}/edit")
    public String edit(@PathVariable UUID id, Model model) {
        Order order = orderRepository
                .findById(OrderId.of(id))
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        Map<MenuItemId, String> nameLookup = new LinkedHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            String name = menuItemRepository
                    .findById(item.getMenuItemId())
                    .map(MenuItem::getName)
                    .orElse("(unknown)");
            nameLookup.put(item.getMenuItemId(), name);
            subtotal = subtotal.add(item.lineTotal());
        }
        model.addAttribute("order", order);
        model.addAttribute("itemNames", nameLookup);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("menuByCategory", menuByCategory());
        return "orders/edit";
    }

    @PostMapping("/orders/{id}/items")
    public String addItem(
            @PathVariable UUID id,
            @RequestParam("menuItemId") UUID menuItemId,
            @RequestParam("quantity") int quantity,
            Authentication authentication) {
        placeOrderService.addItemToOrder(
                OrderId.of(id),
                MenuItemId.of(menuItemId),
                quantity,
                currentUser.currentUserId(authentication));
        return "redirect:/orders/" + id + "/edit";
    }

    @PostMapping("/orders/{id}/items/{itemId}/remove")
    public String removeItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            Authentication authentication) {
        placeOrderService.removeItemFromOrder(
                OrderId.of(id),
                OrderItemId.of(itemId),
                currentUser.currentUserId(authentication));
        return "redirect:/orders/" + id + "/edit";
    }

    @PostMapping("/orders/{id}/submit")
    public String submit(@PathVariable UUID id, Authentication authentication) {
        placeOrderService.submitOrder(OrderId.of(id), currentUser.currentUserId(authentication));
        return "redirect:/tables";
    }

    @PostMapping("/orders/{id}/serve")
    public String serve(@PathVariable UUID id, Authentication authentication) {
        lifecycleService.markServed(OrderId.of(id), currentUser.currentUserId(authentication));
        return "redirect:/tables";
    }

    private Map<Category, List<MenuItem>> menuByCategory() {
        List<Category> categories = categoryRepository.findAllActiveOrderedByDisplayOrder();
        Map<Category, List<MenuItem>> result = new LinkedHashMap<>();
        for (Category category : categories) {
            List<MenuItem> items = menuItemRepository.findActiveByCategoryId(category.getId());
            if (!items.isEmpty()) {
                result.put(category, items);
            }
        }
        return result;
    }
}
