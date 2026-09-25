package com.mycompany.atividadeaula6chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Tarefa (Runnable) que o ThreadPool do ServidorChat executa para CADA
 * cliente conectado: le o apelido, registra, e fica em loop lendo mensagens
 * ate o cliente sair ou cair. Uma thread por cliente = servidor atende
 * varios ao mesmo tempo sem travar esperando um digitar.
 */
public class TarefaCliente implements Runnable {

    private final Socket socket;
    private String apelido;
    private boolean registrado = false; // so vira true APOS passar pela validacao de apelido

    public TarefaCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);

            // 1a linha da conexao = o apelido, texto puro (nao e JSON).
            apelido = entrada.readLine();
            if (apelido == null || apelido.isBlank() || !ServidorChat.registrarSeLivre(apelido, saida)) {
                saida.println(new Mensagem("ERRO", "servidor", null, "Apelido invalido ou ja em uso").paraLinha());
                socket.close();
                return;
            }
            registrado = true;
            ServidorChat.broadcast(new Mensagem("BROADCAST", "servidor", null, apelido + " entrou no chat"));

            // Loop principal: espera (readLine bloqueia) a proxima mensagem
            // deste cliente, ate ele sair ou a conexao cair.
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
                        ServidorChat.enviarPrivada(msg);
                        saida.println(msg.paraLinha()); // eco pro remetente ver a propria mensagem
                        break;
                    case "ARQUIVO":
                        // Bonus: so repassa o CONVITE (ip/porta); o arquivo
                        // em si viaja direto entre os clientes, fora daqui.
                        ServidorChat.enviarPrivada(msg);
                        saida.println(msg.paraLinha());
                        break;
                    case "LISTAR":
                        Mensagem resp = new Mensagem("RESPOSTA", "servidor", apelido, null);
                        resp.lista = ServidorChat.listarUsuarios();
                        saida.println(resp.paraLinha());
                        break;
                    case "SAIR":
                        socket.close(); // aviso aos demais acontece no finally
                        return;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            // Cliente caiu sem avisar -- so afeta esta thread, nao o servidor.
        } finally {
            // So remove/avisa se chegou a registrar (evita "saida fantasma"
            // de um apelido que foi rejeitado).
            if (registrado) {
                ServidorChat.remover(apelido);
                ServidorChat.broadcast(new Mensagem("BROADCAST", "servidor", null, apelido + " saiu do chat"));
            }
        }
    }
}
