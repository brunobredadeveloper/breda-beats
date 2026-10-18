# Mixer e aparência — Studio 0.7.0

Toque **MIXER** em qualquer tela para abrir a mesa por cima do conteúdo. Ao fechar, a tela anterior e sua posição de rolagem continuam disponíveis. Os knobs e PLAY / REC PADS / STOP ficam acima do painel e permanecem operáveis.

## Canais sem rolagem horizontal

Em retrato, a mesa mostra duas fileiras de oito canais: os 16 faders ficam visíveis ao mesmo tempo. Em paisagem larga, usa uma fileira de 16. Cada canal mostra número, nome abreviado, volume percentual e pico amostrado de áudio. A seleção tem contorno destacado; os indicadores M/S mostram mute e solo.

Arraste ou toque um fader para ajustar o volume. O ajuste é ouvido durante o gesto e salvo ao soltar. O canal selecionado oferece panorama L/C/R, centralização, Mute, Solo, CUE e renomeação ao tocar no nome. Os faders também expõem a ação acessível de ajustar progresso.

O master está no topo da mesa; CLIP indica pico excedido. Os parâmetros usam o mesmo modelo de mixagem e persistência do projeto. UNDO/REDO continua disponível. **FX** abre os ajustes de efeitos e resampling. O botão ×, Voltar, arrastar para baixo ou tocar MIXER novamente fecha a mesa.

## Cores independentes

Abra **PROJETO → CORES DO ESTÚDIO**, ou **PADS → AJUSTES → CORES DO ESTÚDIO**.

| Configuração | Opções |
| --- | --- |
| Fundo | Carvão, Grafite, Oceano, Ameixa, Floresta e Claro |
| Controles | Âmbar, Coral, Violeta, Azul, Menta, Lima, Laranja e Branco |
| Cor personalizada | HEX RGB de seis dígitos para cada cor |
| Restaurar | Fundo carvão e controles âmbar |

As opções prontas são aplicadas ao tocar; as cores HEX são aplicadas pelo botão APLICAR CORES. Entradas inválidas desabilitam o botão. Textos, painéis e cores de primeiro plano se adaptam ao fundo; tons de destaque podem ser ajustados para leitura. As cores RGB de identificação das famílias de pads permanecem reconhecíveis.

As preferências são gravadas em arquivo atômico, em uma tarefa de I/O, separadas dos samples, sequências e backups. Não entram em UNDO/REDO musical. Falhas de gravação são informadas e restauram a aparência anterior.

## Validação reproduzível

`MixerAppearanceTest` cobre retrato/paisagem, limites dos 16 faders, interação real por arraste, transporte, preservação da tela anterior, ajuste independente de canal, mute, HEX e recriação da Activity. `AppearanceCodecTest` e `StudioViewModelTest` cobrem persistência e falhas. As capturas são produzidas no emulador pelo workflow Android; [veja como obtê-las](evidencias.md).

A avaliação auditiva e a precisão do toque no aparelho físico continuam fazendo parte da validação do produto.
