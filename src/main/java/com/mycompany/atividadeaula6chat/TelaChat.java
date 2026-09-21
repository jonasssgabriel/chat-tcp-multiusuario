package com.mycompany.atividadeaula6chat;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * TELA (cliente) do chat -- Swing puro, no mesmo espirito das telas feitas
 * em aula (TelaHello, TelaBusca, TelaCliente): JFrame com JLabel, JTextField,
 * JButton e JTextArea, so que escrita a mao em vez de gerada pelo editor
 * visual do NetBeans. Dentro do NetBeans o projeto abre e roda normalmente;
 * so nao aparece no modo "Design" (arrastar/soltar), porque falta o arquivo
 * .form -- para MEXER no layout, edita direto o metodo montarTela() abaixo.
 *
 * Por que uma THREAD SEPARADA para receber mensagens (ver conectar()):
 * o requisito diz "o cliente deve continuar recebendo mensagens enquanto o
 * usuario digita (o recebimento nao pode travar a digitacao)". O metodo
 * entrada.readLine() BLOQUEIA esperando o servidor mandar algo -- se isso
 * rodasse na mesma thread da tela, a janela inteira congelaria toda vez que
 * nao houvesse mensagem chegando. Por isso a leitura roda numa Thread por
 * conta propria, igual ao Coringa 10 do guia de threads.
 *
 * SwingUtilities.invokeLater: componentes Swing (JTextArea etc.) SO podem
 * ser alterados pela thread da interface grafica (a "Event Dispatch
 * Thread"). Como a mensagem chega numa thread diferente (a de leitura),
 * toda atualizacao da tela precisa ser "empacotada" com invokeLater, que
 * entrega esse pedaco de codigo pra thread certa executar.
 */
public class TelaChat extends JFrame {

    private JTextField tfApelido;
    private JButton btnConectar;
    private JTextArea taChat;
    private JTextField tfDestino;
    private JTextField tfMensagem;
    private JButton btnTodos;
    private JButton btnPrivada;
    private JButton btnListar;
    private JButton btnSair;

    private Socket socket;
    private PrintWriter saida;
    private String apelido;

    public TelaChat() {
        super("Chat TCP");
        montarTela();
        atualizarEstadoConectado(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 480);
        setLocationRelativeTo(null);
    }

    private void montarTela() {
        setLayout(new BorderLayout(5, 5));

        // ---- topo: apelido + conectar ----
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tfApelido = new JTextField(12);
        btnConectar = new JButton("Conectar");
        painelTopo.add(new JLabel("Apelido:"));
        painelTopo.add(tfApelido);
        painelTopo.add(btnConectar);
        add(painelTopo, BorderLayout.NORTH);

        // ---- centro: area de chat (log das mensagens) ----
        taChat = new JTextArea();
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        add(new JScrollPane(taChat), BorderLayout.CENTER);

        // ---- baixo: destino (privada) + mensagem + botoes ----
        JPanel painelBaixo = new JPanel(new BorderLayout(5, 5));

        JPanel painelDestino = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tfDestino = new JTextField(10);
        painelDestino.add(new JLabel("Destino (so p/ Privada):"));
        painelDestino.add(tfDestino);
        painelBaixo.add(painelDestino, BorderLayout.NORTH);

        tfMensagem = new JTextField();
        painelBaixo.add(tfMensagem, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new GridLayout(1, 4, 5, 0));
        btnTodos = new JButton("Enviar Todos");
        btnPrivada = new JButton("Enviar Privada");
        btnListar = new JButton("Listar");
        btnSair = new JButton("Sair");
        painelBotoes.add(btnTodos);
        painelBotoes.add(btnPrivada);
        painelBotoes.add(btnListar);
        painelBotoes.add(btnSair);
        painelBaixo.add(painelBotoes, BorderLayout.SOUTH);

        add(painelBaixo, BorderLayout.SOUTH);

        // ---- ligar os eventos aos metodos (equivalente ao duplo-clique
        // no NetBeans, que gera o xxxActionPerformed automaticamente) ----
        btnConectar.addActionListener(e -> conectar());
        btnTodos.addActionListener(e -> enviarTodos());
        btnPrivada.addActionListener(e -> enviarPrivada());
        btnListar.addActionListener(e -> listar());
        btnSair.addActionListener(e -> sair());
        tfMensagem.addActionListener(e -> enviarTodos()); // Enter = enviar pra todos
    }

    // habilita/desabilita os controles de acordo com o estado da conexao
    private void atualizarEstadoConectado(boolean conectado) {
        tfApelido.setEnabled(!conectado);
        btnConectar.setEnabled(!conectado);
        tfDestino.setEnabled(conectado);
        tfMensagem.setEnabled(conectado);
        btnTodos.setEnabled(conectado);
        btnPrivada.setEnabled(conectado);
        btnListar.setEnabled(conectado);
        btnSair.setEnabled(conectado);
    }

    private void log(String texto) {
        taChat.append(texto + "\n");
    }

    // ---- botao Conectar ----
    private void conectar() {
        apelido = tfApelido.getText().trim();
        if (apelido.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite um apelido.");
            return;
        }
        try {
            socket = new Socket("localhost", 9999);
            saida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            saida.println(apelido); // 1a linha da conexao = apelido (ver ClienteHandler)

            // Thread separada SO para ficar recebendo mensagens do servidor,
            // pra nao travar a tela enquanto o usuario digita (ver comentario
            // da classe, no topo do arquivo).
            new Thread(() -> receberMensagens(entrada)).start();

            atualizarEstadoConectado(true);
            log("Conectado como " + apelido + ". Use os botoes abaixo.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Nao foi possivel conectar: " + ex.getMessage());
        }
    }

    // roda inteira dentro da thread de leitura (nunca na thread da tela)
    private void receberMensagens(BufferedReader entrada) {
        try {
            String linha;
            while ((linha = entrada.readLine()) != null) {
                Mensagem msg = Mensagem.fromLinha(linha);
                String texto = formatar(msg);
                // aqui e onde o invokeLater entra: "texto" foi montado nesta
                // thread, mas quem escreve na JTextArea tem que ser a EDT.
                SwingUtilities.invokeLater(() -> log(texto));
            }
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> {
                log("Conexao encerrada.");
                atualizarEstadoConectado(false);
            });
        }
    }

    private String formatar(Mensagem msg) {
        if ("RESPOSTA".equals(msg.tipo) && msg.lista != null) {
            return "Usuarios conectados: " + msg.lista;
        }
        if ("ERRO".equals(msg.tipo)) {
            return "[ERRO] " + msg.texto;
        }
        if ("PRIVADA".equals(msg.tipo)) {
            return "(privado) " + msg.remetente + ": " + msg.texto;
        }
        return msg.remetente + ": " + msg.texto;
    }

    // ---- botao Enviar Todos ----
    private void enviarTodos() {
        String texto = tfMensagem.getText().trim();
        if (texto.isEmpty()) {
            return;
        }
        saida.println(new Mensagem("BROADCAST", apelido, null, texto).paraLinha());
        tfMensagem.setText("");
    }

    // ---- botao Enviar Privada ----
    private void enviarPrivada() {
        String destino = tfDestino.getText().trim();
        String texto = tfMensagem.getText().trim();
        if (destino.isEmpty() || texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha o destino e a mensagem.");
            return;
        }
        saida.println(new Mensagem("PRIVADA", apelido, destino, texto).paraLinha());
        tfMensagem.setText("");
    }

    // ---- botao Listar ----
    private void listar() {
        saida.println(new Mensagem("LISTAR", apelido, null, null).paraLinha());
    }

    // ---- botao Sair ----
    private void sair() {
        try {
            saida.println(new Mensagem("SAIR", apelido, null, null).paraLinha());
            socket.close();
        } catch (IOException ex) {
            // ja estava fechando mesmo, pode ignorar
        }
        atualizarEstadoConectado(false);
        log("Voce saiu do chat.");
    }

    public static void main(String[] args) {
        // cria a tela na thread certa do Swing (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> new TelaChat().setVisible(true));
    }
}
