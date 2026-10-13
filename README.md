# Breda Beats · breda_dev

Android sampler and 16-track beat studio: a SP-404MKII-inspired hardware panel with original breda_dev identity, pads, stereo channel mixer, quantized pad overdub, waveform editing, microphone sampling and WAV/stem export.

## What's new in 0.2
- UNDO/REDO for the last 20 committed edits in the current session: sample replacements, chop, destructive edits, volume/pan, mute/solo, notes, track erase and complete overdub takes. Failed saves never modify history. A new edit after undo clears the redo branch.
- 16 channel strips with individual volume 0–100%, equal-power stereo pan, mute, solo, post-channel peak display and cue. Kick 70% and snare 100% remain independent. Master is stored with the project. Mix/export both apply channel settings.
- 16 sample tracks, 1–8 bars (4/4), a piano-roll-style note matrix, per-track step editor, BPM/swing, quantized 1/16 live overdub and ERASE TRACK. Playback continues while committing mixer adjustments. One take is one undo action.
- MIC starts OFF on every app launch. Enabling MIC does not start capture. REC PADS records touches only; REC SAMPLE requires MIC enabled and OS permission. Every mic take needs KEEP/DISCARD before replacing a sample. Disabling MIC/background stops and discards an in-progress capture. A generation token prevents a finishing old take from appearing after cancellation.
- Export stereo PCM16 WAV 44.1kHz mix and a ZIP containing 16 aligned stereo WAV stems and session notes. Stems use current mute/solo, pan, channel levels and master; disabled tracks export silence. ZIP streams to the selected document rather than holding all stems in memory.
- Atomic metadata plus immutable checksum-addressed sample files. Fader edits do not rewrite all audio. Existing schema1/2 projects migrate on successful save to schema3; invalid/corrupt files remain preserved.

## Workflow
1. Import WAV PCM16 mono/stereo (8–96kHz, up to10seconds per sample) or explicitly enable MIC and capture a take. Imported stereo is converted to mono; the mixer positions it in stereo.
2. Select SAMPLE, trim/reverse/gain/drive, apply, or CHOP into16equal pieces. UNDO restores the previous samples.
3. Select PISTAS, choose1–8bars, use REC PADS and finger-drum layers. END TAKE/STOP commits the quantized overdub; another take adds notes to the existing tracks. ERASE TRACK removes only its notes, not its audio, and can be undone.
4. Use MIXER to name tracks, balance channels, pan, mute/solo and set master. Reduce levels if CLIP appears.
5. Export a mix or stems to continue in Ableton Live, FL Studio or Logic. Keep sample rights in mind.

## Architecture and validation
Pure Kotlin core contains codec, synthesis, edits, project/channel values, bounded command history, pattern logic and stereo render. Android adapters contain AudioTrack/AudioRecord and atomic file persistence. MVVM StateFlow separates presentation and lifecycle from audio and storage; injected repository/engine factories/dispatchers support deterministic ViewModel tests. Repository/Adapter, Factory, Observer and command-history patterns solve concrete boundaries.

JDK17, SDK37.0, Kotlin2.4.21, AGP9.2.1, Gradle9.4.1; compatible pinned versions. Android8+.
`./gradlew :core:test :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug`
GitHub Actions signs/verifies a debug APK and uploads APK/check reports. No signing keys are committed. Different runs may require uninstalling an older debug APK because certificates differ. **Export your existing audio before uninstalling; uninstall removes private projects.** Commercial signing requires a separate private owner-controlled key.

## Honest product boundaries
This is a sample-based beat studio, not a full desktop DAW or Koala/Roland emulation. One workspace, bank A, 16 tracks, up to8bars and10s per captured/imported sample. No long vocal timeline recording, song arrangement across patterns, instruments/plugins, MIDI, time-stretch, calibrated latency compensation or effect buses yet. Loop/gate apply to live pad performance; sequenced/exported notes use one-shot playback. Undo history and pending mic takes are session-only; committed project/audio survive restart. Unused sample files are retained to avoid destructive cleanup.

The Kotlin PCM engine has32live voices, frame-clock sequencing and stereo interpolation, on an audio-priority worker. Output bounds hard-clip; this is not a mastering limiter. Offline rendering includes all scheduled voices, so dense overlapping patterns exceeding32voices can differ from live playback. No measured latency claim; evaluate touch/audio on the actual phone with wired headphones. Exports crop tails at the end of the final bar. Meter display is a sampled peak indicator, not calibrated loudness metering. No background recording, internet, analytics, ads, authentication or payments. Device tests and listening tests remain necessary.

Original kit is synthesized from source. Product is independently built by breda_dev, without Roland affiliation or proprietary sound/visual assets.
