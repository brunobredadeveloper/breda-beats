# Breda Beats · breda_dev

Android sampler and beat workstation with a hardware-inspired panel, original branding, 16 performance pads, microphone sampling, waveform trimming, equal slicing, reverse, gain, saturation, gate/loop playback, a 16-step pattern, BPM/swing controls and WAV export.

## Run
Android 8+, JDK17, SDK37.0 and committed Gradle9.4.1 wrapper.
`./gradlew :core:test :androidApp:lintDebug :androidApp:assembleDebug`
GitHub Actions produces a debug APK artifact. No signing key is committed; different runners may produce different debug certificates. Export test work before uninstalling an earlier build if signatures differ.

## Architecture
Pure Kotlin `core` owns samples, strict PCM WAV codec, edit transforms, deterministic original synthesis and offline pattern rendering. Android `data` adapters provide atomic project persistence, AudioRecord capture and AudioTrack mixing. `StudioViewModel` injects the repository and exposes immutable StateFlow; Compose observes lifecycle state. Repository/Adapter, composition-root Factory and observer patterns serve actual boundaries.

## Scope and workflow
One local project, bank A, 16 pads and one 4/4 bar. Select a pad and import WAV PCM16 mono/stereo (8–96kHz, up to10s); stereo is averaged to mono. REC captures microphone up to10s. SAMPLE trims/reverses and commits edits; CHOP confirms replacement of all16pads with equal slices. PATTERN selects pad tracks and toggles semiquavers; export renders one bar to mono PCM16 WAV44100Hz. Tails beyond the bar are cropped. Gain saturation applies destructively only when confirmed. No time-stretch, effect emulation, additional banks, MIDI, pitch control or cloud in this release.

The panel is inspired by the workflow of tabletop samplers including SP-404MKII; this is an independent product without Roland affiliation, proprietary samples or visual assets. The included kit is synthesized by source code.

## Audio and privacy
32-voice Kotlin mixer using a dedicated audio-priority thread; pattern events are scheduled by output frames, independent of Compose animation. No measured/professional latency claim; validate on device and use wired headphones. Audio focus, background stop and recording interruption need device verification. RECORD_AUDIO is requested only for REC. No internet/ads/analytics/accounts/billing. Project is stored privately through AtomicFile. Corrupt project remains preserved and editing is blocked if load fails. Background stops transport/loops and discards unfinished recording. No background microphone service. Import/export uses document picker.

## Validation
Six meaningful domain tests cover malformed WAV, PCM roundtrip, swing duration, pattern events/render, sample edits and invalid tempo. GitHub build/device validation status must be checked on Actions; no listening tests have yet been performed. UI evaluation: simultaneous taps, loop/gate, permission denial, mic start/stop/10s/background, WAV invalid input, waveform cuts, chop confirmation, automatic project recovery and exported audio.

## Product next steps
Multiple projects/banks, audio focus routing and interruption handling, native Oboe engine measured on devices, nondestructive effect buses, realtime recording/undo, true waveform scrub, persistence migration and product monetization require additional work. This release does not charge users.
