# Follow-ups

## Done (2026-10-09)

Every small issue deferred from the two code reviews has been fixed:

- **Audio**
  - Cues stop when you tap End.
  - A failed voice call releases audio focus.
  - Beeps replay two prebuilt tracks, and audio errors can't crash a workout.
  - Music ducks once for the whole 5…1 countdown.
- **Voice:** "Max Dips" and "Max time Pull up negatives".
- **Widget**
  - Offers Start for the next workout after "Workout complete".
  - Refreshes at least every 30 minutes, so it can't stay frozen after the app is killed.
  - Starting from it while editing returns you to the editor with your draft intact.
- **Editor**
  - Lowering the circuit count keeps overrides until you save, so "+" brings them back.
  - Unsaved drafts survive activity recreation and process death.
  - Block cards are keyed by block.
- **Live screen**
  - Recomposes about once a second instead of every tick.
  - Says "This block" on regular workouts.
  - A "Last set" strip keeps DONE in place.
  - Keep-screen-on is off on the Finished screen.
- **Other**
  - A 1-set exercise shows no set rest.
  - Corrupt-file backups are timestamped, so earlier backups are never overwritten.
  - The tick loop doesn't run for an empty plan.

## To check on the device

- With Spotify playing during a workout, earbud play/pause should still control the music (Kinetik's media session has no play/pause actions). If presses are swallowed, replace the media-style notification with a plain public notification with action buttons.
