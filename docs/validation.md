# Validation · Breda Beats 0.2.0

GitHub Actions run [38338274243](https://github.com/brunobredadeveloper/breda-beats/actions/runs/38338274243) completed successfully on 12 October 2026 in Brazil (13 October UTC). App commit: `9676cf858a080bace2d49a1d57c6df997fca7a31`.

- 18 core tests and 4 ViewModel tests passed, zero failed/skipped.
- Checks include channel levels, stereo pan/mute/solo, independent volume undo/redo, bounded undo/redo branching, track erase, eight-bar export, aligned stems, failed-save preservation, microphone OFF by default and a single undoable pad-overdub take without microphone capture.
- Android lint: zero errors, three version-update warnings. Pinned compatible versions were retained.
- Android debug APK assembled and verified by apksigner, v2/v3 signatures.
- APK: 11,952,485 bytes; SHA256 `8eaff26a374610bac5e6c96eca68dbb5e39ca6bc7f6a1cbd0eead213aabbde37`.
- Device listening, touch latency, microphone hardware and UI instrumentation have not been tested here.

## Phone evaluation
1. App opens with MIC OFF; REC PADS adds notes without microphone permission/use.
2. Set kick70%, snare100%; audition and export. UNDO/REDO restore each edit independently.
3. Pan left/right, mute, solo, and master act on live output and exported WAV/stems.
4. Record multiple overdub layers over1–8bars, stop/save, undo the entire take. ERASE TRACK removes only its notes and UNDO restores them.
5. Enable MIC: no capture starts. REC SAMPLE explicitly requests permission/captures; END TAKE presents KEEP/DISCARD. Reject a take and ensure the original pad is unchanged.
6. Disable MIC/background during capture; no late take should appear. Denied permission must preserve pad operation.
7. Reopen committed project; channel names/levels/notes/samples persist. Undo history intentionally starts fresh.
8. Corrupt/missing sample files must block loading without deleting/resetting the workspace.
9. Export16aligned stereo stems and import into a desktop DAW. Muted/unsoloed stems are silent. Reduce output levels to avoid hard clipping.
10. Debug certificates may differ from a prior APK. Export existing audio before any uninstall; uninstall deletes private app data.
