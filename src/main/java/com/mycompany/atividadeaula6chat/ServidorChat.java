package com.mycompany.atividadeaula6chat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor central do chat TCP: ServerSocket + ThreadPool (uma TarefaCliente
 * por conexao) + mapa de usuarios conectados. O mapa e a Regiao Critica
 * (Aula 3) -- so acessado pelos metodos synchronized abaixo.
 */
public class ServidorChat {

    // Regiao Critica: NUNCA acessar "usuarios" fora de um metodo synchronized.
    private static final Map<String, PrintWriter> usuarios = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(9999);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        System.out.println("Servidor de chat rodando na porta 9999...");

        // Servidor iterativo "infinito": nunca para de aceitar conexoes novas.
        // A saida ou queda de UM cliente (TarefaCliente) nao afeta este laco.
        while (true) {
            Socket cliente = servidor.accept();          // bloqueia ate alguem conectar
            pool.execute(new TarefaCliente(cliente));    // entrega pro pool e volta direto pro accept()
        }
    }

    // ---- Os metodos abaixo sao a unica porta de entrada pra Regiao Critica ----

    // Checa e registra o apelido na MESMA chamada synchronized (check-then-act
    // atomico) -- em dois metodos separados, duas threads poderiam passar
    // pela checagem antes de qualquer uma registrar.
    static synchronized boolean registrarSeLivre(String apelido, PrintWriter saida) {
        if (usuarios.containsKey(apelido)) {
            return false;
        }
        usuarios.put(apelido, saida);
        return true;
    }

    static synchronized void remover(String apelido) {
        usuarios.remove(apelido);
    }

    static synchronized List<String> listarUsuarios() {
        // devolve uma COPIA da lista de apelidos: quem chamou pode mexer
        // nela a vontade sem risco de interferir no mapa original.
        return new ArrayList<>(usuarios.keySet());
    }

    static synchronized void broadcast(Mensagem msg) {
        for (PrintWriter saida : usuarios.values()) {
            saida.println(msg.paraLinha());
        }
    }

    // Repassa a mensagem so pro apelido em msg.destino. Usada pela PRIVADA e
    // pelo convite ARQUIVO (bonus) -- neste ultimo caso so o convite (ip e
    // porta) passa por aqui, nunca o arquivo: essa e a "intermediacao da
    // conexao" que o enunciado pede, sem o servidor tocar no arquivo.
    static synchronized void enviarPrivada(Mensagem msg) {
        PrintWriter saida = usuarios.get(msg.destino);
        if (saida != null) {
            saida.println(msg.paraLinha());
        }
        // se saida == null, o destinatario nao existe/desconectou: por
        // simplicidade so ignoramos (dava pra responder ERRO pro remetente).
    }
}
