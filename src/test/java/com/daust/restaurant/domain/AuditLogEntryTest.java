package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AuditLogEntryTest {

    private static final UserId USER = UserId.generate();

    @Test
    void newEntry_capturesProvidedFieldsAndGeneratesIdAndTimestamp() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        AuditLogEntry entry = new AuditLogEntry(
                USER,
                Role.MANAGER,
                "ORDER_CANCELLED",
                "Order",
                "abc-123",
                "{\"state\":\"PLACED\"}",
                "{\"state\":\"CANCELLED\"}");

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getId().value()).isNotNull();
        assertThat(entry.getTimestamp()).isBetween(before, after);
        assertThat(entry.getUserId()).isEqualTo(USER);
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.MANAGER);
        assertThat(entry.getEventType()).isEqualTo("ORDER_CANCELLED");
        assertThat(entry.getAffectedEntityType()).isEqualTo("Order");
        assertThat(entry.getAffectedEntityId()).isEqualTo("abc-123");
        assertThat(entry.getBeforeValue()).isEqualTo("{\"state\":\"PLACED\"}");
        assertThat(entry.getAfterValue()).isEqualTo("{\"state\":\"CANCELLED\"}");
    }

    @Test
    void constructor_rejectsNullUserId() {
        assertThatThrownBy(() -> new AuditLogEntry(null, Role.MANAGER, "X", null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNullUserRoleAtTime() {
        assertThatThrownBy(() -> new AuditLogEntry(USER, null, "X", null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNullEventType() {
        assertThatThrownBy(() -> new AuditLogEntry(USER, Role.MANAGER, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsBlankEventType() {
        assertThatThrownBy(() -> new AuditLogEntry(USER, Role.MANAGER, "", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditLogEntry(USER, Role.MANAGER, "   ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_acceptsAllOptionalFieldsNull() {
        AuditLogEntry entry = new AuditLogEntry(USER, Role.WAITER, "LOGIN_SUCCESS", null, null, null, null);

        assertThat(entry.getAffectedEntityType()).isNull();
        assertThat(entry.getAffectedEntityId()).isNull();
        assertThat(entry.getBeforeValue()).isNull();
        assertThat(entry.getAfterValue()).isNull();
    }

    @Test
    void constructor_acceptsAffectedEntityWithoutBeforeOrAfter() {
        AuditLogEntry entry = new AuditLogEntry(
                USER, Role.ADMIN, "USER_VIEWED", "User", "uid-9", null, null);

        assertThat(entry.getAffectedEntityType()).isEqualTo("User");
        assertThat(entry.getAffectedEntityId()).isEqualTo("uid-9");
        assertThat(entry.getBeforeValue()).isNull();
        assertThat(entry.getAfterValue()).isNull();
    }

    @Test
    void newEntries_haveDistinctIds() {
        AuditLogEntry a = new AuditLogEntry(USER, Role.MANAGER, "X", null, null, null, null);
        AuditLogEntry b = new AuditLogEntry(USER, Role.MANAGER, "X", null, null, null, null);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    void reconstitute_acceptsExplicitIdAndTimestamp() {
        AuditLogId id = AuditLogId.generate();
        LocalDateTime ts = LocalDateTime.of(2026, 1, 15, 10, 30);

        AuditLogEntry entry = AuditLogEntry.reconstitute(
                id, ts, USER, Role.MANAGER, "ORDER_PLACED", "Order", "ord-1", null, "{\"state\":\"PLACED\"}");

        assertThat(entry.getId()).isEqualTo(id);
        assertThat(entry.getTimestamp()).isEqualTo(ts);
    }

    @Test
    void auditLogEntry_hasNoSetters_isAppendOnly() {
        // Append-only invariant: there must be no public setter / mutator on AuditLogEntry.
        // (Getters are allowed; reconstitute is a static factory, not a mutator.)
        for (Method method : AuditLogEntry.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            boolean looksLikeMutator = name.startsWith("set")
                    || name.startsWith("update")
                    || name.startsWith("change")
                    || name.startsWith("mark")
                    || name.startsWith("add")
                    || name.startsWith("remove")
                    || name.startsWith("delete");
            assertThat(looksLikeMutator)
                    .as("AuditLogEntry must not expose mutator method '%s' (append-only)", name)
                    .isFalse();
        }
    }
}
