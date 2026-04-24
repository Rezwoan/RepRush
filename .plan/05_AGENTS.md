# AGENTS.md — AI Pair-Programming Guide
## RepRush | Android Development Standards v1.0

---

## 1. Core Philosophy

AI is a collaborator, not an autocomplete. The developer remains the architect and decision-maker at all times. AI assistance is used to accelerate the implementation of well-understood, well-scoped tasks — not to generate features the developer has not thought through.

The goal of every AI interaction is that the developer could reproduce the output themselves given enough time. If the AI produces code the developer cannot read, explain, or debug, the interaction has failed regardless of whether the code runs.

---

## 2. Interaction Rules (Non-Negotiable)

### 2.1 Scope Discipline

- Every AI task must be scoped to a **single, named unit**: one function, one Fragment layout XML, one DAO method, one Repository function, or one data class. Multi-file requests are prohibited.
- If a task requires changes to more than one file, break it into **separate, sequential AI interactions**, each addressing one file.
- Never ask: *"Build the whole workout logging screen."* Always ask: *"Write the WorkoutSessionDao with three methods: insertSession, getSessionById, and getSessionsByUserId."*

### 2.2 Explain Before Code

- Before writing any code, the AI must output a **plain-English explanation** covering:
  - What the code will do
  - Why this specific approach was chosen over alternatives
  - What the developer must understand to read the code correctly
- If the explanation references a concept the developer has not confirmed they understand, the developer must ask for clarification before allowing the AI to proceed.
- Explanations must reference the project's specific architecture (MVVM, Repository pattern, XML layouts, ViewBinding, Room) and not give generic Android advice.

### 2.3 Annotation Requirements

- Every **non-obvious line** of generated code must have an inline comment explaining what it does and why.
- Non-obvious means: any Coroutine operator, any Flow transformation, any Room annotation, any LiveData observer, any Firestore transaction, any ViewBinding reference, and any lambda that is not immediately readable.
- Obvious lines (variable assignments with clear names, simple if/else, return statements) do not need comments.

### 2.4 Third-Party Library Flagging

Whenever the AI uses a third-party library, it must explicitly state:
- The library name and version
- Why it was chosen over alternatives
- Any known limitations or gotchas for this project

### 2.5 Security & Scale Flags

- Any Firebase security rule implication must be flagged explicitly: *"This code reads from leaderboard/{month} — ensure the security rule allows read for all authenticated users, not just the document owner."*
- Any pattern that will require refactoring at scale must be flagged: *"This approach works fine for single-user queries but will need pagination if the leaderboard exceeds 1000 entries."*
- Any hardcoded value that should become a constant must be flagged: *"This 500 cap should be extracted to PointsConstants.DAILY_CAP so it can be changed in one place."*

---

## 3. Project Context Block

Paste this at the start of **every new AI session** to establish project context:

```
PROJECT: RepRush — Android gym tracker + gamification app

STACK: Kotlin, XML layouts, ViewBinding, Material 3 (XML theme), Room,
Firebase Auth (Google Sign-In), Firestore, DataStore, Kotlin Coroutines + Flow,
LiveData, MVVM + Repository pattern, Navigation Component (Fragment-based),
MPAndroidChart, Glide

ARCHITECTURE: UI (XML Fragment) -> ViewModel (LiveData) -> Repository -> Room + Firestore

UI RULES: No Jetpack Compose. All layouts are XML. ViewBinding only, no
findViewById. Each screen is a Fragment. Navigation via NavController.
RecyclerView for all lists.

RULES:
1. Explain before code.
2. One file or function per task.
3. Annotate every non-obvious line.
4. Flag all third-party libraries with name, version, and rationale.
5. Flag all Firebase security rule implications.
6. Flag any pattern that needs to change at scale.
7. No unsolicited features.

TASK: [describe one specific, scoped task here]
```

---

## 4. Task-Specific Prompt Templates

### 4.1 Writing a Room DAO

```
[Project context block]

Write the [EntityName]Dao interface for Room.
Include only these methods: [list method names and what they return].
Use suspend functions for insert, update, and delete.
Use Flow<> for queries that the UI needs to observe in real time.
Use one-shot suspend fun for queries the ViewModel calls once.
Annotate every non-obvious line.
Do not add any methods I have not listed.
```

### 4.2 Writing a Repository Function

```
[Project context block]

Write the [functionName] function in [RepositoryName].
It should: [one-sentence description].
It reads from Room first, then writes to Firestore in parallel.
Handle the case where Firestore is offline — Firestore offline persistence
handles this automatically, but explain why no extra error handling is needed.
Annotate every non-obvious line.
```

### 4.3 Writing a Fragment + XML Layout

```
[Project context block]

Write the [ScreenName]Fragment class and its layout XML file fragment_[name].xml.
The Fragment observes a ViewModel of type [ViewModelName].
The UI state comes from a LiveData<[StateType]> named [fieldName].
The layout must include: [list every View element needed with its purpose].
The Fragment must handle: loading state (show ProgressBar), empty state
(show layout_empty), and error state (show Snackbar).
Use ViewBinding. No findViewById anywhere.
Annotate every non-obvious line in the Fragment class.
```

### 4.4 Writing a ViewModel

```
[Project context block]

Write the [FeatureName]ViewModel.
It should expose these LiveData fields: [list field names and types].
It should handle these user events as functions: [list function names].
All repository calls must be in viewModelScope.launch.
Explain why viewModelScope is used and what happens to running coroutines
when the ViewModel is cleared.
Annotate every non-obvious line.
```

### 4.5 Writing an XML Layout Only

```
[Project context block]

Write only the XML layout file for [ScreenName].
Root view: [ConstraintLayout / LinearLayout / CoordinatorLayout].
Include these Views: [list every element, its type, ID naming convention,
and rough position or constraint].
All IDs must follow the naming convention:
  btn_ for Buttons, tv_ for TextViews, et_ for EditTexts,
  iv_ for ImageViews, rv_ for RecyclerViews, layout_ for ViewGroups.
No hardcoded strings — use @string/ references.
No hardcoded dimensions except 0dp — use @dimen/ references.
```

---

## 5. Post-Generation Checklist

Run this after every AI-generated code block before committing:

### 5.1 Understanding Check
- [ ] I can explain in one sentence what every function in this file does
- [ ] I can explain why this approach was chosen over the obvious alternative
- [ ] I understand every import at the top of the file — no mystery dependencies
- [ ] I can identify which part of the MVVM architecture this code belongs to

### 5.2 Code Quality Check
- [ ] No hardcoded strings — all in strings.xml
- [ ] No hardcoded colors — all in colors.xml referencing the theme
- [ ] No hardcoded dimensions — all in dimens.xml
- [ ] No TODO comments left by the AI that have not been addressed
- [ ] No unused parameters, unused imports, or dead code blocks
- [ ] All suspend functions are called from a coroutine scope
- [ ] All LiveData observers are set in onViewCreated, not onCreate
- [ ] ViewBinding view references are nulled out in onDestroyView

### 5.3 Security Check
- [ ] No Firebase reads or writes that bypass the security rule structure in Document 2
- [ ] No user ID hardcoded — always pulled from FirebaseAuth.getInstance().currentUser?.uid
- [ ] No sensitive data in logs (no Log.d with passwords, tokens, or UIDs)

### 5.4 Offline Check
- [ ] Any Firestore operation is wrapped in a try/catch that handles offline gracefully
- [ ] Room is the read source for all data the user sees — Firestore is never queried synchronously for UI display

### 5.5 UI Correctness
- [ ] All Views have an ID if they are referenced in code
- [ ] All IDs follow naming convention: btn_, tv_, et_, iv_, rv_, layout_
- [ ] No ViewBinding field accessed after onDestroyView
- [ ] RecyclerView has a LayoutManager set and an Adapter set in onViewCreated
- [ ] All EditTexts have android:hint and android:inputType set in XML

---

## 6. Prohibited Patterns

The following patterns must never appear in generated code. Reject any AI output that includes them:

| Prohibited Pattern | Reason | Correct Alternative |
|---|---|---|
| `findViewById` | Error-prone, null-unsafe | ViewBinding always |
| `SharedPreferences` for user prefs | Synchronous, limited | DataStore Preferences |
| `AsyncTask` | Deprecated and removed | Kotlin Coroutines |
| `runBlocking` in production code | Blocks the calling thread | viewModelScope.launch |
| `GlobalScope.launch` | Not lifecycle-aware, causes leaks | viewModelScope.launch |
| `allowDestructiveMigration()` | Destroys user data on schema change | Write explicit Room Migration classes |
| Hardcoded UIDs or user data | Security and correctness risk | Always use FirebaseAuth.getInstance().currentUser?.uid |
| Firestore get() in ViewModel | Breaks MVVM, skips repository | All Firestore calls go through Repository |
| Network calls on main thread | ANR risk | All I/O in Dispatchers.IO coroutine |
| `notifyDataSetChanged()` | Full rebind, no diffing | DiffUtil.ItemCallback in ListAdapter |
| Hardcoded strings in XML | Breaks localization | Always use @string/ resource references |

---

## 7. Naming Conventions

### Kotlin Files
- `ViewModel`: `WorkoutSessionViewModel`
- `Repository`: `WorkoutSessionRepository`
- `Room DAO`: `WorkoutSessionDao`
- `Room Entity`: `WorkoutSessionEntity`
- `Fragment`: `ActiveSessionFragment`

### XML Files
- Layout: `fragment_active_session.xml`, `activity_main.xml`, `item_exercise.xml`
- Menu: `menu_home.xml`
- Navigation: `nav_graph.xml`

### View IDs in XML
- Buttons: `btn_start_workout`, `btn_finish_session`
- TextViews: `tv_session_timer`, `tv_exercise_name`
- EditTexts: `et_weight`, `et_reps`
- ImageViews: `iv_profile_photo`, `iv_pr_badge`
- RecyclerViews: `rv_exercises`, `rv_leaderboard`
- ViewGroups: `layout_empty_state`, `layout_streak_display`

---

*AGENTS.md v1.0 — RepRush Project | Updated April 2026*
