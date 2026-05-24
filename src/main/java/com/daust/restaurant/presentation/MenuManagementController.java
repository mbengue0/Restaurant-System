package com.daust.restaurant.presentation;

import com.daust.restaurant.application.CategoryHasActiveItemsException;
import com.daust.restaurant.application.CategoryNotFoundException;
import com.daust.restaurant.application.MenuItemNotFoundException;
import com.daust.restaurant.application.MenuManagementService;
import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.MenuItemRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MenuManagementController {

    private final MenuManagementService menuService;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final CurrentUserHelper currentUser;

    public MenuManagementController(
            MenuManagementService menuService,
            CategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            CurrentUserHelper currentUser) {
        this.menuService = menuService;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.currentUser = currentUser;
    }

    // ----- Index -----

    @GetMapping("/admin/menu")
    public String index(Model model) {
        List<Category> categories = new ArrayList<>(menuService.listCategories());
        categories.sort(
                Comparator.comparing(Category::isActive).reversed()
                        .thenComparingInt(Category::getDisplayOrder)
                        .thenComparing(Category::getName));

        Map<CategoryId, List<MenuItem>> itemsByCategory = new LinkedHashMap<>();
        for (Category c : categories) {
            itemsByCategory.put(c.getId(), new ArrayList<>());
        }
        List<MenuItem> orphans = new ArrayList<>();
        for (MenuItem item : menuService.listMenuItems()) {
            List<MenuItem> bucket = itemsByCategory.get(item.getCategoryId());
            if (bucket == null) {
                orphans.add(item);
            } else {
                bucket.add(item);
            }
        }
        for (List<MenuItem> bucket : itemsByCategory.values()) {
            bucket.sort(
                    Comparator.comparing(MenuItem::isActive).reversed().thenComparing(MenuItem::getName));
        }

        model.addAttribute("categories", categories);
        model.addAttribute("itemsByCategory", itemsByCategory);
        model.addAttribute("orphans", orphans);
        return "admin/menu/index";
    }

    // ----- Categories -----

    @GetMapping("/admin/menu/categories/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("mode", "new");
        model.addAttribute("name", "");
        model.addAttribute("displayOrder", 0);
        return "admin/menu/category_form";
    }

    @PostMapping("/admin/menu/categories")
    public String createCategory(
            @RequestParam("name") String name,
            @RequestParam(value = "displayOrder", defaultValue = "0") int displayOrder,
            Authentication authentication,
            RedirectAttributes flash) {
        menuService.createCategory(name, displayOrder, currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Category '" + name + "' created.");
        return "redirect:/admin/menu";
    }

    @GetMapping("/admin/menu/categories/{id}/edit")
    public String editCategoryForm(@PathVariable UUID id, Model model) {
        Category category = categoryRepository
                .findById(CategoryId.of(id))
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
        model.addAttribute("mode", "edit");
        model.addAttribute("categoryId", category.getId().value());
        model.addAttribute("name", category.getName());
        model.addAttribute("displayOrder", category.getDisplayOrder());
        return "admin/menu/category_form";
    }

    @PostMapping("/admin/menu/categories/{id}")
    public String updateCategory(
            @PathVariable UUID id,
            @RequestParam("name") String name,
            @RequestParam(value = "displayOrder", defaultValue = "0") int displayOrder,
            Authentication authentication,
            RedirectAttributes flash) {
        menuService.updateCategory(
                CategoryId.of(id), name, displayOrder, currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Category updated.");
        return "redirect:/admin/menu";
    }

    @PostMapping("/admin/menu/categories/{id}/deactivate")
    public String deactivateCategory(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes flash) {
        try {
            menuService.deactivateCategory(
                    CategoryId.of(id), currentUser.currentUserId(authentication));
            flash.addFlashAttribute("message", "Category deactivated.");
        } catch (CategoryHasActiveItemsException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/menu";
    }

    // ----- Menu items -----

    @GetMapping("/admin/menu/items/new")
    public String newItemForm(
            @RequestParam(value = "categoryId", required = false) UUID categoryId, Model model) {
        model.addAttribute("mode", "new");
        model.addAttribute("name", "");
        model.addAttribute("description", "");
        model.addAttribute("unitPrice", "");
        model.addAttribute("preselectedCategoryId", categoryId);
        model.addAttribute("categories", categoryRepository.findAllActiveOrderedByDisplayOrder());
        return "admin/menu/item_form";
    }

    @PostMapping("/admin/menu/items")
    public String createItem(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("unitPrice") BigDecimal unitPrice,
            @RequestParam("categoryId") UUID categoryId,
            Authentication authentication,
            RedirectAttributes flash) {
        menuService.createMenuItem(
                name,
                description,
                unitPrice,
                CategoryId.of(categoryId),
                currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Menu item '" + name + "' created.");
        return "redirect:/admin/menu";
    }

    @GetMapping("/admin/menu/items/{id}/edit")
    public String editItemForm(@PathVariable UUID id, Model model) {
        MenuItem item = menuItemRepository
                .findById(MenuItemId.of(id))
                .orElseThrow(() -> new MenuItemNotFoundException("MenuItem not found: " + id));
        model.addAttribute("mode", "edit");
        model.addAttribute("itemId", item.getId().value());
        model.addAttribute("name", item.getName());
        model.addAttribute("description", item.getDescription() == null ? "" : item.getDescription());
        model.addAttribute("unitPrice", item.getUnitPrice());
        return "admin/menu/item_form";
    }

    @PostMapping("/admin/menu/items/{id}")
    public String updateItem(
            @PathVariable UUID id,
            @RequestParam("name") String name,
            @RequestParam("unitPrice") BigDecimal unitPrice,
            Authentication authentication,
            RedirectAttributes flash) {
        menuService.updateMenuItem(
                MenuItemId.of(id), name, unitPrice, currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Menu item updated.");
        return "redirect:/admin/menu";
    }

    @PostMapping("/admin/menu/items/{id}/deactivate")
    public String deactivateItem(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes flash) {
        menuService.deactivateMenuItem(MenuItemId.of(id), currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "Menu item deactivated.");
        return "redirect:/admin/menu";
    }
}
