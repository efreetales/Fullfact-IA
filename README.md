# Fullfact-IA

Aplicativo Android experimental para testar transcrição e checagem automatizada de debates com fontes.

[**Baixar o APK mais recente**](https://github.com/efreetales/Fullfact-IA/releases/latest/download/Fullfact-IA.apk)

## Testar no celular

1. Instale o APK mais recente e abra **Preparar**.
2. Cole seu código de ativação pessoal do Fullfact-IA e toque em **Ativar e verificar conexão**. Não é a chave do Gemini. O código fica protegido pelo Android Keystore.
3. Escolha **Vídeo público do YouTube**, informe o link e o horário inicial. O vídeo de referência do teste já vem preenchido em 10:18.
4. Toque em **Continuar para acompanhamento** e em **Analisar 25 segundos deste ponto**.
5. Confira a transcrição e o andamento. Abra **Checagens** para ver afirmações selecionadas, explicação e fontes clicáveis. Alguns trechos contêm apenas opiniões, promessas ou frases incompletas e não geram checagens.
6. Opcionalmente, ative a análise dos próximos trechos em sequência. O processamento segue o horário escolhido, não os comandos de pausa e busca do player. Ao sair da tela ou do aplicativo, o avanço automático para.

## Captar a TV

Escolha câmera + microfone ou somente áudio. Após o aviso e sua autorização, o app coleta trechos de aproximadamente 20 segundos com pequena sobreposição e envia ao serviço para análise. No modo câmera, uma imagem reduzida da transmissão é enviada junto como contexto visual.

Volume baixo, distorção e fala pouco compreensível recebem orientações distintas. Enquanto o serviço está ocupado, novos intervalos podem não ser enviados; o app informa esse caso. Ao sair da tela de acompanhamento, a captação para. O trecho parcial final, menor que uma janela completa, não é enviado.

## O que esta versão faz

- Interface Android com tema claro, cartões, campos e navegação em três etapas.
- Leitura de áudio e imagem de trechos de vídeo público do YouTube pelo Gemini, com horário definido.
- Captação real da câmera e do microfone para uma TV ou segundo dispositivo.
- Transcrição e seleção de até duas afirmações factuais completas por trecho.
- Pesquisa com Google Search e resultados com explicação, fontes vinculadas e data da checagem.
- Bloqueio de classificação definitiva quando faltam evidências vinculadas à resposta.
- Cadastro manual dos nomes e contexto; campo de data do debate.
- Análise de uma fala colada em texto para revisar transcrição ou testar uma afirmação.

## Limitações de teste

- Não há reconhecimento biométrico de rosto ou voz nem identificação automática confiável dos participantes. A autoria permanece não identificada quando não confirmada.
- Não há captura da tela de outros aplicativos, de streamings protegidos ou suporte garantido a transmissões ao vivo. O modo YouTube usa a leitura do vídeo público pelo link.
- A pesquisa e a transcrição são automatizadas e podem conter erros. A conclusão precisa ser conferida com as fontes; o app não determina intenção ou mentira deliberada.
- Um teste com Gemini e fontes reais valida o fluxo, não comprova a precisão em todos os debates.
- O acesso pessoal tem limite diário de requisições e prazo de validade. Pedidos sem código são bloqueados.
- A chave do Gemini fica no cofre do serviço Supabase e não é incluída no APK ou no repositório.
- Não armazenamos áudio, imagens ou transcrições no banco Fullfact-IA. O provedor de IA aplica suas próprias condições de retenção e uso dos dados.

## Gerar e baixar novas versões

Em **Actions**, abra **Gerar APK de teste**, toque em **Run workflow**, selecione **main** e confirme. A publicação em **Releases** só acontece se testes, análise do código e conferência de navegação no Android emulado passarem. O link de download acima é atualizado após cada publicação bem-sucedida.

A compilação usa Java 17, Gradle 8.11.1, AGP 8.9.2 e SDK 35. A versão mínima é Android 9. A assinatura de teste é mantida em cache para as novas compilações; esta não é uma assinatura de publicação na loja.

## Serviço

Código em `supabase/functions/fullfact`. As rotas exigem um código pessoal e impõem limite de uso no servidor. O serviço usa Gemini 3.5 Flash-Lite com pesquisa Google Search. A chave é acessada por uma função restrita ao servidor. Nenhuma chave real deve ser adicionada ao código, arquivos de configuração ou variáveis de compilação do APK.

Testes do processamento: `node --test supabase/tests/engine.test.mjs`.
