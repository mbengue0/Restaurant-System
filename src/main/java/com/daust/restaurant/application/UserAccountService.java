package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.PasswordHasher;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC17a / UC17b / UC17c / UC17d — User account administration.
 *
 * <p>Owns two cross-aggregate guards that Domain cannot enforce alone:
 * <ul>
 *   <li>Username uniqueness on {@link #createUser} — pre-check via
 *       {@link UserRepository#findByUsername} (DB UK is the backstop).</li>
 *   <li>FR14 last-active-Admin guard on {@link #deactivateUser} — uses
 *       {@link UserRepository#findActiveByRole}.</li>
 * </ul>
 *
 * <p>NFR-SEC-1: passwords are stored only as BCrypt hashes via {@link PasswordHasher}. The plain
 * temporary password from create/reset is returned to the caller exactly once (so the admin can
 * read it to the new user) and never persisted, logged, or sent through any other channel.
 *
 * <p>NFR-SEC-2: both create and admin-reset force {@code mustChangePassword=true} so the user
 * must replace the temp password at first login.
 */
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuditLogRepository auditLogRepository;
    private final TemporaryPasswordGenerator passwordGenerator;

    public UserAccountService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditLogRepository = auditLogRepository;
        this.passwordGenerator = new TemporaryPasswordGenerator();
    }

    @Transactional
    public CreatedUserResult createUser(String username, String fullName, Role role, UserId adminId) {
        User admin = loadActor(adminId);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameTakenException("Username already taken: " + username);
        }

        String tempPassword = passwordGenerator.generate();
        String hash = passwordHasher.hash(tempPassword);
        User user = new User(username, hash, fullName, role, /* mustChangePassword */ true);
        userRepository.save(user);

        audit(admin, "USER_CREATED", user.getId(),
                null,
                "username=" + user.getUsername() + ", role=" + user.getRole()
                        + ", fullName=" + user.getFullName());

        return new CreatedUserResult(user.getId(), tempPassword);
    }

    @Transactional
    public void updateUser(UserId userId, String fullName, Role role, UserId adminId) {
        User user = loadTarget(userId);
        User admin = loadActor(adminId);

        String before = "fullName=" + user.getFullName() + ", role=" + user.getRole();
        user.updateFullName(fullName);
        user.updateRole(role);
        userRepository.save(user);
        String after = "fullName=" + user.getFullName() + ", role=" + user.getRole();

        audit(admin, "USER_UPDATED", user.getId(), before, after);
    }

    @Transactional
    public void deactivateUser(UserId userId, UserId adminId) {
        User user = loadTarget(userId);
        User admin = loadActor(adminId);

        // FR14: never strand the system without an active Admin.
        if (user.getRole() == Role.ADMIN && user.isActive()) {
            List<User> activeAdmins = userRepository.findActiveByRole(Role.ADMIN);
            boolean targetIsTheOnlyAdmin =
                    activeAdmins.size() == 1
                            && activeAdmins.get(0).getId().equals(user.getId());
            if (targetIsTheOnlyAdmin) {
                throw new LastActiveAdminException(
                        "Cannot deactivate '" + user.getUsername()
                                + "': they are the last active Admin (FR14)");
            }
        }

        user.deactivate();
        userRepository.save(user);

        audit(admin, "USER_DEACTIVATED", user.getId(), "active=true", "active=false");
    }

    @Transactional
    public String resetPassword(UserId userId, UserId adminId) {
        User user = loadTarget(userId);
        User admin = loadActor(adminId);

        String tempPassword = passwordGenerator.generate();
        String hash = passwordHasher.hash(tempPassword);
        user.resetPasswordByAdmin(hash);
        userRepository.save(user);

        // Hash content is never recorded in the audit log (NFR-SEC-1 keeps secrets out of audit).
        audit(admin, "USER_PASSWORD_RESET", user.getId(),
                null,
                "username=" + user.getUsername() + ", mustChangePassword=true");

        return tempPassword;
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    private User loadActor(UserId adminId) {
        return userRepository
                .findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + adminId));
    }

    private User loadTarget(UserId userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private void audit(User actor, String eventType, UserId targetId, String before, String after) {
        auditLogRepository.save(new AuditLogEntry(
                actor.getId(),
                actor.getRole(),
                eventType,
                "User",
                targetId.value().toString(),
                before,
                after));
    }
}
