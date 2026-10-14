# Breda Beats Studio 0.4 — guia de produção

O fluxo de trabalho é **pads → amostras → sequências → arranjo → mixagem → exportação**.

## Performance e edição dos pads
Os 16 pads ficam fixos em retrato e paisagem. Toque para ouvir; mantenha pressionado para abrir controles. O áudio pode começar antes de o toque longo ser reconhecido. Ative EDIT para selecionar e editar sem disparar som. O toque longo é desabilitado durante gravação de notas.

Cada pad possui volume de 0 a 100%, panorama estéreo equal-power, pitch de ±24 semitons, reverse, loop, gate e quatro grupos de corte (choke). Pitch altera velocidade e duração, sem time-stretch independente. Gate vale para o toque ao vivo, não para duração de notas da sequência. Volume e panorama podem ser ouvidos durante o ajuste e são salvos ao soltar o controle.

Copiar/trocar inclui amostra, nome e ajustes; as notas continuam associadas ao número do pad. Normalização e remoção de silêncio alteram o PCM e podem ser desfeitas. A exclusão exige confirmação.

## Amostras, microfone e preparação
Importação/gravação: WAV PCM16 mono/estéreo, até 60 segundos por pad e arquivo de entrada de até 24 MB. Entrada estéreo é convertida em mono. A duração total dos pads é limitada a 180 segundos. CHOP divide a amostra igualmente em 16 partes e substitui os pads; faça backup antes.

Microfone começa desligado, exige armamento explícito, permissão e REC. A tomada precisa ser confirmada ou descartada. Sair de primeiro plano cancela uma captura incompleta. MIC OFF durante a preparação impede captura posterior. Use fones: o microfone pode captar o clique que sai pelo alto-falante.

REC usa preparação padrão de três segundos; também há um compasso no BPM atual ou início imediato. O clique interno não entra nas exportações.

## Sequências e arranjo
Oito sequências, com 1 a 8 compassos, BPM global de 40 a 200 e swing. Selecione uma sequência com transporte parado; mantenha outro slot pressionado para duplicar a sequência ativa, após confirmação. Construa a música com até 16 partes e 64 compassos no total. A reprodução repete a ordem; a exportação do arranjo renderiza uma passagem.

As vozes são interrompidas nas fronteiras dos clipes; o delay continua entre eles. Não existe gravador contínuo multifaixa ou automação de pitch em piano roll.

## Mixer, efeitos e resampling
Mixer de 16 canais com nível, pan, mute/solo e picos amostrados. O master oferece passa-baixas, saturação e delay de colcheia sincronizado ao tempo. Alterações ao vivo guardam os valores finais ao parar; não viram pistas de automação.

Resampling interno renderiza a sequência com mix/FX, converte estéreo em mono e substitui o pad selecionado após confirmação. Não usa microfone. Os efeitos master podem processar novamente a amostra resultante quando ela for reproduzida.

## Exportação e persistência
Exporte mix WAV estéreo, arranjo WAV, 16 stems alinhados ou backup editável ZIP. Stems omitem efeitos master, mas mantêm nível, panorama, mute/solo e ganho master. Render corta caudas de amostra/efeito nos limites da sequência/música; arranjos densos podem diferir das 32 vozes máximas ao vivo. A saída usa hard clipping, sem limiter de masterização.

Schema 4 migra versões locais legíveis depois de uma gravação bem-sucedida. O ZIP editável inclui amostras, sequências, arranjo, ajustes e efeitos. Importação verifica caminhos, quantidade/tamanho das entradas e SHA-256 das amostras antes de confirmar substituição. WAV é áudio consolidado e não restaura edição de notas.

Undo/redo guarda até 20 ações, sujeito a orçamento de memória PCM compartilhada. Histórico de desfazer e tomadas não confirmadas não sobrevivem ao reinício. Salvamento do projeto é atômico.

## Instalação e limites
A avaliação 0.4 usa `dev.breda.beats.preview04`, ao lado da versão `.debug`; dados não são migrados automaticamente entre IDs. A assinatura CI é temporária. Atualizações comerciais exigem credenciais privadas estáveis, não versionadas.

Inspiração em samplers compactos, com marca própria. Sem promessa de paridade com Koala, SP-404 ou DAW desktop. Não há time-stretch independente, separação de stems por IA, MIDI, plugins, gravação contínua multifaixa ou automação. Sem afiliação à Roland/Akai; nenhum som proprietário dessas marcas é incluído. Latência, áudio e roteamento precisam de avaliação física.
