package com.mycompany.atividadeaula6chat;

import java.util.List;
import com.google.gson.Gson;

/**
 * PROTOCOLO do chat: define os tipos de mensagem trocados entre cliente e
 * servidor. Cada linha do socket (TCP, texto terminado em '\n') e um
 * Mensagem serializado em JSON pelo Gson, ex: {"tipo":"PRIVADA",...}.
 *
 * Tipos (campo "tipo"): BROADCAST, PRIVADA, LISTAR, SAIR, RESPOSTA, ERRO
 * e ARQUIVO (bonus). A 1a linha da conexao (o apelido) nao e um Mensagem,
 * e so texto cru (ver TarefaCliente).
 *
 * ARQUIVO (bonus, +1,0): o servidor so intermedia a CONEXAO (relay deste
 * convite com ip/porta), nunca o arquivo. Quem manda abre um ServerSocket
 * e avisa aqui o ip:porta; o arquivo trafega numa conexao TCP direta entre
 * os dois clientes, fora do ServidorChat (ver TelaChat.enviarArquivo /
 * receberOfertaArquivo).
 */
public class Mensagem {

    String tipo;
    String remetente;
    String destino;
    String texto;
    List<String> lista;

    // Campos do tipo ARQUIVO: dados do CONVITE (nunca o arquivo em si).
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
