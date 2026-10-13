# Breda Beats Studio 0.4

A compact mobile production workflow: pads → samples → sequences → arrangement → mix → export.

## Performance
The 16 pads remain fixed in portrait and landscape. Rows have distinct accents. Press a pad to audition immediately; hold to open its controls (the sound may start before the long press is recognized). Enable EDIT to select a pad and open its editor without triggering sound. Long press is disabled during pad recording.

The editor controls volume 0–100%, equal-power stereo pan, pitch ±24 semitones, reverse, loop, gate and four choke groups. Pitch changes playback speed and duration; it is not independent time-stretch. Gate applies to live finger holds, not sequenced note lengths. Volume and pan preview while dragging and save on release. Copy/swap include sample, name and settings, while sequence notes remain associated with pad numbers. Normalize and trim silence modify PCM and are undoable. Deletion asks for confirmation.

## Sequences and arrangement
Eight sequences with 1–8 bars each, global BPM 40–200 and swing. Tap a slot while stopped to select it; long press another slot to duplicate the active sequence after confirmation. Append slots to an ordered song (up to 16 parts, 64 total bars). Song playback loops this order. Song export renders the arrangement once. Playback currently stops voices at clip boundaries; delay carries between clips. There is no continuous audio-track recorder or piano-roll pitch automation.

## Mix and FX
16 pad channels, mute/solo, pan, level and meters. Master low-pass, saturation and tempo-synced eighth-note delay. Live movements during recording/song playback save the final settings when stopped; they are not recorded as automation. Internal resampling renders the current sequence with mix and FX, converts stereo to mono, and replaces the selected pad after confirmation. Microphone is not used for resampling. Exported stems omit master FX, retaining level, pan, mute/solo and master gain. Click is never included in internal exports (speaker sound can still leak into microphone recordings; use headphones).

## Persistence and safety
Project schema 4 migrates readable older local schemas after a successful save. Editable ZIP backups include samples, eight sequence slots, arrangement, pad settings and FX. Import checks path names, entry counts, decompressed lengths and SHA-256 sample hashes before asking to replace the current project. This is a project backup, unlike a flattened WAV export. Keep backups before major edits. Session undo/redo holds up to 20 actions with a shared-PCM memory budget; it is not preserved after restarting.

Imported/recorded WAV: PCM16, mono/stereo (stereo import downmixes to mono), up to 60 seconds per pad and 24 MB input. Total pad duration capped at 180 seconds. Microphone requires permission, explicit arming and REC; it turns off/cancels on leaving foreground. Offline render is bounded by sequence/song lengths; live engine caps polyphony at 32 voices. Device latency and production audio quality must be assessed on physical phones.

## Installation
The 0.4 evaluation uses `dev.breda.beats.preview04`, labelled “Breda Beats Studio 0.4”, to coexist with `dev.breda.beats.debug`. Existing app data remains in the old app; it is not migrated between application IDs. Old WAV exports can be imported as audio, but do not restore editable sequences. CI signing is still ephemeral; no persistent signing key is configured. Future updates require securely provisioned signing credentials.

## Scope
Inspired by compact samplers and pad instruments, with original breda_dev branding. This release does not claim full Koala parity: no independent time-stretch, stem separation, MIDI, plugins, multitrack continuous audio recording or parameter automation.
