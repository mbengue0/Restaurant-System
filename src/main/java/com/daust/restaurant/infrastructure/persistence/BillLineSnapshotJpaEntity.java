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

@Entity
@Table(name = "bill_line_snapshots")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class BillLineSnapshotJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "menu_item_name", nullable = false, length = 100)
    private String menuItemName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "recorded_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal recordedUnitPrice;
}
