package com.daust.restaurant.application.bootstrap;

import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.PasswordHasher;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedDataService implements ApplicationRunner {

    private final ConfigurationRepository configurationRepository;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final PasswordHasher passwordHasher;

    public SeedDataService(
            ConfigurationRepository configurationRepository,
            UserRepository userRepository,
            TableRepository tableRepository,
            CategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            PasswordHasher passwordHasher) {
        this.configurationRepository = configurationRepository;
        this.userRepository = userRepository;
        this.tableRepository = tableRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedConfiguration();
        seedUsers();
        seedTables();
        seedMenu();
    }

    private void seedConfiguration() {
        if (configurationRepository.load().isPresent()) {
            return;
        }
        Configuration config = new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                BigDecimal.ZERO,
                false,
                EnumSet.of(PaymentMethod.CASH, PaymentMethod.CARD));
        configurationRepository.save(config);
    }

    private void seedUsers() {
        if (!userRepository.findAll().isEmpty()) {
            return;
        }
        userRepository.save(new User(
                "admin", passwordHasher.hash("admin123"), "Default Admin", Role.ADMIN, false));
        userRepository.save(new User(
                "manager", passwordHasher.hash("manager123"), "Default Manager", Role.MANAGER, false));
        userRepository.save(new User(
                "waiter", passwordHasher.hash("waiter123"), "Default Waiter", Role.WAITER, false));
        userRepository.save(new User(
                "kitchen", passwordHasher.hash("kitchen123"), "Default Kitchen Staff",
                Role.KITCHEN_STAFF, false));
    }

    private void seedTables() {
        if (!tableRepository.findAll().isEmpty()) {
            return;
        }
        int[] capacities = {2, 2, 4, 4, 6};
        for (int capacity : capacities) {
            tableRepository.save(new Table(capacity));
        }
    }

    private void seedMenu() {
        if (!categoryRepository.findAll().isEmpty()) {
            return;
        }
        Category starters = new Category("Starters", 1);
        Category mains = new Category("Mains", 2);
        categoryRepository.save(starters);
        categoryRepository.save(mains);

        menuItemRepository.save(new MenuItem(
                "Salade Niçoise", null, new BigDecimal("4500"), starters.getId()));
        menuItemRepository.save(new MenuItem(
                "Pastels", null, new BigDecimal("3000"), starters.getId()));
        menuItemRepository.save(new MenuItem(
                "Soupe du Jour", null, new BigDecimal("3500"), starters.getId()));

        menuItemRepository.save(new MenuItem(
                "Thieboudienne", null, new BigDecimal("8500"), mains.getId()));
        menuItemRepository.save(new MenuItem(
                "Yassa Poulet", null, new BigDecimal("7500"), mains.getId()));
        menuItemRepository.save(new MenuItem(
                "Grillé Poisson", null, new BigDecimal("9500"), mains.getId()));
    }
}
