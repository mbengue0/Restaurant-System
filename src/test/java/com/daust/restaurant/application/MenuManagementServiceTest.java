package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuManagementServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private MenuItemRepository menuItemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MenuManagementService menuService;

    @Test
    void createCategory_persistsAndAudits() {
        User admin = newAdmin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        CategoryId id = menuService.createCategory("Starters", 1, admin.getId());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        Category saved = categoryCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Starters");
        assertThat(saved.getDisplayOrder()).isEqualTo(1);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getId()).isEqualTo(id);

        ArgumentCaptor<AuditLogEntry> entryCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(entryCaptor.capture());
        AuditLogEntry entry = entryCaptor.getValue();
        assertThat(entry.getEventType()).isEqualTo("CATEGORY_CREATED");
        assertThat(entry.getAfterValue()).contains("name=Starters", "displayOrder=1");
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.ADMIN);
    }

    @Test
    void createMenuItem_persistsAndAudits() {
        User admin = newAdmin();
        Category category = new Category("Mains", 2);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        MenuItemId id = menuService.createMenuItem(
                "Thieboudienne",
                "National dish",
                new BigDecimal("5000"),
                category.getId(),
                admin.getId());

        ArgumentCaptor<MenuItem> itemCaptor = ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemRepository).save(itemCaptor.capture());
        MenuItem saved = itemCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getName()).isEqualTo("Thieboudienne");
        assertThat(saved.getUnitPrice()).isEqualByComparingTo("5000");
        assertThat(saved.getCategoryId()).isEqualTo(category.getId());
        assertThat(saved.isActive()).isTrue();

        ArgumentCaptor<AuditLogEntry> entryCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(entryCaptor.capture());
        AuditLogEntry entry = entryCaptor.getValue();
        assertThat(entry.getEventType()).isEqualTo("MENU_ITEM_CREATED");
        assertThat(entry.getAfterValue()).contains("name=Thieboudienne", "price=5000");
    }

    @Test
    void createMenuItem_throwsWhenCategoryMissing() {
        User admin = newAdmin();
        CategoryId missing = CategoryId.generate();
        when(categoryRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.createMenuItem(
                        "X", null, new BigDecimal("1"), missing, admin.getId()))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(menuItemRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void changeMenuItemPrice_persistsNewPriceAndAudits() {
        User admin = newAdmin();
        MenuItem item = new MenuItem("Yassa", null, new BigDecimal("3000"), CategoryId.generate());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        menuService.changeMenuItemPrice(item.getId(), new BigDecimal("3500"), admin.getId());

        assertThat(item.getUnitPrice()).isEqualByComparingTo("3500");
        verify(menuItemRepository).save(item);

        ArgumentCaptor<AuditLogEntry> entryCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(entryCaptor.capture());
        AuditLogEntry entry = entryCaptor.getValue();
        assertThat(entry.getEventType()).isEqualTo("MENU_ITEM_PRICE_CHANGED");
        assertThat(entry.getBeforeValue()).contains("price=3000");
        assertThat(entry.getAfterValue()).contains("price=3500");
    }

    @Test
    void changeMenuItemPrice_rejectsNonPositive() {
        User admin = newAdmin();
        MenuItem item = new MenuItem("X", null, new BigDecimal("100"), CategoryId.generate());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> menuService.changeMenuItemPrice(
                        item.getId(), BigDecimal.ZERO, admin.getId()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(menuItemRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void deactivateMenuItem_flipsActiveFalseAndAudits() {
        User admin = newAdmin();
        MenuItem item = new MenuItem("X", null, new BigDecimal("100"), CategoryId.generate());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        menuService.deactivateMenuItem(item.getId(), admin.getId());

        assertThat(item.isActive()).isFalse();
        verify(menuItemRepository).save(item);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("MENU_ITEM_DEACTIVATED");
    }

    @Test
    void deactivateCategory_throwsCategoryHasActiveItems_whenItemsRemain() {
        User admin = newAdmin();
        Category category = new Category("Drinks", 0);
        MenuItem activeItem = new MenuItem("Bissap", null, new BigDecimal("500"), category.getId());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(menuItemRepository.findActiveByCategoryId(category.getId()))
                .thenReturn(List.of(activeItem));

        assertThatThrownBy(() -> menuService.deactivateCategory(category.getId(), admin.getId()))
                .isInstanceOf(CategoryHasActiveItemsException.class)
                .hasMessageContaining("FR13");

        assertThat(category.isActive()).isTrue();
        verify(categoryRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void deactivateCategory_succeedsWhenNoActiveItems() {
        User admin = newAdmin();
        Category category = new Category("Empty", 0);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(menuItemRepository.findActiveByCategoryId(category.getId())).thenReturn(List.of());

        menuService.deactivateCategory(category.getId(), admin.getId());

        assertThat(category.isActive()).isFalse();
        verify(categoryRepository).save(category);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CATEGORY_DEACTIVATED");
    }

    private static User newAdmin() {
        return new User("root", HASH, "Root Admin", Role.ADMIN, false);
    }
}
