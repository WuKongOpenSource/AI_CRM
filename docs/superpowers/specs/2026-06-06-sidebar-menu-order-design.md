# Sidebar Menu Order Design

## Goal

Allow each logged-in user to reorder the five dynamic sections in the desktop left sidebar: recent chat, customers, projects, relations, and address book.

## Scope

The feature applies to the desktop primary sidebar in `frontend/src/layouts/MainLayout.vue`. Mobile menu ordering is not changed. Existing module visibility rules still apply: if a user lacks access to a module, the saved order is kept but invisible modules are filtered out at render time.

## User Experience

Add a compact settings button in the bottom area of the left sidebar, above the personal account row and aligned to the right. It uses the current sidebar icon-button style and black tooltip pattern.

Clicking the button enters sorting mode. In sorting mode, the five modules render as closed header rows only. They cannot be expanded, and their previous expanded/collapsed state is preserved for when sorting mode ends. Each row shows a drag handle icon on the right. Users can drag rows up or down. The footer of the sorting panel exposes Save, Cancel, and Restore Default actions.

Save applies the draft order immediately, persists it to the current user's preferences in the backend, and writes it to localStorage. Cancel discards the draft and restores the order that was active when sorting started. Restore Default resets the draft to `recent`, `customer`, `project`, `relation`, `addressBook`; it takes effect permanently only after Save.

## Persistence

Backend persistence is user-scoped. Add a JSON-text preference column to `manager_user` so this lightweight UI preference can travel with `/auth/userInfo`. The backend validates and normalizes the sidebar order: it accepts only the five known keys, removes duplicates, and appends missing keys in default order.

Frontend persistence is localStorage plus server refresh. On page load/login, the app first uses localStorage for immediate rendering, then refreshes from `/auth/userInfo` and overwrites localStorage if the server returns a valid preference.

## Data Shape

`uiPreferences.sidebarModuleOrder` stores an array of strings:

```json
["recent", "customer", "project", "relation", "addressBook"]
```

## Error Handling

If backend save fails, keep the previous applied order and show an error message. If the saved preference is invalid or missing, normalize to default order without breaking sidebar rendering.

## Testing

Backend tests cover normalization of valid, duplicate, unknown, and missing order keys. Frontend validation is covered by TypeScript helper functions where possible, and the full app is verified with the frontend type/lint command available in the project.
