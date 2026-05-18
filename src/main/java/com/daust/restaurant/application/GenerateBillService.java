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
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerateBillService {

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final ConfigurationRepository configurationRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public GenerateBillService(
            OrderRepository orderRepository,
            BillRepository billRepository,
            ConfigurationRepository configurationRepository,
            MenuItemRepository menuItemRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.configurationRepository = configurationRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public BillId generateBill(OrderId orderId, UserId managerId) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getState() != OrderState.SERVED) {
            throw new IllegalStateException(
                    "Bill can only be generated for an Order in state SERVED; was " + order.getState());
        }

        if (!billRepository.findByOrderId(orderId).isEmpty()) {
            throw new BillAlreadyGeneratedException("Bill already exists for Order " + orderId);
        }

        Configuration configuration = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration singleton not initialized"));

        User manager = userRepository
                .findById(managerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + managerId));

        Map<MenuItemId, String> nameLookup = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            MenuItemId menuItemId = item.getMenuItemId();
            if (nameLookup.containsKey(menuItemId)) {
                continue;
            }
            MenuItem menuItem = menuItemRepository
                    .findById(menuItemId)
                    .orElseThrow(() -> new MenuItemNotFoundException("MenuItem not found: " + menuItemId));
            nameLookup.put(menuItemId, menuItem.getName());
        }

        Bill bill = new Bill(order, configuration, nameLookup);
        billRepository.save(bill);

        auditLogRepository.save(new AuditLogEntry(
                manager.getId(),
                manager.getRole(),
                "BILL_GENERATED",
                "Bill",
                bill.getId().value().toString(),
                "Order=" + order.getId().value(),
                "billNumber=" + bill.getBillNumber() + ", total=" + bill.getTotal()));

        return bill.getId();
    }
}
