package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
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
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public PlaceOrderService(
            OrderRepository orderRepository,
            TableRepository tableRepository,
            MenuItemRepository menuItemRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public OrderId startOrder(TableId tableId, UserId waiterId) {
        Table table = tableRepository
                .findById(tableId)
                .orElseThrow(() -> new TableNotFoundException("Table not found: " + tableId));
        User waiter = loadUser(waiterId);

        if (table.getStatus() != TableStatus.OCCUPIED) {
            throw new TableNotOccupiedException(
                    "Table " + tableId + " must be OCCUPIED to start an Order; was " + table.getStatus());
        }

        Order order = new Order(table.getId());
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                waiter.getId(),
                waiter.getRole(),
                "ORDER_STARTED",
                "Order",
                order.getId().value().toString(),
                null,
                "PLACED"));

        return order.getId();
    }

    @Transactional
    public void addItemToOrder(OrderId orderId, MenuItemId menuItemId, int quantity, UserId waiterId) {
        Order order = loadOrder(orderId);
        MenuItem menuItem = menuItemRepository
                .findById(menuItemId)
                .orElseThrow(() -> new MenuItemNotFoundException("MenuItem not found: " + menuItemId));

        // FR9 / BR2 — cross-aggregate guard: reject items whose Category is inactive.
        // Order doesn't know about Category, so the check lives here (deferred-guard pattern).
        Category category = categoryRepository
                .findById(menuItem.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category not found: " + menuItem.getCategoryId()));
        if (!category.isActive()) {
            throw new InactiveCategoryException(
                    "Cannot add item '" + menuItem.getName()
                            + "' — its category '" + category.getName() + "' is inactive (FR9/BR2).");
        }

        User waiter = loadUser(waiterId);

        order.addItem(menuItem, quantity);
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                waiter.getId(),
                waiter.getRole(),
                "ORDER_ITEM_ADDED",
                "Order",
                order.getId().value().toString(),
                null,
                menuItem.getName() + " x" + quantity));
    }

    @Transactional
    public void removeItemFromOrder(OrderId orderId, OrderItemId orderItemId, UserId waiterId) {
        Order order = loadOrder(orderId);
        User waiter = loadUser(waiterId);

        OrderItem target = order.getItems().stream()
                .filter(i -> i.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "OrderItem " + orderItemId + " not part of Order " + orderId));

        order.removeItem(target);
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                waiter.getId(),
                waiter.getRole(),
                "ORDER_ITEM_REMOVED",
                "Order",
                order.getId().value().toString(),
                target.getMenuItemId().value().toString(),
                null));
    }

    @Transactional
    public void submitOrder(OrderId orderId, UserId waiterId) {
        Order order = loadOrder(orderId);
        User waiter = loadUser(waiterId);

        order.submit();
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                waiter.getId(),
                waiter.getRole(),
                "ORDER_SUBMITTED",
                "Order",
                order.getId().value().toString(),
                null,
                "submittedAt=" + order.getSubmittedAt()));
    }

    private Order loadOrder(OrderId orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private User loadUser(UserId userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
}
