# Fluxos de uso — Breda Beats Studio

Oferecer um fluxo compacto de produção de batidas com marca original e controle explícito de gravação e microfone.

## Mapa de telas

| Tela / área | O que a pessoa faz | Referência de implementação |
|---|---|---|
| Faixa de performance fixa | VOL e CTRL 1–3, seletor rápido de FX, PLAY/REC/STOP e undo/redo em todas as telas. | [MainActivity.kt](../androidApp/src/main/kotlin/dev/breda/beats/MainActivity.kt) |
| Pads | Tocar kit e preparar REC. | [PerformanceScreen.kt](../androidApp/src/main/kotlin/dev/breda/beats/PerformanceScreen.kt) |
| Editor do pad | Ajustar volume, pan, pitch, loop/gate e grupo de corte. | [ProductionPanels.kt](../androidApp/src/main/kotlin/dev/breda/beats/ProductionPanels.kt) |
| Sample | Editar waveform, recortar e dividir amostra. | [StudioPanels.kt](../androidApp/src/main/kotlin/dev/breda/beats/StudioPanels.kt) |
| Pistas e mixer | Editar notas, arranjar sequências e mixar canais. | [ProductionPanels.kt](../androidApp/src/main/kotlin/dev/breda/beats/ProductionPanels.kt) |
| Projeto | Salvar backup editável e exportar áudio. | [ProjectCodec.kt](../androidApp/src/main/kotlin/dev/breda/beats/data/ProjectCodec.kt) |

## Demonstração guiada

Use dados fictícios, em ambiente de avaliação.

1. Toque o kit com MIC OFF e escolha BPM e preparação de três segundos.
2. Grave notas nos pads depois da contagem; pare e reproduza.
3. Segure o kick e ajuste 70%; mantenha a caixa em 100%.
4. Duplique uma sequência, altere as notas e monte o arranjo.
5. Escolha Vinyl Color, Lo-Fi, Room Reverb, Beat Repeat ou Filter Sweep no seletor FX fixo; use CTRL 1 para o parâmetro principal, CTRL 2 para drive e CTRL 3 para delay enquanto toca. Nos pads, o brilho RGB acompanha o toque e marca as notas do passo atual. No editor SAMPLE, ajuste RECORTE, GAIN, DRIVE e REVERSO; aplicar confirma, e CHOP divide o trecho selecionado nos 16 pads.
6. Ajuste o mixer, exporte WAV/stems e salve também um ZIP editável.

VOL controla o master. Em STUDIO, CTRL 1 ajusta o filtro; nos modos de performance, controla a intensidade do efeito ativo. CTRL 2 ajusta drive e CTRL 3 delay. PLAY, REC PADS e STOP permanecem visíveis ao navegar ou rolar. A faixa respeita as áreas seguras de status, recorte e navegação do Android. O arraste vertical altera os knobs; os parâmetros continuam disponíveis no Mixer.

As escolhas de FX master ficam no backup do projeto. Backups anteriores continuam abrindo com a cadeia de estúdio padrão. Stems permanecem secos para mixagem externa.

## Casos que merecem atenção

- STOP, background ou MIC OFF durante preparação cancelam captura pendente.
- Backup inválido deve preservar o projeto atual.
- Metronômo não entra no render interno; pode vazar pelo alto-falante para o microfone.

## Escopo e resultado esperado

Não é paridade completa com Koala, MPC ou DAW desktop. Sem time-stretch independente, MIDI, plugins, automação ou gravação contínua multifaixa. Pitch muda velocidade/duração; vozes ao vivo limitadas a 32. Render corta caudas nos limites e usa hard clipping; latência física não medida.

## Galeria e contexto

As imagens abaixo representam um instante de uma execução automatizada. Uma captura estática não demonstra som, suavidade de animação, latência ou persistência.

### Pads fixos em retrato

![Breda Beats Studio — captura real](imagens/ui-portrait.png)

### Editor do pad por toque longo

![Breda Beats Studio — captura real](imagens/ui-pad-editor.png)

### Pads fixos em paisagem

![Breda Beats Studio — captura real](imagens/ui-landscape.png)

## Checklist para validação do proprietário

- [ ] Instalar/abrir a versão correspondente às evidências.
- [ ] Percorrer a jornada principal e uma condição de falha.
- [ ] Confirmar navegação, retorno, teclado e leitura em tela pequena.
- [ ] Fechar/reabrir e verificar os dados que devem persistir.
- [ ] Avaliar os recursos nativos pertinentes no aparelho real.
- [ ] Registrar versão, aparelho, resultado esperado e observado em eventual relato.

[Voltar ao README](../README.md) · [Evidências](evidencias.md)
