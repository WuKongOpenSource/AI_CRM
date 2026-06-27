# Address Book AI Bulk Import Design

## Goal

Let a user add employees to the address book in bulk by uploading a file in the "通讯录" (address book) chat application. The AI recognizes the roster from the upload, lists the parsed employees back to the user, asks for confirmation, and only then creates the accounts. This works regardless of file type — spreadsheets, Word, PDF, plain text, or photos of a roster/business cards — as long as the model can recognize employee rows from the content.

## Scope

The feature lives entirely in the AI chat subsystem and the user-management service. There is no new standalone upload page and no downloadable Excel template (unlike the existing customer import). The entry point is the existing "通讯录" chat application context (`ChatApplicationCodes.ADDRESS_BOOK`), which already supports file attachments and SSE streaming.

In scope per imported employee: 姓名 (realname, required), 用户名 (username, required), 密码 (password, default `123456`), 职位 (post), 所属部门 (department, matched by name), 手机 (mobile), 邮箱 (email), 员工状态 (employee status: 在职/离职/停用).

Out of scope for the first version: 系统角色 (roles) and 直属上级 (reporting manager). Both require name→ID resolution with many failure modes; they can be added in a later iteration. Department is supported by name match only.

## How Files Reach the Model (existing behavior, no change)

`ChatServiceImpl` already feeds attachment content to the model: `buildAttachmentContext()` extracts text from documents (Excel/CSV/Word/PDF) via Tika and appends it to the user message; image attachments are passed as `Media` to vision-capable models via `buildMediaList()`. Therefore the model can already "see" the uploaded roster. The only missing capability is a write tool to create employees. Extraction/recognition is the model's responsibility over content the chat pipeline already assembles.

## User Experience / Data Flow

1. In the 通讯录 chat, the user uploads a file and asks to add the people to the address book.
2. The model recognizes the roster and calls `previewEmployeeImport(employeesJson)` with the extracted rows as a JSON array.
3. The tool parses the JSON, validates required fields (realname, username), detects duplicate usernames against existing users, stores the draft batch keyed by session, and returns a human-readable summary for the model to relay: a table of 姓名/用户名/部门/状态 with a 新建 or 覆盖更新 marker per row, plus any error rows.
4. The model shows the list and asks the user to confirm.
5. On confirmation the model calls `confirmEmployeeImport()`. The tool reads the stored draft and creates each account; rows whose username already exists update that user's profile (not the password); it returns a summary: created X / updated Y / skipped Z (with reasons).
6. If the user changes their mind, the model calls `cancelEmployeeImport()` to clear the draft.

This mirrors the existing customer-creation pending→confirm→cancel pattern (`PendingCustomerCreationStore` + `CustomerTools`).

If the model cannot recognize any employee rows in the upload, it states that plainly and does not call the import tools.

## Backend Components

### `ai/tools/EmployeeTools.java` (new)

A `@Component` exposing three `@Tool` methods, following the `CustomerTools` convention (String params, String returns, `@AiToolPermission`):

- `previewEmployeeImport(String employeesJson)` — guarded by `@AiToolPermission(value = "user:create", action = "预览批量导入员工")`. Parses the JSON array (each item: `realname`, `username`, optional `password`, `post`, `deptName`, `mobile`, `email`, `employeeStatus`), trims/normalizes, validates required fields, resolves `deptName`→`deptId` (unresolved → left empty with a note), normalizes `employeeStatus` via `EmployeeStatusEnum`, flags duplicate usernames against existing `manager_user` rows, saves the validated batch to `PendingEmployeeImportStore`, and returns a preview summary.
- `confirmEmployeeImport()` — same permission. Reads and removes the draft for the current session. For each non-error row: if username is new, calls `ManageUserService.addUser` (password defaults to `123456` when blank); if username exists, updates that user's profile fields (realname/post/dept/mobile/email/employeeStatus) without changing the password. Returns created/updated/skipped counts with reasons.
- `cancelEmployeeImport()` — clears the draft for the current session.

Session id comes from `AiContextHolder` (same source the pending customer flow uses).

### `ai/state/PendingEmployeeImportStore.java` (new)

Mirrors `PendingCustomerCreationStore`: a `ConcurrentHashMap<Long, PendingEmployeeImport>` keyed by session id, 30-minute TTL, holding a `List<UserAddBO>` (the validated batch) plus per-row preview metadata (duplicate flag, existing userId, row errors). Defensive copies on save.

### `ai/app/ChatApplicationRegistry.java` (edit)

- Add `public static final String TOOL_GROUP_EMPLOYEE = "employee";`.
- Add `TOOL_GROUP_EMPLOYEE` to the `ADDRESS_BOOK` application's tool group list.
- Extend the `ADDRESS_BOOK` system prompt: when the user uploads a roster/name list, first call `previewEmployeeImport` and show the parsed employees, ask the user to confirm, then call `confirmEmployeeImport`; only claim accounts were created after the tool confirms success; default password is `123456`.
- Add a recommended question such as "上传名单帮我批量添加员工".

### `ai/DynamicChatClientProvider.java` (edit)

Inject `EmployeeTools` and add `case ChatApplicationRegistry.TOOL_GROUP_EMPLOYEE -> addTool(tools, employeeTools);` to the `resolveTools` switch.

### Reused services

`ManageUserService.addUser` for creation (it already hashes the password with BCrypt, enforces unique username, stamps status/employeeStatus). Profile update for the overwrite branch reuses the existing update path. Department lookup uses the existing dept mapper/service.

## Duplicate Handling

Duplicate is determined by exact `username` match against existing `manager_user` rows. Duplicates are marked in the preview as 覆盖更新 and, on confirm, update the existing user's profile fields while leaving the password unchanged. This reflects the user's explicit choice of overwrite-update over skip-or-error.

## Frontend

No frontend changes required. The 通讯录 chat application, file upload, attachment handling, and SSE consumption already exist. The new recommended question and prompt text come from the backend registry and surface automatically.

## Error Handling

- Invalid/unparseable JSON or an empty array → the tool returns a clear message; nothing is stored.
- Rows missing realname or username are marked as error rows in the preview and skipped on confirm.
- Unresolved department name → the row imports without a department and the preview notes it.
- Creating an account that races into a duplicate username (created between preview and confirm) is caught by `addUser`'s uniqueness check and reported as skipped, not a transaction failure for the whole batch.
- A draft that has expired (TTL) or is missing on confirm → the tool asks the user to re-run the preview.

## Testing

Backend unit tests for `EmployeeTools`: JSON parsing, required-field validation, duplicate detection and the overwrite-update branch, default password application, department name resolution (hit and miss), employee-status normalization, and the created/updated/skipped summary. `PendingEmployeeImportStore` tests cover per-session isolation and TTL expiry. Tests follow the project convention of overriding shared infra via env/local profile, since there is no `application-test.yml`.
