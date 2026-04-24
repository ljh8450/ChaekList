# architecture.md

## High-level structure
- `frontend/`: user-facing web UI
- `backend/`: REST API and business logic

## Change boundaries
- UI, components, pages, client routing, API calls -> `frontend/`
- controllers, services, persistence, auth, business rules -> `backend/`

## General expectations
- Reuse existing patterns first.
- Keep interfaces stable unless explicitly asked to change them.
- Avoid coupling unrelated frontend and backend changes in one edit unless required.