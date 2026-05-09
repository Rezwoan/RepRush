# RepRush — Task Breakdown and Development Roadmap
**Work Assignment: Rezwoan (R) and Dipon (D) | v1.0 | May 2026**

---

## Roadmap Philosophy

Every milestone ends with a fully demo-able app state. No milestone ends with a broken screen, placeholder layout, or unfinished flow. If a feature cannot be completed within a milestone it is deferred to the next one and never shipped half-built.

Work is divided by layer. **Rezwoan** owns the backend layer: Room schema, Firestore queries, repositories, ViewModels, API integrations (Gemini, Wger), and all data logic. **Dipon** owns the frontend layer: all XML layouts, Fragment classes, RecyclerView adapters, animations, and UI state handling. Both contribute to integration testing at the end of each milestone.

---

## Milestone 1 — Foundation

### Goal
Project builds, Firebase is connected, Google Sign-In works end-to-end, Room schema is fully defined, and role-based routing sends admin to the dashboard shell and member to the pending screen.

### Rezwoan's Tasks
- Create Android project with Kotlin. Enable ViewBinding in build.gradle. Configure compileSdk and targetSdk to latest stable.
- Add all Gradle dependencies: Firebase BOM (Auth, Firestore), Room with KSP processor, Jetpack Navigation Component, MPAndroidChart, Kizitonwose Calendar view, DataStore, Glide, Kotlin Coroutines.
- Set up Firebase project: enable Authentication with Google provider, enable Firestore, download and place google-services.json.
- Enable Firestore offline persistence in the Application class.
- Define the full Room database schema: all 16 entity data classes with correct annotations, all DAO interfaces with method signatures only, and the AppDatabase class at version 1 with all entities registered.
- Implement Google Sign-In using Firebase Auth SDK. Handle success (read role, route accordingly), failure (show Snackbar with message), and already-signed-in (skip to correct home).
- Implement role-checking logic: after successful login, read the role field from Firestore users/{uid}. Route to admin navigation or member navigation accordingly.
- Pre-seed one admin account document in Firestore manually with role set to admin.
- Write initial Firestore security rules covering the users collection.

### Dipon's Tasks
- Set up Material 3 XML theme (Theme.Material3.DayNight) in themes.xml. Define all color tokens in colors.xml. Define spacing and dimension values in dimens.xml.
- Set up nav_graph.xml with all Fragment destinations defined as empty placeholder Fragments.
- Build sign-in screen layout (activity_sign_in.xml): RepRush logo ImageView, tagline TextView, Google Sign-In MaterialButton, loading ProgressBar.
- Build member pending approval screen layout (fragment_pending.xml): status icon, status title TextView, message TextView, gym contact TextView.
- Build member suspended or expired status screen layout (fragment_status_blocked.xml): status icon, message TextView, contact gym Button.
- Build admin bottom navigation shell: BottomNavigationView with Dashboard, Members, Payments, Settings tabs.
- Build member bottom navigation shell: BottomNavigationView with Home, Workout, Progress, Profile tabs.
- Build member onboarding questionnaire layouts: 5 Fragment layouts with RadioGroups or ChipGroups for each step plus a progress indicator Toolbar.

### Demo State
App opens to Google Sign-In screen. Admin account routes to the admin navigation shell. New member account lands on the pending screen. After manually updating Firestore status to active, the member completes onboarding and reaches the home placeholder.

---

## Milestone 2 — Admin Core (Member and Package Management)

### Goal
Admin can manage the full member lifecycle: view pending requests, approve with a package, browse the member directory, create packages, and suspend or reactivate accounts.

### Rezwoan's Tasks
- Write Firestore listener for pending members: query users where membershipStatus == pending, ordered by createdAt.
- Implement member approval function: update Firestore status to active, write packageId, membershipStartDate, and membershipEndDate based on selected package.
- Implement member rejection function: update Firestore status to rejected, write notification to notifications/{uid}/items.
- Implement manual member registration: create a Firestore user document with pre-filled data from admin input.
- Implement member directory query: all users where role == member, with status filter support.
- Implement suspend, reactivate, and remove member functions with appropriate Firestore writes.
- Implement membership package CRUD: create, read, update, and deactivate operations in Firestore.
- Write ViewModels: PendingMembersViewModel, MemberDirectoryViewModel, MemberDetailViewModel, PackageViewModel.
- Write security rules for membership_packages collection.

### Dipon's Tasks
- Build PendingMembersFragment layout (fragment_pending_members.xml): RecyclerView of pending request cards each with name, email, submission date, approve button, and reject button.
- Build pending member item layout (item_pending_member.xml): all fields above with approve MaterialButton (filled) and reject MaterialButton (outlined).
- Build approval bottom sheet layout (layout_approval_sheet.xml): package spinner or DropdownMenu, start date picker, confirm button.
- Build MemberDirectoryFragment layout (fragment_member_directory.xml): search TextInputEditText, filter Chip group for status, RecyclerView of member items.
- Build member item layout (item_member.xml): CircleImageView avatar, name TextView, package name TextView, status Chip, arrow ImageView.
- Build MemberDetailFragment layout (fragment_member_detail.xml): profile header, membership card, TabLayout with Payment History and Attendance tabs.
- Build PackageListFragment layout (fragment_package_list.xml): RecyclerView of package cards, FAB to add new package.
- Build CreatePackageFragment layout (fragment_create_package.xml): name, price, duration, description TextInputEditText fields, save Button.
- Build empty states and loading indicators for all admin screens in this milestone.

### Demo State
Admin can create packages, view and approve or reject pending members, browse the member directory filtered by status, view a member's profile, and suspend or reactivate accounts.

---

## Milestone 3 — Admin Billing and Attendance

### Goal
Admin can record payments with receipt generation, track daily attendance, post announcements, and see the business dashboard with live metrics.

### Rezwoan's Tasks
- Implement payment recording: write to payments Firestore collection, update member's membershipEndDate in users document, generate unique receipt number.
- Implement in-app notification delivery: write receipt notification to notifications/{uid}/items after every payment recording.
- Implement email receipt sending via JavaMail API in a Dispatchers.IO coroutine. Failure is silent and does not block payment recording.
- Implement void payment function: update isVoided to true and write voidReason in Firestore.
- Implement payment log query: filterable by date range and member, ordered by createdAt DESC.
- Implement attendance marking: check for existing attendance entry for the member on today's date, write only if not already present.
- Implement attendance aggregate queries: average visits per week per member, busiest day of week, list of members with no attendance in 14+ days.
- Implement auto-suspension check: runs on admin app open, queries all active members with membershipEndDate past today plus grace period, updates status to suspended.
- Implement expiry notification scheduling: after every login or app foreground, check members with 7, 3, and 0 days remaining and write notifications if not already sent.
- Write ViewModels: PaymentViewModel, AttendanceViewModel, AdminDashboardViewModel.
- Write Firestore security rules for payments, attendance, announcements collections.

### Dipon's Tasks
- Build AdminDashboardFragment layout (fragment_admin_dashboard.xml): 2-column GridLayout of 6 MaterialCardViews (active members, pending, expiring, checked-in today, monthly revenue, yearly revenue), quick action row below, recent announcements section.
- Build RecordPaymentFragment layout (fragment_record_payment.xml): member AutoCompleteTextView, package spinner, amount TextInputEditText, payment method RadioGroup, date picker, record button.
- Build ReceiptViewFragment layout (fragment_receipt.xml): formatted receipt card with all receipt fields, download or share button.
- Build PaymentLogFragment layout (fragment_payment_log.xml): date range picker header, RecyclerView of payment records.
- Build payment item layout (item_payment.xml): member name, amount, method, date, receipt button.
- Build AttendanceFragment layout (fragment_attendance.xml): date header, RecyclerView of all active members with a Mark Present toggle per row.
- Build PostAnnouncementFragment layout (fragment_post_announcement.xml): title and body TextInputEditText, post button.
- Build member notification inbox layout (fragment_notifications.xml): RecyclerView of notification cards with type icon, title, body, timestamp, and unread accent border.

### Demo State
Admin can record a payment, see the receipt generated, and the member immediately receives it in their notification inbox. Admin can mark attendance, view aggregate stats, post announcements, and see the live dashboard with all six business metrics.

---

## Milestone 4 — Exercise Library and AI Plan Generation

### Goal
Member can browse the full Wger exercise library with professional images, generate an AI workout plan via Gemini, and save it.

### Rezwoan's Tasks
- Implement Wger API client: paginated GET requests to exercise, exerciseimage, exercisecategory, and muscle endpoints until all data is fetched.
- Implement exercise data normalization: trim whitespace, apply title casing, remove duplicates by name.
- Implement first-launch sync flow: check DataStore isLibrarySynced flag, show sync progress screen, paginate all endpoints, join exercises to images by wgerId, save all to Room, set flag to true.
- Implement Gemini API client: build HTTP request with API key, construct prompt string from questionnaire answers and equipment-filtered exercise names from Room, parse JSON response.
- Implement PlanImportValidator: validate all JSON fields against schema v1, return typed error list or success result.
- Implement PlanImportRepository: parse valid JSON, match exercise names to Room IDs (exact match on normalized name), write workout_plan, plan_days, and plan_exercises in a single Room transaction.
- Write ViewModels: ExerciseLibraryViewModel, ExerciseDetailViewModel, PlanGenerationViewModel.
- Store Gemini API key in local.properties and access via BuildConfig.GEMINI_API_KEY.

### Dipon's Tasks
- Build library sync loading screen layout (fragment_library_sync.xml): app logo, progress text TextView, exercise count TextView, CircularProgressIndicator.
- Build ExerciseLibraryFragment layout (fragment_exercise_library.xml): search TextInputEditText, horizontal HorizontalScrollView of muscle filter Chips, equipment filter Chip row, RecyclerView of exercise items, FAB for add custom.
- Build exercise item layout (item_exercise.xml): thumbnail ImageView loaded with Glide, name TextView, primary muscle TextView, equipment TextView, chevron ImageView.
- Build ExerciseDetailFragment layout (fragment_exercise_detail.xml): full-width exercise ImageView, muscle diagram ImageView, info section, PR section, MPAndroidChart LineChart for progress.
- Build AddCustomExerciseFragment layout (fragment_add_exercise.xml): name, muscle group, equipment, category input fields, save button.
- Build plan questionnaire layouts: 8 Fragment layouts navigated via ViewPager2 with a step indicator at the top.
- Build Gemini loading screen layout (fragment_gemini_loading.xml): animated indicator, generating your plan... text.
- Build PlanSummaryFragment layout (fragment_plan_summary.xml): plan name TextInputEditText, goal TextView, expandable RecyclerView of days with exercises, set as active button.
- Build PlanListFragment layout (fragment_plan_list.xml): RecyclerView of saved plan cards each with name, goal, active badge, switch and delete options.

### Demo State
On first launch after approval, exercises sync from Wger with images visible. Member can browse, search, and filter exercises with real photos. They can generate an AI plan via Gemini, see the parsed plan summarized with all exercises, save it, and set it as active.

---

## Milestone 5 — Workout Logging

### Goal
Member can log a complete workout session from plan or blank, including rest timer with foreground service, warmup sets, and session save.

### Rezwoan's Tasks
- Implement SessionRepository: load plan day exercises from Room for the start-from-plan flow, manage in-memory session state with a data class holding exercise and set lists.
- Implement session save transaction: write workout_session record then all logged_set records in a single Room transaction after Finish is confirmed.
- Implement rest timer foreground service: notification with exercise name, countdown, +30 seconds action, and skip action.
- Implement auto-save: serialize current session state to Room every 30 seconds using a repeating coroutine job in the ViewModel.
- Implement session recovery on app reopen: detect incomplete session in Room and offer to resume or discard.
- Write SessionViewModel: manages all live session state as LiveData, handles add exercise, add set, update set, mark warmup, mark complete, and finish events.

### Dipon's Tasks
- Build ActiveSessionFragment layout (fragment_active_session.xml): CoordinatorLayout with Toolbar showing elapsed timer TextView, NestedScrollView of exercise cards, ExtendedFloatingActionButton for add exercise, finish MaterialButton.
- Build exercise card layout (item_session_exercise.xml): exercise thumbnail ImageView, name TextView, collapse toggle, LinearLayout of set rows, add set TextButton.
- Build set row layout (item_set_row.xml): set number TextView, weight TextInputEditText, reps TextInputEditText, warmup FilterChip, complete ImageButton with AnimatedVectorDrawable.
- Build FinishWorkoutFragment layout (fragment_finish_workout.xml): duration, total sets, total volume summary cards, confirm finish MaterialButton, keep training OutlinedButton.
- Build rest timer BottomSheetDialogFragment layout (fragment_timer_sheet.xml): exercise name TextView, countdown TextView, CircularProgressIndicator, plus 30 seconds MaterialButton, skip MaterialButton.
- Build session notes collapsible TextInputEditText above the exercise list.

### Demo State
Member can start today's workout from home with all exercises pre-loaded with images, log sets with weight and reps, mark warmups, use the rest timer including with screen off, add extra exercises, and finish to see a session summary. All data saved correctly to Room.

---

## Milestone 6 — Gamification and Post-Workout

### Goal
Points, PRs, streaks, achievements, and leaderboard are all fully functional. Post-workout celebration screen is polished and motivating.

### Rezwoan's Tasks
- Implement PointsCalculator: pure Kotlin class with all point rules as named constants, daily cap logic, and streak bonus detection.
- Implement PRDetector: on session save, query Room for the historical max weight per exercise per rep count. Identify every set that exceeds the previous best.
- Implement PRRecordRepository: write new pr_records to Room, mark isPersonalRecord on each logged_set that set a PR.
- Implement streak logic: compare lastWorkoutDate in Room against today's date and the plan schedule to determine increment or reset.
- Implement AchievementChecker: evaluate all 11 badge conditions after every session save and on app open. Write to achievements table if newly unlocked.
- Integrate all gamification into the session finish flow: PointsCalculator, PRDetector, streak update, AchievementChecker, all run sequentially after Room session write.
- Implement Firestore leaderboard write: after points are calculated, run a Firestore transaction on leaderboard/monthly_{yyyy_MM}/{uid} to update points and prCount.
- Implement Firestore monthly points sync: update monthlyPoints and totalPoints on users/{uid} document.
- Write PostWorkoutViewModel, LeaderboardViewModel.

### Dipon's Tasks
- Build PostWorkoutFragment layout (fragment_post_workout.xml): tertiary container gradient background, animated points TextView (ValueAnimator), points breakdown LinearLayout, PR cards HorizontalScrollView, achievement badge cards HorizontalScrollView, streak update row, back to home MaterialButton.
- Build PR card layout (item_pr_card.xml): exercise name, rep count, weight with PR badge, scale-in entrance animation using ObjectAnimator.
- Build achievement card layout (item_achievement_card.xml): badge icon, badge name, unlock description, bounce animation using DynamicAnimation.
- Build GlobalLeaderboardFragment layout (fragment_leaderboard.xml): RecyclerView with rank number, CircleImageView avatar, name, points, PR count. User's own row highlighted and pinned at bottom if outside top 10.
- Build leaderboard item layout (item_leaderboard.xml): rank TextView, avatar ImageView, name TextView, points TextView.
- Update HomeFragment with real LiveData: streak display with flame ImageView, monthly points TextView, rank badge TextView, recent PRs row.
- Build member membership status card within HomeFragment: package name, expiry date, days remaining countdown, color-coded background.

### Demo State
Finishing a workout shows the celebration screen with animated points, new PRs scaling in, and achievement badges bouncing in. Home dashboard shows live streak and points. Leaderboard shows real Firestore data updating after workouts.

---

## Milestone 7 — Workout Volume Heatmap

### Goal
The GitHub-style volume heatmap is fully functional on both the home screen and the progress overview tab.

### Rezwoan's Tasks
- Write Room query: group logged_sets by calendar date, sum weight times reps for working completed sets, filter to past 6 months, return as `Map<LocalDate, Float>`.
- Implement intensity normalization in ViewModel: find the max volume day in the result, divide each day's volume by that max to get a 0.0–1.0 intensity float.
- Expose normalized map as LiveData from HeatmapViewModel. This ViewModel is shared between HomeFragment and ProgressOverviewFragment.

### Dipon's Tasks
- Add Kizitonwose HeatMapCalendar to the HomeFragment layout (fragment_home.xml): place between the streak display and the monthly points card.
- Add the same HeatMapCalendar to the ProgressOverviewFragment layout (fragment_progress_overview.xml).
- Implement the day binder in both Fragments: receive the intensity float for each date, interpolate color between Surface Variant (0.0), Primary Container (0.5), and Primary (1.0) using ArgbEvaluator, set as the cell background.
- Add month labels above column groups and M, W, F labels on the left side.
- Implement cell tap tooltip: show a small PopupWindow or Snackbar with date, total volume in kg, and exercise count for that day.
- Handle empty state: if fewer than 7 days of data, show a Start training to see your heatmap message below the calendar.

### Demo State
Both the home screen and the progress overview tab show the heatmap with real workout data. Cells are colored by volume intensity. Tapping a day shows the tooltip.

---

## Milestone 8 — Progress and Analytics

### Goal
Full Progress tab with all three sub-tabs (Overview, Body, Strength) functional with real data and charts.

### Rezwoan's Tasks
- Write Room query for per-exercise max weight over time: ordered by loggedAt for use in exercise progress chart.
- Write Room query for weekly sets per muscle group: group by week and primaryMuscle for the weekly volume bar chart.
- Implement body weight DAO: insert, getAll for user ordered by loggedDate, and getLatest for the current value display.
- Implement rolling average computation: 7-day average over body weight entries for the chart overlay.
- Implement Strength Score calculation: fetch latest oneRepMax from pr_records for each of the 5 key compound lifts, sum them.
- Implement 12-week total volume aggregation: sum weight times reps for working sets, grouped by ISO week number.
- Write ViewModels: ProgressOverviewViewModel, BodyViewModel, StrengthViewModel, ExerciseHistoryViewModel.

### Dipon's Tasks
- Build ProgressFragment layout (fragment_progress.xml): TabLayout with ViewPager2 holding Overview, Body, and Strength tabs.
- Build ProgressOverviewFragment layout (fragment_progress_overview.xml): HeatMapCalendar at top, workout history RecyclerView below, weekly volume MPAndroidChart BarChart at bottom.
- Build history item layout (item_workout_history.xml): date, day label, duration, volume, points TextView fields.
- Build SessionDetailFragment layout (fragment_session_detail.xml): session header card, RecyclerView of exercise groups each with their set rows, warmup and PR badges.
- Build set detail item layout (item_set_detail.xml): set number, weight, reps, warmup icon if applicable, PR badge if applicable.
- Build BodyFragment layout (fragment_body.xml): weight entry TextInputEditText with log button, MPAndroidChart LineChart with rolling average overlay, stat chips row (starting, current, monthly, all-time, lowest).
- Build StrengthFragment layout (fragment_strength.xml): Strength Score large TextView, level label, MPAndroidChart LineChart for score history, 5 per-lift CardViews each with 1RM, 30-day delta, sparkline chart.
- Build per-lift card layout (item_lift_card.xml): exercise name, current 1RM, delta TextView (green positive, red negative), mini MPAndroidChart LineChart.
- Build 12-week volume MPAndroidChart BarChart at bottom of StrengthFragment.

### Demo State
Full Progress tab functional across all three sub-tabs. Real data visible in all charts. Body weight entry works. Strength Score displays and updates.

---

## Milestone 9 — Social Profile, Settings, and AI Chatbot

### Goal
Member profile complete with all stats and badges, settings fully functional, and AI trainer chatbot integrated as the bonus feature.

### Rezwoan's Tasks
- Implement all-time stats computation from Room: total workouts count, total PRs count, total volume all-time, longest streak.
- Implement settings persistence in DataStore: rest timer default duration, auto-timer toggle, weight unit preference, leaderboard opt-in.
- Implement sign-out flow: clear all Room data, clear DataStore preferences, sign out from Firebase Auth, navigate to sign-in screen.
- Implement Gemini chatbot API call: construct system prompt with member fitness profile and active plan context, append conversation history from Room, call API, save response to Room.
- Write ChatViewModel: manages message list as LiveData, handles send message event.

### Dipon's Tasks
- Build ProfileFragment layout (fragment_profile.xml): 80dp CircleImageView avatar, display name TextView, stats row (workouts, PRs, streak, points in small CardViews), achievements GridLayout with 4 columns, active plan CardView, membership summary CardView, edit and settings OutlinedButtons.
- Build achievement item layout (item_achievement.xml): badge icon ImageView (colored if unlocked, grey with lock overlay if not), badge name TextView below.
- Build achievement detail dialog layout (dialog_achievement.xml): badge icon, name, description, unlock date if unlocked.
- Build ProfileEditFragment layout (fragment_profile_edit.xml): pre-filled questionnaire same as onboarding, save button.
- Build SettingsFragment layout (fragment_settings.xml): rest timer SeekBar with value label, auto-timer Switch, weight unit RadioGroup, leaderboard Switch, sign out MaterialButton.
- Build ChatFragment layout (fragment_chat.xml): RecyclerView of messages, typing indicator animation view, message TextInputEditText, send ImageButton.
- Build user message item layout (item_message_user.xml): right-aligned bubble with message text.
- Build AI message item layout (item_message_ai.xml): left-aligned bubble with RepRush trainer avatar, message text.

### Demo State
Profile shows all stats and achievement badges correctly. Settings persist across app restarts. AI chatbot responds with personalized fitness advice using the member's plan as context.

---

## Milestone 10 — Polish and Demo Preparation

### Goal
App is visually polished, handles all edge cases with proper states, and is loaded with realistic demo data ready for the teacher demonstration.

### Rezwoan's Tasks
- Seed demo member data: 4 weeks of workout history, PRs on at least 5 exercises, a 14-day streak, 6 achievement badges unlocked, body weight entries for 3 weeks.
- Seed admin demo data: 2 membership packages, 5 payment records for different members, 3 weeks of attendance data across 4 demo members.
- Test Gemini plan generation end-to-end with a real API call and verify the JSON response parses correctly.
- Test Wger sync on a fresh install emulator.
- Test offline behavior: disable WiFi, log a workout, re-enable WiFi, verify Firestore sync completes.
- Test on a physical device for real-world performance and smooth scrolling.
- Fix any crash or data error found during testing.

### Dipon's Tasks
- Audit every RecyclerView screen for an empty state layout with icon, message, and call-to-action button.
- Audit every screen that loads async data for a loading state using CircularProgressIndicator.
- Audit every screen that writes to Firestore for an error state using Snackbar with retry action.
- Apply consistent Material 3 Expressive motion: MaterialSharedAxis for Fragment transitions, ValueAnimator for numeric reveals, DynamicAnimation spring for badge entrances.
- Test light mode and dark mode on every screen and fix any contrast or overflow issues.
- Add contentDescription to every ImageView and ImageButton that does not already have one.
- Verify all touch targets are at minimum 48x48dp.
- Prepare the demo script covering the full admin flow and the full member flow in sequence.
