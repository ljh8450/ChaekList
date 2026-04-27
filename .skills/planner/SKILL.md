# Planner Skill

Use this skill when the user asks for a plan, when a task is large or ambiguous, or when a task may touch multiple project areas.

## Workflow
1. Read relevant existing files and instructions first.
2. Identify the task scope: frontend, backend, docs, validation, or mixed.
3. List the smallest safe sequence of steps.
4. Identify files likely to change.
5. Identify validation commands or explain why validation may not apply.
6. After proposing a plan, create a plan document under `docs/plan/yyyy-mm-dd/` using the filename format `task-summary-nn.md`.
   - Use lowercase kebab-case for `task-summary`.
   - Use a two-digit sequence number for `nn`, starting at `01`.
   - Example: `docs/plan/2026-04-27/add-planner-plan-doc-rule-01.md`.
7. Wait for user approval before making code or implementation changes.

## Constraints
- While acting as planner, create or update only the plan document under `docs/plan/yyyy-mm-dd/` without separate approval.
- Do not edit code or implementation files while acting only as planner unless the user explicitly asks to proceed.
- Keep plans short and concrete.
- Call out risky changes such as database schema, environment variables, package dependencies, CI, or deployment files.
- Other new documentation files still require reporting the proposed path, purpose, and outline before creation.
