# Physical-device evaluation · Studio 0.4

Use wired headphones. These are suggested manual checks, not measured device results.

1. Install Studio 0.4 alongside the older app; verify the older icon/project is still available.
2. Tap each pad. Hold a pad to open controls. Enable EDIT and tap another pad: the editor should open without audio. Turn EDIT off to finger-drum.
3. Set kick to 70%, snare to 100%, pan a hat to the right. Close/reopen controls, restart the app and check persistence. Use UNDO/REDO before restarting to compare mixes.
4. Assign open/closed hats to the same choke group. Trigger open then closed; the earlier voice should stop. Try reverse and +12 pitch on a short melodic sample. Pitch should shorten duration, not preserve it.
5. In SAMPLE, trim, apply and undo. Back up before CHOP because it replaces all pads. Test normalization and silence trim on quiet imported audio.
6. In TRACKS, record a pattern after the three-second count-in. Stop, duplicate S1 to S2 with a long press, select S2 while stopped, change its notes and build S1 → S2 → S1. Play the song; verify part advances and loops.
7. In MIXER, test channel mute/solo, meters, low-pass, saturation and delay. Move levels during song playback and stop to save the final settings. They are not automation lanes.
8. Stop playback, select a destination pad, resample with confirmation. Listen, then undo. The operation uses internal mix audio, not the microphone; master effects can process the resulting sample again during playback.
9. Export a mix, arrangement WAV and stems. Verify lengths and stereo placement in a DAW. Stems omit master FX. Offline render crops at sequence boundaries; very dense patterns can differ from the 32-voice live engine.
10. Export an editable project ZIP, edit notes/mix, reopen the ZIP and confirm replacement. Verify sample names, pad settings, sequence slots and arrangement. An invalid backup must leave the current project intact.
11. MIC starts off. Arm it explicitly, grant permission, record with headphones and test KEEP/DISCARD. Stop/background cancels an unfinished take. Disabling MIC during preparation must never start later capture.

Assess audible glitches, physical touch/audio delay, routing changes, app interruptions, small screens and large system fonts. No calibrated latency or loudness guarantee is made by automated tests.
