# Full Validation Skill

Use this skill when validating changes across the project, when both frontend and backend may be affected, or when the user asks for complete validation.

## Workflow
1. Read `docs/commands.md`.
2. Check changed files to identify affected areas: frontend, backend, docs, or mixed.
3. For frontend changes, run the frontend validation command in `frontend/`.
4. For backend changes, run the backend validation command in `backend/`.
5. If both frontend and backend changed, run both validation commands.
6. For docs-only changes, explain why build validation is not required.
7. Report each command, result, and any failure cause.

## Constraints
- Do not modify files while validating unless the user asks for fixes.
- Do not claim full validation passed unless all relevant commands were run successfully.
- If a validation command is unavailable or fails due to environment issues, report the exact reason.
