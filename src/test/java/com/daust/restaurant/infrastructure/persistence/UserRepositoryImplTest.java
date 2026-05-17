package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserRepositoryImpl.class)
class UserRepositoryImplTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindById_roundTripsThroughMapper() {
        User original = new User("alice", HASH, "Alice Diop", Role.WAITER, true);

        userRepository.save(original);

        Optional<User> loaded = userRepository.findById(original.getId());
        assertThat(loaded).isPresent();
        User found = loaded.get();
        assertThat(found.getId()).isEqualTo(original.getId());
        assertThat(found.getUsername()).isEqualTo("alice");
        assertThat(found.getHashedPassword()).isEqualTo(HASH);
        assertThat(found.getFullName()).isEqualTo("Alice Diop");
        assertThat(found.getRole()).isEqualTo(Role.WAITER);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getLastLoginAt()).isNull();
        assertThat(found.isMustChangePassword()).isTrue();
    }

    @Test
    void save_persistsPasswordAndRoleChangeAndDeactivate() {
        User user = new User("bob", HASH, "Bob Sow", Role.WAITER, true);
        String newHash = "$2a$10$rotatedHashAfterPasswordChange....................";
        user.changeHashedPassword(newHash);
        user.updateRole(Role.MANAGER);
        user.deactivate();

        userRepository.save(user);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getHashedPassword()).isEqualTo(newHash);
        assertThat(reloaded.getRole()).isEqualTo(Role.MANAGER);
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.isMustChangePassword()).isFalse();
    }

    @Test
    void findByUsername_returnsMatchingUser() {
        userRepository.save(new User("alice", HASH, "Alice", Role.WAITER, false));
        userRepository.save(new User("bob", HASH, "Bob", Role.KITCHEN_STAFF, false));

        Optional<User> found = userRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
        assertThat(found.get().getRole()).isEqualTo(Role.WAITER);
    }

    @Test
    void findByUsername_returnsEmptyWhenMissing() {
        Optional<User> found = userRepository.findByUsername("ghost");

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_returnsAllSavedUsers() {
        userRepository.save(new User("alice", HASH, "Alice", Role.ADMIN, false));
        userRepository.save(new User("bob", HASH, "Bob", Role.MANAGER, false));
        userRepository.save(new User("carol", HASH, "Carol", Role.WAITER, false));

        List<User> all = userRepository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    void findActiveByRole_excludesInactiveAndOtherRoles() {
        User activeAdminA = new User("admin_a", HASH, "Admin A", Role.ADMIN, false);
        User activeAdminB = new User("admin_b", HASH, "Admin B", Role.ADMIN, false);
        User retiredAdmin = new User("old_admin", HASH, "Old Admin", Role.ADMIN, false);
        retiredAdmin.deactivate();
        User manager = new User("manager", HASH, "Manager", Role.MANAGER, false);

        userRepository.save(activeAdminA);
        userRepository.save(activeAdminB);
        userRepository.save(retiredAdmin);
        userRepository.save(manager);

        List<User> activeAdmins = userRepository.findActiveByRole(Role.ADMIN);

        assertThat(activeAdmins)
                .extracting(User::getId)
                .containsExactlyInAnyOrder(activeAdminA.getId(), activeAdminB.getId());
    }

    @Test
    void save_rejectsDuplicateUsername() {
        userRepository.save(new User("alice", HASH, "Alice One", Role.WAITER, false));

        assertThatThrownBy(() -> {
            userRepository.save(new User("alice", HASH, "Alice Two", Role.MANAGER, false));
            // Force flush so the unique-constraint violation surfaces before the test method exits.
            userRepository.findAll();
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
