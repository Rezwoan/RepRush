# Contributing to RepRush

This document defines the strict Git workflow and Kanban board process for Rezwoan and Dipon. Follow these steps to manage work, avoid conflicts, and merge safely.

## 0. Workflow & Kanban Rules

Before writing any code, you must follow the project management board. All project requirements and design specifications are located in the [E:\RepRush\.plan](e:\RepRush\.plan) folder.

1. **Select an Issue:** Open the [GitHub Project Kanban board](https://github.com/users/Rezwoan/projects/8). Start from `Milestone 1` and pick an issue assigned to you from the **To Do** column.
2. **Read the Specs:** Reference the `.plan` folder (e.g., `01_PRD_API_Contract.md`, `05_UIUX_Specification.md`) to understand the exact requirements for your task.
3. **Move to In Progress:** Drag the issue card to the **In Progress** column on the Kanban board.
4. **Create your Branch:** Create your branch locally (see Section 2 below) and implement the feature.
5. **Move to In Review:** Once you push your code and create a Pull Request to `dev`, drag the issue card to the **In Review** column.
6. **Code Review:** The other developer must review the PR. Rezwoan checks Dipon's ViewBinding IDs. Dipon checks Rezwoan's ViewModels.
7. **Merge and Done:** Once approved and merged into `dev`, the issue is automatically or manually moved to the **Done** column.

## 1. Checking Status and Fetching Changes

Before doing anything, check your current state and see if GitHub has new updates.

**Check your local state:**
```bash
git status
```
*Shows your current branch, uncommitted changes, and if you are behind the remote.*

**Check for changes on GitHub without modifying your files:**
```bash
git fetch origin
```
*Downloads the latest metadata from GitHub. Run `git status` after this to see if your local branch is behind.*

**Pull latest code into your active branch:**
```bash
git pull origin <branch-name>
```
*Example: `git pull origin dev` pulls latest updates from the remote dev branch into your current local branch.*

## 2. Managing Branches

Branches are strictly issue-based. Name format: `<issue-ID>-<short-description>`. (e.g., `1-m1-setup-android-project-sdks`). **Do not use `backend/` or `frontend/` prefixes.**

**List all local branches:**
```bash
git branch
```

**List all branches (local and remote):**
```bash
git branch -a
```

**Switch to an existing branch:**
```bash
git checkout <branch-name>
```

**Create and switch to a new branch from your current branch:**
```bash
git checkout -b <new-branch-name>
```

**Create a new branch specifically from `dev`:**
```bash
git checkout dev
git pull origin dev
git checkout -b <new-branch-name>
```

## 3. Saving Changes (Committing)

Do not write descriptions in your commit messages. Use a single line with one of these exact prefixes: `feat:`, `fix:`, `ui:`, `chore:`, `refactor:`.

**Stage specific files:**
```bash
git add <file/path/...>
```

**Stage all changed files:**
```bash
git add .
```

**Commit the staged changes:**
```bash
git commit -m "feat: login screen xml layout"
```

## 4. Pushing Code and Creating Pull Requests

Never push directly to `dev` or `main`. Push your branch to GitHub, then open a Pull Request.

**Push your new branch to GitHub for the first time:**
```bash
git push -u origin <branch-name>
```

**Push subsequent commits to the same branch:**
```bash
git push
```

**Create a Pull Request (via GitHub CLI):**
```bash
gh pr create --base dev --title "Fix: [Issue title]" --body "Resolves #<issue_number>"
```
*Or use the link provided in the terminal output after your first push to create it via the GitHub website.*

## 5. Merging Workflow (dev vs main)

- **Feature PRs:** All daily tasks and features are PR'd and merged into **`dev`**.
- **Milestone PRs:** Only when a full Milestone is thoroughly tested on `dev`, open a PR from `dev` to **`main`**.

## 6. Rebasing & Resolving Merge Conflicts

If `dev` gets updated while you are working on your branch, update your branch by rebasing to prevent cluttered history.

**Rebase your active branch onto the latest `dev`:**
```bash
git fetch origin
git rebase origin/dev
```

**If you hit a Merge Conflict:**
1. Open the conflicting files (marked correctly in `git status`) in VS Code.
2. Accept the incoming/current changes and remove the Git markers (`<<<<<<<`, `=======`, `>>>>>>>`).
3. Save the files.
4. Tell Git you have resolved it:
   ```bash
   git add .
   git rebase --continue
   ```

**To abort a messy rebase:**
If you make a mistake and want to start over:
```bash
git rebase --abort
```
