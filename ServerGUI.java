// Importazione dei pacchetti necessari per la creazione dell'interfaccia grafica, la gestione degli eventi,
// la lettura/scrittura di flussi di dati e la gestione delle connessioni di rete.
import javax.swing.*; // Importa le classi per l'interfaccia grafica Swing.
import java.awt.*; // Importa classi per utilizzare componenti dell'interfaccia grafica.
import java.io.*; // Importa classi per input/output, come la lettura e scrittura di flussi.
import java.net.ServerSocket; // Importa la classe ServerSocket per ascoltare le connessioni in entrata.
import java.net.Socket; // Importa la classe Socket per rappresentare una connessione tra il server e il client.
import java.util.concurrent.CopyOnWriteArrayList; // Importa la classe per una lista thread-safe che crea una copia del contenuto ad ogni modifica.

// Definizione della classe ServerGUI che estende JFrame per creare l'interfaccia grafica del server.
public class ServerGUI extends JFrame {
    private JTextArea textArea; // Area di testo per visualizzare i log di sistema e le informazioni.
    private JTextArea chatArea; // Area di testo per la chat tra client e server.
    private JTextField chatInput; // Campo di testo per inserire messaggi da inviare ai client.
    private JButton toggleButton; // Pulsante per avviare o fermare il server.
    private boolean isRunning; // Variabile di controllo dello stato del server.
    private ServerSocket serverSocket; // Server socket per accettare connessioni.
    private CopyOnWriteArrayList<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>(); // Lista thread-safe di gestori client.

    public ServerGUI() {
        super("ServerGUI"); // Imposta il titolo della finestra del JFrame.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Chiude l'applicazione alla chiusura della finestra.
        setSize(800, 600); // Dimensioni della finestra.
        setLocationRelativeTo(null); // Centra la finestra sullo schermo.

        textArea = new JTextArea(); // Inizializza l'area di testo per i log.
        textArea.setEditable(false); // Impedisce la modifica del testo.
        chatArea = new JTextArea(); // Inizializza l'area di testo per la chat.
        chatArea.setEditable(false); // Impedisce la modifica del testo.

        // Crea un pannello diviso per separare i log dalla chat.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(chatArea));
        splitPane.setDividerLocation(200); // Imposta la posizione del divisore.
        add(splitPane, BorderLayout.CENTER); // Aggiunge il pannello diviso al frame.

        // Crea un pannello per gli elementi di input e controllo.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendMessageToAllClients(chatInput.getText())); // Aggiunge un listener per inviare messaggi.
        bottomPanel.add(chatInput, BorderLayout.CENTER);

        toggleButton = new JButton("Avvia Server"); // Pulsante per avviare il server.
        toggleButton.addActionListener(e -> toggleServer()); // Listener per gestire l'attivazione del server.
        bottomPanel.add(toggleButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH); // Aggiunge il pannello inferiore al frame.

        setVisible(true); // Rende visibile la finestra.
    }

    private void toggleServer() {
        if (!isRunning) {
            startServer(12345); // Avvia il server su una porta specifica.
            toggleButton.setText("Ferma Server");
            isRunning = true;
        } else {
            stopServer(); // Ferma il server.
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
        clientHandlers.forEach(handler -> handler.sendMessage(message));
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
