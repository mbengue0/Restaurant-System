package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private OrderRepository orderRepository;
    @Mock private TableRepository tableRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private PlaceOrderService service;

    private static Category activeCategory(CategoryId id) {
        return Category.reconstitute(id, "Starters", 1, true);
    }

    private static Category inactiveCategory(CategoryId id) {
        return Category.reconstitute(id, "Retired", 1, false);
    }

    @Test
    void startOrder_createsOrderAndAudits_whenTableOccupied() {
        Table table = new Table(4);
        table.seatCustomers();
        User waiter = new User("w", HASH, "W", Role.WAITER, false);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        OrderId id = service.startOrder(table.getId(), waiter.getId());

        assertThat(id).isNotNull();
        verify(orderRepository).save(any(Order.class));
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_STARTED");
    }

    @Test
    void startOrder_throws_whenTableNotOccupied() {
        Table table = new Table(2);
        User waiter = new User("w", HASH, "W", Role.WAITER, false);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        assertThatThrownBy(() -> service.startOrder(table.getId(), waiter.getId()))
                .isInstanceOf(TableNotOccupiedException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void addItemToOrder_addsAndAudits() {
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId());
        CategoryId catId = CategoryId.generate();
        MenuItem item = new MenuItem("Thieb", null, new BigDecimal("8500"), catId);
        User waiter = new User("w", HASH, "W", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(activeCategory(catId)));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        service.addItemToOrder(order.getId(), item.getId(), 2, waiter.getId());

        assertThat(order.getItems()).hasSize(1);
        verify(orderRepository).save(order);
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_ITEM_ADDED");
    }

    @Test
    void addItemToOrder_throwsInactiveCategoryException_whenCategoryInactive_FR9() {
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId());
        CategoryId catId = CategoryId.generate();
        // Item itself is active, but its category is not.
        MenuItem item = new MenuItem("Old Special", null, new BigDecimal("5000"), catId);
        User waiter = new User("w", HASH, "W", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(categoryRepository.findById(catId)).thenReturn(Optional.of(inactiveCategory(catId)));

        assertThatThrownBy(() -> service.addItemToOrder(order.getId(), item.getId(), 1, waiter.getId()))
                .isInstanceOf(InactiveCategoryException.class)
                .hasMessageContaining("inactive");

        assertThat(order.getItems()).isEmpty();
        verify(orderRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void addItemToOrder_throwsCategoryNotFound_whenCategoryMissing() {
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId());
        CategoryId catId = CategoryId.generate();
        MenuItem item = new MenuItem("Item", null, new BigDecimal("1000"), catId);
        var waiterId = com.daust.restaurant.domain.UserId.generate();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(categoryRepository.findById(catId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItemToOrder(order.getId(), item.getId(), 1, waiterId))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void addItemToOrder_throwsWhenMenuItemMissing() {
        Table table = new Table(2);
        table.seatCustomers();
        Order order = new Order(table.getId());
        var missingId = com.daust.restaurant.domain.MenuItemId.generate();
        var waiterId = com.daust.restaurant.domain.UserId.generate();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItemToOrder(order.getId(), missingId, 1, waiterId))
                .isInstanceOf(MenuItemNotFoundException.class);
    }

    @Test
    void submitOrder_setsSubmittedAndAudits() {
        Table table = new Table(2);
        table.seatCustomers();
        Order order = new Order(table.getId());
        MenuItem item = new MenuItem("Pastels", null, new BigDecimal("3000"), CategoryId.generate());
        order.addItem(item, 1);
        User waiter = new User("w", HASH, "W", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        service.submitOrder(order.getId(), waiter.getId());

        assertThat(order.isVisibleToKitchen()).isTrue();
        assertThat(order.getSubmittedAt()).isNotNull();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_SUBMITTED");
    }

    @Test
    void removeItemFromOrder_removesAndAudits() {
        Table table = new Table(2);
        table.seatCustomers();
        Order order = new Order(table.getId());
        MenuItem item = new MenuItem("Pastels", null, new BigDecimal("3000"), CategoryId.generate());
        order.addItem(item, 1);
        var orderItemId = order.getItems().get(0).getId();
        User waiter = new User("w", HASH, "W", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        service.removeItemFromOrder(order.getId(), orderItemId, waiter.getId());

        assertThat(order.getItems()).isEmpty();
        verify(orderRepository).save(order);
    }
}
