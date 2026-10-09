# Kinetik — design spec

Date: 2026-10-09
Status: open questions resolved, awaiting final approval
Visual reference: [`docs/design/mockup.html`](../../design/mockup.html) (open in a browser; the rest screens are animated)

## 1. What this is

Kinetik is a personal Android app for running calisthenics circuit workouts. It replaces a generic timer app with one that understands the owner's training plan: reps that drop each circuit, circuits that swap or replace exercises, and long fixed rests.

It is for one person, on one phone (Galaxy Z Fold8 Ultra), with no accounts, no cloud and no workout history. It should be fun to look at as well as useful.

**Success looks like:** the owner can do a full Pull, Push, Legs or Abs session by looking at the cover screen and tapping DONE after each set. The voice says what to do next, the beeps count down each rest, and they never have to work out reps in their head.

## 2. The training model

A **workout** (Pull, Push, Legs, Abs) is a number of **circuits**. Every circuit runs the same ordered list of **exercises**. After each exercise there is a short rest, and after the last exercise of a circuit there is a longer rest. There is no rest after the final set of the workout.

Workouts belong to **groups** (e.g. "Calisthenics split"). Groups and workouts can be renamed, reordered, and workouts can be moved between groups.

### 2.1 Rules, not tables

Each workout stores:

| Field | Example (Pull) |
|---|---|
| Name | Pull |
| Circuit count | 5 |
| Rep drop per circuit | 2 |
| Rest between exercises | 3:00 |
| Rest between circuits | 4:00 |
| Exercises (ordered) | Pull ups 8, Chin ups 8, Inverted pull up rows 9, Inverted chin up rows 9 |
| Overrides | see 2.3 |

Reps for exercise *e* in circuit *c* (0-based) = `max(1, startReps(e) − c × drop)`.

### 2.2 Exercise kinds

| Kind | Shown as | During the set |
|---|---|---|
| **Reps** | `8` | Target reps. Tap DONE when finished. |
| **Max reps** | `MAX` | No target. Optional **max break** (e.g. 10 s): a BREAK button starts that countdown with beeps, and you tap again to continue. Tap DONE when finished. |
| **Max time** | `MAX TIME` | A stopwatch counts up from when the set starts. Tap DONE to stop it. Nothing is saved. |

Optional modifiers on any exercise: **weight** (e.g. 10 kg, display only) and **sets** (e.g. ×4: the exercise is repeated that many times back to back with **no rest between repeats**. Tap DONE after each one, and the next starts at once and is announced. The normal between-exercise rest follows the last repeat).

### 2.3 Overrides

An override changes one circuit of a workout. There are two scopes:

- **Slot override** replaces one exercise in one circuit; the other exercises in that circuit run as normal. Example: Pull C4, slots 1 and 2 → max-time negatives.
- **Whole-circuit override** replaces the entire circuit with its own list (usually a single max set). Example: Push C5 → Max dips (10 s max break).

Overrides do not count towards the rep-drop rule; the plan grid shows them as striped cells.

### 2.4 Seed data

The app ships with the owner's four workouts already entered (they can edit everything):

| Workout | Circuits | Drop | Exercises (start reps) | Overrides |
|---|---|---|---|---|
| Pull | 5 | −2 | Pull ups 8, Chin ups 8, Inverted pull up rows 9, Inverted chin up rows 9 | C4: slots 1–2 → max-time negatives ×4 each, no rest between repeats. C5: whole → max alternating chin-up / pull-up negatives |
| Push | 6 | −1 | Dips 13 (10 kg), Pike push ups 11, Archer push ups 12, Pseudo push ups 11 | C5: whole → max dips, 10 s break. C6: whole → max push ups, 10 s break |
| Legs | 6 | −1 | Shrimp squats 11 (each leg), Hand-assisted Nordic curls 10, Hand-assisted pistol squats 11 (each leg), Single calf raises 15 | C5: whole → max hand-assisted Nordic curls, 10 s break. C6: whole → max hand-assisted pistol squats, 10 s break |
| Abs | 5 (4 + 1) | −1 | Hanging knee twists 8, Hanging leg raises 9 | C5: max round, slots 1–2 → max reps (no break limit), with normal rests |

All four use 3:00 between exercises. Pull, Push and Legs use 4:00 between circuits and Abs uses 3:00.

## 3. Running a workout

The core loop is: announce → set → DONE → rest with countdown → announce next.

1. **Set starts.** The screen shows the exercise and target. The voice says the exercise name and reps, e.g. "Pull ups, six", "Shrimp squats, eleven, each leg" or "Max dips".
2. **DONE.** Tap the DONE button or press the play/pause button on headphones. This moves on to the rest.
3. **Rest.** The voice says "Rest". The ring drains in amber. It **beeps once a second from 5 to 1, with a longer beep at 0**, then the next set starts automatically and is announced.
4. **Rest controls:** −30 s, pause/resume and skip.
5. **The end** shows a short "Workout complete" screen with total time.

Voice and beeps play over music: Kinetik asks Android to lower other audio briefly rather than stop it. Voice can be switched off in Settings, leaving beeps only.

The workout keeps running with the screen off or the phone in a pocket. This is done with a foreground service, and its notification shows the current step with Pause and Skip.

## 4. Screens

All screens use the mockup's visual language (section 6). The app picks a layout from the screen it's on.

### 4.1 Cover screen (folded, ~412 × 960 dp)

- **Home:** groups with bento workout cards. The next workout gets the large card with a Start button, and each card has a segmented ring glyph (one segment per circuit). A "+" card adds a workout. Long-press a card for a menu: edit, rename, move earlier or later, move to another group, delete. Groups have their own menu.
- **Edit workout:** an editable title, three rule tiles (drop, exercise rest, circuit rest) and the exercise list. Each exercise row shows its start reps, name and tags, and can be dragged to reorder (it lifts with a shadow, level, with no tilt). Below is the **circuit plan grid**, which updates as you edit and shows overrides striped. "+ Override" opens an override editor.
- **Live (exercise and rest):** everything sits in a tight group at the vertical centre: the header (circuit x/y, workout, time left), the circuit progress bars, the ring, then the up-next strip and DONE (or the rest controls).

### 4.2 Main screen (unfolded, ~954 × 860 dp)

Two panes, split at the hinge.

- **Home:** workout cards on the left and the selected workout's detail on the right: big title, stat tiles, full plan grid, Edit and START. The right pane has a teal gradient at the top, tilted 10° (190°), fading to the background about halfway down, and feathered into the left pane over a ~58 px band at the hinge. There's no hard divider.
- **Edit:** the exercises and rules on the left; the live plan grid and override list on the right.
- **Live:** the ring and target on the left. The right pane shows this circuit's exercise queue (now, upcoming with rests, done struck through), circuit tiles 1…N, time left and DONE.

### 4.3 Flex mode (half-folded, standing on the floor)

When the phone is half-folded with a horizontal hinge, Live switches to a two-part layout. The top half shows the ring, the timer and "Next up". The bottom half shows the progress bars and large buttons (DONE during sets; −30 s, Pause and Skip during rest).

### 4.4 Settings

Voice on/off, beep volume, keep screen on during workouts (default on).

## 5. Architecture

**Stack:** Kotlin, Jetpack Compose (Material 3 as a base, heavily themed), single activity, minSdk 31, target/compile SDK 36 (already installed). Built with Gradle in Android Studio and from the command line.

### 5.1 Units

| Unit | Responsibility | Depends on |
|---|---|---|
| `model` | Plain data classes: `Group`, `Workout`, `Exercise`, `Override`, `ExerciseKind`. | — |
| `plan` | A pure function `buildPlan(workout): List<Step>` that expands rules and overrides into an ordered list of `Set` and `Rest` steps. It's also used to draw the plan grid and estimate total time. | `model` |
| `store` | Loads and saves all groups and workouts as one JSON file (kotlinx.serialization, atomic write), seeds on first launch and exposes a `StateFlow`. | `model` |
| `session` | `SessionEngine`: a state machine over the step list (start, done, tick, pause, −30 s, skip, break) with an injected clock. It emits a `SessionState` and has no Android dependencies. | `plan` |
| `cues` | Turns state changes into speech (Android `TextToSpeech`) and beeps (sine tones generated in code and played with `AudioTrack`, so no sound files). It handles audio focus with ducking. | `session` |
| `service` | `WorkoutService`: a foreground service that owns the engine and cues, posts the notification and handles the media button. | `session`, `cues` |
| `ui` | Compose screens plus the theme. Layout is chosen from the window size class and the fold posture (Jetpack WindowManager `FoldingFeature`). | everything above (screens read the store and the session controller directly; no ViewModels) |

Why JSON rather than a database: the data is small, nested and edited as a whole. A single file keeps the store very simple, and the `store` interface lets it switch to Room later if history is ever added.

### 5.2 Data flow

The editor changes a `Workout`, `store` saves it, and Home and the Editor recompose. **Start** sends the workout ID to `WorkoutService`, which calls `buildPlan()` and creates a `SessionEngine`. The engine's `StateFlow` drives both the Live UI and the cues. DONE, Pause and the other controls call the service, which calls the engine.

### 5.3 Error handling

- **Text-to-speech unavailable or still loading:** the app carries on with beeps only and shows a small "Voice unavailable" chip on the Live screen.
- **Audio focus lost (e.g. a phone call):** the timer keeps running, cues are muted until focus returns, and nothing is queued.
- **Corrupt or unreadable store file:** the bad file is kept as `workouts.bad.json`, the seed data is loaded, and a one-time message is shown. Nothing is silently deleted.
- **Invalid edits** (0 circuits, an empty exercise list, an override pointing at a removed slot): blocked in the editor with an inline message. Orphaned overrides are removed when their slot is deleted.
- **App process killed mid-workout:** the session is lost. This is accepted for v1 because the foreground service makes it rare.

## 6. Visual design

| Token | Value |
|---|---|
| Page / background | `#121414` |
| Card / raised card | `#1C2021` / `#252A2B` |
| Line | `#2E3536` |
| Text / muted | `#EEF3F2` / `#8A9796` |
| Teal accent / highlight / dim | `#14B8A6` / `#5EEAD4` / `#0F5F57` |
| Rest | `#F2B544` (amber) |
| Gradient teal | `#134540` |

**Typography:**
- **Archivo Expanded** (Archivo variable, width 125, weights 700–900) for titles, rep numbers and buttons.
- **Manrope** for body text and labels.
- **JetBrains Mono** for every countdown and number in a grid, so the digits don't jitter.
- All three are bundled with the app (OFL licence), so it works offline.

**Shapes and effects:** 26 dp card radius, pill tags. A teal glow is used only on the active ring segment, the current circuit bar and the selected card. **DONE and START are flat, with no glow.** The rest ring and countdown are amber. In the last 5 s the countdown pulses in time with the beeps.

## 7. Testing

- **`plan` unit tests:** golden tests that expand all four seed workouts and assert the exact step lists, covering rep drops, the floor at 1, slot and whole-circuit overrides, ×N sets, and no rest after the final set.
- **`session` unit tests** with a fake clock: done → rest → auto-advance, the beep times (5…0), pause/resume, −30 s clamped at 0, skip, and the max-break countdown.
- **`store` tests:** round-trip save and load, first-launch seeding, and recovery from a corrupt file.
- **UI:** Compose previews for each screen at cover, main and tabletop sizes, plus one instrumented smoke test that starts a workout, taps DONE and checks that a rest starts.
- **On the device:** a full Abs session run on the Fold8 Ultra, both folded and in flex mode, with music playing and the screen locked.

## 8. Out of scope (v1)

Workout history and logging, progress charts, accounts and sync, iOS, a shared exercise library across workouts, an "Exercises" tab (removed from the mockup's nav, leaving Workouts and Settings), demonstration images or video, Wear OS, and widgets.

## 9. Resolved questions

1. **Pull C4:** pull ups and chin ups each become max-time negatives ×4, done back to back with no rest between negatives.
2. **Abs "4+1":** circuit 5 is a max round. Both exercises become max reps, with the normal 3:00 rests.
3. **Whole-circuit overrides:** Pull C5, and Push/Legs C5 and C6, are single max-effort sets, each followed by the 4:00 circuit rest.

## 10. Environment

Android Studio 2026.2 with its bundled JBR (Java 25), SDK platform `android-36`, build-tools 36.1.0, and platform-tools (adb) in `~/Library/Android/sdk`. adb isn't on the shell PATH yet, so builds and installs will call it by its full path or add it to PATH.

## 11. Changes after the first device run (2026-10-09)

- The "each side" option is removed.
- All buttons are rectangles with rounded corners (no pills or circles). Text-field labels and placeholders are grey.
- No glow on the exercise ring or the rest countdown ring. Rings are always circles: their size is the smaller of their maximum and the space available (measured 906 × 902 px on the Fold8 Ultra cover screen).
- Earbud play/pause is **not** handled by Kinetik; it stays with the music app.
- The workout notification is a media-style card with Done / Pause / Resume / Skip, visible on the lock screen.
- Home-screen widget (4×2, resizable down to 4×1, where the buttons move to the right): during a workout it shows circuit x/y, the exercise and reps (or the rest countdown), with Done or Pause/Skip. When idle it shows the up-next workout with Start, which opens the app and starts it.
- Double-tap protection: button presses within 300 ms are ignored, and within 700 ms after DONE. START only starts once. Pause only applies during rests.
- Settings no longer offers "Reset workouts".

## 12. Regular workouts (2026-10-09)

A workout is either **Circuit** (everything above) or **Regular**, chosen when it's created with **+** (fixed afterwards).

- **Structure:** a list of named **blocks**; each block is a list of exercises run as **straight sets** (all sets of A, then all sets of B). Empty blocks are skipped.
- **Exercise:** sets × reps (same reps every set), type (reps / max reps / max time), optional weight and max break, and its own **rest between sets** (default 1:30).
- **Rests:** an optional workout-wide **rest between sets** that overrides every exercise's own (off by default), plus one **rest between exercises** (default 3:00). No rest after the final set.
- **Editor:** two rule tiles (rest between exercises; rest between sets, all exercises — "Off" or a time), one card per block (rename inline; ⋮ to move or delete; drag exercises to reorder; + Add exercise), and + Add block. On the main screen the right pane shows the run order.
- **Live:** the same screens with "Block x/y" instead of "Circuit x/y" and a "Set n of m" tag; voice says e.g. "Bench press, 8, set 2 of 4". Notification, widget and home cards use "Block" and "2 blocks · 5 exercises".
- **Storage:** `Workout.type` (default CIRCUIT), `blocks`, `setRestSec`, and `Exercise.restSec` — all with defaults, so saved circuit workouts load unchanged.

Widget text and buttons are slightly larger (full: 40 sp number, 52 dp buttons; 4×1: 28 sp number, 44 dp buttons).
