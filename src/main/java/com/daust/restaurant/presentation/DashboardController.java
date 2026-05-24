package com.daust.restaurant.presentation;

import com.daust.restaurant.domain.ConfigurationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ConfigurationRepository configurationRepository;

    public DashboardController(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        boolean splitMergeEnabled = configurationRepository
                .load()
                .map(c -> c.isSplitMergePolicyEnabled())
                .orElse(false);
        model.addAttribute("splitMergeEnabled", splitMergeEnabled);
        return "dashboard";
    }
}
