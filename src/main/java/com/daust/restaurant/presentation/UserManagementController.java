package com.daust.restaurant.presentation;

import com.daust.restaurant.application.CreatedUserResult;
import com.daust.restaurant.application.LastActiveAdminException;
import com.daust.restaurant.application.UserAccountService;
import com.daust.restaurant.application.UserNotFoundException;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
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
public class UserManagementController {

    private final UserAccountService userAccountService;
    private final UserRepository userRepository;
    private final CurrentUserHelper currentUser;

    public UserManagementController(
            UserAccountService userAccountService,
            UserRepository userRepository,
            CurrentUserHelper currentUser) {
        this.userAccountService = userAccountService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/admin/users")
    public String list(Model model) {
        model.addAttribute("users", userAccountService.listUsers());
        return "admin/users/list";
    }

    @GetMapping("/admin/users/new")
    public String newForm(Model model) {
        model.addAttribute("username", "");
        model.addAttribute("fullName", "");
        model.addAttribute("selectedRole", "WAITER");
        model.addAttribute("roles", Role.values());
        return "admin/users/new";
    }

    @PostMapping("/admin/users")
    public String create(
            @RequestParam("username") String username,
            @RequestParam("fullName") String fullName,
            @RequestParam("role") Role role,
            Authentication authentication,
            RedirectAttributes flash) {
        CreatedUserResult result = userAccountService.createUser(
                username, fullName, role, currentUser.currentUserId(authentication));
        flash.addFlashAttribute("createdUsername", username);
        flash.addFlashAttribute("temporaryPassword", result.temporaryPassword());
        return "redirect:/admin/users/" + result.userId().value() + "/created";
    }

    @GetMapping("/admin/users/{id}/created")
    public String createdConfirmation(@PathVariable UUID id, Model model) {
        // Page is reached via redirect with flash attributes; if revisited (refresh),
        // the temp password is no longer in flash and we fall back to a generic message.
        model.addAttribute("userId", id);
        return "admin/users/created";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        User user = userRepository
                .findById(UserId.of(id))
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "admin/users/edit";
    }

    @PostMapping("/admin/users/{id}")
    public String update(
            @PathVariable UUID id,
            @RequestParam("fullName") String fullName,
            @RequestParam("role") Role role,
            Authentication authentication,
            RedirectAttributes flash) {
        userAccountService.updateUser(
                UserId.of(id), fullName, role, currentUser.currentUserId(authentication));
        flash.addFlashAttribute("message", "User updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/deactivate")
    public String deactivate(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes flash) {
        try {
            userAccountService.deactivateUser(
                    UserId.of(id), currentUser.currentUserId(authentication));
            flash.addFlashAttribute("message", "User deactivated.");
        } catch (LastActiveAdminException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/reset-password")
    public String resetPassword(
            @PathVariable UUID id,
            Authentication authentication,
            RedirectAttributes flash) {
        String temp = userAccountService.resetPassword(
                UserId.of(id), currentUser.currentUserId(authentication));
        User target = userRepository
                .findById(UserId.of(id))
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        flash.addFlashAttribute("resetUsername", target.getUsername());
        flash.addFlashAttribute("temporaryPassword", temp);
        return "redirect:/admin/users/" + id + "/password-reset";
    }

    @GetMapping("/admin/users/{id}/password-reset")
    public String passwordResetConfirmation(@PathVariable UUID id, Model model) {
        model.addAttribute("userId", id);
        return "admin/users/password_reset";
    }
}
