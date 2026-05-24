package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC04 / UC05 / UC06 / UC07 — Menu management.
 *
 * <p>Owns the FR13 cross-aggregate guard: Category.deactivate() is unconditional in Domain, so
 * this service rejects deactivation when {@link MenuItemRepository#findActiveByCategoryId} returns
 * a non-empty list (CLAUDE.md "Deferred cross-aggregate checks").
 *
 * <p>FR9/BR2 propagation (existing draft orders referencing a freshly-deactivated MenuItem) is a
 * deeper concern handled at order-time by {@link com.daust.restaurant.domain.Order#addItem} which
 * rejects inactive items.
 *
 * <p>{@code UserRepository} is injected (beyond the brief's literal list) so each audit entry can
 * snapshot the actor's role at write time — see {@link ConfigurationService} for the same pattern.
 */
@Service
public class MenuManagementService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public MenuManagementService(
            MenuItemRepository menuItemRepository,
            CategoryRepository categoryRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    // ----- Categories (UC07) -----

    @Transactional
    public CategoryId createCategory(String name, int displayOrder, UserId adminId) {
        User admin = loadActor(adminId);
        Category category = new Category(name, displayOrder);
        categoryRepository.save(category);
        audit(admin, "CATEGORY_CREATED", "Category", category.getId().value().toString(),
                null,
                "name=" + category.getName() + ", displayOrder=" + category.getDisplayOrder());
        return category.getId();
    }

    @Transactional
    public void updateCategory(CategoryId categoryId, String name, int displayOrder, UserId adminId) {
        Category category = loadCategory(categoryId);
        User admin = loadActor(adminId);

        String before = "name=" + category.getName() + ", displayOrder=" + category.getDisplayOrder();
        category.updateName(name);
        category.updateDisplayOrder(displayOrder);
        categoryRepository.save(category);
        String after = "name=" + category.getName() + ", displayOrder=" + category.getDisplayOrder();

        audit(admin, "CATEGORY_UPDATED", "Category", category.getId().value().toString(), before, after);
    }

    @Transactional
    public void deactivateCategory(CategoryId categoryId, UserId adminId) {
        Category category = loadCategory(categoryId);
        User admin = loadActor(adminId);

        // FR13 cross-aggregate guard.
        List<MenuItem> activeItems = menuItemRepository.findActiveByCategoryId(categoryId);
        if (!activeItems.isEmpty()) {
            throw new CategoryHasActiveItemsException(
                    "Cannot deactivate category '" + category.getName() + "': "
                            + activeItems.size() + " active menu item(s) remain (FR13)");
        }

        category.deactivate();
        categoryRepository.save(category);

        audit(admin, "CATEGORY_DEACTIVATED", "Category", category.getId().value().toString(),
                "active=true", "active=false");
    }

    // ----- Menu items (UC04, UC05, UC06) -----

    @Transactional
    public MenuItemId createMenuItem(
            String name,
            String description,
            BigDecimal unitPrice,
            CategoryId categoryId,
            UserId adminId) {
        // Verify the category exists; Domain doesn't carry that reference.
        loadCategory(categoryId);
        User admin = loadActor(adminId);

        MenuItem item = new MenuItem(name, description, unitPrice, categoryId);
        menuItemRepository.save(item);

        audit(admin, "MENU_ITEM_CREATED", "MenuItem", item.getId().value().toString(),
                null,
                "name=" + item.getName() + ", price=" + item.getUnitPrice()
                        + ", categoryId=" + item.getCategoryId().value());
        return item.getId();
    }

    @Transactional
    public void changeMenuItemPrice(MenuItemId menuItemId, BigDecimal newPrice, UserId adminId) {
        MenuItem item = loadMenuItem(menuItemId);
        User admin = loadActor(adminId);

        BigDecimal before = item.getUnitPrice();
        item.changePrice(newPrice);
        menuItemRepository.save(item);

        audit(admin, "MENU_ITEM_PRICE_CHANGED", "MenuItem", item.getId().value().toString(),
                "price=" + before, "price=" + item.getUnitPrice());
    }

    @Transactional
    public void updateMenuItem(
            MenuItemId menuItemId, String name, BigDecimal newPrice, UserId adminId) {
        MenuItem item = loadMenuItem(menuItemId);
        User admin = loadActor(adminId);

        String before = "name=" + item.getName() + ", price=" + item.getUnitPrice();
        item.updateName(name);
        item.changePrice(newPrice);
        menuItemRepository.save(item);
        String after = "name=" + item.getName() + ", price=" + item.getUnitPrice();

        audit(admin, "MENU_ITEM_UPDATED", "MenuItem", item.getId().value().toString(), before, after);
    }

    @Transactional
    public void deactivateMenuItem(MenuItemId menuItemId, UserId adminId) {
        MenuItem item = loadMenuItem(menuItemId);
        User admin = loadActor(adminId);

        item.deactivate();
        menuItemRepository.save(item);

        audit(admin, "MENU_ITEM_DEACTIVATED", "MenuItem", item.getId().value().toString(),
                "active=true", "active=false");
    }

    // ----- Read-side -----

    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listMenuItems() {
        return menuItemRepository.findAll();
    }

    // ----- Helpers -----

    private Category loadCategory(CategoryId id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
    }

    private MenuItem loadMenuItem(MenuItemId id) {
        return menuItemRepository
                .findById(id)
                .orElseThrow(() -> new MenuItemNotFoundException("MenuItem not found: " + id));
    }

    private User loadActor(UserId userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private void audit(
            User actor,
            String eventType,
            String entityType,
            String entityId,
            String before,
            String after) {
        auditLogRepository.save(new AuditLogEntry(
                actor.getId(), actor.getRole(), eventType, entityType, entityId, before, after));
    }
}
