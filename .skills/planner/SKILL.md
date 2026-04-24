# Planner Skill

Use this skill when the user asks for a plan, when a task is large or ambiguous, or when a task may touch multiple project areas.

## Workflow
1. Read relevant existing files and instructions first.
2. Identify the task scope: frontend, backend, docs, validation, or mixed.
3. List the smallest safe sequence of steps.
4. Identify files likely to change.
5. Identify validation commands or explain why validation may not apply.
6. Wait for user approval before making changes.

## Constraints
- Do not edit files while acting only as planner unless the user explicitly asks to proceed.
- Keep plans short and concrete.
- Call out risky changes such as database schema, environment variables, package dependencies, CI, or deployment files.
- If the next step would create a new documentation file, report the proposed path, purpose, and outline before creation.
