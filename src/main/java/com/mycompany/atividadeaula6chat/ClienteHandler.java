package com.mycompany.atividadeaula6chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A TAREFA que o ThreadPool do ServidorChat executa para CADA cliente
 * conectado (implements Runnable, igual as Tarefas da Aula 1-3).
 *
 * Uma instancia desta classe = uma conexao = uma thread do pool cuidando
 * SO daquele cliente do inicio ao fim: le o apelido, registra, fica em
 * loop lendo mensagens (readLine bloqueia esperando a proxima linha) e
 * decide o que fazer de acordo com o "tipo" da Mensagem (ver Mensagem.java
 * pra a lista de tipos do protocolo).
 *
 * E exatamente essa separacao que permite que o servidor atenda varios
 * clientes ao mesmo tempo: enquanto esta thread espera o cliente X digitar
 * algo, outra thread do pool esta livre pra atender o cliente Y.
 */
public class ClienteHandler implements Runnable {

    private final Socket socket;
    private String apelido;
    private boolean registrado = false; // so vira true APOS passar pela validacao de apelido

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

            // Passo 1: a 1a linha que o cliente manda ao conectar e o apelido
            // (texto puro, nao e um Mensagem/JSON -- e so o nome mesmo).
            apelido = entrada.readLine();
            if (apelido == null || apelido.isBlank() || ServidorChat.existeApelido(apelido)) {
                // Requisito: "nao pode repetir apelido de quem ja esta logado".
                saida.println(new Mensagem("ERRO", "servidor", null, "Apelido invalido ou ja em uso").paraLinha());
                socket.close();
                return;
            }
            ServidorChat.registrar(apelido, saida);
            registrado = true; // so a partir daqui esse apelido "existe" de verdade pro resto do servidor
            ServidorChat.broadcast(new Mensagem("BROADCAST", "servidor", null, apelido + " entrou no chat"));

            // Passo 2: loop principal -- fica esperando (readLine bloqueia)
            // a proxima mensagem desse cliente especifico, ate ele sair ou
            // a conexao cair.
            String linha;
            while ((linha = entrada.readLine()) != null) {
                Mensagem msg = Mensagem.fromLinha(linha);
                msg.remetente = apelido;   // nunca confia no remetente que veio do cliente

                switch (msg.tipo) {
                    case "BROADCAST":
                        // "Enviar mensagem para todos os usuarios conectados"
                        ServidorChat.broadcast(msg);
                        break;
                    case "PRIVADA":
                        // "Enviar mensagem privada para um usuario especifico"
                        ServidorChat.enviarPrivada(msg);
                        break;
                    case "LISTAR":
                        // "Ver a lista dos usuarios logados no momento"
                        Mensagem resp = new Mensagem("RESPOSTA", "servidor", apelido, null);
                        resp.lista = ServidorChat.listarUsuarios();
                        saida.println(resp.paraLinha());
                        break;
                    case "SAIR":
                        // "Sair do chat, avisando os demais usuarios" (aviso
                        // acontece no finally, que roda sempre que o metodo termina)
                        socket.close();
                        return;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            // Cliente caiu sem avisar (ex.: fechou a janela na marra).
            // Requisito: "a saida ou a queda de um cliente nao pode derrubar
            // o servidor" -- por isso o catch aqui, so nesta thread, nao
            // afeta as outras conexoes nem o laco de accept() do servidor.
        } finally {
            // Roda tanto no SAIR normal quanto numa queda inesperada: garante
            // que o apelido sempre sai da lista e os outros sao avisados.
            // IMPORTANTE: so faz isso se "registrado" for true -- senao um
            // apelido REJEITADO (duplicado/invalido) geraria um "saiu do
            // chat" fantasma, porque nunca chegou a entrar de verdade.
            if (registrado) {
                ServidorChat.remover(apelido);
                ServidorChat.broadcast(new Mensagem("BROADCAST", "servidor", null, apelido + " saiu do chat"));
            }
        }
    }
}
