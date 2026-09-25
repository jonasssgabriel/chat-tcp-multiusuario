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

// Servidor central: ServerSocket + ThreadPool + mapa de usuarios conectados.
// O mapa e a Regiao Critica, so acessada pelos metodos synchronized abaixo.
public class ServidorChat {

    // Regiao Critica: NUNCA acessar "usuarios" fora de um metodo synchronized.
    private static final Map<String, PrintWriter> usuarios = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(9999);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        System.out.println("Servidor de chat rodando na porta 9999...");

        // nunca para de aceitar conexoes; a queda de um cliente nao afeta o laco
        while (true) {
            Socket cliente = servidor.accept();          // bloqueia ate alguem conectar
            pool.execute(new TarefaCliente(cliente));    // entrega pro pool e volta direto pro accept()
        }
    }

    // ---- Os metodos abaixo sao a unica porta de entrada pra Regiao Critica ----

    // checa e registra na mesma chamada synchronized, pra evitar corrida
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
        // devolve uma copia, pra nao expor o mapa original fora do lock
        return new ArrayList<>(usuarios.keySet());
    }

    static synchronized void broadcast(Mensagem msg) {
        for (PrintWriter saida : usuarios.values()) {
            saida.println(msg.paraLinha());
        }
    }

    // repassa so pro apelido em msg.destino (usada por PRIVADA e ARQUIVO)
    static synchronized void enviarPrivada(Mensagem msg) {
        PrintWriter saida = usuarios.get(msg.destino);
        if (saida != null) {
            saida.println(msg.paraLinha());
        }
    }
}
