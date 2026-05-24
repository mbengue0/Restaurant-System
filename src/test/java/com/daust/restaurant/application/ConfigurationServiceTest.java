package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private ConfigurationRepository configurationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ConfigurationService configurationService;

    @Test
    void getConfiguration_returnsTheSingleton() {
        Configuration config = newConfig();
        when(configurationRepository.load()).thenReturn(Optional.of(config));

        Configuration loaded = configurationService.getConfiguration();

        assertThat(loaded).isSameAs(config);
    }

    @Test
    void getConfiguration_throwsIfSingletonMissing() {
        when(configurationRepository.load()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configurationService.getConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");
    }

    @Test
    void updateRates_savesNewValuesAndWritesAudit() {
        Configuration config = newConfig();
        User admin = newAdmin();
        when(configurationRepository.load()).thenReturn(Optional.of(config));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        configurationService.updateRates(
                new BigDecimal("0.20"),
                new BigDecimal("0.05"),
                new BigDecimal("750"),
                admin.getId());

        assertThat(config.getTaxRate()).isEqualByComparingTo("0.20");
        assertThat(config.getServiceChargeRate()).isEqualByComparingTo("0.05");
        assertThat(config.getCoverChargeAmount()).isEqualByComparingTo("750");
        verify(configurationRepository).save(config);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("CONFIG_UPDATED");
        assertThat(entry.getAffectedEntityType()).isEqualTo("Configuration");
        assertThat(entry.getAffectedEntityId()).isEqualTo("1");
        assertThat(entry.getUserId()).isEqualTo(admin.getId());
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.ADMIN);
        assertThat(entry.getBeforeValue()).contains("tax=0.18", "service=0.10", "cover=500");
        assertThat(entry.getAfterValue()).contains("tax=0.20", "service=0.05", "cover=750");
    }

    @Test
    void updateRates_rejectsTaxRateAboveOne_andDoesNotSave() {
        Configuration config = newConfig();
        User admin = newAdmin();
        when(configurationRepository.load()).thenReturn(Optional.of(config));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> configurationService.updateRates(
                        new BigDecimal("1.5"),
                        new BigDecimal("0.10"),
                        new BigDecimal("500"),
                        admin.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taxRate");

        verify(configurationRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void updateRates_rejectsNegativeCoverCharge_andDoesNotSave() {
        Configuration config = newConfig();
        User admin = newAdmin();
        when(configurationRepository.load()).thenReturn(Optional.of(config));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> configurationService.updateRates(
                        new BigDecimal("0.18"),
                        new BigDecimal("0.10"),
                        new BigDecimal("-1"),
                        admin.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coverChargeAmount");

        verify(configurationRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void updatePaymentMethods_persistsAndAudits() {
        Configuration config = newConfig();
        User admin = newAdmin();
        when(configurationRepository.load()).thenReturn(Optional.of(config));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        configurationService.updatePaymentMethods(
                EnumSet.of(PaymentMethod.CASH, PaymentMethod.MOBILE_MONEY), admin.getId());

        assertThat(config.getAcceptedPaymentMethods())
                .containsExactlyInAnyOrder(PaymentMethod.CASH, PaymentMethod.MOBILE_MONEY);
        verify(configurationRepository).save(config);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAfterValue()).contains("CASH", "MOBILE_MONEY");
    }

    @Test
    void updatePaymentMethods_rejectsEmptySet() {
        Configuration config = newConfig();
        User admin = newAdmin();
        when(configurationRepository.load()).thenReturn(Optional.of(config));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> configurationService.updatePaymentMethods(
                        EnumSet.noneOf(PaymentMethod.class), admin.getId()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(configurationRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void setSplitMergePolicy_togglesAndAudits() {
        Configuration config = newConfig(); // starts disabled
        User admin = newAdmin();
        when(configurationRepository.load()).thenReturn(Optional.of(config));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        configurationService.setSplitMergePolicy(true, admin.getId());

        assertThat(config.isSplitMergePolicyEnabled()).isTrue();
        verify(configurationRepository).save(config);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getBeforeValue()).contains("splitMergePolicyEnabled=false");
        assertThat(entry.getAfterValue()).contains("splitMergePolicyEnabled=true");
    }

    private static Configuration newConfig() {
        Set<PaymentMethod> methods = EnumSet.of(PaymentMethod.CASH, PaymentMethod.CARD);
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500"),
                false,
                methods);
    }

    private static User newAdmin() {
        return new User("root", HASH, "Root Admin", Role.ADMIN, false);
    }
}
