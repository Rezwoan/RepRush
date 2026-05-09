# RepRush — Project Scope and Feature List
**Complete Feature Inventory with Priority Tags | v1.0 | May 2026**

---

## 1. Priority Classification

- **P0** — Must-have for MVP. App is incomplete without it.
- **P1** — Important, ships in first post-demo update.
- **P2** — Nice-to-have, future roadmap only.
- **Effort:** S = under 4 hours, M = 4–8 hours, L = 1–2 days.

---

## 2. Admin Features

### Authentication and Role Routing

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-01 | Google Sign-In for admin | P0 | S | Same login flow as member, role field determines routing |
| A-02 | Role-based navigation after login | P0 | M | Admin sees dashboard shell, member sees home or pending screen |
| A-03 | Admin account pre-seeded in Firestore | P0 | S | No in-app admin registration path needed |

### Member Registration and Management

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-04 | View pending registration requests with applicant details | P0 | M | Core admin gate for new members |
| A-05 | Approve pending registration with package assignment | P0 | M | Approval triggers account activation and membership start |
| A-06 | Reject pending registration with reason | P0 | S | Applicant receives notification with reason |
| A-07 | Manual member registration for walk-in members | P0 | S | Front desk use case |
| A-08 | Full member directory with search and status filter | P0 | M | Primary member management screen |
| A-09 | View individual member profile (details, payments, attendance, stats) | P0 | L | Comprehensive member overview |
| A-10 | Suspend member account | P0 | S | Account access control |
| A-11 | Reactivate suspended member | P0 | S | Reverse suspension without full re-registration |
| A-12 | Permanently remove member | P0 | S | Data deletion with confirmation dialog |

### Membership Package Management

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-13 | Create membership package (name, price, duration, description) | P0 | M | Foundation of the billing system |
| A-14 | Edit existing package details | P0 | S | Prices and terms change over time |
| A-15 | Deactivate package without deleting it | P0 | S | Retire old plans without breaking existing member records |
| A-16 | Assign package to member during approval or renewal | P0 | S | Integrated into both approval and payment flows |

### Payment Recording and Receipts

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-17 | Record payment against a member (amount, method, date) | P0 | M | Core billing action |
| A-18 | Auto-generate digital receipt on payment recording | P0 | M | Professional gym operation standard |
| A-19 | Deliver receipt as in-app notification to member | P0 | S | Primary guaranteed receipt delivery channel |
| A-20 | Send receipt to member's email automatically | P1 | M | Secondary delivery channel, best-effort |
| A-21 | View full payment log with date range and member filters | P0 | M | Admin financial oversight |
| A-22 | Void an incorrect payment with a reason note | P0 | S | Error correction without data deletion |
| A-23 | Monthly and yearly revenue totals on dashboard | P0 | M | Business health at a glance |

### Membership Expiry and Renewal

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-24 | Dashboard widget showing expiring-in-7-days and already-expired | P0 | M | Proactive renewal management |
| A-25 | Configurable grace period before auto-suspension | P0 | S | Gym policy flexibility per owner preference |
| A-26 | Auto-suspend members past grace period | P0 | M | Automated enforcement without admin manual action |
| A-27 | Auto-notify members at 7 days, 3 days, and 0 days before expiry | P0 | M | Reduces admin manual follow-up workload |
| A-28 | Send manual reminder notification to a specific member | P0 | S | Personal follow-up when auto-reminders are insufficient |
| A-29 | Renew member by recording payment and assigning new period | P0 | S | Ties existing payment recording into renewal workflow |

### Attendance Tracking

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-30 | Mark member as present for today from quick-access screen | P0 | S | Daily attendance tracking |
| A-31 | View today's full check-in list on dashboard | P0 | S | Real-time gym occupancy awareness |
| A-32 | View individual member attendance history | P0 | M | Per-member engagement insight |
| A-33 | Aggregate attendance stats (avg visits, busiest days, inactive 14+ days list) | P0 | M | Gym health metrics for owner decisions |

### Announcements

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-34 | Post gym-wide announcement (title and body) | P0 | S | Communication channel to all active members |
| A-35 | Announcement appears as notification and home screen banner | P0 | S | Ensures visibility without extra member action |
| A-36 | Delete outdated announcements | P0 | S | Keeps the announcement feed clean |

### Admin Dashboard

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| A-37 | Dashboard home with 6 metric cards (active, pending, expiring, checked-in today, monthly revenue, yearly revenue) | P0 | L | Business command center, first screen after admin login |
| A-38 | Each metric card tappable to open the relevant detailed screen | P0 | S | Drill-down navigation from the dashboard |

---

## 3. Member Features

### Onboarding and Membership

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-01 | Google Sign-In with auto-creation of pending member account | P0 | S | Frictionless signup |
| M-02 | Pending approval waiting screen with gym contact info | P0 | S | Clear status communication while awaiting approval |
| M-03 | Onboarding questionnaire (fitness level, goal, equipment, injuries) | P0 | M | Data required for AI plan generation |
| M-04 | Membership status card on home screen (package, expiry, countdown) | P0 | M | Always-visible membership information |
| M-05 | Color-coded expiry countdown (green 10+ days, amber 3-10, red under 3) | P0 | S | Visual urgency for renewal |
| M-06 | Payment history with viewable receipts | P0 | M | Financial transparency for members |
| M-07 | In-app notification inbox (approvals, receipts, reminders, announcements) | P0 | M | Central notification hub |

### Exercise Library

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-08 | First-launch Wger API sync with progress screen | P0 | L | Professional exercise database with real images |
| M-09 | Searchable and filterable exercise list with exercise images | P0 | M | Core library browsing experience |
| M-10 | Exercise detail page with full image, muscle diagram, PR history, progress chart | P0 | M | Rich per-exercise information page |
| M-11 | Add custom exercise (name, muscle, equipment, category) | P0 | S | Flexibility for exercises not in Wger database |

### AI Workout Plan Generation

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-12 | 8-step plan generation questionnaire | P0 | M | Collects all inputs for AI prompt construction |
| M-13 | Gemini 1.5 Flash API call with exercise-constrained prompt | P0 | L | Core AI trainer feature |
| M-14 | Loading screen while waiting for Gemini response | P0 | S | Prevents user from thinking the app has frozen |
| M-15 | JSON validation and import to Room | P0 | M | Robust parsing with specific field-level error messages |
| M-16 | Plan summary and confirmation screen | P0 | S | Member reviews the plan before saving |
| M-17 | Multiple saved plans with active plan switching | P0 | M | Supports training periodization across phases |

### AI Trainer Chatbot

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-18 | Chat interface with Gemini API | P1 | L | Bonus AI feature for extra marks |
| M-19 | Chat context includes member profile and active plan | P1 | M | Enables personalized context-aware responses |
| M-20 | Persistent chat history in Room | P1 | S | Members can revisit past advice |

### Workout Logging

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-21 | Start session from active plan (exercises pre-loaded with images) | P0 | M | Primary daily use case |
| M-22 | Start blank session and add exercises manually | P0 | S | Free-form logging for unplanned workouts |
| M-23 | Log sets with weight, reps, and completion checkmark | P0 | L | Core logging interaction, most-used screen |
| M-24 | Warmup set flag excluding set from points, volume, and PRs | P0 | S | Accurate tracking of working volume |
| M-25 | Rest timer with foreground service notification | P0 | M | Works with screen off, standard gym feature |
| M-26 | Session notes free-text field | P0 | S | Contextual logging |
| M-27 | Auto-save session state every 30 seconds | P0 | S | Crash recovery protection |
| M-28 | Finish workout with session summary before confirming | P0 | M | Triggers all downstream calculations |
| M-29 | Post-workout celebration screen | P0 | M | Gamification payoff moment |

### PR Detection and Records

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-30 | Automatic PR detection on session finish per exercise per rep count | P0 | M | Central to the app value proposition |
| M-31 | 1RM estimation using Epley formula on every exercise with history | P0 | S | Computed from existing data at zero cost |
| M-32 | PR history per exercise viewable as list and line chart | P0 | S | Visual strength journey tracking |

### Gamification

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-33 | Points system (50 attend + 10/exercise + 2/set + 100/PR, 500 daily cap) | P0 | M | Core gamification loop |
| M-34 | Streak bonuses (200 pts at 7-day, 1000 pts at 30-day) | P0 | S | Retention mechanic |
| M-35 | Streak tracking based on planned workout days only | P0 | M | Streak resets on missed planned days |
| M-36 | Achievement badge system (11 badges minimum) | P0 | M | Long-term engagement beyond daily points |
| M-37 | Gym-wide monthly leaderboard | P0 | M | Competition within the RepRush gym community |
| M-38 | Leaderboard privacy toggle in settings | P0 | S | User trust and opt-out capability |

### Workout Volume Heatmap

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-39 | GitHub-style heatmap using Kizitonwose HeatMapCalendar | P0 | M | Distinctive visual feature, strong demo impact |
| M-40 | Cell color intensity based on total working volume per day | P0 | M | Normalizes volume against user's personal max |
| M-41 | Displays past 6 months with month and day labels | P0 | S | Same format as GitHub, immediately understood |
| M-42 | Day cell tap shows tooltip with date, volume, and exercise count | P0 | S | Interactive exploration of history |

### Progress and Analytics

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-43 | Progress tab with three sub-tabs: Overview, Body, Strength | P0 | M | Organized analytics by category |
| M-44 | Overview: heatmap, workout history list, weekly volume bar chart | P0 | M | Entry point to all progress data |
| M-45 | History calendar with color-coded days (completed, missed, rest) | P0 | M | Visual attendance pattern recognition |
| M-46 | Session detail view with all exercises, sets, warmup and PR flags | P0 | S | Review past sessions in full |
| M-47 | Per-exercise progress chart in exercise detail | P0 | M | Core motivation feature |
| M-48 | Body tab: weight log entry, line graph with rolling average, stat chips | P0 | M | Weight management and diet effectiveness |
| M-49 | Strength tab: Strength Score, history chart, per-lift trend cards | P0 | M | Overall strength growth in a single number |
| M-50 | 12-week total volume trend bar chart | P0 | M | Progressive overload verification |

### Profile and Settings

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| M-51 | Profile screen (stats, badges, active plan, membership card) | P0 | M | Member identity and achievement hub |
| M-52 | Edit fitness profile (updates onboarding answers) | P0 | S | Goals and conditions change over time |
| M-53 | Settings screen (rest timer, weight unit, leaderboard privacy, sign out) | P0 | M | App customization |

---

## 4. Technical Features

| ID | Feature | Priority | Effort | Rationale |
|---|---|---|---|---|
| T-01 | Offline-first operation (Room primary read, Firestore sync) | P0 | S | Gyms have poor WiFi — non-negotiable |
| T-02 | Cross-device sync via Firestore | P0 | M | Google login enables seamless device switching |
| T-03 | AI JSON schema versioning (schema_version field) | P0 | S | Future-proofs plan import against format changes |
| T-04 | Weight unit toggle (kg / lbs) | P0 | S | International usability |
| T-05 | Wger exercise library sync on first launch | P0 | L | Professional exercise images and data |
| T-06 | Gemini 1.5 Flash direct API integration | P0 | L | AI plan generation and chatbot |
| T-07 | Kizitonwose HeatMapCalendar integration | P0 | M | Volume heatmap visualization |
| T-08 | MPAndroidChart for progress charts | P0 | M | Line, bar, and sparkline charts throughout |

---

## 5. Achievement Badge Definitions

| Badge ID | Name | Unlock Condition |
|---|---|---|
| first_rep | First Rep | Log the first workout session |
| on_a_roll | On a Roll | Reach a 7-day streak |
| unstoppable | Unstoppable | Reach a 30-day streak |
| century | Century Club | Log 100 total workout sessions |
| pr_machine | PR Machine | Earn 10 total personal records |
| plan_master | Plan Master | Successfully import first AI-generated plan |
| heavy_bench | Bench Beast | Log a bench press working set at 100kg or above |
| heavy_squat | Squat Royalty | Log a squat working set at 100kg or above |
| heavy_deadlift | Iron Giant | Log a deadlift working set at 100kg or above |
| comeback | Comeback Kid | Log a workout after a gap of 14 or more days |
| full_house | Full House | Complete a session where every planned exercise has all sets done |

---

## 6. Feature Count Summary

| Category | P0 | P1 | P2 | Total |
|---|---|---|---|---|
| Admin Features | 38 | 1 | 0 | 39 |
| Member Features | 53 | 3 | 0 | 56 |
| Technical Features | 8 | 0 | 0 | 8 |
| **Total** | **99** | **4** | **0** | **103** |
