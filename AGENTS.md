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
- Ask before changing database schema, environment variables, package dependencies, CI, or deployment files.

## Validation rules
- After frontend changes, run the frontend validation commands.
- After backend changes, run the backend validation commands.
- If a command fails, explain the exact failure and likely cause.
- Do not claim success without running validation when validation is available.

## Planning rules
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