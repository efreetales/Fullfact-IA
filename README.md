# Fullfact-IA — Debate em Contexto

Base Android experimental para acompanhar debates pela câmera e pelo microfone.

## Estado real desta entrega

- Código para visualização ao vivo da câmera traseira, com CameraX.
- Monitoramento local do nível do microfone: alerta após 3 segundos de sinal baixo e alerta de saturação.
- Cadastro manual persistente de nome e contexto dos participantes, com opção de apagar.
- Interrupção da câmera e do microfone ao sair do aplicativo.
- Fluxo de GitHub Actions configurado para testes, análise Android e geração de APK de depuração.

Consulte o resultado dos testes e a disponibilidade do APK na aba Actions. Teste em aparelho real ainda pendente.
O vídeo é uma visualização ao vivo; não é gravado ou analisado. O áudio é usado apenas para medir o sinal e é descartado.
O aplicativo não envia dados para serviços externos.

## O que falta para o produto proposto

1. Transcrição contínua com timestamps e detecção de fala, ruído, sobreposição e compreensão insuficiente. Volume alto não comprova inteligibilidade. Os limites atuais são provisórios e precisam de calibração em aparelhos e ambientes reais.
2. Setup multimodal: nome, contexto confirmado, foto de referência e amostra de voz com associação manual. Confirmar a qualidade da amostra antes de salvar. Oferecer edição e exclusão.
3. Identificação de turnos de fala. Não atribuir a voz ao rosto visível automaticamente: uma transmissão pode mostrar alguém enquanto outro fala. Havendo dúvida, mostrar “Participante não identificado” e permitir correção.
4. Captura de tela e áudio de outros aplicativos via autorização explícita do Android e serviço de captura em primeiro plano. Respeitar conteúdo protegido e bloqueios de áudio. Não presumir suporte universal a YouTube ou streaming.
5. Pesquisa de contexto dos participantes com fontes e data; separar informações informadas pelo usuário de informações verificadas.
6. Extração de afirmações factuais; busca de checagens existentes; pesquisa em fontes originais; comparação de período, território e indicador.
7. Resultados com trecho literal, autor confirmado ou incerto, horário, fontes, data, justificativa e estado: Em checagem, Verdadeiro, Falso, Enganoso, Fora de contexto ou Evidência insuficiente. Nunca preencher resultados fictícios.
8. Serviço seguro de transcrição e pesquisa, com credenciais fora do APK. Definir custos, retenção de mídia e consentimento antes de enviar gravações.

## Como gerar um APK quando o projeto estiver no GitHub

1. Abra o repositório no GitHub e clique em **Actions**.
2. Abra **Gerar APK de teste** e clique em **Run workflow**.
3. Aguarde a execução ficar verde. Se falhar, o APK ainda não está disponível.
4. Abra a execução e baixe **debate-em-contexto-apk-teste**, na área **Artifacts**.
5. Descompacte o arquivo baixado. O aplicativo de teste será o arquivo **app-debug.apk**.

É uma versão de teste, não uma versão pronta para loja. A configuração usa Java 17, Gradle 8.11.1, Android Gradle Plugin 8.9.2 e SDK 35.
Repositório: https://github.com/efreetales/Fullfact-IA. Não há assinatura de publicação configurada.

## Verificação em um Android real

- Permitir câmera e microfone e conferir a imagem da TV.
- Negar permissões e conferir a orientação exibida.
- Manter silêncio por mais de 3 segundos e conferir o aviso; retomar o áudio e conferir a recuperação.
- Conferir o modo somente microfone sem pedir câmera.
- Sair do aplicativo e conferir que os indicadores de câmera e microfone se apagam.
- Trocar rapidamente entre os modos e verificar se o microfone continua funcionando.
- Cadastrar participantes, fechar e reabrir; conferir a persistência; apagar e conferir a exclusão.
- Validar rotação, tamanho de texto e aparelhos com câmeras diferentes.

## Fontes técnicas

- CameraX: https://developer.android.com/media/camera/camerax
- AudioRecord: https://developer.android.com/reference/android/media/AudioRecord
- Captura de áudio de outros aplicativos: https://developer.android.com/media/platform/av-capture
- Compartilhamento de tela: https://developer.android.com/media/grow/media-projection
