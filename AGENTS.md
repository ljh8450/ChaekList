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
- State assumptions explicitly before implementation when they affect the approach.
- If requirements are unclear or have multiple interpretations, ask before changing code.
- Surface tradeoffs and push back when a simpler or safer approach exists.

## Simplicity rules
- Implement only what was requested.
- Do not add speculative features, configurability, or abstractions.
- Avoid single-use abstractions unless they clearly reduce complexity.
- Do not add error handling for scenarios that cannot happen in the current design.
- If a solution becomes noticeably larger than necessary, simplify before finishing.

## Change discipline
- Touch only lines that directly support the user's request.
- Do not improve adjacent code, comments, formatting, or dead code unless asked.
- Match existing style even when another style is preferred.
- If unrelated dead code or cleanup is noticed, mention it instead of deleting it.
- Remove only imports, variables, functions, or files made unused by your own changes.

## Scope rules
- Frontend-only tasks must stay inside `frontend/`.
- Backend-only tasks must stay inside `backend/`.
- Documentation-focused tasks should use the docs role and stay inside `docs/`, `README.md`, `AGENTS.md`, or `.skills/` unless clearly required.
- Before creating a new documentation file, report the proposed path, purpose, and outline, then wait for approval.
- Planner role plan documents under `docs/plan/yyyy-mm-dd/` do not require this new-document approval step.
- Ask before changing database schema, environment variables (`.env`), package dependencies, CI/CD settings, or deployment settings.

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

## Execution rules
- Define success criteria before implementation when the task is non-trivial.
- For bug fixes, prefer reproducing the bug with a focused test or check before changing code.
- For validation work, make the verification command or check explicit.
- For multi-step tasks, state a brief plan in this format:
  1. `[Step]` -> verify: `[check]`
  2. `[Step]` -> verify: `[check]`
  3. `[Step]` -> verify: `[check]`
- Continue iterating until the stated verification passes or the blocker is clearly explained.

## Language
- All responses must be written in Korean.
- Use clear and concise Korean explanations.
- Keep technical terms in English only when necessary.
- 문서와 계획 파일도 사용자가 다른 언어를 명시적으로 요청하지 않는 한 한국어로 작성한다.

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
