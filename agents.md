# Agent Instructions for RepRush

## Git Commit Rules — STRICT

### Co-authorship — NEVER ADD
- NEVER add `Co-Authored-By` lines to any commit message
- NEVER add `Co-authored-by` lines to any commit message
- NEVER mention Claude, Anthropic, or any AI tool in any commit message, PR description, branch name, or code comment
- The git history must appear as if written entirely by the human developer
- This applies to every single commit, merge commit, and squash commit without exception

### Commit Message Format
Every commit message must follow this format exactly:
```
type: short description
- bullet detail 1
- bullet detail 2
```
No additional lines after the bullets. Nothing else. No signatures, no attributions, no tool names.

Valid types: `feat`, `fix`, `chore`, `ui`, `docs`, `refactor`, `test`

### Branch Rules
- Always branch from `dev`, never from `main`
- Branch naming: `feature/m{milestone}-{short-name}` e.g. `feature/m3-payment-recording`
- Never push directly to `main`
- Never open a PR to `main` — the developer does that manually

### Before Every Commit
1. Run `./gradlew assembleDebug` — must succeed with zero errors
2. Check `git diff --staged` — confirm no planning/doc markdown files are staged
3. Check commit message — confirm no Co-Authored-By line exists

### Remote
- Remote name is `origin`
- `git filter-repo` removes the remote automatically — if you use it, re-add with:
  `git remote add origin https://github.com/Rezwoan/RepRush.git`

---

## Files That Must Never Be Committed

Add these patterns to `.gitignore` before the first commit of every milestone:

```
MILESTONE_*_PLAN.md
MILESTONE_*_COMPLETION_REPORT.md
*_SCRATCH.md
*_NOTES.md
.claude/
```

Planning and documentation files are local-only. Never push them.

---

## Documentation Rules

- Create completion reports locally as `MILESTONE_X_COMPLETION_REPORT.md`
- These are for the developer's reference only — never committed, never pushed
- Code comments are allowed and encouraged
- No markdown docs in commits unless it is `README.md` with a specific developer request

---

## Architecture Rules (RepRush Specific)

- Reuse the nullable `Result<T>?` operationResult pattern — see Milestone 2 Completion Report
- Never use Safe Args — pass navigation arguments via `Bundle`
- Firestore boolean field names: do NOT prefix with `is` — use `active` not `isActive`
- Activity-level WindowInsets only — never add edge-to-edge handling per-fragment
- All weights stored in kg in Room regardless of display unit
- Never call `allowDestructiveMigration()` — always write explicit Room Migration classes
- Never store API keys anywhere except `local.properties` accessed via `BuildConfig`
