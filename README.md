# Breda Beats Studio 0.4 · breda_dev

Android sampler and compact rap production studio. Original branding with a hardware-inspired fixed 4×4 pad surface, distinct row accents, responsive portrait/landscape layout and dedicated Sample, Tracks, Mixer and Project pages.

## Production workflow
- Tap to play; long press for per-pad volume, pan, pitch ±24 semitones, reverse, loop/gate and choke groups. EDIT selects without sound. Copy/swap, normalize, trim silence and delete with undo.
- Import/record PCM16 WAV up to 60 seconds per pad. Waveform trim, destructive reverse/gain/drive and 16-way equal chop. Microphone is explicitly armed, permission-gated and foreground only; takes need confirmation.
- Eight sequences of 1–8 bars. BPM 40–200, swing, 1/16 quantized pad overdub, editable 16-track note matrix. Song arrangement up to 16 parts / 64 bars. Long press a sequence slot to duplicate after confirmation.
- 16-channel stereo mixer: level, pan, mute/solo, sampled peaks. Master low-pass, saturation and tempo-synced delay; internal mono resampling with confirmation.
- Audible metronome and count-in: default three seconds, one musical bar or none. Click is excluded from internal exports.
- Session undo/redo up to 20 edits, subject to shared-audio memory budget. Atomic autosave and checksum-addressed immutable samples.
- Export stereo WAV mix, full arrangement WAV, 16 aligned dry-master-FX stems, or an editable project ZIP backup. Import backup validates archive paths, bounds and sample hashes.

Read the [complete 0.4 workflow, installation and limits](docs/STUDIO-04.md).

## Architecture
Pure Kotlin core contains codec, synthesis, effects, channel/project values, bounded history, patterns and stereo rendering. Android adapters contain AudioTrack/AudioRecord and atomic file persistence. MVVM StateFlow separates UI/lifecycle from audio and storage. Injected repository, engine factory and dispatchers allow deterministic ViewModel tests. Repository/Adapter, Factory, Observer and command-history patterns serve concrete boundaries.

JDK 17, SDK 37.0, Kotlin 2.4.21, AGP 9.2.1, Gradle 9.4.1. Android 8+.

```sh
./gradlew :core:test :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assemblePreview :androidApp:assembleDebugAndroidTest
```

GitHub Actions tests DSP/domain, ViewModel and backup validation; runs lint; builds/signs/verifies APKs; runs native UI tests for fixed pads and long-press editing. The preview uses `dev.breda.beats.preview04` and installs alongside the older `.debug` app. Signing remains ephemeral; no private keys are committed. Old data is not automatically migrated between app IDs.

## Product boundaries
This release is inspired by compact samplers, not a full Koala/desktop DAW implementation. Pitch changes speed/duration; no independent time-stretch, stem separation, MIDI, plugins, continuous multitrack audio recording or parameter automation. Live polyphony caps at 32 voices; dense offline exports can differ. Output hard-clips instead of applying a mastering limiter. Exports crop sample/effect tails at sequence/song boundaries. Meters are sampled peaks, not calibrated loudness. Physical-device listening and latency checks are still required.

There is no login, cloud, billing, analytics or advertising. Original kit is synthesized from source; no proprietary Roland sounds or assets and no affiliation. See docs for manual testing and release evidence.
