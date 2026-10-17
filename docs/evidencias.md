# Evidências e distribuição — Breda Beats Studio

A validação automatizada e a avaliação física são etapas distintas. Esta página aponta para a execução mais recente do workflow e explica como obter a prévia Android.

## Execuções e APK

- [GitHub Actions — Breda Beats](https://github.com/brunobredadeveloper/breda-beats/actions)
- Abra a execução mais recente de **Breda Beats tests and APK** concluída com sucesso e baixe o artefato `breda-beats-android-apk`.
- Extraia `preview/androidApp-preview.apk`. O GitHub pode exigir login; artefatos expiram conforme a política de retenção do repositório.
- A prévia 0.6.3 usa o pacote `dev.breda.beats.preview09` para coexistir com prévias anteriores. Cada instalação mantém projetos próprios; não desinstale a versão antiga se ainda precisar dos dados dela.

A esteira executa testes Kotlin, testes unitários Android, lint, builds Debug/Preview, assinatura/verificação do APK e testes de interface no emulador. Isso não substitui a avaliação auditiva no aparelho real.

## Capturas incorporadas

- [ui-portrait.png](imagens/ui-portrait.png) — pads fixos em retrato.
- [ui-pad-editor.png](imagens/ui-pad-editor.png) — editor do pad por toque longo.
- [ui-landscape.png](imagens/ui-landscape.png) — pads fixos em paisagem.

As capturas vêm de execução automatizada em emulador. Os dados exibidos são de demonstração e as imagens foram mantidas sem retoques. Elas não demonstram áudio, latência física ou persistência após reinstalação.

## Avaliação física pendente

Avalie navegação, acessibilidade, interrupções, recursos do aparelho e instalação/distribuição. Pitch altera velocidade/duração; vozes ao vivo são limitadas a 32. O render corta caudas nos limites e usa hard clipping; latência física não foi medida.

[Fluxos e checklist](fluxos.md) · [README](../README.md)
