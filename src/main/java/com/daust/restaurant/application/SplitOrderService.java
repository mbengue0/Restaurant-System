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
import com.daust.restaurant.domain.OrderItemId;
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
 * UC15 — Split Order. Produces N {@link Bill}s from one {@link Order} atomically, dividing the
 * cover charge by N inside each Bill's split-case constructor (BR3).
 *
 * <p>Gates on {@link Configuration#isSplitMergePolicyEnabled()} (BR6); rejects if the order is not
 * in {@link OrderState#SERVED} or already has any bill, and validates that the groups form a true
 * partition of the order's items (no duplicates, no omissions, no unknown ids, no empty group).
 *
 * <p>Persistence note: the order ↔ bill bridge is many-to-many (Section 7). Each Bill created here
 * carries only this order's id; that's what the split case looks like in the {@code order_bills}
 * table.
 */
@Service
@Transactional
public class SplitOrderService {

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final ConfigurationRepository configurationRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public SplitOrderService(
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

    public List<BillId> splitOrder(
            OrderId orderId, List<List<OrderItemId>> groups, UserId managerId) {

        if (groups == null || groups.size() < 2) {
            throw new InvalidSplitException("Split requires at least 2 groups");
        }

        Configuration configuration = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration singleton not initialized"));
        if (!configuration.isSplitMergePolicyEnabled()) {
            throw new SplitMergeDisabledException(
                    "Split/merge is disabled in Configuration (BR6).");
        }

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getState() != OrderState.SERVED) {
            throw new IllegalStateException(
                    "Order must be in state SERVED to split; was " + order.getState());
        }

        if (!billRepository.findByOrderId(orderId).isEmpty()) {
            throw new BillAlreadyGeneratedException(
                    "Bill already exists for Order " + orderId + " — cannot split.");
        }

        User manager = userRepository
                .findById(managerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + managerId));

        Map<OrderItemId, OrderItem> orderItemsById = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            orderItemsById.put(item.getId(), item);
        }

        Set<OrderItemId> seen = new HashSet<>();
        List<List<OrderItem>> resolvedGroups = new ArrayList<>(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            List<OrderItemId> group = groups.get(i);
            if (group == null || group.isEmpty()) {
                throw new InvalidSplitException("Group " + (i + 1) + " is empty");
            }
            List<OrderItem> resolved = new ArrayList<>(group.size());
            for (OrderItemId itemId : group) {
                OrderItem item = orderItemsById.get(itemId);
                if (item == null) {
                    throw new InvalidSplitException(
                            "Unknown OrderItem " + itemId + " in group " + (i + 1));
                }
                if (!seen.add(itemId)) {
                    throw new InvalidSplitException(
                            "OrderItem " + itemId + " appears in more than one group");
                }
                resolved.add(item);
            }
            resolvedGroups.add(resolved);
        }
        if (seen.size() != orderItemsById.size()) {
            Set<OrderItemId> missing = new HashSet<>(orderItemsById.keySet());
            missing.removeAll(seen);
            throw new InvalidSplitException("OrderItem(s) missing from split: " + missing);
        }

        Map<MenuItemId, String> nameLookup = new HashMap<>();
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

        int splitDivisor = resolvedGroups.size();
        List<BillId> billIds = new ArrayList<>(splitDivisor);
        for (List<OrderItem> items : resolvedGroups) {
            Bill bill = new Bill(order, configuration, items, splitDivisor, nameLookup);
            billRepository.save(bill);
            billIds.add(bill.getId());
        }

        auditLogRepository.save(new AuditLogEntry(
                manager.getId(),
                manager.getRole(),
                "ORDER_SPLIT",
                "Order",
                order.getId().value().toString(),
                null,
                "splitDivisor=" + splitDivisor + ", bills=" + billIds));

        return billIds;
    }
}
