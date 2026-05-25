package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC16 — Merge Orders. Builds one {@link Bill} from N {@link Order}s atomically. The
 * {@code order_bills} bridge ends up multi-element for this bill, which is the structural shape
 * Section 7 of the design report describes.
 *
 * <p>Gates on {@link Configuration#isSplitMergePolicyEnabled()} (BR6). Requires ≥2 distinct
 * orders, each in {@link OrderState#SERVED} with no existing bill. BR3 is computed in
 * {@link Bill#forMergedOrders} over the combined items; cover charge is applied once for the
 * merged party.
 */
@Service
@Transactional
public class MergeOrdersService {

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final ConfigurationRepository configurationRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public MergeOrdersService(
            OrderRepository orderRepository,
            BillRepository billRepository,
            ConfigurationRepository configurationRepository,
            MenuItemRepository menuItemRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.configurationRepository = configurationRepository;
        this.menuItemRepository = menuItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public BillId mergeOrders(List<OrderId> orderIds, UserId managerId) {
        if (orderIds == null || orderIds.size() < 2) {
            throw new InvalidMergeException("Merge requires at least 2 orders");
        }
        Set<OrderId> distinct = new HashSet<>(orderIds);
        if (distinct.size() != orderIds.size()) {
            throw new InvalidMergeException("Duplicate orderId in merge selection");
        }

        Configuration configuration = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration singleton not initialized"));
        if (!configuration.isSplitMergePolicyEnabled()) {
            throw new SplitMergeDisabledException(
                    "Split/merge is disabled in Configuration (BR6).");
        }

        User manager = userRepository
                .findById(managerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + managerId));

        List<Order> orders = new ArrayList<>(orderIds.size());
        for (OrderId id : orderIds) {
            Order order = orderRepository
                    .findById(id)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
            if (order.getState() != OrderState.SERVED) {
                throw new IllegalStateException(
                        "Order " + id + " must be SERVED to merge; was " + order.getState());
            }
            if (!billRepository.findByOrderId(id).isEmpty()) {
                throw new BillAlreadyGeneratedException(
                        "Bill already exists for Order " + id + " — cannot merge.");
            }
            orders.add(order);
        }

        // BR6: orders linked to different tables cannot be merged.
        var firstTable = orders.get(0).getTableId();
        for (Order order : orders) {
            if (!order.getTableId().equals(firstTable)) {
                throw new InvalidMergeException("orders must belong to the same table");
            }
        }

        Map<MenuItemId, String> nameLookup = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                MenuItemId menuItemId = item.getMenuItemId();
                if (nameLookup.containsKey(menuItemId)) {
                    continue;
                }
                MenuItem menuItem = menuItemRepository
                        .findById(menuItemId)
                        .orElseThrow(() -> new MenuItemNotFoundException(
                                "MenuItem not found: " + menuItemId));
                nameLookup.put(menuItemId, menuItem.getName());
            }
        }

        Bill bill = Bill.forMergedOrders(orders, configuration, nameLookup);
        billRepository.save(bill);

        auditLogRepository.save(new AuditLogEntry(
                manager.getId(),
                manager.getRole(),
                "ORDERS_MERGED",
                "Bill",
                bill.getId().value().toString(),
                null,
                "orders=" + orderIds + ", count=" + orders.size()
                        + ", total=" + bill.getTotal()));

        return bill.getId();
    }
}
