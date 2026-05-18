# Project Title: RepRush

**Course:** Mobile Application Development [A]

**Instructor:** Md. Khairul Alam Mazumder

**Group Information:**

| Name | Student ID |
|------|------------|
| Din Muhammad Rezwoan | 23-51712-2 |
| Soumik Das Dipon | 23-51709-2 |

---

## 1. Project Overview

RepRush is an Android gym management app for two users: the gym admin and gym members. Admins manage members, billing, attendance, and view business metrics. Members get AI-generated workout plans, log workouts, track progress, and compete on a leaderboard.

Built with Kotlin, MVVM, Room, Firebase Firestore, Firebase Auth, and the Google Gemini API.

---

## 2. Functional Requirement

### Type of Users

- **Admin** - Gym owner who manages the business side of the gym.
- **Member** - Registered gym member who uses the app for fitness.

### Common Functionality

- Google Sign-In via Firebase Authentication
- In-app notification inbox
- Real-time data sync across devices
- Sign out

### User Specific Functionality

**Admin**

- View, approve, and reject member registration requests
- Manually register walk-in members
- Browse and manage the full member directory (search, filter by status)
- Suspend, reactivate, or remove member accounts
- Create, edit, and deactivate membership packages
- Record payments and auto-generate digital receipts (in-app + email)
- View and void payment records
- Mark daily attendance and view attendance stats
- Send gym-wide announcements
- Configure auto-suspension grace period
- View live dashboard: active members, pending registrations, expiring memberships, daily check-ins, monthly and yearly revenue

**Member**

- Self-register and await admin approval
- View membership card with expiry countdown
- Generate a personalized AI workout plan via an 8-step questionnaire (Gemini API)
- Switch between multiple saved plans
- Browse the Wger exercise library and add custom exercises
- Log workouts: sets, reps, weight, warmup flags, session notes
- Rest timer that runs in the background with screen off
- Auto-save active session every 30 seconds for crash recovery
- Automatic personal record detection per exercise and rep count
- View Strength Score (sum of estimated 1RM across 5 compound lifts)
- Progress analytics: workout heatmap, body weight chart, strength trends, session history
- Points, streaks, 11 achievement badges, and a monthly gym leaderboard
- AI fitness chatbot (Gemini)

---

## 3. Challenges & Difficulties

**Dual-database sync** - Room handles offline fitness data; Firestore handles shared admin state. Keeping both consistent without conflicts required careful repository design and ordered writes.

**Offline-first with selective sync** - Member features must work offline; admin operations must not. Enforcing this boundary cleanly across the repository and ViewModel layers was non-trivial.

**Parsing AI workout output** - Gemini returns free-form text. Extracting structured plan data reliably required a strict JSON prompt schema and a fallback parser for inconsistent responses.

**Rest timer foreground service** - The timer must survive screen-off and app backgrounding. Binding the service to the fragment lifecycle with correct teardown on session end required handling multiple edge cases.

**Crash recovery for active sessions** - Auto-saving session state to Room every 30 seconds and restoring it on re-launch required the ViewModel to detect and resume interrupted sessions.

**Heatmap rendering** - Processing 6 months of daily workout volume data and rendering it as a color-intensity calendar without blocking the UI required careful coroutine scoping and a dedicated transformation step.

**Live admin dashboard** - Managing 6 simultaneous Firestore listeners with proper lifecycle handling and merging their results into a single coherent UI state required a dedicated aggregation pattern in the ViewModel.
