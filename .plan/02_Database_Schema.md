# RepRush — Database Schema
**Room (Local) and Firebase Firestore (Cloud) | Data Architecture v1.0 | May 2026**

---

## 1. Overview and Storage Strategy

RepRush uses a dual-database architecture. Room (SQLite) stores all local fitness data for speed and offline reliability. Firebase Firestore stores all data that must be shared between users or accessed across devices: member profiles, membership status, payments, attendance records, leaderboard entries, and announcements.

Firebase Authentication provides the UID that links both databases. The same UID serves as the primary key in Room tables and as the Firestore document ID under the users collection.

The admin role is entirely cloud-driven. Admin screens read and write exclusively to Firestore because they manage data across all members. Member fitness features (workout logging, plan viewing, PR history, heatmap data) read from Room first and write to both Room and Firestore in parallel.

---

## 2. Room (Local SQLite) Schema

### 2.1 users

Locally cached user profile. Populated from Firestore on login. Contains records for both admin and member accounts.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | Firebase UID |
| displayName | TEXT | NOT NULL | Google display name or custom override |
| email | TEXT | NOT NULL | Google account email |
| photoUrl | TEXT | NULLABLE | Google profile photo URL |
| role | TEXT | NOT NULL | admin or member |
| fitnessLevel | TEXT | NULLABLE | Beginner / Intermediate / Advanced (members only) |
| primaryGoal | TEXT | NULLABLE | Strength / Hypertrophy / Fat Loss / Endurance / General |
| availableEquipment | TEXT | NULLABLE | Full Gym / Barbell+DB / DB Only / Bodyweight |
| injuries | TEXT | NULLABLE | Free text injury and restriction notes |
| membershipStatus | TEXT | NOT NULL DEFAULT 'pending' | pending / active / expired / suspended |
| packageId | TEXT | NULLABLE FK | Currently assigned membership package ID |
| membershipStartDate | TEXT | NULLABLE | yyyy-MM-dd of current period start |
| membershipEndDate | TEXT | NULLABLE | yyyy-MM-dd of current period expiry |
| onboardingComplete | INTEGER | NOT NULL DEFAULT 0 | 0 = incomplete, 1 = complete |
| createdAt | INTEGER | NOT NULL | Unix timestamp of account creation |

### 2.2 membership_packages

Defines all available membership plans. Created and managed by admin only. Cached locally for display on member screens.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| name | TEXT | NOT NULL | Package display name e.g. Monthly Basic |
| price | REAL | NOT NULL | Price in local currency |
| durationDays | INTEGER | NOT NULL | Duration of the package in days |
| description | TEXT | NULLABLE | What the package includes |
| isActive | INTEGER | NOT NULL DEFAULT 1 | 0 if deactivated by admin |
| createdAt | INTEGER | NOT NULL | Unix timestamp |

### 2.3 payment_records

Every payment recorded by admin. Serves as the source of truth for receipt generation. Cached locally for the member payment history screen.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID, also serves as the receipt number |
| memberId | TEXT | FK users.id NOT NULL | Member who made the payment |
| packageId | TEXT | FK membership_packages.id NOT NULL | Package the payment covers |
| amount | REAL | NOT NULL | Amount paid |
| paymentMethod | TEXT | NOT NULL | cash / bank_transfer / mobile_banking |
| paymentDate | TEXT | NOT NULL | yyyy-MM-dd of actual payment |
| periodStart | TEXT | NOT NULL | yyyy-MM-dd membership period start |
| periodEnd | TEXT | NOT NULL | yyyy-MM-dd membership period end |
| isVoided | INTEGER | NOT NULL DEFAULT 0 | 1 if admin has voided this payment |
| voidReason | TEXT | NULLABLE | Reason text if voided |
| recordedBy | TEXT | FK users.id NOT NULL | Admin UID who recorded the payment |
| createdAt | INTEGER | NOT NULL | Unix timestamp of recording |

### 2.4 attendance_logs

Daily gym attendance records marked by admin. One entry per member per calendar day maximum.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| memberId | TEXT | FK users.id NOT NULL | Member who attended |
| date | TEXT | NOT NULL UNIQUE per memberId | yyyy-MM-dd, enforced unique per member |
| markedBy | TEXT | FK users.id NOT NULL | Admin UID who marked attendance |
| createdAt | INTEGER | NOT NULL | Unix timestamp |

### 2.5 announcements

Gym-wide announcements posted by admin. Cached locally so members see them offline after initial sync.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| title | TEXT | NOT NULL | Announcement headline |
| body | TEXT | NOT NULL | Full announcement text |
| postedBy | TEXT | FK users.id NOT NULL | Admin UID who posted |
| isActive | INTEGER | NOT NULL DEFAULT 1 | 0 if deleted by admin |
| createdAt | INTEGER | NOT NULL | Unix timestamp |

### 2.6 exercises

Full exercise library pulled from Wger API on first launch. Includes all image and muscle group data needed for display.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| wgerId | INTEGER | NULLABLE | Wger API exercise ID used for image joining during sync |
| name | TEXT | NOT NULL UNIQUE | Exercise name, normalized (trimmed and title-cased), case-insensitive unique |
| primaryMuscle | TEXT | NOT NULL | Primary muscle group |
| secondaryMuscles | TEXT | NULLABLE | Comma-separated secondary muscles |
| equipment | TEXT | NOT NULL | Equipment required |
| category | TEXT | NOT NULL | Push / Pull / Legs / Hinge / Core / Cardio |
| imageUrl | TEXT | NULLABLE | Wger full-size exercise image URL |
| thumbnailUrl | TEXT | NULLABLE | Wger thumbnail URL for list views |
| muscleImageUrl | TEXT | NULLABLE | Wger muscle diagram URL |
| isCustom | INTEGER | NOT NULL DEFAULT 0 | 1 if created by the user |
| isVerified | INTEGER | NOT NULL DEFAULT 1 | 0 if auto-created during AI plan import |

### 2.7 workout_plans

AI-generated workout plans saved after successful Gemini response import.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| userId | TEXT | FK users.id NOT NULL | Plan owner |
| planName | TEXT | NOT NULL | AI-generated or user-edited plan title |
| goal | TEXT | NOT NULL | Training goal from the AI JSON |
| totalWeeks | INTEGER | NOT NULL | Plan duration in weeks |
| daysPerWeek | INTEGER | NOT NULL | Number of training days per week |
| schemaVersion | INTEGER | NOT NULL DEFAULT 1 | AI JSON schema version for compatibility |
| isActive | INTEGER | NOT NULL DEFAULT 0 | Only one plan active per user at a time |
| createdAt | INTEGER | NOT NULL | Unix timestamp of import |

### 2.8 plan_days

Individual training days belonging to a workout plan.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| planId | TEXT | FK workout_plans.id NOT NULL | Parent plan |
| dayNumber | INTEGER | NOT NULL | 1-based day index within the weekly schedule |
| dayLabel | TEXT | NOT NULL | e.g. Push A, Lower Body, Full Body |

### 2.9 plan_exercises

Exercises prescribed for each training day, including rep and rest targets from the AI.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| dayId | TEXT | FK plan_days.id NOT NULL | Parent training day |
| exerciseId | TEXT | FK exercises.id NOT NULL | Linked exercise from library |
| sets | INTEGER | NOT NULL | Prescribed number of sets |
| repsRange | TEXT | NOT NULL | e.g. 8-10 or 5 |
| restSeconds | INTEGER | NOT NULL | Rest between sets in seconds |
| orderIndex | INTEGER | NOT NULL | Display order within the day |
| notes | TEXT | NULLABLE | AI-provided coaching notes |

### 2.10 workout_sessions

Completed or in-progress workout session records.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| userId | TEXT | FK users.id NOT NULL | Session owner |
| planDayId | TEXT | FK plan_days.id NULLABLE | Null for unplanned or blank sessions |
| startTime | INTEGER | NOT NULL | Unix timestamp of session start |
| endTime | INTEGER | NULLABLE | Null until the Finish button is confirmed |
| notes | TEXT | NULLABLE | User's free-text session notes |
| totalPoints | INTEGER | NOT NULL DEFAULT 0 | Points earned in this session |
| isCompleted | INTEGER | NOT NULL DEFAULT 0 | 1 after Finish is confirmed |

### 2.11 logged_sets

Individual sets logged within a workout session. This table is also the source for the heatmap volume query.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| sessionId | TEXT | FK workout_sessions.id NOT NULL | Parent session |
| exerciseId | TEXT | FK exercises.id NOT NULL | Exercise performed |
| setNumber | INTEGER | NOT NULL | 1-based set index within the exercise |
| weight | REAL | NOT NULL | Weight in kg, always stored as kg regardless of display unit |
| reps | INTEGER | NOT NULL | Repetitions performed |
| isWarmup | INTEGER | NOT NULL DEFAULT 0 | 1 excludes from points, volume, PR calculations, and heatmap |
| isCompleted | INTEGER | NOT NULL DEFAULT 0 | 1 after the checkmark is tapped |
| isPersonalRecord | INTEGER | NOT NULL DEFAULT 0 | Set to 1 by PR detection on session finish |
| loggedAt | INTEGER | NOT NULL | Unix timestamp of set completion |

**Heatmap query against this table:**
```sql
SELECT DATE(loggedAt / 1000, 'unixepoch') as workoutDate,
       SUM(weight * reps) as totalVolume
FROM logged_sets
WHERE userId = :userId
  AND isWarmup = 0
  AND isCompleted = 1
  AND loggedAt >= :sixMonthsAgoTimestamp
GROUP BY workoutDate
ORDER BY workoutDate ASC
```

### 2.12 pr_records

Personal record history stored per exercise per rep count.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| userId | TEXT | FK users.id NOT NULL | PR owner |
| exerciseId | TEXT | FK exercises.id NOT NULL | Exercise the PR is for |
| repCount | INTEGER | NOT NULL | Rep count this PR applies to (5-rep PR and 8-rep PR are separate) |
| weight | REAL | NOT NULL | PR weight in kg |
| oneRepMax | REAL | NOT NULL | Epley-estimated 1RM at time of PR: weight x (1 + reps / 30) |
| achievedAt | INTEGER | NOT NULL | Unix timestamp |
| sessionId | TEXT | FK workout_sessions.id NOT NULL | Session where PR was achieved |

### 2.13 body_weight_logs

Daily body weight entries for the body analytics section.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| userId | TEXT | FK users.id NOT NULL | Log owner |
| weightKg | REAL | NOT NULL | Body weight always stored in kg |
| loggedDate | TEXT | NOT NULL UNIQUE per userId | ISO date string yyyy-MM-dd |
| loggedAt | INTEGER | NOT NULL | Unix timestamp of entry creation |

### 2.14 streaks

One row per user tracking their current and longest workout streak.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| userId | TEXT | PRIMARY KEY, FK users.id | One row per user |
| currentStreak | INTEGER | NOT NULL DEFAULT 0 | Current consecutive planned-day streak |
| longestStreak | INTEGER | NOT NULL DEFAULT 0 | All-time best streak |
| lastWorkoutDate | TEXT | NULLABLE | yyyy-MM-dd of last completed planned session |

### 2.15 achievements

Unlocked achievement badge records per user.

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| userId | TEXT | FK users.id NOT NULL | Achievement owner |
| badgeId | TEXT | NOT NULL | Badge identifier e.g. first_rep, pr_machine, streak_7 |
| unlockedAt | INTEGER | NOT NULL | Unix timestamp of unlock |

### 2.16 chat_messages

Chat message history for the AI trainer chatbot (bonus feature).

| Column | Type | Constraints | Purpose |
|---|---|---|---|
| id | TEXT | PRIMARY KEY | UUID |
| userId | TEXT | FK users.id NOT NULL | Chat owner |
| role | TEXT | NOT NULL | user or assistant |
| content | TEXT | NOT NULL | Message text |
| createdAt | INTEGER | NOT NULL | Unix timestamp |

---

## 3. Firebase Firestore Schema

### 3.1 users/{uid}

Primary user document. Single source of truth for user state across devices. Members can only read their own document. Admins can read and write all documents.

**Fields:** displayName, email, photoUrl, role (admin or member), fitnessLevel, primaryGoal, availableEquipment, injuries, membershipStatus, packageId, membershipStartDate, membershipEndDate, totalPoints, monthlyPoints, currentStreak, longestStreak, totalWorkouts, totalPRs, leaderboardOptIn, onboardingComplete, createdAt

### 3.2 membership_packages/{packageId}

All available membership packages. Readable by all authenticated users. Writable only by admin.

**Fields:** name, price, durationDays, description, isActive, createdAt

### 3.3 payments/{paymentId}

Every payment record. Flat collection for admin aggregate queries. Members query with a filter on their own memberId.

**Fields:** memberId, packageId, amount, paymentMethod, paymentDate, periodStart, periodEnd, isVoided, voidReason, recordedBy, receiptNumber (same as document ID), createdAt

### 3.4 attendance/{attendanceId}

Daily check-in records. Flat collection so admin can run aggregate queries across all members.

**Fields:** memberId, date, markedBy, createdAt

### 3.5 announcements/{announcementId}

Gym-wide announcements. Readable by all authenticated users. Writable only by admin.

**Fields:** title, body, postedBy, isActive, createdAt

### 3.6 leaderboard/monthly_{yyyy_MM}/{uid}

Monthly leaderboard entry per member. Written on every workout completion that awards points. Queried with `orderBy("points", DESCENDING)` for leaderboard display.

**Fields:** displayName, photoUrl, points, prCount, updatedAt

### 3.7 notifications/{uid}/items/{notificationId}

Per-user notification inbox. Admin actions (approval, payment receipt, announcements, reminders, suspension) write entries here. Members read and mark as read.

**Fields:** type (approval / receipt / announcement / reminder / suspension), title, body, isRead, metadata (JSON string containing receipt details, package info, etc.), createdAt

---

## 4. Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper: check if requesting user is admin
    function isAdmin() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }

    // Users: member reads/writes own, admin reads/writes all
    match /users/{uid} {
      allow read, write: if request.auth.uid == uid || isAdmin();
    }

    // Packages: all authenticated read, admin-only write
    match /membership_packages/{pkgId} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }

    // Payments: member reads own, admin reads and writes all
    match /payments/{payId} {
      allow read: if request.auth.uid == resource.data.memberId || isAdmin();
      allow write: if isAdmin();
    }

    // Attendance: admin reads and writes all, members cannot access
    match /attendance/{attId} {
      allow read, write: if isAdmin();
    }

    // Announcements: all authenticated read, admin-only write
    match /announcements/{annId} {
      allow read: if request.auth != null;
      allow write: if isAdmin();
    }

    // Leaderboard: all authenticated read, owner-only write
    match /leaderboard/{month}/{uid} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == uid;
    }

    // Notifications: owner-only read, admin writes all
    match /notifications/{uid}/items/{notifId} {
      allow read: if request.auth.uid == uid;
      allow write: if isAdmin() || request.auth.uid == uid;
    }
  }
}
```

---

## 5. Indexing and Query Strategy

| Query | Collection | Index Required | Notes |
|---|---|---|---|
| Global leaderboard ordered by points | leaderboard/monthly_{date} | points DESC | Composite index on collection group |
| All payments for a member | payments | memberId + createdAt DESC | For member payment history screen |
| All payments in a date range | payments | paymentDate ASC | For admin payment log filtered by date |
| All active members | users | role + membershipStatus | For admin member directory |
| Members expiring soon | users | membershipEndDate ASC | For admin expiry dashboard |
| All attendance for one member | attendance | memberId + date DESC | For member attendance history |
| Today's attendance | attendance | date == today | For admin check-in list |
| Session history for user | Room: workout_sessions | userId + startTime DESC | Room DAO index |
| PRs for exercise | Room: pr_records | userId + exerciseId + achievedAt | Compound Room index |
| Volume per day for heatmap | Room: logged_sets | userId + loggedAt + isWarmup | Room DAO, grouped by date |

---

## 6. Design Decision Notes

- **Payments are a flat collection** rather than nested under each user document. This allows the admin to query all payments across all members in one Firestore query, which is not possible with per-user subcollections without multiple fetches.
- **Attendance is also flat** for the same reason. Admin aggregate stats (busiest day, inactive members) require cross-user access that flat collections support efficiently.
- **Workout logs stay local-only in Room.** Only aggregates (totalPoints, totalPRs, currentStreak) sync to Firestore. This avoids Firestore write costs proportional to every set logged. The heatmap data is computed from local Room data only.
- **Notifications use a per-user subcollection** so each member's inbox is isolated. Admin actions write to the target member's subcollection path.
- **All weights stored in kg** regardless of display preference. The conversion happens at the presentation layer only. This prevents data inconsistency if a user changes their unit preference after logging history.
- **Wger exercise data is cached permanently** in Room after the first sync. No recurring API dependency after initial setup.
- **The heatmap volume query groups logged_sets by calendar date** and sums weight times reps for all non-warmup completed sets. The ViewModel normalizes the results to a 0.0–1.0 intensity scale before passing to the Fragment.
