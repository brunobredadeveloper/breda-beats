# Validação — Breda Beats Studio 0.4

A [execução 38422254625](https://github.com/brunobredadeveloper/breda-beats/actions/runs/38422254625) concluiu com sucesso em 13/10/2026. Revisão: `ec06ee33cd8cebd2fb6864cfdc42b7e75aba7150`.

- Registro de validação anterior: 32 testes de núcleo, 11 unitários/ViewModel Android e três testes nativos de UI, total de 46 aprovados, sem falhas/ignorados.
- Cobertura inclui PCM/WAV, reverse/pitch, loop/choke, sequências, cópia/troca, efeitos, backup com caminhos/hashes e preservação de metadados.
- Lint registrado sem erros e com quatro avisos de versões de dependências.
- APKs debug, preview e instrumentação gerados; assinaturas v2/v3 verificadas na validação original.
- Capturas reais recuperadas do artefato de interface estão em [fluxos](fluxos.md). API 35/Pixel 2, com pads fixos em retrato e paisagem e editor por toque longo.
- APK preview registrado: 11.964.481 bytes; SHA-256 `b29ecee5bf46c92e26be52eb9315211190c3110ebd6e6528a57bd68fcf83cbbb`.

Esses números descrevem a execução referenciada, não uma nova medição feita pela atualização documental. A consulta de 14/10/2026 confirmou o estado remoto e a existência dos artefatos.

## Gravação e preparação
Padrão: três segundos, com preparação audível. Um compasso em 120 BPM dura dois segundos; NONE inicia imediatamente. Preparação é independente do botão de metrônomo. Não há captura de notas/microfone antes da preparação terminar. STOP, segundo REC, background e MIC OFF durante preparação de microfone cancelam a gravação pendente.

## Ainda avaliar no aparelho
Som físico, latência de toque, roteamento, Bluetooth/fones com fio, microfone, fonte ampliada e telas compactas. Assinatura temporária pode divergir do APK antigo. Exporte áudio/backup antes de desinstalar: isso apaga dados privados. Sem garantia de latência profissional.

[Guia do produto](STUDIO-04.md) · [Roteiro manual](TESTING-04.md) · [Evidências atuais](evidencias.md)
