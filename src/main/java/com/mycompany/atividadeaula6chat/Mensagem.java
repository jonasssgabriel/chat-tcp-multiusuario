package com.mycompany.atividadeaula6chat;

import java.util.List;
import com.google.gson.Gson;

/**
 * PROTOCOLO do chat: o "idioma" combinado entre cliente e servidor.
 *
 * O enunciado pede: "o grupo deve definir um protocolo para a troca de
 * mensagens... quais tipos podem ser trocados, quais informacoes cada
 * mensagem deve conter e como serao representadas".
 *
 * Aqui a resposta e essa classe. Cada linha que passa pelo socket (TCP,
 * texto terminado em '\n') e um objeto Mensagem serializado em JSON pelo
 * Gson -- exatamente como no exemplo do enunciado:
 *   {"tipo":"PRIVADA","destino":"Joao","mensagem":"..."}
 *
 * TIPOS DE MENSAGEM (campo "tipo"):
 *   ENTRAR    - nao usamos como mensagem separada: o apelido e a 1a linha
 *               bruta que o cliente manda ao conectar (ver TarefaCliente).
 *   BROADCAST - mensagem para todo mundo que esta conectado.
 *   PRIVADA   - mensagem so para o apelido indicado em "destino".
 *   LISTAR    - pedido do cliente para saber quem esta conectado agora.
 *   SAIR      - aviso de que o cliente vai desconectar.
 *   RESPOSTA  - o servidor usa para responder ao LISTAR (traz "lista").
 *   ERRO      - o servidor usa quando o apelido e invalido/repetido.
 *   ARQUIVO   - envio de arquivo para um apelido especifico (bonus da
 *               atividade). Usa a MESMA rota da PRIVADA no servidor (ver
 *               ServidorChat.enviarPrivada): o servidor so repassa, nao
 *               guarda o arquivo em disco em nenhum momento.
 *
 * Campos que cada tipo usa:
 *   ENTRAR    -> so a linha crua com o apelido (nao e um Mensagem)
 *   BROADCAST -> remetente, texto
 *   PRIVADA   -> remetente, destino, texto
 *   LISTAR    -> remetente (destino e texto ficam vazios)
 *   SAIR      -> remetente
 *   RESPOSTA  -> lista (a lista de apelidos conectados)
 *   ERRO      -> texto (a mensagem de erro)
 *   ARQUIVO   -> remetente, destino, nomeArquivo, dadosArquivo
 */
public class Mensagem {

    String tipo;
    String remetente;
    String destino;
    String texto;
    List<String> lista;

    // Campos usados so pelo tipo ARQUIVO: o nome original do arquivo e o
    // conteudo dele inteiro convertido pra texto em Base64. Base64 existe
    // justamente pra isso -- transformar bytes quaisquer (um PDF, uma
    // imagem) numa string sem quebra de linha, que cabe numa unica linha
    // do protocolo (tudo aqui viaja como texto, readLine() por readLine()).
    String nomeArquivo;
    String dadosArquivo;

    // Construtor vazio: o Gson PRECISA dele para reconstruir o objeto a
    // partir do JSON (fromLinha). Sem isso o desserializador nao funciona.
    public Mensagem() {
    }

    public Mensagem(String tipo, String remetente, String destino, String texto) {
        this.tipo = tipo;
        this.remetente = remetente;
        this.destino = destino;
        this.texto = texto;
    }

    // Construtor usado so para montar uma mensagem do tipo ARQUIVO.
    public static Mensagem novoArquivo(String remetente, String destino, String nomeArquivo, String dadosArquivo) {
        Mensagem msg = new Mensagem("ARQUIVO", remetente, destino, null);
        msg.nomeArquivo = nomeArquivo;
        msg.dadosArquivo = dadosArquivo;
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
