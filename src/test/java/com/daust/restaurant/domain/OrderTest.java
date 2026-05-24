package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderTest {

    private static final TableId TABLE = TableId.generate();
    private static final CategoryId CATEGORY = CategoryId.generate();
    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    private static MenuItem activeMenuItem() {
        return new MenuItem("Pizza", null, new BigDecimal("9.00"), CATEGORY);
    }

    private static User userWith(Role role) {
        return new User("test_" + role.name().toLowerCase(), HASH, "Test " + role.name(), role, false);
    }

    // -------- construction --------

    @Test
    void newOrder_isInPlacedStateWithGeneratedIdAndEmptyItemsAndPlacedAtNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Order order = new Order(TABLE);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getId().value()).isNotNull();
        assertThat(order.getTableId()).isEqualTo(TABLE);
        assertThat(order.getState()).isEqualTo(OrderState.PLACED);
        assertThat(order.getPlacedAt()).isBetween(before, after);
        assertThat(order.getSubmittedAt()).isNull();
        assertThat(order.getServedAt()).isNull();
        assertThat(order.getCompletedAt()).isNull();
        assertThat(order.getCancelledAt()).isNull();
        assertThat(order.getCancellationReason()).isNull();
        assertThat(order.getCancellationNote()).isNull();
        assertThat(order.getCancelledBy()).isNull();
        assertThat(order.isVisibleToKitchen()).isFalse();
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    void constructor_rejectsNullTableId() {
        assertThatThrownBy(() -> new Order(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void newOrders_haveDistinctIds() {
        Order a = new Order(TABLE);
        Order b = new Order(TABLE);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    // -------- addItem --------

    @Test
    void addItem_appendsOrderItem_andAttachesOrderId() {
        Order order = new Order(TABLE);
        MenuItem pizza = activeMenuItem();

        order.addItem(pizza, 2);

        assertThat(order.getItems()).hasSize(1);
        OrderItem line = order.getItems().get(0);
        assertThat(line.getMenuItemId()).isEqualTo(pizza.getId());
        assertThat(line.getQuantity()).isEqualTo(2);
        assertThat(line.getRecordedUnitPrice()).isEqualByComparingTo("9.00");
        assertThat(line.getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void addItem_snapshotPersistsThroughLaterPriceChange_FR4() {
        Order order = new Order(TABLE);
        MenuItem pizza = activeMenuItem();
        order.addItem(pizza, 1);

        pizza.changePrice(new BigDecimal("15.00"));

        assertThat(order.getItems().get(0).getRecordedUnitPrice()).isEqualByComparingTo("9.00");
    }

    @Test
    void addItem_rejectsInactiveMenuItem_BR2() {
        Order order = new Order(TABLE);
        MenuItem retired = activeMenuItem();
        retired.deactivate();

        assertThatThrownBy(() -> order.addItem(retired, 1)).isInstanceOf(IllegalStateException.class);
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    void addItem_rejectsNullMenuItem() {
        Order order = new Order(TABLE);

        assertThatThrownBy(() -> order.addItem(null, 1)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED", "COMPLETED", "CANCELLED"})
    void addItem_rejectsWhenStateIsNotPlaced(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(() -> order.addItem(activeMenuItem(), 1))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------- removeItem --------

    @Test
    void removeItem_removesMatchingItem() {
        Order order = new Order(TABLE);
        MenuItem pizza = activeMenuItem();
        order.addItem(pizza, 1);
        OrderItem line = order.getItems().get(0);

        order.removeItem(line);

        assertThat(order.getItems()).isEmpty();
    }

    @Test
    void removeItem_rejectsItemNotInOrder() {
        Order order = new Order(TABLE);
        OrderItem foreign = new OrderItem(MenuItemId.generate(), 1, new BigDecimal("1.00"));

        assertThatThrownBy(() -> order.removeItem(foreign)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeItem_rejectsNullItem() {
        Order order = new Order(TABLE);

        assertThatThrownBy(() -> order.removeItem(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED", "COMPLETED", "CANCELLED"})
    void removeItem_rejectsWhenStateIsNotPlaced(OrderState state) {
        Order order = new Order(TABLE);
        order.addItem(activeMenuItem(), 1);
        OrderItem line = order.getItems().get(0);
        driveTo(order, state);

        assertThatThrownBy(() -> order.removeItem(line)).isInstanceOf(IllegalStateException.class);
    }

    // -------- submit --------

    @Test
    void submit_setsVisibleToKitchenAndSubmittedAt_stateRemainsPlaced_Path1() {
        Order order = new Order(TABLE);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        order.submit();

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(order.isVisibleToKitchen()).isTrue();
        assertThat(order.getSubmittedAt()).isBetween(before, after);
        assertThat(order.getState()).isEqualTo(OrderState.PLACED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED", "COMPLETED", "CANCELLED"})
    void submit_rejectsWhenStateIsNotPlaced(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(order::submit).isInstanceOf(IllegalStateException.class);
    }

    // -------- startPreparation / markReady / markServed / markCompleted --------

    @Test
    void startPreparation_transitionsPlacedToInPreparation() {
        Order order = new Order(TABLE);

        order.startPreparation();

        assertThat(order.getState()).isEqualTo(OrderState.IN_PREPARATION);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED", "COMPLETED", "CANCELLED"})
    void startPreparation_rejectsFromAnyOtherState(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(order::startPreparation).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markReady_transitionsInPreparationToReady() {
        Order order = orderInState(OrderState.IN_PREPARATION);

        order.markReady();

        assertThat(order.getState()).isEqualTo(OrderState.READY);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"PLACED", "READY", "SERVED", "COMPLETED", "CANCELLED"})
    void markReady_rejectsFromAnyOtherState(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(order::markReady).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markServed_transitionsReadyToServed_andRecordsServedAt() {
        Order order = orderInState(OrderState.READY);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        order.markServed();

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(order.getState()).isEqualTo(OrderState.SERVED);
        assertThat(order.getServedAt()).isBetween(before, after);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"PLACED", "IN_PREPARATION", "SERVED", "COMPLETED", "CANCELLED"})
    void markServed_rejectsFromAnyOtherState(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(order::markServed).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markCompleted_transitionsServedToCompleted_andRecordsCompletedAt() {
        Order order = orderInState(OrderState.SERVED);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        order.markCompleted();

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(order.getState()).isEqualTo(OrderState.COMPLETED);
        assertThat(order.getCompletedAt()).isBetween(before, after);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"PLACED", "IN_PREPARATION", "READY", "COMPLETED", "CANCELLED"})
    void markCompleted_rejectsFromAnyOtherState(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(order::markCompleted).isInstanceOf(IllegalStateException.class);
    }

    // -------- cancel: BR5 tiered authorization --------

    @ParameterizedTest
    @EnumSource(Role.class)
    void cancel_fromPlaced_isAllowedForAnyRole_BR5(Role role) {
        Order order = new Order(TABLE);

        order.cancel(CancellationReason.CUSTOMER_LEFT, "table walked out", userWith(role));

        assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED"})
    void cancel_fromActivePostPlacedStates_isAllowedForManager_BR5(OrderState state) {
        Order order = orderInState(state);

        order.cancel(CancellationReason.KITCHEN_ERROR, "stove failure", userWith(Role.MANAGER));

        assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED"})
    void cancel_fromActivePostPlacedStates_isRejectedForWaiter_BR5(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(() ->
                        order.cancel(CancellationReason.CUSTOMER_COMPLAINT, "guest unhappy", userWith(Role.WAITER)))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(order.getState()).isEqualTo(state);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED"})
    void cancel_fromActivePostPlacedStates_isRejectedForKitchenStaff_BR5(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(() ->
                        order.cancel(CancellationReason.OTHER, "no comment", userWith(Role.KITCHEN_STAFF)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderState.class, names = {"IN_PREPARATION", "READY", "SERVED"})
    void cancel_fromActivePostPlacedStates_isRejectedForAdmin_BR5(OrderState state) {
        Order order = orderInState(state);

        assertThatThrownBy(() ->
                        order.cancel(CancellationReason.OTHER, "audit override", userWith(Role.ADMIN)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void cancel_fromCompleted_isRejectedForAllRoles(Role role) {
        Order order = orderInState(OrderState.COMPLETED);

        assertThatThrownBy(() -> order.cancel(CancellationReason.OTHER, null, userWith(role)))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void cancel_fromCancelled_isRejectedForAllRoles(Role role) {
        Order order = orderInState(OrderState.CANCELLED);

        assertThatThrownBy(() -> order.cancel(CancellationReason.OTHER, null, userWith(role)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancel_recordsReasonNoteUserAndTimestamp() {
        Order order = new Order(TABLE);
        User manager = userWith(Role.MANAGER);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        order.cancel(CancellationReason.CUSTOMER_LEFT, "table 4 walked", manager);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.CUSTOMER_LEFT);
        assertThat(order.getCancellationNote()).isEqualTo("table 4 walked");
        assertThat(order.getCancelledBy()).isEqualTo(manager.getId());
        assertThat(order.getCancelledAt()).isBetween(before, after);
    }

    @Test
    void cancel_acceptsNullNote() {
        Order order = new Order(TABLE);

        order.cancel(CancellationReason.OTHER, null, userWith(Role.WAITER));

        assertThat(order.getCancellationNote()).isNull();
        assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
    }

    @Test
    void cancel_rejectsNullReason() {
        Order order = new Order(TABLE);

        assertThatThrownBy(() -> order.cancel(null, "note", userWith(Role.WAITER)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void cancel_rejectsNullUser() {
        Order order = new Order(TABLE);

        assertThatThrownBy(() -> order.cancel(CancellationReason.OTHER, "note", null))
                .isInstanceOf(NullPointerException.class);
    }

    // -------- getItems immutability --------

    @Test
    void getItems_returnsImmutableView() {
        Order order = new Order(TABLE);
        order.addItem(activeMenuItem(), 1);

        assertThatThrownBy(() -> order.getItems().add(new OrderItem(MenuItemId.generate(), 1, new BigDecimal("1.00"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -------- happy-path full sequence --------

    @Test
    void fullHappyPathSequence_PlacedThroughCompleted() {
        Order order = new Order(TABLE);
        order.addItem(activeMenuItem(), 1);
        order.submit();
        order.startPreparation();
        order.markReady();
        order.markServed();
        order.markCompleted();

        assertThat(order.getState()).isEqualTo(OrderState.COMPLETED);
        assertThat(order.getSubmittedAt()).isNotNull();
        assertThat(order.getServedAt()).isNotNull();
        assertThat(order.getCompletedAt()).isNotNull();
        assertThat(order.isVisibleToKitchen()).isTrue();
    }

    // -------- helpers --------

    private static Order orderInState(OrderState target) {
        Order order = new Order(TABLE);
        driveTo(order, target);
        return order;
    }

    /** Drive an Order from PLACED to the target state through legal transitions. */
    private static void driveTo(Order order, OrderState target) {
        if (target == OrderState.PLACED) {
            return;
        }
        if (target == OrderState.CANCELLED) {
            order.cancel(CancellationReason.OTHER, "test setup", userWith(Role.MANAGER));
            return;
        }
        // Forward chain: PLACED -> IN_PREPARATION -> READY -> SERVED -> COMPLETED
        order.startPreparation();
        if (target == OrderState.IN_PREPARATION) return;
        order.markReady();
        if (target == OrderState.READY) return;
        order.markServed();
        if (target == OrderState.SERVED) return;
        order.markCompleted();
        if (target == OrderState.COMPLETED) return;
        throw new IllegalArgumentException("unknown target state: " + target);
    }

    @Test
    void driveTo_helperReachesEveryNonInitialState() {
        // Sanity check on the test helper itself
        for (OrderState s : EnumSet.allOf(OrderState.class)) {
            assertThat(orderInState(s).getState()).isEqualTo(s);
        }
    }
}
