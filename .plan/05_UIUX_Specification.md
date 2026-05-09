# RepRush — UI/UX Specification
**Design System and Screen Definitions | Material 3 Expressive | v1.0 | May 2026**

---

## 1. Design System

The app follows Google's Material 3 Expressive design language. All components use the `Theme.Material3.DayNight` base XML theme with dynamic color support enabled on Android 12+ devices. The design emphasizes spring-based motion, rounded expressive shapes, bold typography, and high-contrast color coding to communicate status clearly.

---

### 1.1 Color Palette

| Token | Hex | Usage |
|---|---|---|
| Primary | #1B6CA8 | Buttons, active nav items, FAB, primary actions, high-volume heatmap cells |
| On Primary | #FFFFFF | Text and icons on primary-colored surfaces |
| Primary Container | #D1E4FF | Selected chips, card highlights, PR badges, medium-volume heatmap cells |
| On Primary Container | #001D36 | Text on primary container surfaces |
| Secondary | #535F70 | Secondary actions, less prominent UI elements |
| Secondary Container | #D7E3F7 | Filter chips selected state, secondary badges |
| Tertiary | #6B5778 | Streak flame icon, PR celebration accent |
| Tertiary Container | #F2DAFF | Achievement badge backgrounds, celebration screen gradient |
| Background | #1A1C1E | Main screen background (dark mode default) |
| Surface | #1A1C1E | Card and bottom sheet backgrounds |
| Surface Variant | #43474E | Input field backgrounds, dividers, empty heatmap cells |
| On Surface | #E2E2E6 | Primary text on all surface colors |
| On Surface Variant | #C3C7CF | Secondary text, captions, timestamps |
| Error | #FFB4AB | Validation errors, suspended status, expired membership |
| Outline | #8D9199 | Borders, separators, input field outlines |
| Points Yellow | #FFD700 | Points total display, streak counter number |
| PR Green | #4CAF50 | PR badges, positive trend indicators, active membership status |
| Miss Red | #F44336 | Missed days on calendar, negative trends, expired membership |
| Admin Accent | #2196F3 | Admin dashboard metric cards, admin-specific UI accents |

---

### 1.2 Typography Scale

| Style | Font | Size | Weight | Usage |
|---|---|---|---|---|
| Display Medium | Roboto | 45sp | Regular | Strength Score number, revenue total |
| Headline Large | Roboto | 32sp | Bold | Screen titles, post-workout points total |
| Headline Medium | Roboto | 28sp | Bold | Section headings, dashboard metric numbers |
| Headline Small | Roboto | 24sp | Bold | Card titles, exercise names in active session |
| Title Large | Roboto | 22sp | Medium | Toolbar titles |
| Title Medium | Roboto | 16sp | Medium | Card subtitles, section labels |
| Body Large | Roboto | 16sp | Regular | Primary body text, exercise descriptions |
| Body Medium | Roboto | 14sp | Regular | Secondary body text, notes, receipt details |
| Body Small | Roboto | 12sp | Regular | Captions, timestamps, tooltip text |
| Label Large | Roboto | 14sp | Medium | Button text |
| Label Small | Roboto | 11sp | Medium | Badge text, small chips, heatmap labels |

---

### 1.3 Spacing System

Base unit: 4dp. All spacing values are multiples of 4dp.

| Token | Value | Usage |
|---|---|---|
| spacing_xs | 4dp | Icon padding, chip internal padding |
| spacing_sm | 8dp | Between related elements within a card |
| spacing_md | 16dp | Card internal padding, standard section spacing |
| spacing_lg | 24dp | Between major sections on a screen |
| spacing_xl | 32dp | Screen edge padding on large screens |
| spacing_xxl | 48dp | Post-workout celebration element spacing |

---

### 1.4 Shape Scale

| Token | Corner Radius | Applied To |
|---|---|---|
| shape_xs | 4dp | Chips, small badges, heatmap day cells |
| shape_sm | 8dp | Input fields, small info cards |
| shape_md | 12dp | Standard cards, exercise items, notification cards |
| shape_lg | 16dp | Bottom sheets, modal dialogs |
| shape_xl | 24dp | Large feature cards, plan cards, dashboard metric cards |
| shape_full | 50% pill | FAB, navigation bar pill indicator, toggle buttons |

---

### 1.5 Naming Conventions for All View IDs

Every View ID in every XML layout file must follow these conventions without exception.

| View Type | Prefix | Example |
|---|---|---|
| TextView | `textView_` | `textView_memberName` |
| EditText / TextInputEditText | `editText_` | `editText_weight` |
| Button / MaterialButton | `button_` | `button_startWorkout` |
| ImageView | `imageView_` | `imageView_exercisePhoto` |
| ImageButton | `imageButton_` | `imageButton_completeSet` |
| RecyclerView | `recyclerView_` | `recyclerView_exercises` |
| FloatingActionButton | `fab_` | `fab_addExercise` |
| ExtendedFloatingActionButton | `fab_` | `fab_newPackage` |
| Chip / FilterChip | `chip_` | `chip_muscleFilter` |
| MaterialCardView | `card_` | `card_membershipStatus` |
| LinearLayout / ConstraintLayout / FrameLayout | `layout_` | `layout_streakDisplay` |
| ProgressBar / CircularProgressIndicator | `progressBar_` | `progressBar_loading` |
| HorizontalScrollView | `scrollView_` | `scrollView_prCards` |
| TabLayout | `tabLayout_` | `tabLayout_progress` |
| ViewPager2 | `viewPager_` | `viewPager_onboarding` |
| BottomNavigationView | `bottomNav_` | `bottomNav_member` |
| Switch | `switch_` | `switch_leaderboardOptIn` |
| RadioGroup | `radioGroup_` | `radioGroup_paymentMethod` |
| SeekBar | `seekBar_` | `seekBar_restTimer` |
| HeatMapCalendar | `heatmap_` | `heatmap_workoutVolume` |
| Icons in drawable | `icon_` | `icon_streak_flame`, `icon_home_filled` |
| Menu XML files | `menu_` | `menu_member_home` |
| Layout XML files | See below | See below |

**Layout file naming:**
- Screens: `fragment_screen_name.xml`, `activity_screen_name.xml`
- RecyclerView items: `item_thing_name.xml`
- Dialogs and bottom sheets: `dialog_name.xml`, `sheet_name.xml`
- Reusable partial layouts: `partial_name.xml`

---

### 1.6 Animation Guidelines — Material 3 Expressive

| Interaction | Animation | Duration | Implementation |
|---|---|---|---|
| Fragment navigation | MaterialSharedAxis (Z axis) | 300ms | Set as enterTransition and exitTransition on Fragment |
| Screen fade | MaterialFadeThrough | 250ms | For tab switches within ViewPager2 |
| PR card entrance | Scale from 0.8 + fade in | 400ms | ObjectAnimator on scaleX, scaleY, alpha |
| Points count-up | Numeric animation | 600ms | ValueAnimator from 0 to final value |
| Set completion checkmark | Animated vector draw | 200ms | AnimatedVectorDrawable on imageButton_completeSet |
| FAB extend to label | Material built-in | 300ms | ExtendedFloatingActionButton.extend() |
| Achievement badge unlock | Scale bounce | 500ms | Spring via DynamicAnimation with custom stiffness |
| Rest timer countdown | Circular sweep | Per second | CircularProgressIndicator setProgressCompat |
| Bottom sheet expand | Spring slide up | 250ms | BottomSheetBehavior built-in |
| Dashboard card entrance | Stagger fade | 400ms | LayoutAnimation with 80ms delay between items |
| Heatmap cell color | Instant on bind | N/A | Color set directly in DayBinder, no animation needed |
| Revenue number reveal | Count-up | 800ms | ValueAnimator from 0 to actual revenue value |

---

## 2. Navigation Structure

### 2.1 Admin Navigation

BottomNavigationView with 4 tabs. All admin screens are Fragments within the admin section of nav_graph.xml. The admin never accesses member-facing screens.

| Tab | Icon Drawable | Fragment | Description |
|---|---|---|---|
| Dashboard | icon_dashboard | AdminDashboardFragment | Business metrics, expiry alerts, quick actions |
| Members | icon_people | MemberDirectoryFragment | Full member list, pending requests tab |
| Payments | icon_payments | PaymentLogFragment | All payments, record new, revenue overview |
| Settings | icon_settings_admin | AdminSettingsFragment | Grace period, sign out |

### 2.2 Member Navigation

BottomNavigationView with 4 tabs. All member screens are Fragments within the member section of nav_graph.xml.

| Tab | Icon Drawable | Fragment | Description |
|---|---|---|---|
| Home | icon_home_filled / icon_home_outline | HomeFragment | Today's workout, heatmap, streak, membership |
| Workout | icon_fitness_center | WorkoutFragment | Plans, exercise library, AI plan generation |
| Progress | icon_bar_chart_filled / icon_bar_chart_outline | ProgressFragment | History, charts, body weight, strength |
| Profile | icon_person_filled / icon_person_outline | ProfileFragment | Stats, badges, settings, chatbot access |

### 2.3 Full Screen Map

**Common screens:**
- SignInActivity -> (role check) -> AdminDashboardFragment or HomeFragment or PendingFragment

**Admin navigation:**
- AdminDashboardFragment -> PendingMembersFragment -> (approval dialog) -> MemberDirectoryFragment
- AdminDashboardFragment -> AttendanceFragment
- MemberDirectoryFragment -> MemberDetailFragment -> RecordPaymentFragment -> ReceiptViewFragment
- MemberDetailFragment -> AttendanceHistoryFragment
- PaymentLogFragment -> RecordPaymentFragment -> ReceiptViewFragment
- AdminSettingsFragment -> PostAnnouncementFragment

**Member navigation:**
- HomeFragment -> ActiveSessionFragment -> FinishWorkoutFragment -> PostWorkoutFragment -> HomeFragment
- WorkoutFragment -> PlanListFragment -> PlanSummaryFragment
- WorkoutFragment -> PlanQuestionnaireFragment (8 steps) -> GeminiLoadingFragment -> PlanSummaryFragment
- WorkoutFragment -> ExerciseLibraryFragment -> ExerciseDetailFragment
- ExerciseLibraryFragment -> AddCustomExerciseFragment
- ProgressFragment -> HistoryCalendarFragment -> SessionDetailFragment
- ProgressFragment -> ExerciseDetailFragment
- ProfileFragment -> ProfileEditFragment
- ProfileFragment -> SettingsFragment
- ProfileFragment -> ChatFragment
- ProfileFragment -> NotificationsFragment
- ProfileFragment -> MembershipDetailFragment

---

## 3. Screen Specifications

### 3.1 AdminDashboardFragment

**Purpose:** Gym owner's daily command center. First screen after admin login.

**Layout:** NestedScrollView > LinearLayout (vertical) > greeting section + 2-column GridLayout of metric cards + quick actions row + announcements section.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Greeting | TextView | textView_adminGreeting | Good morning, Admin. Based on time of day. |
| Active members card | MaterialCardView | card_activeMembers | Large number in Headline Medium. Subtext: active members. Taps to member directory filtered to active. |
| Pending requests card | MaterialCardView | card_pendingRequests | Number in amber color if greater than 0. Taps to pending members list. |
| Expiring soon card | MaterialCardView | card_expiringSoon | Number in amber. Subtext: in next 7 days. Taps to filtered member list. |
| Checked in today card | MaterialCardView | card_checkedInToday | Number updated in real time from Firestore listener. Taps to today's attendance list. |
| Monthly revenue card | MaterialCardView | card_monthlyRevenue | Amount with count-up animation on screen entry. Taps to payment log filtered to this month. |
| Yearly revenue card | MaterialCardView | card_yearlyRevenue | Amount. Taps to payment log filtered to this year. |
| Quick action: Mark Attendance | MaterialButton | button_quickAttendance | Opens AttendanceFragment. |
| Quick action: Record Payment | MaterialButton | button_quickPayment | Opens RecordPaymentFragment with no pre-selected member. |
| Quick action: Register Member | MaterialButton | button_quickRegister | Opens manual registration form. |
| Announcements section | LinearLayout | layout_announcements | Latest 2 announcements as cards. Post New button at bottom. |

**States:**
- Loading: CircularProgressIndicator centered, metric cards hidden.
- Loaded: all 6 metric cards visible with real data.
- Offline: cached Firestore data shown. Small textView_syncBanner at top reads Showing cached data. Sync in progress.

---

### 3.2 MemberDirectoryFragment

**Purpose:** Searchable, filterable list of all members. Primary member management hub.

**Layout:** CoordinatorLayout > AppBarLayout (search + filters) + RecyclerView.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Search field | TextInputEditText | editText_searchMembers | Filters list in real time via TextWatcher. Hint: Search by name or email. |
| Status filter: All | FilterChip | chip_filterAll | Default selected. Shows all members. |
| Status filter: Active | FilterChip | chip_filterActive | Shows only active members. |
| Status filter: Pending | FilterChip | chip_filterPending | Shows pending registrations. Badge shows count. |
| Status filter: Expired | FilterChip | chip_filterExpired | Shows expired members. |
| Status filter: Suspended | FilterChip | chip_filterSuspended | Shows suspended members. |
| Member list | RecyclerView | recyclerView_members | item_member.xml per row. Tap opens MemberDetailFragment. |
| Register FAB | FloatingActionButton | fab_registerMember | Opens manual registration bottom sheet. |
| Empty state | LinearLayout | layout_emptyMembers | Icon, No members match your search TextView, Clear Filters button. |

**Member item (item_member.xml):**
- imageView_memberAvatar: 48dp circle, Glide-loaded from photoUrl, fallback to initials drawable.
- textView_memberName: Headline Small.
- textView_packageName: Body Medium, secondary color.
- chip_memberStatus: FilterChip with status text and color (green active, amber expired, red suspended, blue pending).
- imageView_chevron: icon_chevron_right.

---

### 3.3 MemberDetailFragment

**Purpose:** Full profile view of a member. Admin's primary tool for managing one account.

**Layout:** NestedScrollView > profile header card + membership card + tab layout with Payment History and Attendance tabs.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Avatar | ImageView | imageView_detailAvatar | 80dp circle. |
| Member name | TextView | textView_detailName | Headline Medium. |
| Member email | TextView | textView_detailEmail | Body Medium, secondary color. |
| Status chip | Chip | chip_detailStatus | Color coded. Tappable to change status. |
| Package name | TextView | textView_packageName | Current package. Body Large. |
| Membership start | TextView | textView_membershipStart | yyyy-MM-dd. |
| Membership end | TextView | textView_membershipEnd | yyyy-MM-dd. Color coded by proximity. |
| Days remaining | TextView | textView_daysRemaining | X days remaining or Expired in red. |
| Suspend button | MaterialButton (outlined) | button_suspendMember | Shown only if member is active. Confirmation dialog before executing. |
| Reactivate button | MaterialButton (filled) | button_reactivateMember | Shown only if member is suspended. |
| Remove button | MaterialButton (outlined, error color) | button_removeMember | Confirmation dialog. Permanent. |
| Send reminder button | MaterialButton (text style) | button_sendReminder | Opens reminder message input dialog. |
| Record payment button | MaterialButton | button_recordPayment | Navigates to RecordPaymentFragment pre-filled with this member. |
| Payment history tab | RecyclerView | recyclerView_payments | item_payment.xml. Each row: date, amount, method, receipt icon. |
| Attendance tab | RecyclerView | recyclerView_attendanceHistory | item_attendance_day.xml. Calendar grid of attendance dots. |

---

### 3.4 RecordPaymentFragment

**Purpose:** Admin records a cash, bank, or mobile banking payment for a member.

**Layout:** NestedScrollView > form card.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Member selector | AutoCompleteTextView | editText_selectMember | Searches active members in real time. Pre-filled if navigated from MemberDetailFragment. |
| Package selector | MaterialSpinner / ExposedDropdownMenu | editText_selectPackage | Shows all active packages with price and duration. |
| Amount field | TextInputEditText | editText_paymentAmount | inputType: numberDecimal. Pre-filled with package price. Editable. |
| Payment method | RadioGroup | radioGroup_paymentMethod | Radio buttons: Cash, Bank Transfer, Mobile Banking. |
| Payment date | TextInputEditText | editText_paymentDate | Shows date picker dialog on tap. Defaults to today. |
| Coverage period preview | TextView | textView_coveragePeriod | Auto-computed from package duration. Updates as package selection changes. |
| Record payment button | MaterialButton | button_recordPayment | Disabled until all fields are valid. Shows loading spinner during Firestore write. |

**States:**
- Success: Navigate to ReceiptViewFragment.
- Error (network): Snackbar with Failed to record payment. Retry button.
- Error (validation): Inline error messages below each invalid field.

---

### 3.5 HomeFragment (Member)

**Purpose:** Member's daily entry point. Shows today's workout, heatmap, membership status, streak, and points.

**Layout:** NestedScrollView > LinearLayout (vertical) > greeting + announcement banner + membership card + heatmap card + today's workout card + streak row + points card + recent PRs row + body weight prompt.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Greeting | TextView | textView_memberGreeting | Good morning, Aryan. Time-aware. |
| Announcement banner | MaterialCardView | card_announcement | Latest gym announcement. Dismiss button in top-right corner. Gone if no active announcement. |
| Membership status card | MaterialCardView | card_membershipStatus | Package name, expiry date, days remaining countdown. Background tint changes by proximity: transparent = 10+ days, amber = 3-10 days, error = under 3 days or expired. Tap opens MembershipDetailFragment. |
| Heatmap card | MaterialCardView | card_heatmap | Card wrapping the HeatMapCalendar. Title: Your Workout Volume. Month labels above, M/W/F labels left. |
| Heatmap calendar | HeatMapCalendar | heatmap_workoutVolume | Kizitonwose HeatMapCalendar. Displays past 26 weeks. Color by volume intensity. Cell tap shows tooltip. |
| Heatmap empty label | TextView | textView_heatmapEmpty | Start training to see your workout history. Visible only if fewer than 7 days of data. |
| Today's workout card | MaterialCardView | card_todayWorkout | Day label, exercise count, estimated duration. Start Workout filled button inside. |
| No plan card | MaterialCardView | card_noPlan | Shown when no active plan. Generate a Plan filled button navigates to questionnaire. |
| Rest day card | MaterialCardView | card_restDay | Shown on scheduled rest days. Motivational message. No start button. |
| Streak layout | LinearLayout | layout_streak | icon_streak_flame ImageView + textView_streakCount (Points Yellow) + textView_streakLabel. Gone if streak is 0. |
| Monthly points card | MaterialCardView | card_monthlyPoints | textView_monthlyPoints in Points Yellow. textView_rankBadge showing hash + rank + globally. |
| Recent PRs row | LinearLayout | layout_recentPrs | Last 3 PRs as rows: exercise name + weight x reps. Tap each to navigate to ExerciseDetailFragment. Gone if no PRs yet. |
| Body weight prompt | MaterialCardView (outlined) | card_logWeightPrompt | textView_logWeightMessage + button_logWeight. Visible only if no weight logged today. |

**States:**
- Loading: Skeleton card placeholders using shimmer effect or ProgressBar overlay.
- Loaded: all content visible.
- Membership expired: card_membershipStatus shows error red. card_todayWorkout replaced with textView_expiredMessage and button_contactGym.
- No plan active: card_noPlan shown, card_todayWorkout gone.
- Rest day: card_restDay shown, card_todayWorkout gone.
- Offline: All data served from Room. Heatmap functional from Room. textView_syncBanner shown at top.

---

### 3.6 ActiveSessionFragment

**Purpose:** Primary workout logging screen. Used during the actual gym session.

**Layout:** CoordinatorLayout > Toolbar (elapsed timer + finish button) + NestedScrollView of exercise cards + ExtendedFloatingActionButton.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Elapsed timer | TextView | textView_elapsedTimer | mm:ss counting up from session start. Updated every second via Handler. |
| Finish button | MaterialButton | button_finish | Top right of Toolbar. Navigates to FinishWorkoutFragment. |
| Exercise card | MaterialCardView | card_exercise_{index} | Contains exercise image, name, set rows, add set button. Collapsible via header tap. |
| Exercise image | ImageView | imageView_exercisePhoto_{index} | Wger image loaded with Glide. 120dp height, full width, scaleType: centerCrop. |
| Exercise name | TextView | textView_exerciseName_{index} | Headline Small. |
| Set row | ConstraintLayout | layout_setRow_{exerciseIndex}_{setIndex} | One row per set. |
| Set number | TextView | textView_setNumber_{e}_{s} | Body Medium, secondary color. |
| Weight input | TextInputEditText | editText_weight_{e}_{s} | inputType: numberDecimal. Hint: last logged weight in kg. |
| Reps input | TextInputEditText | editText_reps_{e}_{s} | inputType: number. Hint: planned rep range from plan. |
| Warmup chip | FilterChip | chip_warmup_{e}_{s} | Label W. Row alpha reduces to 0.5 when checked. |
| Complete button | ImageButton | imageButton_completeSet_{e}_{s} | AnimatedVectorDrawable plays on tap. Triggers rest timer bottom sheet. |
| Add set | MaterialButton (text style) | button_addSet_{e} | Appends new row pre-filled from previous set's values. |
| Add exercise FAB | ExtendedFloatingActionButton | fab_addExercise | Opens exercise library bottom sheet for selection. |
| Rest timer sheet | BottomSheetDialogFragment | Separate Fragment | Auto-shown after set completion. See 3.8. |

**States:**
- Active: all exercise cards visible. Timer running.
- Empty session (no plan): only FAB visible initially. fab_addExercise floats above hint textView_emptySession.
- Saving: Progress overlay shown during Room transaction. Button disabled.

---

### 3.7 PostWorkoutFragment

**Purpose:** Celebration screen after workout completion. Full-screen, non-scrollable, festive.

**Layout:** FrameLayout with tertiary container gradient background > centered LinearLayout.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Points earned header | TextView | textView_pointsEarned | Display Medium size. ValueAnimator count-up from 0 to total over 600ms. Points Yellow color. |
| Points label | TextView | textView_pointsLabel | points earned this session. Body Large. |
| Breakdown layout | LinearLayout | layout_pointsBreakdown | Attendance row + per-exercise row + per-set row + PR bonus row. Each a horizontal row with label and value. |
| PR cards scroll | HorizontalScrollView | scrollView_prCards | Horizontally scrollable. One card per new PR. Animated with ObjectAnimator scale and fade on entry. |
| PR card | MaterialCardView | card_pr_{index} | item_pr_card.xml: exercise name, rep count, weight, PR badge ImageView. |
| Achievement cards | HorizontalScrollView | scrollView_achievementCards | Horizontally scrollable. Gone if no achievements unlocked this session. |
| Achievement card | MaterialCardView | card_achievement_{index} | Badge icon, badge name, unlocked text. Bounce animation via DynamicAnimation. |
| Streak update | LinearLayout | layout_streakUpdate | imageView_streakFlame + textView_newStreak + day streak label. Visible if streak increased. |
| Back to home | MaterialButton | button_backHome | Navigates to HomeFragment, clears back stack. |

---

### 3.8 Rest Timer Bottom Sheet

**Purpose:** Automatically displayed after a set is marked complete. Counts down the configured rest duration.

**Layout:** BottomSheetDialogFragment > LinearLayout.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Exercise label | TextView | textView_restExerciseName | Shows name of exercise just completed. Body Medium, secondary color. |
| Countdown | TextView | textView_restCountdown | mm:ss counting down. Headline Large. |
| Progress indicator | CircularProgressIndicator | progressBar_restTimer | Sweeps from full to empty as countdown progresses. |
| Add 30 seconds | MaterialButton (outlined) | button_addThirtySeconds | Adds 30 seconds to remaining time. |
| Skip | MaterialButton (text style) | button_skipRest | Dismisses sheet immediately. |

**Behavior:** Sheet auto-dismisses when countdown reaches 0:00. Vibrates device at 0. If the user navigates away from the screen, the rest timer continues in the foreground service notification.

---

### 3.9 ExerciseLibraryFragment

**Purpose:** Full exercise database with search and filter. Used during session and standalone browsing.

**Layout:** CoordinatorLayout > AppBarLayout (search + filters) + RecyclerView + FAB.

| Element | Type | ID | Behavior |
|---|---|---|---|
| Search field | TextInputEditText | editText_searchExercises | Filters list in real time. Hint: Search exercises... inputType: text. |
| Muscle filter chips | HorizontalScrollView > LinearLayout of FilterChip | chip_muscle_{groupName} | All, Chest, Back, Shoulders, Biceps, Triceps, Legs, Core. Single select. Deselecting returns to All. |
| Equipment filter chips | HorizontalScrollView > LinearLayout of FilterChip | chip_equipment_{typeName} | All, Barbell, Dumbbell, Cable, Machine, Bodyweight. Single select. |
| Exercise list | RecyclerView | recyclerView_exercises | item_exercise.xml. LinearLayoutManager. Tap: navigate to ExerciseDetailFragment. |
| Add custom FAB | FloatingActionButton | fab_addCustomExercise | Navigates to AddCustomExerciseFragment. |
| Empty state | LinearLayout | layout_emptyExercises | imageView_emptyIcon + textView_emptyTitle (No exercises found) + textView_emptySub (Try clearing filters) + button_clearFilters. |
| Loading state | CircularProgressIndicator | progressBar_exerciseLoading | Centered. Visible only during initial Room query. |

---

### 3.10 ExerciseDetailFragment

**Purpose:** Full information page for one exercise. Includes image, muscles, PR history, and progress chart.

**Layout:** NestedScrollView > LinearLayout (vertical).

| Element | Type | ID | Behavior |
|---|---|---|---|
| Exercise image | ImageView | imageView_exerciseFull | Full width, 200dp height, scaleType: centerCrop. Loaded from Wger imageUrl via Glide. Placeholder while loading. |
| Muscle diagram | ImageView | imageView_muscleDiagram | Full width, 120dp height. Loaded from Wger muscleImageUrl via Glide. |
| Exercise name | TextView | textView_exerciseDetailName | Headline Large. |
| Primary muscle | TextView | textView_primaryMuscle | Body Large. Label: Primary: followed by muscle name. |
| Secondary muscles | TextView | textView_secondaryMuscles | Body Medium, secondary color. Hidden if null. |
| Equipment | TextView | textView_equipment | Body Medium. |
| Category | TextView | textView_exerciseCategory | Body Small. |
| Custom badge | Chip | chip_customBadge | Label: Custom. Visible only if isCustom is true. |
| Unverified badge | Chip | chip_unverifiedBadge | Label: Unverified. Visible only if isVerified is false. |
| All-time PR label | TextView | textView_prLabel | Body Medium bold: All-Time PR. |
| PR value | TextView | textView_prValue | Headline Small in PR Green. Shows best weight x reps. No history yet if empty. |
| Estimated 1RM | TextView | textView_oneRepMax | Body Medium. Estimated 1RM: X kg. Hidden if fewer than 3 sessions. |
| Progress chart | LineChart (MPAndroidChart) | chart_exerciseProgress | Weight over time. X axis: dates. Y axis: kg. Toggle between max weight, estimated 1RM, and total volume. Minimum 2 data points to render. |
| Chart toggle | SegmentedButton group | layout_chartToggle | Max Weight, Est 1RM, Volume options. |
| PR history list | RecyclerView | recyclerView_prHistory | item_pr_record.xml: date, reps x weight, estimated 1RM. Sorted newest first. |
| Insufficient data | TextView | textView_insufficientData | Log more sessions to see your progress chart. Visible only when fewer than 2 data points. |

---

### 3.11 AI Plan Questionnaire Flow

8 Fragment screens navigated via ViewPager2. Progress indicator Toolbar shows Step X of 8.

| Step | Fragment ID | Input Type | Question |
|---|---|---|---|
| 1 | PlanGoalFragment | ChipGroup single select | What is your primary goal? (Strength, Hypertrophy, Fat Loss, Endurance, General Fitness) |
| 2 | PlanDaysFragment | ChipGroup single select | How many days per week? (3, 4, 5, 6) |
| 3 | PlanSplitFragment | ChipGroup single select | Preferred split? (Push-Pull-Legs, Upper-Lower, Full Body, Bro Split, Custom) |
| 4 | PlanDurationFragment | ChipGroup single select | Session duration? (45 min, 60 min, 90 min) |
| 5 | PlanEquipmentFragment | ChipGroup single select | Equipment available? (Full Gym, Barbell+Dumbbells, Dumbbells Only, Bodyweight) |
| 6 | PlanLevelFragment | ChipGroup single select | Fitness level? (Beginner, Intermediate, Advanced) |
| 7 | PlanWeeksFragment | ChipGroup single select | Plan duration? (4 weeks, 8 weeks, 12 weeks) |
| 8 | PlanInjuriesFragment | TextInputEditText multiline | Any injuries or restrictions? (Optional free text) |

Each screen: Next MaterialButton (disabled until selection made), Back MaterialButton always enabled, progress bar at top.

Gemini loading screen (GeminiLoadingFragment): RepRush logo, textView_generatingMessage (Generating your personalized plan...), indeterminate CircularProgressIndicator, textView_generatingHint (This may take a few seconds). Retry button and error textView shown if API call fails.

---

### 3.12 HeatMapCalendar Implementation Details

**Library:** `com.kizitonwose.calendar:view:2.5.0`

**Color mapping:**

| Intensity Range | Color Token | Meaning |
|---|---|---|
| No session (null) | Surface Variant #43474E | No workout that day |
| 0.0 to 0.33 | Primary Container #D1E4FF | Low volume workout |
| 0.34 to 0.66 | Midpoint interpolated between Primary Container and Primary | Medium volume workout |
| 0.67 to 1.0 | Primary #1B6CA8 | High volume workout |

**Interpolation code:**
```kotlin
val color = ArgbEvaluatorCompat.getInstance().evaluate(
    intensity, // 0.0f to 1.0f
    ContextCompat.getColor(context, R.color.primaryContainer),
    ContextCompat.getColor(context, R.color.primary)
)
```

**Day cell layout (item_heatmap_day.xml):**
- Root: FrameLayout, fixed size 14dp x 14dp, corner radius 4dp (shape_xs)
- No text inside the cell. Date shown only in tooltip on tap.

**Labels:**
- Month labels: textView style Label Small above each month's column group.
- Day labels: M, W, F textViews at Label Small to the left of the grid rows.

**Tooltip on cell tap:**
- PopupWindow anchored to the tapped cell.
- Shows: textView_tooltipDate (Body Small bold), textView_tooltipVolume (Body Small: X kg total), textView_tooltipExerciseCount (Body Small: X exercises).
- Dismisses on tap outside.
- ContentDescription for accessibility: Date {date}. Volume: {volume}kg. {exerciseCount} exercises.

---

### 3.13 Progress Tab Structure

**Layout:** Fragment with TabLayout + ViewPager2.

| Tab | Fragment | Key Content |
|---|---|---|
| Overview | ProgressOverviewFragment | HeatMapCalendar (full 6-month view), workout history list RecyclerView, weekly volume BarChart |
| Body | BodyFragment | Weight entry TextInputEditText + log button, LineChart with rolling average overlay, stat chips row |
| Strength | StrengthFragment | Strength Score large textView + level label Chip, score history LineChart, 5 per-lift cards, 12-week volume BarChart |

---

### 3.14 ChatFragment — AI Trainer Chatbot

**Purpose:** Member converses with the Gemini-powered AI trainer.

**Layout:** ConstraintLayout > recyclerView_chatMessages (fills space) + layout_chatInput (pinned to bottom).

| Element | Type | ID | Behavior |
|---|---|---|---|
| Chat messages | RecyclerView | recyclerView_chatMessages | LinearLayoutManager. Scrolls to bottom automatically after each new message. |
| User message item | item_message_user.xml | Right-aligned bubble. Primary Container background. textView_userMessage inside. |
| AI message item | item_message_ai.xml | Left-aligned bubble. Surface Variant background. imageView_aiAvatar (24dp RepRush icon) + textView_aiMessage. |
| Typing indicator | LinearLayout | layout_typingIndicator | Three dots animation. Visible while Gemini API call is in progress. Gone otherwise. |
| Message input | TextInputEditText | editText_chatMessage | Hint: Ask your AI trainer... inputType: textMultiLine. maxLines: 4. |
| Send button | ImageButton | imageButton_sendMessage | icon_send drawable. Disabled while API call in progress. |

**States:**
- Empty chat: textView_chatWelcome centered saying Hi! I am your AI trainer. Ask me anything about your workouts.
- Loading (API call): layout_typingIndicator visible, imageButton_sendMessage disabled.
- Error (API failed): Snackbar: Could not reach AI trainer. Check your connection and try again.
- Chat history loaded from Room on Fragment start.

---

## 4. Accessibility Requirements

| Element | Requirement | Implementation |
|---|---|---|
| All ImageViews and ImageButtons | contentDescription required | Set in XML or set programmatically when content is dynamic |
| Exercise images | Dynamic description | contentDescription = {exerciseName} exercise demonstration |
| Heatmap cells | Dynamic description per cell | contentDescription = {date}. Volume: {volume}kg. {count} exercises |
| All tap targets | Minimum 48x48dp | Use minHeight and minWidth attributes. Apply Modifier-equivalent padding in XML. |
| Charts (MPAndroidChart) | Non-visual summary | setContentDescription dynamically: Bench press trend: up 15kg over 3 months |
| Color-only information | Must have secondary indicator | Expired membership: red background AND Expired label text. PR: green badge AND PR text label. Missed day: red AND X icon. |
| All TextInputEditTexts | Hint and error text | android:hint always set. Use TextInputLayout setError() for validation errors. |
| RecyclerView items | Focusable and clickable | android:focusable=true, android:clickable=true on root view of item layouts |
| Bottom navigation items | Labels always visible | Never set labelVisibilityMode to unlabeled |
| Dialogs and bottom sheets | Focus trap | BottomSheetDialogFragment and AlertDialog handle this automatically |
| Font scaling | No layout breakage at 200% | All text sizes in sp. No fixed-height containers that clip text. Use wrap_content for text containers. |

---

## 5. Edge Case Requirements

### Authentication and Onboarding

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Sign-in | Google Sign-In cancelled by user | Return to sign-in screen silently. No error message. |
| Sign-in | Network unavailable | Snackbar: No internet connection. Please try again. Retry button. |
| Sign-in | Firebase Auth service unavailable | Snackbar: Sign-in service is currently unavailable. Try again later. |
| Pending screen | Admin approves while member has app open | Firestore listener detects status change. Automatically navigate to onboarding without requiring app restart. |
| Onboarding | User taps back on step 1 | Navigate back to pending screen. Onboarding answers not saved until final step. |
| Onboarding | App force-closed on step 4 of 5 | On reopen, resume from step 1. Answers not partially saved to prevent inconsistent state. |

### Admin - Member Management

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Pending list | No pending requests | Empty state: imageView_emptyIcon + textView_emptyPending: No pending registrations. All caught up. |
| Approval dialog | Admin selects no package and taps confirm | Package field shows inline error: Please select a membership package. Confirm button stays disabled. |
| Member directory | All members filtered out by search and status | Empty state with textView: No members match your search. Clear Filters button. |
| Remove member | Tap remove on the only admin account | This action is blocked. Admin cannot remove another admin or themselves through the member screen. |
| Suspend member | Member currently has an active session open | Session is saved as-is. Suspension takes effect on their next app open. |

### Admin - Payments and Billing

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Record payment | Network goes offline mid-write | Firestore offline persistence queues the write. Show: Payment saved. Will sync when back online. |
| Record payment | Amount entered as 0 | Inline error below editText_paymentAmount: Amount must be greater than 0. |
| Record payment | Same member already has an active payment for overlapping period | Warning dialog: This member's current membership does not expire until {date}. Proceeding will extend their period. Continue? |
| Payment log | No payments recorded yet | Empty state: No payments recorded yet. Use the Record Payment button to get started. |
| Void payment | Void a payment that is the member's only active period | Warning dialog: Voiding this payment will leave this member with no active membership. Their account will show as expired. Continue? |
| Email receipt | Member has no email address on record | In-app notification still delivered. Email send skipped silently. No error shown to admin. |
| Revenue cards | No payments recorded for this month | Revenue cards show 0 with a Start recording payments message on first tap. |

### Admin - Attendance

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Mark attendance | Member already marked present today | button_markPresent disabled and shows Already checked in today label. |
| Attendance list | No members registered | Empty state: No active members to check in. |
| Attendance history | Member has never attended | Empty state: No attendance recorded for this member yet. |

### Member - Exercise Library and Sync

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Library sync | Network unavailable on first launch | Show: Could not load exercise library. Please connect to the internet and try again. Retry button. App cannot proceed to main features until sync succeeds. |
| Library sync | Wger API returns partial data (API error mid-pagination) | Stop pagination. Save what was received. Show: Library partially loaded. Some exercises may be missing. You can retry in Settings. |
| Library sync | Sync completes but 0 exercises returned | Show: Exercise library appears empty. Please try again later. Do not set isLibrarySynced flag so it retries next launch. |
| Exercise library | Search returns 0 results | Empty state with clear filters button. |
| Exercise detail | Wger image URL fails to load | Glide fallback to icon_exercise_placeholder drawable. No broken image icon shown. |
| Exercise detail | Exercise has no PR history | PR section shows textView_noPrYet: No personal records yet. Start logging to set your first PR. Chart section hidden. |
| Add custom exercise | Name already exists in library (case-insensitive) | Inline error: An exercise with this name already exists. |

### Member - AI Plan Generation

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Plan questionnaire | User has no equipment selected from profile | Equipment step pre-selects Bodyweight as the safest fallback. User can change. |
| Gemini API call | Network timeout (over 30 seconds) | Show: Your AI trainer is taking too long to respond. Check your connection and try again. Retry button. |
| Gemini API call | API key invalid or quota exceeded | Show: AI trainer is temporarily unavailable. Please try again later. |
| Gemini response | Returns malformed JSON | Show: Your AI trainer returned an unexpected response. Try generating your plan again. |
| Gemini response | Returns JSON but exercise names do not match library | Auto-create unverified exercises for unmatched names. Import succeeds. Post-import screen lists auto-created exercises with a note: These exercises were not found in your library and were added as custom. Please verify them. |
| Plan import | All exercises in the plan are unverified | Allow import. Show warning on plan summary: This plan uses {count} unverified exercises. Review them in your library. |
| Plan import | JSON has 0 schedule days | Reject import. Show: The plan has no training days. Please regenerate your plan. |

### Member - Workout Logging

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Active session | App force-closed mid-session | Auto-save from 30-second interval is the recovery point. On next open, show dialog: You have an unfinished session from {time}. Resume or Discard? |
| Active session | User tries to complete a set with empty weight or reps | Shake animation on the empty field. Inline hint in red: Please enter a value before completing this set. |
| Active session | Weight input receives a non-numeric value | TextInputLayout error: Please enter a valid number. |
| Active session | Session has 0 working sets on Finish | Warning dialog: You have not logged any working sets. Sessions without working sets earn 0 points. Finish anyway or Keep Training? |
| Finish workout | Session is under 10 minutes | Allow save. Show Snackbar on post-workout screen: Sessions under 10 minutes do not earn attendance points. |
| Rest timer | App goes to background during countdown | Timer continues in foreground service notification. Notification shows time remaining. |
| Rest timer | User dismisses the notification | Timer continues in background. Sheet reopens if user returns to the active session screen before time expires. |
| Add exercise | Exercise library is empty (sync failed) | Show: Your exercise library is not loaded. Sync your library in Settings before logging exercises. |

### Member - Gamification and Leaderboard

| Screen | Edge Case | Required Behavior |
|---|---|---|
| PR detection | Working set weight equals previous PR (not greater) | No PR recorded. Tie does not count. |
| PR detection | First ever set for an exercise | Always recorded as a PR since it exceeds the previous best of nothing. |
| Streak | Member completes workout on a non-planned day | No streak effect. Workout saved and earns points. Streak only counts planned days. |
| Streak | Planned day falls on a day the member logged two sessions | Only the first completed session counts toward the streak. Both sessions earn their individual points. |
| Leaderboard | Member opts out then opts back in | They re-enter with their actual current monthly points. No reset. |
| Leaderboard | No other members have opted in | Show own row with rank 1. textView_leaderboardSub: You are the top ranked member this month. Invite others to compete! |
| Points calculation | Session earns exactly 500 points before streak bonus | Cap applied, streak bonus added on top. Total shown as 700 if 7-day streak bonus applies. |

### Member - Heatmap

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Heatmap | No workout data at all | textView_heatmapEmpty visible: Start training to see your workout history. Calendar cells all shown in Surface Variant color. |
| Heatmap | Only 1 day of data | Show that one colored cell. All others Surface Variant. No minimum threshold. |
| Heatmap | All workouts have equal volume | All session days show the same mid-range color (Primary Container). No cell is darker than another. |
| Heatmap | Extremely high volume outlier day | That day shows Primary. Other days color correctly relative to it. No cell overflows the color scale. |
| Heatmap | User taps a Surface Variant (no-session) cell | Tooltip shows: {date}. No workout recorded. No volume or count text shown. |
| Heatmap | Today has a partially completed session (no Finish tapped) | That session's sets are not included. Only completed sessions with isCompleted = 1 contribute to heatmap volume. |

### Member - Progress and Analytics

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Body weight chart | Fewer than 3 entries | Chart not shown. textView_insufficientWeightData: Log at least 3 days to see your weight trend. Existing points rendered as dots only. |
| Body weight | User tries to log weight twice on the same day | Second log updates the existing entry for that date. No duplicate created. |
| Strength score | Fewer than 2 key lifts logged | textView_insufficientLifts: Log the 5 main compound lifts to unlock your Strength Score. No score number shown. |
| Per-lift cards | A key lift has never been logged | Card shown with textView_liftNotLogged: Not yet recorded. Tap to find this exercise. |
| 12-week volume chart | Fewer than 3 weeks of data | Show available weeks. textView_volumeDataNote: More data will appear as you log more sessions. |
| History calendar | Month has no sessions | All days shown in Surface Variant. Rest days shown in Background. No empty state — blank calendar is informative itself. |

### Member - Membership and Notifications

| Screen | Edge Case | Required Behavior |
|---|---|---|
| Membership card | Admin has not assigned a package | Card shows: No membership assigned. Contact RepRush gym to get started. |
| Membership card | Membership expired but still in grace period | Card shows Expired in error red with Days in grace period: X remaining in amber below. Features remain accessible. |
| Membership card | Grace period elapsed, account auto-suspended | Member sees fragment_status_blocked on next app open. All main features inaccessible until admin reactivates. |
| Notifications | No notifications yet | Empty state: textView_noNotifications: No notifications yet. You will see membership updates and announcements here. |
| Receipt notification | Member taps receipt notification | Opens ReceiptViewFragment showing the full formatted receipt. |

### General App-Wide Edge Cases

| Context | Edge Case | Required Behavior |
|---|---|---|
| All data screens | Network offline, Firestore cache available | Serve cached data. textView_syncBanner at top: Showing cached data. Changes will sync when you reconnect. |
| All data screens | Network offline, no Firestore cache | Room data shown for all member fitness screens. Admin screens show empty states with Offline indicator. |
| All RecyclerViews | Data loads successfully but returns an empty list | Each RecyclerView has a designed empty state layout with icon, message, and relevant call-to-action. |
| Long display names | Name over 25 characters in leaderboard | android:ellipsize=end + android:maxLines=1 on textView_memberName. Full name shown in MemberDetailFragment. |
| Very large numbers | Member earns over 9,999 points in a month | Format with thousands separator: 10,240 not 10240. Use NumberFormat.getNumberInstance(). |
| Session data | User changes weight unit mid-session | All inputs reformat immediately to the new unit. Stored value in Room is always in kg. |
| App update | Room schema version increases | Always use explicit Migration classes. Never allowDestructiveMigration() in production builds. |
