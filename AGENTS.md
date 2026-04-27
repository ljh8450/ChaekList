# AGENTS.md

## Project overview
- This repository is a full-stack web app.
- frontend/: React app
- backend/: Spring Boot app

## Working style
- Make the smallest safe change possible.
- Read existing files and patterns before editing.
- Do not refactor unrelated code.
- Prefer updating existing code over creating new abstractions.
- Keep explanations short and concrete.

## Scope rules
- Frontend-only tasks should stay inside `frontend/` unless clearly required.
- Backend-only tasks should stay inside `backend/` unless clearly required.
- Documentation-focused tasks should use the docs role and stay inside `docs/`, `README.md`, `AGENTS.md`, or `.skills/` unless clearly required.
- Before creating a new documentation file, report the proposed path, purpose, and outline, then wait for approval.
- Planner role plan documents under `docs/plan/yyyy-mm-dd/` do not require this new-document approval step.
- Ask before changing database schema, environment variables, package dependencies, CI, or deployment files.

## Role rules
- Use the planner role when the user asks for a plan, when work is large or ambiguous, or when a task may touch multiple areas.
- Use the docs role for documentation, repository guidance, and skill guidance changes.
- Use the frontend role for React frontend changes.
- Use the backend role for Spring Boot backend changes.
- Use the full-validation role when validating changes across the project or when the user asks for complete validation.

## Validation rules
- After frontend changes, run the frontend validation commands.
- After backend changes, run the backend validation commands.
- If a command fails, explain the exact failure and likely cause.
- Do not claim success without running validation when validation is available.

## Planning rules
- When using the planner role, inspect relevant existing files before proposing steps.
- While acting as planner, create or update only the plan document under `docs/plan/yyyy-mm-dd/` without separate approval.
- Do not edit code or implementation files while acting only as planner unless the user explicitly asks to proceed.
- Wait for user approval before moving from planning to code or implementation changes.
- For tasks touching both frontend and backend, first propose a short plan.
- For large tasks, break work into small sequential steps.
- Avoid broad rewrites unless explicitly requested.

## Language
- All responses must be written in Korean.
- Use clear and concise Korean explanations.
- Keep technical terms in English only when necessary.

## Output rules
- Summarize:
  1. what changed
  2. which files changed
  3. validation results
  4. any remaining risks or follow-ups

## Project commands
See `docs/commands.md`.

## Architecture notes
See `docs/architecture.md`.
