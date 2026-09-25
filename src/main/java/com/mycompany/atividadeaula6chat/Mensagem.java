package com.mycompany.atividadeaula6chat;

import java.util.List;
import com.google.gson.Gson;

// Protocolo do chat: cada linha do socket e um Mensagem serializado em JSON.
// Tipos: BROADCAST, PRIVADA, LISTAR, SAIR, RESPOSTA, ERRO e ARQUIVO (bonus).
public class Mensagem {

    String tipo;
    String remetente;
    String destino;
    String texto;
    List<String> lista;

    // campos do tipo ARQUIVO: dados do convite (nunca o arquivo em si)
    String nomeArquivo;
    long tamanhoArquivo;
    String ip;
    int porta;

    // Construtor vazio: exigido pelo Gson para reconstruir o objeto do JSON.
    public Mensagem() {
    }

    public Mensagem(String tipo, String remetente, String destino, String texto) {
        this.tipo = tipo;
        this.remetente = remetente;
        this.destino = destino;
        this.texto = texto;
    }

    // Monta o convite de transferencia direta (bonus).
    public static Mensagem novaOfertaArquivo(String remetente, String destino, String nomeArquivo,
            long tamanhoArquivo, String ip, int porta) {
        Mensagem msg = new Mensagem("ARQUIVO", remetente, destino, null);
        msg.nomeArquivo = nomeArquivo;
        msg.tamanhoArquivo = tamanhoArquivo;
        msg.ip = ip;
        msg.porta = porta;
        return msg;
    }

    // Objeto -> texto (uma linha JSON), pronto pra mandar no println() do socket.
    public String paraLinha() {
        return new Gson().toJson(this);
    }

    // Texto (uma linha JSON) -> objeto, depois de ler com readLine() no socket.
    public static Mensagem fromLinha(String linha) {
        return new Gson().fromJson(linha, Mensagem.class);
    }
}
