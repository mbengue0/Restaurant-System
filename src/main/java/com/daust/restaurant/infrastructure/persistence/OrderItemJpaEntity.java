package com.daust.restaurant.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "order_items")
@Check(name = "ck_order_items_quantity_positive", constraints = "quantity >= 1")
@Check(name = "ck_order_items_unit_price_nonneg", constraints = "recorded_unit_price >= 0")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class OrderItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "recorded_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal recordedUnitPrice;
}
