# Sidebar Menu Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build per-user drag sorting for the desktop left sidebar modules and persist the order across logins.

**Architecture:** Backend stores a normalized UI preference JSON string on `manager_user` and exposes it through current-user responses. Frontend keeps a small sidebar-order helper, hydrates from localStorage first, syncs from `/auth/userInfo`, and saves through a current-user preference endpoint. `MainLayout.vue` renders the five reorderable modules through an ordered descriptor list.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, PostgreSQL/Flyway, Vue 3 Composition API, Pinia, Element Plus, Tailwind utility classes.

---

### Task 1: Backend Preference Model And Normalization

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/BO/UserPreferenceUpdateBO.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/VO/UserPreferenceVO.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/service/support/UserPreferenceSupport.java`
- Create: `backend/src/test/java/com/kakarote/ai_crm/service/support/UserPreferenceSupportTest.java`

- [ ] **Step 1: Write failing normalization tests**

Add tests that assert default order, removal of duplicate keys, filtering of unknown keys, and appending of missing keys.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=UserPreferenceSupportTest`

Expected: compilation failure because `UserPreferenceSupport` does not exist.

- [ ] **Step 3: Add minimal support classes**

Create `UserPreferenceSupport` with constants for `recent`, `customer`, `project`, `relation`, and `addressBook`; JSON parse/serialize helpers; and normalization methods.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -Dtest=UserPreferenceSupportTest`

Expected: tests pass.

### Task 2: Backend Persistence And API

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/entity/PO/ManagerUser.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/entity/VO/ManageUserVO.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/ManageUserService.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/impl/ManageUserServiceImpl.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/controller/ManagerUserController.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/impl/AuthSessionServiceImpl.java`
- Modify: `backend/src/main/resources/mapper/ManageUserMapper.xml`
- Create: `backend/src/main/resources/db/migration/V42__manager_user_ui_preferences.sql`

- [ ] **Step 1: Add database column**

Create a Flyway migration that adds `ui_preferences TEXT` to `manager_user` and documents it as user-level UI preferences JSON.

- [ ] **Step 2: Wire entity and VO fields**

Add `uiPreferences` to `ManagerUser`, and add `UserPreferenceVO preferences` to `ManageUserVO`.

- [ ] **Step 3: Expose preferences in current-user responses**

In `queryLoginUser` and `AuthSessionServiceImpl.buildUserInfo`, parse `ManagerUser.uiPreferences` through `UserPreferenceSupport`.

- [ ] **Step 4: Add current-user update endpoint**

Add `ManageUserService.updateCurrentUserPreferences(UserPreferenceUpdateBO)` and `POST /managerUser/preferences`; update only `UserUtil.getUserId()` in the current tenant.

- [ ] **Step 5: Run backend checks**

Run: `cd backend && mvn test -Dtest=UserPreferenceSupportTest`

Expected: tests pass.

### Task 3: Frontend Preference API And Helpers

**Files:**
- Modify: `frontend/src/types/api.d.ts`
- Modify: `frontend/src/api/auth.ts`
- Modify: `frontend/src/stores/user.ts`
- Create: `frontend/src/utils/sidebarModuleOrder.ts`

- [ ] **Step 1: Add TypeScript helper**

Create `sidebarModuleOrder.ts` with default order, normalization, localStorage read/write, and server preference extraction helpers.

- [ ] **Step 2: Add API and store wiring**

Add `updateUserPreferences` API and store method. Hydrate local user info preferences when login and `fetchUserInfo` complete.

- [ ] **Step 3: Run frontend type check**

Run: `cd frontend && npx vue-tsc -b --noEmit`

Expected: no type errors.

### Task 4: Desktop Sidebar Sorting UI

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Refactor module rendering through ordered descriptors**

Represent the five sidebar modules with stable keys and render order from the helper-normalized preference.

- [ ] **Step 2: Add sorting mode controls**

Add the bottom-right sort settings button, draft order state, drag handlers, and Save/Cancel/Restore Default actions.

- [ ] **Step 3: Keep modules closed during sorting**

When sorting mode is active, render closed module rows with drag handles and hide their content panels/actions.

- [ ] **Step 4: Persist save**

On save, call `userStore.updatePreferences`, write localStorage, apply order, exit sorting mode, and show success. On failure, restore previous order and show error.

- [ ] **Step 5: Run frontend verification**

Run: `cd frontend && npm run lint`

Expected: lint completes and auto-fixes only relevant source files.

### Task 5: Final Verification

**Files:**
- Review all touched files.

- [ ] **Step 1: Run backend targeted test**

Run: `cd backend && mvn test -Dtest=UserPreferenceSupportTest`

Expected: tests pass.

- [ ] **Step 2: Run frontend lint**

Run: `cd frontend && npm run lint`

Expected: lint passes.

- [ ] **Step 3: Inspect git diff**

Run: `git diff --stat` and review touched files for accidental generated output.

Expected: only docs, backend source/migration/test, and frontend source files are changed.
