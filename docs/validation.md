# Validation · Breda Beats 0.3.0

GitHub Actions run [38384860851](https://github.com/brunobredadeveloper/breda-beats/actions/runs/38384860851) completed successfully on 13 October 2026. App commit: `6b26f9502e182dd0933a1dc68cb11b8e9c218c1a`.

- **23 core tests + 8 ViewModel tests + 2 native UI tests = 33 passed**, zero failures.
- Core checks cover PCM/WAV, stereo mixing, edit history, tempo-based count-in rounding, exactly 3 seconds at different BPMs and original bounded click PCM.
- ViewModel tests include no pad recording before 3000 ms, countdown 3 → 2 → 1, STOP cancellation with no delayed recording, immediate/one-bar options, MIC OFF during preparation with no capture/error, independent volumes, failed-save preservation and single undoable overdub.
- Android lint: zero errors, three version-update warnings. Compatible pinned dependencies were retained.
- Main and instrumentation APKs assembled; apksigner verified v2/v3 signatures on both.
- Emulator: Android15/API35, Pixel2 profile. In portrait and landscape every one of the 16 pads was displayed and its complete bounds remained within the screen. The pad grid had neither vertical nor horizontal scroll semantics. Native screenshots were visually inspected.
- APK main: 12,001,736 bytes; SHA256 `0d9aa458ae83a58ccb04ccd7ee5fd53b8e8293cb6b1ec4c09e35d3befff113a2`. Downloaded artifact digest, ZIP integrity and APK signing block were additionally checked.

## Recording behavior

Default REC waits 3 seconds with audible preparation ticks. One-bar mode counts four beats at the current BPM; at 120 BPM it lasts 2 seconds. NONE records immediately. Count-in ticks are independent of the metronome on/off switch. The metronome accents the start of each 4/4 bar and uses the pattern audio-frame clock during transport; it is not digitally routed into WAV/stem exports. A physical microphone can hear speaker output: use headphones when recording with the click on.

During preparation there are no captured pad notes or microphone samples. STOP, a second REC, background and MIC OFF during mic preparation cancel pending recording. Pads stay fixed; full settings open explicitly via AJUSTES. Landscape places controls beside the pads; only the control pane may scroll on constrained landscape screens. Other editing/mixer pages retain their own scrolling.

## Remaining device evaluation

Physical touch/audio latency, microphone hardware, audio routing, Bluetooth/wired headphone behavior and extra-small/large-font configurations have not been measured here. There is no professional latency claim. Evaluate the final APK on the actual phone.

Debug certificates may differ from an older APK. Export existing audio before any uninstall; uninstall deletes private project data. Committed projects persist through normal app restarts; undo history and unfinished takes remain session-only.
