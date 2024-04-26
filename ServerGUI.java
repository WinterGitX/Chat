import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerGUI extends JFrame {
    private JTextArea textArea; // Area di testo per visualizzare informazioni di sistema e log.
    private JTextArea chatArea; // Area di testo dedicata alla chat tra client e server.
    private JTextField chatInput; // Campo di input per scrivere messaggi nella chat.
    private JButton toggleButton; // Pulsante per avviare/fermare il server.
    private boolean isRunning; // Flag per tenere traccia dello stato del server (in esecuzione o meno).
    private ServerSocket serverSocket; // Socket del server per ascoltare le connessioni in entrata.
    private CopyOnWriteArrayList<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>();

    public ServerGUI() {
        super("ServerGUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        textArea = new JTextArea();
        textArea.setEditable(false);
        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(chatArea));
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);

         // Pannello inferiore per il campo di input e il pulsante
         JPanel bottomPanel = new JPanel(new BorderLayout());
         chatInput = new JTextField();
         chatInput.addActionListener(e -> sendMessageToAllClients(chatInput.getText()));
         bottomPanel.add(chatInput, BorderLayout.CENTER);
 
         toggleButton = new JButton("Avvia Server");
         toggleButton.addActionListener(e -> toggleServer());
         bottomPanel.add(toggleButton, BorderLayout.EAST);
 
         add(bottomPanel, BorderLayout.SOUTH);
 
         setVisible(true);
    }

    private void toggleServer() {
        if (!isRunning) {
            startServer(12345);
            toggleButton.setText("Ferma Server");
            isRunning = true;
        } else {
            stopServer();
            toggleButton.setText("Avvia Server");
            isRunning = false;
        }
    }

    private void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            textArea.append("Server connesso sulla porta " + port + "\n");
            new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        Socket socket = serverSocket.accept();
                        textArea.append("Client connesso: " + socket.getInetAddress().getHostAddress() + "\n");
                        ClientHandler handler = new ClientHandler(socket);
                        clientHandlers.add(handler);
                        handler.start();
                    }
                } catch (IOException e) {
                    textArea.append("Server interrotto.\n");
                }
            }).start();
        } catch (IOException e) {
            textArea.append("Non è possibile avviare il server: " + e.getMessage() + "\n");
        }
    }

    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                textArea.append("Server fermato.\n");
            }
        } catch (IOException e) {
            textArea.append("Errore durante la fermata del server: " + e.getMessage() + "\n");
        }
    }

    private void sendMessageToAllClients(String message) {
        for (ClientHandler handler : clientHandlers) {
            handler.sendMessage(message);
        }
        chatArea.append("Server: " + message + "\n");
        chatInput.setText("");
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    chatArea.append(socket.getInetAddress().getHostAddress() + ": " + inputLine + "\n");
                    out.println("Server: " + inputLine); // Optional: Echo the message back to the client
                }
            } catch (IOException e) {
                System.out.println("Client disconnected.");
            } finally {
                try {
                    socket.close();
                    clientHandlers.remove(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}
