package com.kakarote.ai_crm.ai.state;

import com.kakarote.ai_crm.entity.BO.UserAddBO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PendingEmployeeImportStoreTest {

    private EmployeeImportRow row(String username) {
        EmployeeImportRow r = new EmployeeImportRow();
        UserAddBO u = new UserAddBO();
        u.setUsername(username);
        u.setRealname(username);
        r.setUser(u);
        return r;
    }

    @Test
    void saveAndGetReturnsRowsForSession() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice"), row("bob")));

        PendingEmployeeImportStore.PendingEmployeeImport pending = store.get(100L);

        assertThat(pending).isNotNull();
        assertThat(pending.rows()).hasSize(2);
    }

    @Test
    void sessionsAreIsolated() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice")));

        assertThat(store.get(200L)).isNull();
    }

    @Test
    void expiredDraftIsRemovedOnGet() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice")));
        @SuppressWarnings("unchecked")
        Map<Long, PendingEmployeeImportStore.PendingEmployeeImport> map =
            (Map<Long, PendingEmployeeImportStore.PendingEmployeeImport>)
                ReflectionTestUtils.getField(store, "pendingRequests");
        map.put(100L, new PendingEmployeeImportStore.PendingEmployeeImport(
            List.of(row("alice")), Instant.now().minusSeconds(1)));

        assertThat(store.get(100L)).isNull();
    }

    @Test
    void removeReturnsAndClearsDraft() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice")));

        assertThat(store.remove(100L)).isNotNull();
        assertThat(store.get(100L)).isNull();
    }

    @Test
    void nullSessionIdIsIgnored() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(null, List.of(row("alice")));

        assertThat(store.get(null)).isNull();
    }
}
