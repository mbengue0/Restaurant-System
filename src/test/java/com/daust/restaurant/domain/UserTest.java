package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Test
    void newUser_isActiveWithGeneratedIdAndCreatedAtNow() {
        Instant before = Instant.now();
        User user = new User("alice", HASH, "Alice Diop", Role.WAITER, true);
        Instant after = Instant.now();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getId().value()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getHashedPassword()).isEqualTo(HASH);
        assertThat(user.getFullName()).isEqualTo("Alice Diop");
        assertThat(user.getRole()).isEqualTo(Role.WAITER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isBetween(before, after);
        assertThat(user.getLastLoginAt()).isNull();
        assertThat(user.isMustChangePassword()).isTrue();
    }

    @Test
    void newUser_trimsUsernameAndFullName() {
        User user = new User("  bob  ", HASH, "  Bob Sow  ", Role.MANAGER, false);

        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getFullName()).isEqualTo("Bob Sow");
    }

    @Test
    void constructor_rejectsBlankUsername() {
        assertThatThrownBy(() -> new User("", HASH, "Name", Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("   ", HASH, "Name", Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullUsername() {
        assertThatThrownBy(() -> new User(null, HASH, "Name", Role.ADMIN, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsUsernameLongerThan50Chars() {
        assertThatThrownBy(() -> new User("x".repeat(51), HASH, "Name", Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsBlankHashedPassword() {
        assertThatThrownBy(() -> new User("alice", "", "Name", Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("alice", "   ", "Name", Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullHashedPassword() {
        assertThatThrownBy(() -> new User("alice", null, "Name", Role.ADMIN, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsBlankFullName() {
        assertThatThrownBy(() -> new User("alice", HASH, "", Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User("alice", HASH, "   ", Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullFullName() {
        assertThatThrownBy(() -> new User("alice", HASH, null, Role.ADMIN, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsFullNameLongerThan100Chars() {
        assertThatThrownBy(() -> new User("alice", HASH, "x".repeat(101), Role.ADMIN, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullRole() {
        assertThatThrownBy(() -> new User("alice", HASH, "Name", null, false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void changeHashedPassword_updatesHashAndClearsMustChangeFlag() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, true);
        String newHash = "$2a$10$newHashAfterPasswordChange...........................";

        user.changeHashedPassword(newHash);

        assertThat(user.getHashedPassword()).isEqualTo(newHash);
        assertThat(user.isMustChangePassword()).isFalse();
    }

    @Test
    void changeHashedPassword_rejectsBlank() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, true);

        assertThatThrownBy(() -> user.changeHashedPassword("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> user.changeHashedPassword("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeHashedPassword_rejectsNull() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, true);

        assertThatThrownBy(() -> user.changeHashedPassword(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateFullName_updatesAndTrims() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);

        user.updateFullName("  Alice Diop  ");

        assertThat(user.getFullName()).isEqualTo("Alice Diop");
    }

    @Test
    void updateFullName_rejectsBlank() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);

        assertThatThrownBy(() -> user.updateFullName("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRole_changesRole() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);

        user.updateRole(Role.MANAGER);

        assertThat(user.getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    void updateRole_rejectsNull() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);

        assertThatThrownBy(() -> user.updateRole(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void deactivate_setsActiveFalse() {
        User user = new User("alice", HASH, "Alice", Role.ADMIN, false);

        user.deactivate();

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void deactivate_isUnconditional_lastAdminCheckIsApplicationConcern() {
        User adminUser = new User("root", HASH, "Root Admin", Role.ADMIN, false);

        adminUser.deactivate();

        assertThat(adminUser.isActive()).isFalse();
    }

    @Test
    void mustChangePassword_initialValueIsHonored() {
        User mustChange = new User("alice", HASH, "Alice", Role.WAITER, true);
        User noChange = new User("bob", HASH, "Bob", Role.WAITER, false);

        assertThat(mustChange.isMustChangePassword()).isTrue();
        assertThat(noChange.isMustChangePassword()).isFalse();
    }

    @Test
    void resetPasswordByAdmin_setsHashAndForcesMustChangePassword() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);
        String newHash = "$2a$10$adminResetHash................................";

        user.resetPasswordByAdmin(newHash);

        assertThat(user.getHashedPassword()).isEqualTo(newHash);
        assertThat(user.isMustChangePassword()).isTrue();
    }

    @Test
    void resetPasswordByAdmin_throwsOnInactiveUser() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);
        user.deactivate();

        assertThatThrownBy(() -> user.resetPasswordByAdmin("$2a$10$x")).isInstanceOf(
                IllegalStateException.class);
    }

    @Test
    void resetPasswordByAdmin_rejectsBlankHash() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);

        assertThatThrownBy(() -> user.resetPasswordByAdmin("")).isInstanceOf(
                IllegalArgumentException.class);
    }

    @Test
    void recordLogin_updatesLastLoginAtOnActiveUser() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);
        Instant when = Instant.parse("2026-05-17T10:15:30Z");

        user.recordLogin(when);

        assertThat(user.getLastLoginAt()).isEqualTo(when);
    }

    @Test
    void recordLogin_throwsOnInactiveUser() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);
        user.deactivate();

        assertThatThrownBy(() -> user.recordLogin(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordLogin_rejectsNullTimestamp() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);

        assertThatThrownBy(() -> user.recordLogin(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void newUsers_haveDistinctIds() {
        User a = new User("alice", HASH, "Alice", Role.WAITER, false);
        User b = new User("bob", HASH, "Bob", Role.WAITER, false);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }
}
