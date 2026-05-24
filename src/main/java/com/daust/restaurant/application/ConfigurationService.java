package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC18 — Configure System Settings.
 *
 * <p>Wraps the {@link Configuration} singleton with audited mutators per NFR-MNT-3 (settings
 * modifiable at runtime by Admin without redeployment) and BR6 (rates and policies live in
 * Configuration, never hard-coded). Domain enforces all value invariants (rate ∈ [0, 1],
 * non-negative cover, non-empty payment-method set); the service is purely orchestration +
 * auditing.
 *
 * <p>Note: {@code UserRepository} is injected (beyond the brief's literal list) so the audit
 * entry can snapshot the actor's {@code Role} at the time of the change — same pattern used by
 * every other audited service (see {@link GenerateBillService}, {@link CancelOrderService}).
 */
@Service
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public ConfigurationService(
            ConfigurationRepository configurationRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.configurationRepository = configurationRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Configuration getConfiguration() {
        return loadConfiguration();
    }

    @Transactional
    public void updateRates(
            BigDecimal taxRate,
            BigDecimal serviceChargeRate,
            BigDecimal coverChargeAmount,
            UserId adminId) {
        Configuration config = loadConfiguration();
        User admin = loadAdmin(adminId);

        String before = String.format(
                "tax=%s, service=%s, cover=%s",
                config.getTaxRate(), config.getServiceChargeRate(), config.getCoverChargeAmount());

        config.updateTaxRate(taxRate);
        config.updateServiceChargeRate(serviceChargeRate);
        config.updateCoverChargeAmount(coverChargeAmount);
        configurationRepository.save(config);

        String after = String.format(
                "tax=%s, service=%s, cover=%s",
                config.getTaxRate(), config.getServiceChargeRate(), config.getCoverChargeAmount());

        auditLogRepository.save(new AuditLogEntry(
                admin.getId(),
                admin.getRole(),
                "CONFIG_UPDATED",
                "Configuration",
                "1",
                before,
                after));
    }

    @Transactional
    public void updatePaymentMethods(Set<PaymentMethod> accepted, UserId adminId) {
        Configuration config = loadConfiguration();
        User admin = loadAdmin(adminId);

        String before = "acceptedPaymentMethods=" + sortedString(config.getAcceptedPaymentMethods());
        config.setAcceptedPaymentMethods(accepted);
        configurationRepository.save(config);
        String after = "acceptedPaymentMethods=" + sortedString(config.getAcceptedPaymentMethods());

        auditLogRepository.save(new AuditLogEntry(
                admin.getId(),
                admin.getRole(),
                "CONFIG_UPDATED",
                "Configuration",
                "1",
                before,
                after));
    }

    @Transactional
    public void setSplitMergePolicy(boolean enabled, UserId adminId) {
        Configuration config = loadConfiguration();
        User admin = loadAdmin(adminId);

        String before = "splitMergePolicyEnabled=" + config.isSplitMergePolicyEnabled();
        if (enabled) {
            config.enableSplitMergePolicy();
        } else {
            config.disableSplitMergePolicy();
        }
        configurationRepository.save(config);
        String after = "splitMergePolicyEnabled=" + config.isSplitMergePolicyEnabled();

        auditLogRepository.save(new AuditLogEntry(
                admin.getId(),
                admin.getRole(),
                "CONFIG_UPDATED",
                "Configuration",
                "1",
                before,
                after));
    }

    private Configuration loadConfiguration() {
        return configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration singleton not initialized"));
    }

    private User loadAdmin(UserId adminId) {
        return userRepository
                .findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + adminId));
    }

    private static String sortedString(Set<PaymentMethod> methods) {
        var sorted = new TreeSet<String>();
        for (PaymentMethod m : methods) {
            sorted.add(m.name());
        }
        return sorted.toString();
    }
}
