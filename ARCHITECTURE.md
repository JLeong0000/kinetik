# Kinetik architecture

Read this first. It's the map; the code is small (~3.6k lines of main source).

**What it is:** a personal, single-user Android workout timer for a Galaxy Z Fold8 Ultra (Android 17 / API 37).
- It runs **circuit** or **regular** workouts with voice cues, countdown beeps, a lock-screen media card and a home-screen widget.
- There are no accounts, no network and no workout history.

## Build, test, device

| What | Command / value |
|---|---|
| Debug build | `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` |
| Unit tests (JVM, ~91) | `./gradlew :app:testDebugUnitTest` |
| On-device tests | `./gradlew :app:connectedDebugAndroidTest`. With two devices attached, set `ANDROID_SERIAL`. Note: this **uninstalls the app afterwards**. |
| adb | `~/Library/Android/sdk/platform-tools/adb`. It's not on PATH. Always pass `-s <serial>` when the phone and the emulator are both attached. |
| Emulator | AVD `Pixel_9` (412 dp wide, so **wider than the Fold cover**). To simulate the main screen: `wm size 2256x2504 && wm density 378`. |
| Fold screenshots | `screencap -d 4630946379103915412 -p …` for the cover display. Display `4630946722019192211` is the inner one. |
| App data | `run-as dev.kinetik cat files/workouts.json`. Writing it back: `adb exec-in run-as dev.kinetik sh -c 'cat > files/workouts.json' < file`. |

**Toolchain (pinned on purpose):**
- AGP 8.13.2, Gradle wrapper 9.3.1, Kotlin 2.2.21, Compose BOM 2025.10.01, Glance 1.1.1.
- compile/target SDK 36, min SDK 31.
- Gradle runs on **Java 25**, the only JDK on this Mac. Do **not** add `jvmToolchain(17)`; bytecode is targeted at 17 through `compilerOptions` instead.

## Layering

```
model ──► plan ──► session ──► cues            (pure Kotlin, unit-tested; no Android imports
  │                  │                          except Beeper/CuePlayer)
  └──► store         ├──► SessionController ──► WorkoutService (FGS, wake lock, media card)
                     │          ▲
                     └──► ui/live/LiveModel, widget/WidgetModel   (pure view models, tested)
ui/* Compose screens read StateFlows from KinetikApp; no ViewModels.
```

`KinetikApp` (Application) owns the singletons, reached through `context.app`:
- `store: WorkoutStore` (`StateFlow<Library>`)
- `cues: CuePlayer`
- `session: SessionController` (`StateFlow<SessionState?>`)
- `widget: StateFlow<WidgetModel>` (deduped)
- `expandedGroups`, which is process-lifetime home UI state

## File map (`app/src/main/java/dev/kinetik/`)

| File | Responsibility |
|---|---|
| `model/Model.kt` | `@Serializable` data: `Library(groups, lastCompletedWorkoutId, settings)` → `Group` → `Workout(type, circuits, repDrop, restExerciseSec, restCircuitSec, exercises, overrides, blocks, setRestSec)`; `Exercise(name, kind, startReps, weightKg, sets, maxBreakSec, restSec)`; `CircuitOverride(circuit, scope SLOT/WHOLE, slot, exercises)`; `Block`; `Settings`. Every new field **must have a default** (old JSON must load). |
| `model/LibraryOps.kt` | Group and workout list edits, `upNext()` (rotates after `lastCompletedWorkoutId`), and `List.moved()`. |
| `model/WorkoutEdits.kt` | Circuit edits (moving or deleting an exercise remaps slot overrides), `setCircuits`, and `validate()`, which branches on type. |
| `model/BlockEdits.kt` | `newCircuitWorkout()` / `newRegularWorkout()`, the block edit functions, `Workout.sections` (circuits or non-empty blocks), and `summary()` (singular/plural). |
| `model/Seed.kt` | The owner's Pull / Push / Legs / Abs plan, written on first launch. |
| `plan/Plan.kt` | `buildPlan(w): List<Step>`, where a step is `Work(PlannedSet)` or `Rest(seconds, kind, circuit)`. Circuit: reps = max(1, start − c·drop); overrides apply; repeats run back to back. Regular: block by block, straight sets, set rest = `setRestSec ?: e.restSec`, then the exercise rest. Rests of 0 s and the trailing rest are dropped. Also `estimateSeconds` (45 s per set). |
| `plan/PlanGrid.kt` | Columns and cells for the circuit plan grid (Reps / Max / Swapped / whole-circuit label). |
| `session/Session.kt` | `SessionState` and `reduce(state, event)`, the **pure state machine**. Events: Tick(ms), Done, TogglePause (rests only), MinusThirty, Skip, ToggleBreak. `advance()` never moves more than one step and always resumes. `section` is "Circuit" or "Block". |
| `session/Format.kt` | `countdown` (rounds up) / `stopwatch`, `notificationText`, `Control` + `controlsFor(state)` (the media-card and widget buttons), `serviceShouldRun`, `canStart`. |
| `session/PressGate.kt` | Drops button presses within 300 ms of the last accepted one, or 700 ms after DONE. |
| `session/SessionController.kt` | Owns the state and a 100 ms `elapsedRealtime` tick loop, all on the main thread. `send()` gates presses, reduces, plays cues and records completion. It starts and stops `WorkoutService`. |
| `cues/Cues.kt` | `announce(set)` and `cuesFor(prev, event, next)`: speech on step change, short beeps at 5..1, a long beep when a rest or break ends **by tick** only. |
| `cues/Beeper.kt`, `cues/CuePlayer.kt` | Sine beeps through AudioTrack, and TTS. Each cue takes transient audio focus with ducking; a cue that's refused focus is dropped. Speech after a long beep waits 650 ms. |
| `service/WorkoutService.kt` | specialUse foreground service plus a 3 h partial wake lock. MediaStyle notification and a MediaSession with **custom actions only** (no play/pause, so earbuds stay with the music app). Stops itself when `serviceShouldRun` is false. |
| `store/WorkoutStore.kt` | A single `files/workouts.json`, written atomically. A corrupt file is kept as `workouts.bad.json` and the seed is loaded (`recoveredFromCorruption` triggers the snackbar). |
| `widget/WidgetModel.kt` | `widgetModel(state, library)` (live or idle up-next) and `widgetModels()`, deduped. |
| `widget/KinetikWidget.kt` | Glance widget and receiver. `SizeMode.Responsive`: below 110 dp tall it uses the compact 4×1 row. `ControlAction` switches to Main before calling `session.send`. Start opens `MainActivity` with `EXTRA_START`. |
| `MainActivity.kt` | Edge-to-edge (light system-bar icons), notification permission, fold posture → `LayoutMode`. Widget Start runs only on a fresh launch (`shouldStartFromIntent`). |
| `ui/LayoutMode.kt` | COVER (< 600 dp) / MAIN / TABLETOP (half-open, horizontal hinge). |
| `ui/KinetikNav.kt` | Routes `home`, `edit/{id}`, `live`, `settings`. Jumps to `live` whenever a session exists. |
| `ui/home/HomeScreen.kt` | Bento cards, collapsible groups, card menus, the Circuit/Regular choice dialog, and the main-screen detail pane with the hinge gradient. |
| `ui/home/Groups.kt` | `expandedOnStart` (only the up-next group). |
| `ui/editor/EditorScreen.kt` | Draft editing with validate-before-save and a discard prompt. Branches into circuit (rule tiles, exercise list, plan grid, overrides) or regular. |
| `ui/editor/RegularEditor.kt`, `Fields.kt`, `Sheets.kt` | Block cards, rule tiles, `ExerciseFields` (with a `regular` flag), `StepperDialog`, and the exercise/override bottom sheets (always fully expanded). |
| `ui/live/LiveModel.kt` | `liveModel(state)`: big text (reps / MAX / clock), tags (weight, "Set n of m"), segments, queue, next. |
| `ui/live/LiveScreen.kt` | Cover (grouped around the centre) / main (ring + queue) / tabletop layouts, the finish screen, and keep-screen-on. `RingBlock` fits text inside the circle's inscribed square using BasicText autoSize. |
| `ui/components/*` | `BigButton`, `Tag`, `Label`, `StatTile`, `ProgressBars`, `SwitchRow`, `kFieldColors`; `SegmentedRing` / `RestRing` (no glow) and `ringBox(max)` (always square); `cssGradientEndpoints`; `ReorderableColumn` (long-press drag, fixed row height); `PlanGridView`; `RegularSummary` / `setsLabel`. |
| `ui/theme/*` | Colour tokens `K` (bg #121414, teal #14B8A6, rest amber #F2B544…), `KText` (Archivo at width 125 for display, Manrope for body, JetBrains Mono for numbers), `KShape` (rounded rectangles, never pills). |

## Runtime flow

1. Start (home button or widget) → `SessionController.start(id)`: build the plan, announce the first set, start `WorkoutService`, start ticking.
2. Each tick → `reduce` → a new `SessionState` → `cuesFor` plays beeps and speech → the live UI recomposes. The notification only re-posts when its text changes, and the widget only updates when its `WidgetModel` changes.
3. DONE → rest; the rest ends on a tick → long beep → next set announced. The final DONE → FINISHED: completion is recorded (moving up-next along), the service stops itself, and the finish screen stays until CLOSE calls `stop()`.

## Invariants worth protecting
- `reduce` is pure and never skips a set. DONE during a rest is ignored. Skip and −30 s resume a paused timer.
- Saved JSON must stay backward compatible: give new fields defaults (`ignoreUnknownKeys = true`).
- The UI only sees a workout once it has passed `validate()`, so plans are never empty in practice.
- Rings must be sized with `ringBox()`. The cover screen is **360 dp** wide, and a fixed size squashes rings into ovals.
- Buttons are flat rounded rectangles: no glow, no pills.

## Where to change things
- **New exercise or workout field:** `Model.kt` (with a default), then `Plan.kt`, then `LiveModel` / `WidgetModel` if it's shown, then `Fields.kt`.
- **Timing or controls behaviour:** `Session.kt` `reduce` (add a `SessionTest`), then `controlsFor` for the buttons.
- **What's spoken or beeped:** `Cues.kt` (add a `CuesTest`).
- **Live screen look:** `LiveScreen.kt` / `LiveModel.kt`. Home look: `HomeScreen.kt`.
- **Specs and history:**
  - `docs/superpowers/specs/2026-10-09-kinetik-design.md`. §11–12 record post-launch changes.
  - The plan is in `docs/superpowers/plans/`.
  - Deferred items are in `docs/follow-ups.md`.
  - The design reference is `docs/design/mockup.html`.

## Gotchas
- Glance's Intent-based `actionStartActivity` is in `androidx.glance.appwidget.action`, not in `androidx.glance.action`.
- A Glance widget recomposes on every emission it collects, so feed it the deduped `app.widget` flow, never the raw session state.
- Android replays the original launch intent when it recreates an activity, which is why `shouldStartFromIntent` exists.
- `enableEdgeToEdge()` follows the system theme. The app forces `SystemBarStyle.dark`.
- `ModalBottomSheet` opens half-expanded by default and hides the buttons under the gesture bar.
