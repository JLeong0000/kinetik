# CLAUDE.md

## 0. This project: Kinetik

Personal Android workout timer (Kotlin + Jetpack Compose) for a Galaxy Z Fold8 Ultra.

**Read `ARCHITECTURE.md` before exploring.** It maps every file, the runtime flow, the invariants and the gotchas, so you rarely need to open more than the files you're changing.

- **Tests:** `./gradlew :app:testDebugUnitTest`. On-device: `./gradlew :app:connectedDebugAndroidTest` (set `ANDROID_SERIAL`; it uninstalls the app afterwards).
- **adb:** `~/Library/Android/sdk/platform-tools/adb -s <serial>` (not on PATH).
- **Core logic is pure and unit-tested:** `model/`, `plan/`, `session/`, `cues/Cues.kt`, `ui/live/LiveModel.kt`, `widget/WidgetModel.kt`. Change behaviour there with a failing test first.
- **Don't:**
  - add `jvmToolchain(17)` (only Java 25 is installed);
  - add model fields without defaults (saved JSON must keep loading);
  - size rings without `ringBox()` (the cover screen is 360 dp);
  - feed the widget anything but `app.widget`.
- **Design rules:** flat rounded-rectangle buttons, no glow on buttons or rings, teal `#14B8A6` / amber `#F2B544`, Archivo Expanded for display text.
- **Records:** spec in `docs/superpowers/specs/` (§11–12 cover post-launch changes); deferred work in `docs/follow-ups.md`.

## General guidelines

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

Explore, then plan, then code. Read the relevant files and existing patterns before changing anything. Skip the plan when the diff fits in one sentence.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"
- "Change the UI" → "Screenshot the result and compare it to what was asked"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 5. Evidence, Not Claims

**IMPORTANT: Never say something works without showing the check that proves it.**

- Report the command you ran and what it returned (test output, build exit code, screenshot).
- If a check fails or was skipped, say so plainly.
- Fix root causes. Don't suppress errors, skip tests, or loosen assertions to get green.
- If the same fix has failed twice, stop patching. Re-read the error, question your assumption, and try a different approach.

## 6. Protect Context

**Context is the scarcest resource. Spend it deliberately.**

- Scope investigations narrowly. Read the parts of a file you need, not whole directories.
- Send long command output to a file and read the tail.
- When compacting, preserve the list of modified files, open decisions, and the test commands.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, clarifying questions come before implementation rather than after mistakes, and every "done" comes with proof.
