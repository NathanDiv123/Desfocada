# Mirage Auto (APK)

App Android que faz o auto-farm sozinho (andar → atacar → skill → curar) usando um
serviço de acessibilidade, com painel flutuante **Iniciar / Parar / Calibrar**.
Não precisa de root, de PC nem de Termux depois de instalado.

> Este projeto foi escrito mas **não foi compilado nem testado** no ambiente onde foi
> gerado. Se o build der erro, mande a mensagem de erro que eu corrijo.

## Gerar o APK

### Opção A: GitHub Actions (não precisa instalar nada)
1. Crie um repositório no GitHub e envie todo o conteúdo desta pasta (incluindo `.github/`).
2. Abra a aba **Actions** > **Build APK** > **Run workflow**.
3. Quando terminar, abra a execução e baixe o artefato **mirage-auto-apk** (é um .zip com o `app-debug.apk`).

### Opção B: Android Studio
1. Abra esta pasta no Android Studio.
2. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar e usar
1. Instale o APK no celular (permita "instalar apps desconhecidos").
2. Abra o **Mirage Auto** > **Abrir Acessibilidade** > ative o serviço **Mirage Auto**.
   - Android 13+: se a opção estiver acinzentada, vá em Informações do app > menu ⋮ >
     **Permitir configurações restritas** e tente de novo.
3. Ajuste os parâmetros do ciclo no app (opcional) e toque em **Salvar parâmetros**.
4. Abra o Mirage Realms. O painel flutuante aparece (arraste pelo ≡).
5. Toque em **Calibrar** e, na ordem, toque no botão de **ataque**, **skill** (ou Pular),
   **cura** (ou Pular) e no **centro do joystick**.
6. Toque em **▶ Iniciar**. Para parar, **⏸ Parar** (ou desative o serviço em Acessibilidade).

Ele para sozinho após o tempo definido (padrão 120 min).

## Observações
- O app não enxerga a tela: não detecta HP baixo, morte ou monstros.
- Calibre de novo se mudar a orientação da tela ou a posição dos botões do jogo.
- Automação pode ser proibida pelas regras do jogo e render ban. Use por sua conta e risco.
