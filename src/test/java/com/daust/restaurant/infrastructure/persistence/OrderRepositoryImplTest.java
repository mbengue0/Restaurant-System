package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.daust.restaurant.domain.CancellationReason;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.User;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(OrderRepositoryImpl.class)
class OrderRepositoryImplTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";
    private static final CategoryId CATEGORY = CategoryId.generate();

    @Autowired
    private OrderRepository orderRepository;

    private static MenuItem menuItem(String name, String price) {
        return new MenuItem(name, null, new BigDecimal(price), CATEGORY);
    }

    @Test
    void saveAndFindById_roundTripsOrderWithMultipleItems() {
        TableId table = TableId.generate();
        MenuItem pizza = menuItem("Pizza", "9.00");
        MenuItem coffee = menuItem("Coffee", "3.00");
        Order order = new Order(table);
        order.addItem(pizza, 2);
        order.addItem(coffee, 1);
        order.submit();

        orderRepository.save(order);

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(order.getId());
        assertThat(reloaded.getTableId()).isEqualTo(table);
        assertThat(reloaded.getState()).isEqualTo(OrderState.PLACED);
        assertThat(reloaded.isVisibleToKitchen()).isTrue();
        assertThat(reloaded.getSubmittedAt()).isNotNull();
        assertThat(reloaded.getPlacedAt()).isNotNull();
        assertThat(reloaded.getItems()).hasSize(2);

        OrderItem pizzaLine = reloaded.getItems().stream()
                .filter(line -> line.getMenuItemId().equals(pizza.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(pizzaLine.getQuantity()).isEqualTo(2);
        assertThat(pizzaLine.getRecordedUnitPrice()).isEqualByComparingTo("9.00");
        assertThat(pizzaLine.getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void save_persistsStateTransitionsAndTimestamps() {
        TableId table = TableId.generate();
        Order order = new Order(table);
        order.addItem(menuItem("Pizza", "9.00"), 1);
        orderRepository.save(order);

        Order loaded = orderRepository.findById(order.getId()).orElseThrow();
        loaded.startPreparation();
        loaded.markReady();
        loaded.markServed();
        loaded.markCompleted();
        orderRepository.save(loaded);

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(OrderState.COMPLETED);
        assertThat(reloaded.getServedAt()).isNotNull();
        assertThat(reloaded.getCompletedAt()).isNotNull();
    }

    @Test
    void save_persistsCancellationAttributes() {
        TableId table = TableId.generate();
        User manager = new User("mgr", HASH, "Manager", Role.MANAGER, false);
        Order order = new Order(table);
        order.addItem(menuItem("Pizza", "9.00"), 1);
        order.startPreparation();
        order.cancel(CancellationReason.KITCHEN_ERROR, "stove broke", manager);

        orderRepository.save(order);

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(OrderState.CANCELLED);
        assertThat(reloaded.getCancellationReason()).isEqualTo(CancellationReason.KITCHEN_ERROR);
        assertThat(reloaded.getCancellationNote()).isEqualTo("stove broke");
        assertThat(reloaded.getCancelledBy()).isEqualTo(manager.getId());
        assertThat(reloaded.getCancelledAt()).isNotNull();
    }

    @Test
    void findActiveByTableId_excludesCompletedAndCancelledOrders() {
        TableId table = TableId.generate();
        TableId otherTable = TableId.generate();
        User manager = new User("mgr", HASH, "Manager", Role.MANAGER, false);

        Order placed = newOrderWithItem(table);
        Order inPrep = newOrderWithItem(table);
        inPrep.startPreparation();
        Order ready = newOrderWithItem(table);
        ready.startPreparation();
        ready.markReady();
        Order served = newOrderWithItem(table);
        served.startPreparation();
        served.markReady();
        served.markServed();
        Order completed = newOrderWithItem(table);
        completed.startPreparation();
        completed.markReady();
        completed.markServed();
        completed.markCompleted();
        Order cancelled = newOrderWithItem(table);
        cancelled.cancel(CancellationReason.CUSTOMER_LEFT, null, manager);
        Order foreign = newOrderWithItem(otherTable);

        orderRepository.save(placed);
        orderRepository.save(inPrep);
        orderRepository.save(ready);
        orderRepository.save(served);
        orderRepository.save(completed);
        orderRepository.save(cancelled);
        orderRepository.save(foreign);

        List<Order> active = orderRepository.findActiveByTableId(table);

        assertThat(active)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(placed.getId(), inPrep.getId(), ready.getId(), served.getId());
    }

    @Test
    void findByState_returnsOnlyMatchingOrders() {
        TableId table = TableId.generate();
        Order placedA = newOrderWithItem(table);
        Order placedB = newOrderWithItem(table);
        Order ready = newOrderWithItem(table);
        ready.startPreparation();
        ready.markReady();

        orderRepository.save(placedA);
        orderRepository.save(placedB);
        orderRepository.save(ready);

        List<Order> placedResults = orderRepository.findByState(OrderState.PLACED);
        List<Order> readyResults = orderRepository.findByState(OrderState.READY);

        assertThat(placedResults).extracting(Order::getId).containsExactlyInAnyOrder(placedA.getId(), placedB.getId());
        assertThat(readyResults).extracting(Order::getId).containsExactly(ready.getId());
    }

    private static Order newOrderWithItem(TableId table) {
        Order order = new Order(table);
        order.addItem(menuItem("Pizza", "9.00"), 1);
        return order;
    }
}
