package com.daust.restaurant.presentation;

import com.daust.restaurant.application.ConfigurationService;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConfigurationController {

    private final ConfigurationService configurationService;
    private final CurrentUserHelper currentUser;

    public ConfigurationController(
            ConfigurationService configurationService, CurrentUserHelper currentUser) {
        this.configurationService = configurationService;
        this.currentUser = currentUser;
    }

    @GetMapping("/admin/configuration")
    public String view(Model model) {
        Configuration config = configurationService.getConfiguration();
        model.addAttribute("config", config);
        model.addAttribute("allPaymentMethods", PaymentMethod.values());
        return "admin/configuration";
    }

    @PostMapping("/admin/configuration")
    public String updateRates(
            @RequestParam("taxRate") BigDecimal taxRate,
            @RequestParam("serviceChargeRate") BigDecimal serviceChargeRate,
            @RequestParam("coverChargeAmount") BigDecimal coverChargeAmount,
            Authentication authentication,
            RedirectAttributes flash) {
        configurationService.updateRates(
                taxRate,
                serviceChargeRate,
                coverChargeAmount,
                currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Rates updated.");
        return "redirect:/admin/configuration";
    }

    @PostMapping("/admin/configuration/payment-methods")
    public String updatePaymentMethods(
            @RequestParam(value = "methods", required = false) List<PaymentMethod> methods,
            Authentication authentication,
            RedirectAttributes flash) {
        Set<PaymentMethod> accepted =
                methods == null || methods.isEmpty()
                        ? EnumSet.noneOf(PaymentMethod.class)
                        : EnumSet.copyOf(methods);
        configurationService.updatePaymentMethods(accepted, currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Accepted payment methods updated.");
        return "redirect:/admin/configuration";
    }

    @PostMapping("/admin/configuration/policy")
    public String setSplitMergePolicy(
            @RequestParam(value = "splitMergeEnabled", defaultValue = "false") boolean enabled,
            Authentication authentication,
            RedirectAttributes flash) {
        configurationService.setSplitMergePolicy(enabled, currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Split/merge policy updated.");
        return "redirect:/admin/configuration";
    }
}
