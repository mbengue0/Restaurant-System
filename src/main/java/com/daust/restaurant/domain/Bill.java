package com.daust.restaurant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Bill {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final DateTimeFormatter BILL_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom BILL_NUMBER_RNG = new SecureRandom();

    private final BillId id;
    private final String billNumber;
    private final Set<OrderId> orderIds;
    private final LocalDateTime generatedAt;
    private final List<BillLineSnapshot> lineItems;
    private final BigDecimal itemsSubtotal;
    private final BigDecimal taxAmount;
    private final BigDecimal serviceChargeAmount;
    private final BigDecimal coverChargeAmount;
    private final BigDecimal total;
    private boolean paid;

    public Bill(Order order, Configuration configuration, Map<MenuItemId, String> nameLookup) {
        this(order, configuration, itemsOf(order), 1, nameLookup);
    }

    public Bill(
            Order order,
            Configuration configuration,
            List<OrderItem> items,
            int splitDivisor,
            Map<MenuItemId, String> nameLookup) {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(nameLookup, "nameLookup must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Bill requires at least one item");
        }
        if (splitDivisor < 1) {
            throw new IllegalArgumentException("splitDivisor must be >= 1");
        }
        Set<OrderItemId> orderLineIds = new HashSet<>();
        for (OrderItem orderItem : order.getItems()) {
            orderLineIds.add(orderItem.getId());
        }
        List<BillLineSnapshot> snapshots = new ArrayList<>(items.size());
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            if (!orderLineIds.contains(item.getId())) {
                throw new IllegalArgumentException(
                        "OrderItem " + item.getId() + " does not belong to Order " + order.getId());
            }
            String name = nameLookup.get(item.getMenuItemId());
            if (name == null) {
                throw new IllegalArgumentException(
                        "nameLookup missing entry for menu item " + item.getMenuItemId());
            }
            BillLineSnapshot snap = new BillLineSnapshot(name, item.getQuantity(), item.getRecordedUnitPrice());
            snapshots.add(snap);
            subtotal = subtotal.add(snap.recordedUnitPrice().multiply(BigDecimal.valueOf(snap.quantity())));
        }

        BigDecimal subtotalRounded = roundMoney(subtotal);
        BigDecimal tax = roundMoney(configuration.getTaxRate().multiply(subtotalRounded));
        BigDecimal service = roundMoney(configuration.getServiceChargeRate().multiply(subtotalRounded));
        BigDecimal cover = configuration
                .getCoverChargeAmount()
                .divide(BigDecimal.valueOf(splitDivisor), MONEY_SCALE, MONEY_ROUNDING);
        BigDecimal computedTotal = subtotalRounded.add(tax).add(service).add(cover);

        this.id = BillId.generate();
        this.billNumber = generateBillNumber();
        this.orderIds = Set.of(order.getId());
        this.generatedAt = LocalDateTime.now();
        this.lineItems = List.copyOf(snapshots);
        this.itemsSubtotal = subtotalRounded;
        this.taxAmount = tax;
        this.serviceChargeAmount = service;
        this.coverChargeAmount = cover;
        this.total = computedTotal;
        this.paid = false;
    }

    public static Bill reconstitute(
            BillId id,
            String billNumber,
            Set<OrderId> orderIds,
            LocalDateTime generatedAt,
            List<BillLineSnapshot> lineItems,
            BigDecimal itemsSubtotal,
            BigDecimal taxAmount,
            BigDecimal serviceChargeAmount,
            BigDecimal coverChargeAmount,
            BigDecimal total,
            boolean paid) {
        return new Bill(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(billNumber, "billNumber"),
                Objects.requireNonNull(orderIds, "orderIds"),
                Objects.requireNonNull(generatedAt, "generatedAt"),
                Objects.requireNonNull(lineItems, "lineItems"),
                Objects.requireNonNull(itemsSubtotal, "itemsSubtotal"),
                Objects.requireNonNull(taxAmount, "taxAmount"),
                Objects.requireNonNull(serviceChargeAmount, "serviceChargeAmount"),
                Objects.requireNonNull(coverChargeAmount, "coverChargeAmount"),
                Objects.requireNonNull(total, "total"),
                paid);
    }

    private Bill(
            BillId id,
            String billNumber,
            Set<OrderId> orderIds,
            LocalDateTime generatedAt,
            List<BillLineSnapshot> lineItems,
            BigDecimal itemsSubtotal,
            BigDecimal taxAmount,
            BigDecimal serviceChargeAmount,
            BigDecimal coverChargeAmount,
            BigDecimal total,
            boolean paid) {
        this.id = id;
        this.billNumber = billNumber;
        this.orderIds = Set.copyOf(orderIds);
        this.generatedAt = generatedAt;
        this.lineItems = List.copyOf(lineItems);
        this.itemsSubtotal = itemsSubtotal;
        this.taxAmount = taxAmount;
        this.serviceChargeAmount = serviceChargeAmount;
        this.coverChargeAmount = coverChargeAmount;
        this.total = total;
        this.paid = paid;
    }

    public void markPaid() {
        if (this.paid) {
            throw new IllegalStateException("Bill " + billNumber + " is already paid");
        }
        this.paid = true;
    }

    public BillId getId() {
        return id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public Set<OrderId> getOrderIds() {
        return orderIds;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public List<BillLineSnapshot> getLineItems() {
        return lineItems;
    }

    public BigDecimal getItemsSubtotal() {
        return itemsSubtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getServiceChargeAmount() {
        return serviceChargeAmount;
    }

    public BigDecimal getCoverChargeAmount() {
        return coverChargeAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public boolean isPaid() {
        return paid;
    }

    private static List<OrderItem> itemsOf(Order order) {
        return Objects.requireNonNull(order, "order must not be null").getItems();
    }

    private static BigDecimal roundMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static String generateBillNumber() {
        String date = LocalDate.now().format(BILL_NUMBER_DATE);
        String suffix = String.format("%08X", BILL_NUMBER_RNG.nextInt());
        return "BILL-" + date + "-" + suffix;
    }
}
