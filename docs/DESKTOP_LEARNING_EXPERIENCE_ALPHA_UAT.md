# Desktop Learning Experience Alpha — Product Owner UAT

Automated tests validate decoding through the real Desktop adapter up to decoded PCM writes.
They cannot prove that a particular machine, mixer, volume setting, or physical speaker produces
audible sound. Record the package, Windows version, output device, and result when running this
checklist.

## A. Audio

1. Import a real package containing prompt, answer, and example MP3 assets from a path with spaces.
2. Start Learn and play the word/prompt pronunciation; confirm it is audible and the control shows
   Starting/Playing state.
3. Press `R` several times and confirm each replay starts cleanly without overlap.
4. Reveal with `Space`; play every answer and example control and confirm each label identifies its
   semantic role.
5. Start one clip and immediately start another; confirm only the second remains audible.
6. Start audio and rate into the next item; confirm the prior clip stops and never resumes.
7. Start audio and press `Escape`; confirm playback stops while the ACTIVE session remains resumable.
8. Exercise a missing or invalid media reference; confirm a concise inline error and an otherwise
   usable study session.
9. Repeat with a package and media path containing non-ASCII characters.

## B. Learning workspace

1. Start a session and confirm the prompt, pronunciation, audio, and image—not application chrome—
   form the first visual focus.
2. Confirm header, sidebar, status bar, dashboard counters, and service names do not dominate the
   active workspace; confirm Undo and Pause remain reachable.
3. Confirm Question state contains no answer-only fields. Reveal with `Space` and `Enter`.
4. Confirm answer and bilingual examples appear as coherent sections with role-labelled audio.
5. Rate with keys `1` through `4`; confirm all four visible actions look enabled and equivalent.
6. Replay with `R`, undo with `Ctrl+Z`, and pause with `Escape`.
7. Resize from a common small laptop window to a large monitor; confirm centered readable content,
   preserved image aspect ratio, scrolling for long content, and reachable actions.
8. Test one item with an image, one without an image, long text, multiple examples, a new item, and
   a scheduled review item.
9. Repeat in Light and Dark themes.
10. Study at least 20 consecutive items and record any stale audio, hidden action, focus loss,
    hierarchy regression, or layout break.

## C. Adaptive learning scenes

1. Study a text-only item and confirm a calm recall prompt rather than field/debug labels.
2. Study an audio item and confirm the listening action leads the scene.
3. Study an image item and confirm the image leads without distortion.
4. Reveal an answer and confirm Meaning and Examples join the same experience in that order.
5. Repeat the same items after restart and confirm deterministic scene selection.
6. Confirm no typing input is shown; Typing remains a future placeholder only.

Physical speaker output remains **pending** until the Product Owner records an audible pass.
