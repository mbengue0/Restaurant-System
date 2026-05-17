package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BillTest {

    private static final CategoryId CATEGORY = CategoryId.generate();

    // -------- helpers --------

    private static Configuration sampleConfig() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                false,
                EnumSet.of(PaymentMethod.CASH));
    }

    private static Configuration configWith(String tax, String service, String cover) {
        return new Configuration(
                new BigDecimal(tax),
                new BigDecimal(service),
                new BigDecimal(cover),
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

    // -------- normal constructor: identity & metadata --------

    @Test
    void newBill_hasGeneratedIdAndBillNumberAndGeneratedAt() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza));

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(bill.getId()).isNotNull();
        assertThat(bill.getId().value()).isNotNull();
        assertThat(bill.getBillNumber()).isNotNull();
        assertThat(bill.getGeneratedAt()).isBetween(before, after);
        assertThat(bill.isPaid()).isFalse();
        assertThat(bill.getOrderIds()).containsExactly(order.getId());
    }

    @Test
    void billNumber_matchesExpectedFormat() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza));

        assertThat(bill.getBillNumber()).matches("BILL-\\d{8}-[0-9A-F]{8}");
    }

    @Test
    void twoBills_normallyHaveDistinctBillNumbers() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Bill a = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));
        Bill b = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));

        // Same-second collision is possible with a 32-bit suffix but vanishingly rare.
        assertThat(a.getBillNumber()).isNotEqualTo(b.getBillNumber());
    }

    // -------- BR3 calculation correctness --------

    @Test
    void calculatesItemsSubtotal_singleItem_singleUnit() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza));

        assertThat(bill.getItemsSubtotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void calculatesItemsSubtotal_singleItem_multipleUnits() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = new Order(TableId.generate());
        order.addItem(pizza, 3);

        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza));

        assertThat(bill.getItemsSubtotal()).isEqualByComparingTo("3000.00");
    }

    @Test
    void calculatesItemsSubtotal_multipleItems() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        MenuItem coffee = menuItem("Coffee", "500.00");
        Order order = new Order(TableId.generate());
        order.addItem(pizza, 2); // 2000
        order.addItem(coffee, 3); // 1500

        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza, coffee));

        assertThat(bill.getItemsSubtotal()).isEqualByComparingTo("3500.00");
    }

    @Test
    void calculatesTaxAmount_asRateTimesSubtotal() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Configuration cfg = configWith("0.18", "0.00", "0.00");

        Bill bill = new Bill(order, cfg, lookupFor(pizza));

        assertThat(bill.getTaxAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void calculatesServiceChargeAmount_asRateTimesSubtotal() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Configuration cfg = configWith("0.00", "0.10", "0.00");

        Bill bill = new Bill(order, cfg, lookupFor(pizza));

        assertThat(bill.getServiceChargeAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculatesCoverChargeAmount_fromConfig_normalCase() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Configuration cfg = configWith("0.00", "0.00", "500.00");

        Bill bill = new Bill(order, cfg, lookupFor(pizza));

        assertThat(bill.getCoverChargeAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void calculatesTotal_asSumOfFourComponents() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Configuration cfg = configWith("0.18", "0.10", "500.00");

        Bill bill = new Bill(order, cfg, lookupFor(pizza));

        // 1000 + 180 + 100 + 500
        assertThat(bill.getTotal()).isEqualByComparingTo("1780.00");
    }

    @Test
    void calculatesAmounts_withFractionalCents_roundedHalfUp() {
        MenuItem item = menuItem("Item", "9.99");
        Order order = orderWith(item);
        Configuration cfg = configWith("0.18", "0.10", "0.00");

        Bill bill = new Bill(order, cfg, lookupFor(item));

        // subtotal 9.99
        // tax 9.99 * 0.18 = 1.7982 -> 1.80
        // service 9.99 * 0.10 = 0.999 -> 1.00
        // cover 0
        // total 9.99 + 1.80 + 1.00 = 12.79
        assertThat(bill.getItemsSubtotal()).isEqualByComparingTo("9.99");
        assertThat(bill.getTaxAmount()).isEqualByComparingTo("1.80");
        assertThat(bill.getServiceChargeAmount()).isEqualByComparingTo("1.00");
        assertThat(bill.getCoverChargeAmount()).isEqualByComparingTo("0.00");
        assertThat(bill.getTotal()).isEqualByComparingTo("12.79");
    }

    @Test
    void calculatesAmounts_withZeroRates() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Configuration cfg = configWith("0.00", "0.00", "0.00");

        Bill bill = new Bill(order, cfg, lookupFor(pizza));

        assertThat(bill.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(bill.getServiceChargeAmount()).isEqualByComparingTo("0.00");
        assertThat(bill.getCoverChargeAmount()).isEqualByComparingTo("0.00");
        assertThat(bill.getTotal()).isEqualByComparingTo("1000.00");
    }

    // -------- snapshot semantics --------

    @Test
    void billAmounts_doNotChange_whenConfigurationMutatedAfterConstruction() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Configuration cfg = sampleConfig();

        Bill bill = new Bill(order, cfg, lookupFor(pizza));
        BigDecimal originalTax = bill.getTaxAmount();
        BigDecimal originalCover = bill.getCoverChargeAmount();
        BigDecimal originalTotal = bill.getTotal();

        cfg.updateTaxRate(new BigDecimal("0.50"));
        cfg.updateServiceChargeRate(new BigDecimal("0.50"));
        cfg.updateCoverChargeAmount(new BigDecimal("9999.00"));

        assertThat(bill.getTaxAmount()).isEqualByComparingTo(originalTax);
        assertThat(bill.getCoverChargeAmount()).isEqualByComparingTo(originalCover);
        assertThat(bill.getTotal()).isEqualByComparingTo(originalTotal);
    }

    @Test
    void billLineItems_preserveRecordedUnitPrice_evenIfMenuItemPriceChangesLater_FR4() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza));

        pizza.changePrice(new BigDecimal("9999.00"));

        assertThat(bill.getLineItems().get(0).recordedUnitPrice()).isEqualByComparingTo("1000.00");
        assertThat(bill.getItemsSubtotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void billLineItems_captureMenuItemNameFromLookup() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        Bill bill = new Bill(order, sampleConfig(), Map.of(pizza.getId(), "Margherita Pizza"));

        assertThat(bill.getLineItems().get(0).menuItemName()).isEqualTo("Margherita Pizza");
    }

    // -------- normal constructor: validation --------

    @Test
    void normalConstructor_rejectsNullOrder() {
        assertThatThrownBy(() -> new Bill(null, sampleConfig(), Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalConstructor_rejectsNullConfiguration() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        assertThatThrownBy(() -> new Bill(order, null, lookupFor(pizza)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalConstructor_rejectsNullNameLookup() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        assertThatThrownBy(() -> new Bill(order, sampleConfig(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void normalConstructor_rejectsOrderWithNoItems() {
        Order order = new Order(TableId.generate());

        assertThatThrownBy(() -> new Bill(order, sampleConfig(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalConstructor_rejectsLookupMissingMenuItemName() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Map<MenuItemId, String> emptyLookup = new HashMap<>();

        assertThatThrownBy(() -> new Bill(order, sampleConfig(), emptyLookup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------- split constructor --------

    @Test
    void splitConstructor_usesOnlyTheSubsetOfItems() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        MenuItem coffee = menuItem("Coffee", "500.00");
        Order order = orderWith(pizza, coffee);
        OrderItem pizzaLine = order.getItems().get(0);
        Configuration cfg = configWith("0.00", "0.00", "0.00");

        Bill split = new Bill(order, cfg, List.of(pizzaLine), 2, lookupFor(pizza, coffee));

        assertThat(split.getLineItems()).hasSize(1);
        assertThat(split.getLineItems().get(0).menuItemName()).isEqualTo("Pizza");
        assertThat(split.getItemsSubtotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void splitConstructor_dividesCoverChargeByDivisor() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        OrderItem pizzaLine = order.getItems().get(0);
        Configuration cfg = configWith("0.00", "0.00", "500.00");

        Bill split = new Bill(order, cfg, List.of(pizzaLine), 3, lookupFor(pizza));

        // 500 / 3 = 166.6666... -> 166.67
        assertThat(split.getCoverChargeAmount()).isEqualByComparingTo("166.67");
    }

    @Test
    void splitConstructor_divisorOne_matchesNormalCase() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        Configuration cfg = sampleConfig();

        Bill normal = new Bill(order, cfg, lookupFor(pizza));
        Bill splitWithDivisorOne = new Bill(order, cfg, order.getItems(), 1, lookupFor(pizza));

        assertThat(splitWithDivisorOne.getItemsSubtotal()).isEqualByComparingTo(normal.getItemsSubtotal());
        assertThat(splitWithDivisorOne.getTaxAmount()).isEqualByComparingTo(normal.getTaxAmount());
        assertThat(splitWithDivisorOne.getServiceChargeAmount())
                .isEqualByComparingTo(normal.getServiceChargeAmount());
        assertThat(splitWithDivisorOne.getCoverChargeAmount()).isEqualByComparingTo(normal.getCoverChargeAmount());
        assertThat(splitWithDivisorOne.getTotal()).isEqualByComparingTo(normal.getTotal());
    }

    @Test
    void splitConstructor_appliesSameSnapshotSemanticsAsNormal() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);
        OrderItem pizzaLine = order.getItems().get(0);
        Configuration cfg = sampleConfig();

        Bill split = new Bill(order, cfg, List.of(pizzaLine), 2, lookupFor(pizza));
        BigDecimal originalTotal = split.getTotal();

        cfg.updateTaxRate(new BigDecimal("0.99"));
        pizza.changePrice(new BigDecimal("9999.00"));

        assertThat(split.getTotal()).isEqualByComparingTo(originalTotal);
    }

    @Test
    void splitConstructor_rejectsDivisorZero() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        assertThatThrownBy(() -> new Bill(order, sampleConfig(), order.getItems(), 0, lookupFor(pizza)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void splitConstructor_rejectsDivisorNegative() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        assertThatThrownBy(() -> new Bill(order, sampleConfig(), order.getItems(), -1, lookupFor(pizza)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void splitConstructor_rejectsEmptyItemSubset() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Order order = orderWith(pizza);

        assertThatThrownBy(() -> new Bill(order, sampleConfig(), List.of(), 2, lookupFor(pizza)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void splitConstructor_rejectsItemsFromADifferentOrder() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        MenuItem coffee = menuItem("Coffee", "500.00");
        Order order1 = orderWith(pizza);
        Order order2 = orderWith(coffee);
        OrderItem coffeeFromOrder2 = order2.getItems().get(0);

        assertThatThrownBy(() ->
                        new Bill(order1, sampleConfig(), List.of(coffeeFromOrder2), 2, lookupFor(pizza, coffee)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------- markPaid --------

    @Test
    void markPaid_setsPaidTrue() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Bill bill = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));

        bill.markPaid();

        assertThat(bill.isPaid()).isTrue();
    }

    @Test
    void markPaid_throwsWhenCalledTwice() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Bill bill = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));
        bill.markPaid();

        assertThatThrownBy(bill::markPaid).isInstanceOf(IllegalStateException.class);
    }

    // -------- immutability of exposed collections --------

    @Test
    void getLineItems_returnsImmutableList() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Bill bill = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));

        assertThatThrownBy(() -> bill.getLineItems().add(new BillLineSnapshot("X", 1, new BigDecimal("1"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getOrderIds_returnsImmutableSet() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        Bill bill = new Bill(orderWith(pizza), sampleConfig(), lookupFor(pizza));

        assertThatThrownBy(() -> bill.getOrderIds().add(OrderId.generate()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -------- line item count and ordering --------

    @Test
    void lineItems_oneSnapshotPerOrderItem_preservingOrder() {
        MenuItem pizza = menuItem("Pizza", "1000.00");
        MenuItem coffee = menuItem("Coffee", "500.00");
        MenuItem tea = menuItem("Tea", "200.00");
        Order order = orderWith(pizza, coffee, tea);

        Bill bill = new Bill(order, sampleConfig(), lookupFor(pizza, coffee, tea));

        assertThat(bill.getLineItems())
                .extracting(BillLineSnapshot::menuItemName)
                .containsExactly("Pizza", "Coffee", "Tea");
    }
}
