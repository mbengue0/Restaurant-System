package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillLineSnapshot;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.TableId;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(BillRepositoryImpl.class)
class BillRepositoryImplTest {

    @Autowired
    private BillRepository billRepository;

    private static final CategoryId CATEGORY = CategoryId.generate();

    private static Configuration sampleConfig() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                false,
                EnumSet.of(PaymentMethod.CASH));
    }

    private static MenuItem menuItem(String name, String price) {
        return new MenuItem(name, null, new BigDecimal(price), CATEGORY);
    }

    private static Map<MenuItemId, String> lookupFor(MenuItem... items) {
        Map<MenuItemId, String> lookup = new HashMap<>();
        for (MenuItem mi : items) {
            lookup.put(mi.getId(), mi.getName());
        }
        return lookup;
    }

    private static Order orderWith(MenuItem... items) {
        Order order = new Order(TableId.generate());
        for (MenuItem mi : items) {
            order.addItem(mi, 1);
        }
        return order;
    }

    @Test
    void saveAndFindById_roundTripsBillWithMultipleLineItems() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        MenuItem coffee = menuItem("Coffee", "500.00");
        Order order = new Order(TableId.generate());
        order.addItem(pizza, 2);
        order.addItem(coffee, 1);

        Bill original = new Bill(order, sampleConfig(), lookupFor(pizza, coffee));
        billRepository.save(original);

        Bill reloaded = billRepository.findById(original.getId()).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(original.getId());
        assertThat(reloaded.getBillNumber()).isEqualTo(original.getBillNumber());
        assertThat(reloaded.getOrderIds()).containsExactly(order.getId());
        assertThat(reloaded.getItemsSubtotal()).isEqualByComparingTo(original.getItemsSubtotal());
        assertThat(reloaded.getTaxAmount()).isEqualByComparingTo(original.getTaxAmount());
        assertThat(reloaded.getServiceChargeAmount()).isEqualByComparingTo(original.getServiceChargeAmount());
        assertThat(reloaded.getCoverChargeAmount()).isEqualByComparingTo(original.getCoverChargeAmount());
        assertThat(reloaded.getTotal()).isEqualByComparingTo(original.getTotal());
        assertThat(reloaded.isPaid()).isFalse();
        assertThat(reloaded.getLineItems())
                .extracting(BillLineSnapshot::menuItemName)
                .containsExactlyInAnyOrder("Pizza", "Coffee");
    }

    @Test
    void markPaid_persistsAcrossReload() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Bill bill = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));
        billRepository.save(bill);

        Bill loaded = billRepository.findById(bill.getId()).orElseThrow();
        loaded.markPaid();
        billRepository.save(loaded);

        Bill reloaded = billRepository.findById(bill.getId()).orElseThrow();
        assertThat(reloaded.isPaid()).isTrue();
    }

    @Test
    void findByOrderId_returnsBillsLinkedViaBridgeTable() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza));

        billRepository.save(bill);

        List<Bill> bills = billRepository.findByOrderId(order.getId());

        assertThat(bills).extracting(Bill::getId).containsExactly(bill.getId());
    }

    @Test
    void findByOrderId_returnsMultipleBillsForSplitOrder() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        MenuItem coffee = menuItem("Coffee", "500.00");
        Order order = orderWith(pizza, coffee);
        OrderItem pizzaLine = order.getItems().get(0);
        OrderItem coffeeLine = order.getItems().get(1);
        Map<MenuItemId, String> lookup = lookupFor(pizza, coffee);

        Bill billA = new Bill(order, sampleConfig(), List.of(pizzaLine), 2, lookup);
        Bill billB = new Bill(order, sampleConfig(), List.of(coffeeLine), 2, lookup);

        billRepository.save(billA);
        billRepository.save(billB);

        List<Bill> bills = billRepository.findByOrderId(order.getId());

        assertThat(bills)
                .extracting(Bill::getId)
                .containsExactlyInAnyOrder(billA.getId(), billB.getId());
    }

    @Test
    void findByOrderId_returnsEmptyListForUnknownOrder() {
        List<Bill> bills = billRepository.findByOrderId(com.daust.restaurant.domain.OrderId.generate());

        assertThat(bills).isEmpty();
    }

    @Test
    void save_rejectsDuplicateBillNumber() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Bill firstBill = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));
        billRepository.save(firstBill);

        // Build a second Bill, then clone it via reconstitute using the first bill's number.
        Bill secondBill = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));
        Bill duplicateNumber = Bill.reconstitute(
                secondBill.getId(),
                firstBill.getBillNumber(),
                secondBill.getOrderIds(),
                secondBill.getGeneratedAt(),
                secondBill.getLineItems(),
                secondBill.getItemsSubtotal(),
                secondBill.getTaxAmount(),
                secondBill.getServiceChargeAmount(),
                secondBill.getCoverChargeAmount(),
                secondBill.getTotal(),
                secondBill.isPaid());

        assertThatThrownBy(() -> {
            billRepository.save(duplicateNumber);
            // Force flush via a JPQL query — plain findById would return the entity from the
            // persistence-context cache and never hit the DB, so the unique-constraint
            // violation would not surface during the test method.
            billRepository.findByOrderId(com.daust.restaurant.domain.OrderId.generate());
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
