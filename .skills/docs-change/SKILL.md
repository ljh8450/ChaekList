# Docs Change Skill

Use this skill when a task mainly affects project documentation, agent instructions, or skill guidance.

## Workflow
1. Read the relevant existing documentation or instruction files first.
2. For new documentation files, report the proposed path, purpose, and outline before creating the file, then wait for user approval.
3. Prefer updating existing documentation over creating a new file when it fits the current structure.
4. Keep wording clear, concrete, and consistent with nearby documents.
5. Make the smallest safe change.
6. Report changed files and validation results.

## Scope
- Documentation files in `docs/`.
- Repository-level guidance such as `README.md` and `AGENTS.md`.
- Local agent or skill guidance in `.skills/`.

## Constraints
- Do not edit frontend or backend source files unless required for the documentation task.
- Do not change package dependencies, CI, deployment, environment variables, or database schema without user approval.
- If validation is not applicable because the change is documentation-only, state that clearly in the final report.
