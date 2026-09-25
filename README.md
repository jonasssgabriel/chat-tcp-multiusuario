# Chat TCP multiusuário — Atividade Aula 6 (SPD)

Chat com arquitetura cliente-servidor centralizada, usando Socket TCP em Java.

## Classes

| Classe | Função |
|---|---|
| `Mensagem.java` | Protocolo do chat (JSON via Gson): define os tipos de mensagem trocados entre cliente e servidor |
| `ServidorChat.java` | `main` do servidor: `ServerSocket` na porta 9999 + pool de threads (`ExecutorService`) + lista de usuários conectados (Região Crítica sincronizada) |
| `TarefaCliente.java` | Tarefa (`Runnable`) executada numa thread do pool para cada cliente conectado |
| `TelaChat.java` | `main` do cliente: interface Swing (JFrame) com thread separada para receber mensagens sem travar a digitação |

## Protocolo

Cada linha trocada pelo socket é um objeto `Mensagem` serializado em JSON. Campo `tipo`:

- `BROADCAST` — mensagem para todos os conectados
- `PRIVADA` — mensagem só para o apelido em `destino`
- `LISTAR` — pede a lista de quem está conectado
- `SAIR` — avisa que vai desconectar
- `RESPOSTA` — o servidor usa para responder o `LISTAR` (traz o campo `lista`)
- `ERRO` — o servidor usa quando o apelido é inválido ou já está em uso
- `ARQUIVO` — convite de transferência de arquivo para um apelido específico (bônus, +1,0). O servidor só repassa o convite (`nomeArquivo`, `tamanhoArquivo`, `ip`, `porta`) pela mesma rota da `PRIVADA` — **o arquivo em si nunca passa pelo servidor**. Quem envia abre um `ServerSocket` próprio e espera; quem recebe, ao aceitar, conecta direto nesse `ip:porta` (uma conexão TCP nova, fora do `ServidorChat`) e baixa os bytes, salvando em `arquivos_recebidos/`.

A primeira linha enviada pelo cliente ao conectar é só o apelido (texto puro, sem JSON).

Exemplo de mensagem privada:
```json
{"tipo":"PRIVADA","remetente":"Ana","destino":"Joao","texto":"Você terminou?"}
```

## Como rodar no NetBeans

1. `File > Open Project...` → selecione esta pasta.
2. Deixe o Maven baixar o `gson` (precisa de internet na primeira vez).
3. Rode `ServidorChat.java` (botão direito → Run File).
4. Rode `TelaChat.java` duas ou mais vezes (uma janela por usuário), cada uma com um apelido diferente.
5. Em cada janela: digite o apelido → Conectar. Depois use os campos e botões (Enviar Todos, Enviar Privada, Listar, Enviar Arquivo, Sair).
6. Para enviar arquivo: preencha o destino, clique em "Enviar Arquivo" e escolha o arquivo na janela que abre. Quem recebe vê um diálogo perguntando se aceita; ao aceitar, a conexão TCP direta é aberta e o arquivo é salvo em `arquivos_recebidos/` (criada automaticamente do lado de quem recebe).

## Teste de concorrência

Testado com 3 clientes simultâneos (broadcast, privada e listagem ao mesmo tempo, e saída de um cliente
no meio da conversa) — servidor e demais clientes continuam funcionando normalmente.
