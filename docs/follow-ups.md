# Follow-ups

Small issues found in the two code reviews and deliberately left for later (2026-10-09).

- audio focus may stay held if tts.speak returns ERROR (check return, release focus).
- AudioTrack built per beep without try/catch; prebuilt static tracks would be safer and lower-latency.
- queued cues still play after End (add CuePlayer.stop()).
- corruption recovery overwrites a previous workouts.bad.json.
- voice wording "Dips, max" / "each side" differs from spec examples "Max dips" / "each leg".
- lowering circuits in the editor drops overrides immediately (draft only; Discard recovers).
- editor draft not rememberSaveable (lost on uiMode/locale change or process death).
- Live screen recomposes every 100 ms tick (could map to LiveModel + distinctUntilChanged).
- music ducks/unducks for each of the 5 countdown beeps.
- keep-screen-on still active on the Finished screen.
- on the last set DONE shifts up (no up-next strip in the centred group).
- media-button routing to Kinetik on set 1 depends on Android's last-playing-app choice — verify on device.
- "This circuit" label on regular workouts (main-screen live).
- set rest shown for 1-set exercises in setsLabel.
- widget stays on "Workout complete" until the app's CLOSE.
- widget Start while editor open drops unsaved edits without prompt.
- BlockCard state not keyed by block id.
- widget frozen on live controls after process death until tapped.
- tick loop runs for an empty plan.

## To check on the device

- Earbud play/pause during a workout with Spotify playing should still control the music (Kinetik's media session has no play/pause actions). If presses are swallowed, replace the media-style notification with a plain public notification with action buttons.
