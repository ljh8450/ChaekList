# Backend Change Skill

Use this skill when a task mainly affects the Spring Boot backend.

## Workflow
1. Read relevant files in `backend/`.
2. Read `docs/architecture.md` and `docs/commands.md` when the change depends on project structure or validation rules.
3. Follow existing controller, service, request/response, and test patterns.
4. Make the smallest safe change.
5. Run backend validation commands if available.
6. Report changed files and validation results.

## Constraints
- Do not edit frontend files unless required.
- Reuse existing backend patterns before adding new abstractions.
- Ask before changing database schema, environment variables, package dependencies, CI, or deployment files.
- If backend validation fails, report the exact command, failure, and likely cause.
