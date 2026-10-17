# Evidências e distribuição — Breda Beats Studio

Consulta realizada em **17/10/2026**. Esta página separa o que existe no código, o que foi executado e o que ainda precisa de validação física.

## Execução consultada

- [GitHub Actions #39075907079](https://github.com/brunobredadeveloper/breda-beats/actions/runs/39075907079)
- Commit validado: `e123200fbc8f5bc1fd38960115af858448dad2b1`.
- Workflow: **Breda Beats tests and APK**.
- Conclusão retornada pelo GitHub: **success**.
- O job Android passou em testes Kotlin, testes unitários, lint, builds Debug/Preview e verificação do APK; o job de interface em emulador também passou.

## Artefatos encontrados

- `breda-beats-android-apk` — ID `12117490143`; inclui `preview/androidApp-preview.apk` e builds de avaliação; expira em 15/01/2027.
- `breda-beats-fixed-pads-ui` — ID `12117271398`; verificação de interface executada em emulador.
- `breda-beats-check-reports` — ID `12117211351`; relatórios da esteira.

Para baixar o APK: abra a execução, vá a **Artifacts**, baixe `breda-beats-android-apk` e extraia `preview/androidApp-preview.apk`. O GitHub pode exigir login. Builds iOS de simulador ou sem assinatura não instalam diretamente no iPhone.

## Capturas incorporadas

- [ui-portrait.png](imagens/ui-portrait.png) — artefato de testes de interface do aplicativo.
- [ui-pad-editor.png](imagens/ui-pad-editor.png) — artefato de testes de interface do aplicativo.
- [ui-landscape.png](imagens/ui-landscape.png) — artefato de testes de interface do aplicativo.

Origem: https://github.com/brunobredadeveloper/breda-beats/actions/runs/38422254625. Os dados exibidos são de demonstração. As capturas foram mantidas sem retoques.

## Avaliação física pendente

Testes automatizados e compilação não substituem avaliação de acessibilidade, interrupções, recursos do aparelho e instalação/distribuição pelo titular. Não é paridade completa com Koala, MPC ou DAW desktop. Sem time-stretch independente, MIDI, plugins, automação ou gravação contínua multifaixa. Pitch muda velocidade/duração; vozes ao vivo limitadas a 32. Render corta caudas nos limites e usa hard clipping; latência física não medida.

[Fluxos e checklist](fluxos.md) · [README](../README.md)
