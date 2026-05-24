package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.daust.restaurant.application.DailySummary.MethodBreakdown;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillLineSnapshot;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentId;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.PaymentRepository;
import com.daust.restaurant.domain.UserId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesSummaryServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 5, 24);

    @Mock private BillRepository billRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks private SalesSummaryService service;

    @Test
    void summaryForDay_emptyDay_returnsZeros() {
        when(paymentRepository.findByRecordedAtBetween(any(), any())).thenReturn(List.of());

        DailySummary summary = service.summaryForDay(DAY);

        assertThat(summary.date()).isEqualTo(DAY);
        assertThat(summary.billCount()).isZero();
        assertThat(summary.totalRevenue()).isEqualByComparingTo("0.00");
        assertThat(summary.totalTax()).isEqualByComparingTo("0.00");
        assertThat(summary.totalServiceCharge()).isEqualByComparingTo("0.00");
        for (PaymentMethod m : PaymentMethod.values()) {
            MethodBreakdown b = summary.byMethod().get(m);
            assertThat(b).isNotNull();
            assertThat(b.count()).isZero();
            assertThat(b.total()).isEqualByComparingTo("0.00");
        }
    }

    @Test
    void summaryForDay_passesFullDayWindowToRepository() {
        when(paymentRepository.findByRecordedAtBetween(any(), any())).thenReturn(List.of());

        service.summaryForDay(DAY);

        ArgumentCaptor<LocalDateTime> fromCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCap = ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.Mockito.verify(paymentRepository)
                .findByRecordedAtBetween(fromCap.capture(), toCap.capture());
        assertThat(fromCap.getValue()).isEqualTo(DAY.atStartOfDay());
        assertThat(toCap.getValue()).isEqualTo(DAY.atTime(LocalTime.MAX));
    }

    @Test
    void summaryForDay_aggregatesRevenueBillCountTaxAndService() {
        Bill b1 = bill("1000.00", "180.00", "100.00");
        Bill b2 = bill("2000.00", "360.00", "200.00");
        Bill b3 = bill("500.00", "90.00", "50.00");

        Payment p1 = payment(b1.getId(), "1280.00", PaymentMethod.CASH);
        Payment p2 = payment(b2.getId(), "2560.00", PaymentMethod.CARD);
        Payment p3 = payment(b3.getId(), "640.00", PaymentMethod.CASH);

        when(paymentRepository.findByRecordedAtBetween(any(), any())).thenReturn(List.of(p1, p2, p3));
        when(billRepository.findById(b1.getId())).thenReturn(Optional.of(b1));
        when(billRepository.findById(b2.getId())).thenReturn(Optional.of(b2));
        when(billRepository.findById(b3.getId())).thenReturn(Optional.of(b3));

        DailySummary summary = service.summaryForDay(DAY);

        assertThat(summary.billCount()).isEqualTo(3);
        // amountDue values: 1280 + 2560 + 640 = 4480
        assertThat(summary.totalRevenue()).isEqualByComparingTo("4480.00");
        // tax: 180 + 360 + 90 = 630
        assertThat(summary.totalTax()).isEqualByComparingTo("630.00");
        // service: 100 + 200 + 50 = 350
        assertThat(summary.totalServiceCharge()).isEqualByComparingTo("350.00");
    }

    @Test
    void summaryForDay_breaksDownPerPaymentMethod() {
        Bill b1 = bill("1000.00", "0.00", "0.00");
        Bill b2 = bill("2000.00", "0.00", "0.00");
        Bill b3 = bill("500.00", "0.00", "0.00");
        Bill b4 = bill("750.00", "0.00", "0.00");

        Payment cash1 = payment(b1.getId(), "1000.00", PaymentMethod.CASH);
        Payment cash2 = payment(b2.getId(), "2000.00", PaymentMethod.CASH);
        Payment card = payment(b3.getId(), "500.00", PaymentMethod.CARD);
        Payment mm = payment(b4.getId(), "750.00", PaymentMethod.MOBILE_MONEY);

        when(paymentRepository.findByRecordedAtBetween(any(), any()))
                .thenReturn(List.of(cash1, cash2, card, mm));
        when(billRepository.findById(b1.getId())).thenReturn(Optional.of(b1));
        when(billRepository.findById(b2.getId())).thenReturn(Optional.of(b2));
        when(billRepository.findById(b3.getId())).thenReturn(Optional.of(b3));
        when(billRepository.findById(b4.getId())).thenReturn(Optional.of(b4));

        DailySummary summary = service.summaryForDay(DAY);

        assertThat(summary.byMethod().get(PaymentMethod.CASH).count()).isEqualTo(2);
        assertThat(summary.byMethod().get(PaymentMethod.CASH).total())
                .isEqualByComparingTo("3000.00");
        assertThat(summary.byMethod().get(PaymentMethod.CARD).count()).isEqualTo(1);
        assertThat(summary.byMethod().get(PaymentMethod.CARD).total())
                .isEqualByComparingTo("500.00");
        assertThat(summary.byMethod().get(PaymentMethod.MOBILE_MONEY).count()).isEqualTo(1);
        assertThat(summary.byMethod().get(PaymentMethod.MOBILE_MONEY).total())
                .isEqualByComparingTo("750.00");
    }

    @Test
    void summaryForDay_throwsIfBillBehindPaymentIsMissing() {
        Bill b1 = bill("1000.00", "0.00", "0.00");
        Payment p1 = payment(b1.getId(), "1000.00", PaymentMethod.CASH);
        when(paymentRepository.findByRecordedAtBetween(any(), any())).thenReturn(List.of(p1));
        when(billRepository.findById(b1.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summaryForDay(DAY))
                .isInstanceOf(BillNotFoundException.class);
    }

    private static Bill bill(String subtotal, String tax, String service) {
        BigDecimal sub = new BigDecimal(subtotal);
        BigDecimal taxAmount = new BigDecimal(tax);
        BigDecimal svc = new BigDecimal(service);
        BigDecimal total = sub.add(taxAmount).add(svc);
        return Bill.reconstitute(
                BillId.generate(),
                "BILL-TEST-" + Integer.toHexString(BillId.generate().hashCode()),
                Set.of(OrderId.generate()),
                LocalDateTime.now(),
                List.of(new BillLineSnapshot("Item", 1, sub)),
                sub,
                taxAmount,
                svc,
                new BigDecimal("0.00"),
                total,
                true);
    }

    private static Payment payment(BillId billId, String amount, PaymentMethod method) {
        BigDecimal amt = new BigDecimal(amount);
        return Payment.reconstitute(
                PaymentId.generate(),
                billId,
                amt,
                amt,
                method,
                null,
                LocalDateTime.now(),
                UserId.of(java.util.UUID.randomUUID()),
                new BigDecimal("0.00"));
    }
}
