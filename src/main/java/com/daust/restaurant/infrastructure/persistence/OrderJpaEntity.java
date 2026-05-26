package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.CancellationReason;
import com.daust.restaurant.domain.OrderState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "orders")
// Set-membership check encoded as a LIKE over a concatenated sentinel string.
// Equivalent to: state IN ('PLACED','IN_PREPARATION','READY','SERVED','COMPLETED','CANCELLED').
// Written this way because H2 2.4.x mis-parses CHECK ... IN (...) on string columns;
// LIKE/|| is portable to PostgreSQL and gives the same set-membership guarantee.
@Check(
        name = "ck_orders_state",
        constraints =
                "',PLACED,IN_PREPARATION,READY,SERVED,COMPLETED,CANCELLED,'"
                        + " like '%,' || state || ',%'")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class OrderJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "table_id", nullable = false)
    private UUID tableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private OrderState state;

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "served_at")
    private LocalDateTime servedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 30, columnDefinition = "varchar(30)")
    private CancellationReason cancellationReason;

    @Column(name = "cancellation_note", columnDefinition = "TEXT")
    private String cancellationNote;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "visible_to_kitchen", nullable = false)
    private boolean visibleToKitchen;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItemJpaEntity> items = new ArrayList<>();
}
