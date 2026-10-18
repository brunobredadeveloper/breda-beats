# Breda Beats Studio

**Sampler Android com 16 pads fixos, sequências, arranjo, mixer e exportação de áudio.**

Projeto de **Bruno Breda · breda_dev**. Oferecer um fluxo compacto de produção de batidas com marca original e controle explícito de gravação e microfone.

**Plataforma:** Android nativo · Kotlin · Compose · AudioTrack/AudioRecord  
**Estado:** versão 0.7.0 em evolução; a prévia mais recente fica nas execuções do GitHub Actions.

[Fluxos e demonstração](docs/fluxos.md) · [Engenharia](docs/engenharia.md) · [Builds e evidências](docs/evidencias.md) · [Portfólio completo](https://github.com/brunobredadeveloper/breda-park-wash/blob/main/docs/portfolio-breda-dev.md)

## O que você pode avaliar

- Pads 4×4 fixos em retrato/paisagem; toque longo para volume, pan, pitch e modos.
- Importação WAV PCM16, gravação armada, trim, reverse, normalização e chop igual em 16 partes.
- Oito sequências, arranjo, swing, quantização, metrônomo e contagem de preparação.
- Mixer sobreposto em bottom sheet: 16 canais visíveis em retrato, volume, pan, mute/solo e picos por canal. Veja o [guia do Mixer e das cores](docs/MIXER-APPEARANCE.md).
- Aparência com seis fundos, oito cores de controles e valores HEX personalizados; preferências persistentes independentes do projeto.
- FX master com Vinyl Sim, DJFX Looper, Tape Echo, Isolator, Scatter, Slicer, Chorus, Compressor, Lo-Fi, Reverb e Filter Sweep. Faixa fixa em todas as telas com volume, CTRL 1–3, seletor de FX e transporte PLAY/REC/STOP; inclui undo/redo, backup editável e exportação WAV/stems.

## Telas reais

Capturas do aplicativo executado em emulador Android, recuperadas dos artefatos da esteira. Não são mockups nem fotografias de um aparelho físico. A origem e os limites estão em [evidências](docs/evidencias.md).

<img src="docs/imagens/ui-portrait.png" alt="Breda Beats Studio — Pads fixos em retrato" width="260" />
<img src="docs/imagens/ui-pad-editor.png" alt="Breda Beats Studio — Editor do pad por toque longo" width="260" />

## Fluxo principal

1. Toque o kit com MIC OFF e escolha BPM e preparação de três segundos.
2. Grave notas nos pads depois da contagem; pare e reproduza.
3. Segure o kick e ajuste 70%; mantenha a caixa em 100%.
4. Duplique uma sequência, altere as notas e monte o arranjo.
5. Escolha o FX no seletor superior, ajuste-o pelos knobs fixos durante a reprodução, depois refine o mixer e exporte WAV/stems.
6. Salve também um ZIP editável para reabrir o projeto.

## Executar e validar

JDK 17, SDK 37 e Android 8+. Use fones para avaliar microfone e metrônomo.

```sh
./gradlew :core:test :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assemblePreview :androidApp:assembleDebugAndroidTest
```

[Abrir as execuções do GitHub Actions](https://github.com/brunobredadeveloper/breda-beats/actions). Consulte a execução mais recente concluída com sucesso para baixar o APK e ver as evidências.

## Limites do produto

Os FX são interpretações próprias em DSP; não replicam o circuito nem cobrem todo o catálogo da SP-404MKII. Não é paridade completa com Koala, MPC ou DAW desktop. Sem time-stretch independente, MIDI, plugins, automação ou gravação contínua multifaixa. Pitch muda velocidade/duração; vozes ao vivo limitadas a 32. Render corta caudas nos limites e usa hard clipping; latência física não medida.

## Para quem revisa o portfólio

Produtores de rap e equipes que avaliam DSP, persistência e interfaces de performance. As [decisões de engenharia](docs/engenharia.md) conectam responsabilidades, arquivos e testes. O [roteiro funcional](docs/fluxos.md) ajuda a avaliar comportamento, falhas e preservação de dados. Funcionalidade planejada não é apresentada como implementada.

## Próximos passos

- Avaliar no dispositivo real os novos FX e registrar os resultados.
- Revisar acessibilidade, interrupções e falhas de armazenamento/rede aplicáveis.
- Concluir requisitos de distribuição, suporte e segurança antes de uso comercial.

Código, mídias e dependências mantêm seus respectivos termos. Consulte os créditos e os avisos existentes no repositório.

