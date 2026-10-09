# Kinetik

A personal Android workout timer built for the Galaxy Z Fold8 Ultra (Kotlin + Jetpack Compose).

- **Circuit workouts.** Rounds of the same exercises, with reps dropping each circuit. Per-circuit overrides can swap one exercise or replace the whole circuit with a max set.
- **Regular workouts.** Blocks of straight sets. Each exercise has its own rest between sets, and an optional workout-wide set rest overrides them all.
- **Hands-free running.** Spoken cues ("Pull ups, 6"), countdown beeps at 5…1, and rests that start the next set automatically. It keeps running with the screen locked.
- **Lock screen and widget.** A lock-screen media card has Done / Pause / Skip. The home-screen widget comes in 4×2 and 4×1.
- **Fold layouts.** Separate layouts for the cover screen, the main screen (two panes at the hinge) and flex mode.

## Build

Requires the Android SDK (API 36). Gradle runs on Java 25.

```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:connectedDebugAndroidTest   # on-device tests (needs a device or emulator)
```

## Docs

- **Start here:** `ARCHITECTURE.md`. It maps every file, the runtime flow, the invariants and the gotchas.
- Design spec: `docs/superpowers/specs/2026-10-09-kinetik-design.md`
- Implementation plan: `docs/superpowers/plans/2026-10-09-kinetik.md`
- Mockup: `docs/design/mockup.html`
- Follow-ups: `docs/follow-ups.md`

## License

Code: MIT, see `LICENSE`. Fonts (Archivo, Manrope, JetBrains Mono) are under the SIL Open Font License; see `docs/licenses/`.
