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
 * SERVIDOR central do chat TCP (arquitetura cliente-servidor centralizada,
 * como pede o enunciado: todo mundo fala com o servidor, ninguem conhece o
 * Socket dos outros clientes diretamente).
 *
 * REGIAO CRITICA: o mapa "usuarios" guarda apelido -> saida (PrintWriter) de
 * cada cliente conectado. Ele fica em memoria aqui no servidor e e lido e
 * escrito por VARIAS threads ao mesmo tempo (uma ClienteHandler por cliente
 * conectado) -- exatamente a situacao que a Aula 3 chamou de Regiao Critica.
 * Por isso NENHUM metodo mexe em "usuarios" direto: tudo passa por um dos
 * metodos synchronized abaixo, que garantem exclusao mutua (so uma thread
 * por vez mexendo no mapa).
 *
 * THREAD POOL: o enunciado pede "atender varios clientes ao mesmo tempo
 * usando um pool de threads" (igual a Aula 3, Coringa 4). Cada cliente que
 * conecta vira uma tarefa (ClienteHandler) que o pool executa numa thread
 * livre -- assim o servidor nao trava esperando um cliente digitar.
 */
public class ServidorChat {

    // Regiao Critica: NUNCA acessar "usuarios" fora de um metodo synchronized.
    private static final Map<String, PrintWriter> usuarios = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket servidor = new ServerSocket(9999);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        System.out.println("Servidor de chat rodando na porta 9999...");

        // Servidor iterativo "infinito": nunca para de aceitar conexoes novas.
        // A saida ou queda de UM cliente (ClienteHandler) nao afeta este laco.
        while (true) {
            Socket cliente = servidor.accept();          // bloqueia ate alguem conectar
            pool.execute(new ClienteHandler(cliente));    // entrega pro pool e volta direto pro accept()
        }
    }

    // ---- Os metodos abaixo sao a unica porta de entrada pra Regiao Critica ----

    static synchronized boolean existeApelido(String apelido) {
        return usuarios.containsKey(apelido);
    }

    static synchronized void registrar(String apelido, PrintWriter saida) {
        usuarios.put(apelido, saida);
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

    static synchronized void enviarPrivada(Mensagem msg) {
        PrintWriter saida = usuarios.get(msg.destino);
        if (saida != null) {
            saida.println(msg.paraLinha());
        }
        // se saida == null, o destinatario nao existe/desconectou: por
        // simplicidade so ignoramos (dava pra responder ERRO pro remetente).
    }
}
