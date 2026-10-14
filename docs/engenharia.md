# Engenharia — Breda Beats Studio

## Problema técnico

Oferecer um fluxo compacto de produção de batidas com marca original e controle explícito de gravação e microfone.

## Responsabilidades e código

| Fronteira | Responsabilidade real | Onde inspecionar |
|---|---|---|
| Núcleo Kotlin | PCM/WAV, efeitos, padrões e mix estéreo | [Audio.kt](../core/src/main/kotlin/dev/breda/beats/core/Audio.kt) |
| Motor Android | AudioTrack/AudioRecord e ciclo de vida | [Studio.kt](../androidApp/src/main/kotlin/dev/breda/beats/data/Studio.kt) |
| Apresentação | StudioViewModel com dependências injetadas | [StudioViewModel.kt](../androidApp/src/main/kotlin/dev/breda/beats/presentation/StudioViewModel.kt) |
| Persistência | Codec de projeto e gravação atômica | [ProjectCodec.kt](../androidApp/src/main/kotlin/dev/breda/beats/data/ProjectCodec.kt) |

## Invariantes e decisões

- STOP, background ou MIC OFF durante preparação cancelam captura pendente.
- Backup inválido deve preservar o projeto atual.
- Metronômo não entra no render interno; pode vazar pelo alto-falante para o microfone.

## Como examinar os testes

Os arquivos abaixo fazem parte do código versionado. A existência de uma suíte não comprova sua aprovação; o resultado remoto está em [evidências](evidencias.md).

- [androidApp/src/androidTest/kotlin/dev/breda/beats/FixedPadsTest.kt](../androidApp/src/androidTest/kotlin/dev/breda/beats/FixedPadsTest.kt)
- [androidApp/src/test/kotlin/dev/breda/beats/data/ProjectArchiveTest.kt](../androidApp/src/test/kotlin/dev/breda/beats/data/ProjectArchiveTest.kt)
- [androidApp/src/test/kotlin/dev/breda/beats/presentation/StudioViewModelTest.kt](../androidApp/src/test/kotlin/dev/breda/beats/presentation/StudioViewModelTest.kt)
- [core/src/test/kotlin/dev/breda/beats/core/AudioTest.kt](../core/src/test/kotlin/dev/breda/beats/core/AudioTest.kt)
- [core/src/test/kotlin/dev/breda/beats/core/CountInTest.kt](../core/src/test/kotlin/dev/breda/beats/core/CountInTest.kt)
- [core/src/test/kotlin/dev/breda/beats/core/MixerTest.kt](../core/src/test/kotlin/dev/breda/beats/core/MixerTest.kt)
- [core/src/test/kotlin/dev/breda/beats/core/ProductionTest.kt](../core/src/test/kotlin/dev/breda/beats/core/ProductionTest.kt)

## Limites e compromissos

Não é paridade completa com Koala, MPC ou DAW desktop. Sem time-stretch independente, MIDI, plugins, automação ou gravação contínua multifaixa. Pitch muda velocidade/duração; vozes ao vivo limitadas a 32. Render corta caudas nos limites e usa hard clipping; latência física não medida.

A separação de responsabilidades documentada é a encontrada no código. Não se afirma certificação SOLID, paridade com produtos comerciais, auditoria de segurança ou operação em produção.

## Reproduzir a validação

```sh
./gradlew :core:test :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assemblePreview :androidApp:assembleDebugAndroidTest
```

[Voltar ao README](../README.md) · [Fluxos](fluxos.md)
