// Importazione dei pacchetti necessari per la creazione dell'interfaccia grafica, la gestione degli eventi,
// la lettura/scrittura di flussi di dati e la gestione delle connessioni di rete.
import javax.swing.*; // Importa le classi per l'interfaccia grafica Swing.
import java.awt.*; // Importa classi per utilizzare componenti dell'interfaccia grafica.
import java.io.*; // Importa classi per input/output, come la lettura e scrittura di flussi.
import java.net.ServerSocket; // Importa la classe ServerSocket per ascoltare le connessioni in entrata.
import java.net.Socket; // Importa la classe Socket per rappresentare una connessione tra il server e il client.
import java.util.concurrent.CopyOnWriteArrayList; // Importa la classe per una lista thread-safe che crea una copia del contenuto ad ogni modifica.

public class ServerGUI extends JFrame { // Crea una classe che estende JFrame per l'interfaccia grafica del server.
    private JTextArea textArea; // Area di testo per visualizzare informazioni di sistema e log.
    private JTextArea chatArea; // Area di testo dedicata alla chat tra client e server.
    private JTextField chatInput; // Campo di input per scrivere messaggi nella chat.
    private JButton toggleButton; // Pulsante per avviare/fermare il server.
    private boolean isRunning; // Flag per tenere traccia dello stato del server (in esecuzione o meno).
    private ServerSocket serverSocket; // Socket del server per ascoltare le connessioni in entrata.
    private CopyOnWriteArrayList<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>(); // Lista di gestori client, thread-safe.

    public ServerGUI() { // Costruttore della classe ServerGUI.
        super("ServerGUI"); // Chiama il costruttore della superclass JFrame con il titolo "ServerGUI".
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Imposta l'operazione di default alla chiusura della finestra (termina l'applicazione).
        setSize(800, 600); // Imposta le dimensioni della finestra.
        setLocationRelativeTo(null); // Posiziona la finestra al centro dello schermo.

        textArea = new JTextArea(); // Inizializza l'area di testo per i log.
        textArea.setEditable(false); // Rende l'area di testo non modificabile.
        chatArea = new JTextArea(); // Inizializza l'area di testo per la chat.
        chatArea.setEditable(false); // Rende l'area di testo non modificabile.

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(chatArea)); // Crea un pannello diviso per contenere i due JTextArea.
        splitPane.setDividerLocation(200); // Imposta la posizione del divisore.
        add(splitPane, BorderLayout.CENTER); // Aggiunge il pannello diviso al frame centrale.

        JPanel bottomPanel = new JPanel(new BorderLayout()); // Crea un pannello per gli elementi inferiori.
        chatInput = new JTextField(); // Crea il campo di testo per l'input della chat.
        chatInput.addActionListener(e -> sendMessageToAllClients(chatInput.getText())); // Aggiunge un listener che invia il messaggio a tutti i client quando si preme Invio.
        bottomPanel.add(chatInput, BorderLayout.CENTER); // Aggiunge il campo di input al pannello inferiore al centro.

        toggleButton = new JButton("Avvia Server"); // Crea il pulsante con il testo "Avvia Server".
        toggleButton.addActionListener(e -> toggleServer()); // Aggiunge un listener che avvia o ferma il server quando il pulsante viene premuto.
        bottomPanel.add(toggleButton, BorderLayout.EAST); // Aggiunge il pulsante al pannello inferiore a est.

        add(bottomPanel, BorderLayout.SOUTH); // Aggiunge il pannello inferiore alla parte sud del frame.

        setVisible(true); // Rende il frame visibile.
    }

    private void toggleServer() { // Metodo per avviare o fermare il server.
        if (!isRunning) { // Controlla se il server non è attualmente in esecuzione.
            startServer(12345); // Avvia il server sulla porta 12345.
            toggleButton.setText("Ferma Server"); // Cambia il testo del pulsante in "Ferma Server".
            isRunning = true; // Imposta il flag di esecuzione a vero.
        } else { // Se il server è già in esecuzione.
            stopServer(); // Ferma il server.
            toggleButton.setText("Avvia Server"); // Cambia il testo del pulsante in "Avvia Server".
            isRunning = false; // Imposta il flag di esecuzione a falso.
        }
    }

    private void startServer(int port) { // Metodo per avviare il server su una porta specifica.
        try {
            serverSocket = new ServerSocket(port); // Crea un nuovo ServerSocket sulla porta specificata.
            textArea.append("Server connesso sulla porta " + port + "\n"); // Logga la connessione del server.
            new Thread(() -> { // Crea un nuovo thread per gestire le connessioni in entrata.
                try {
                    while (!serverSocket.isClosed()) { // Continua a ciclare finché il ServerSocket non è chiuso.
                        Socket socket = serverSocket.accept(); // Accetta una connessione in entrata.
                        textArea.append("Client connesso: " + socket.getInetAddress().getHostAddress() + "\n"); // Logga l'indirizzo del client connesso.
                        ClientHandler handler = new ClientHandler(socket); // Crea un nuovo gestore per il client.
                        clientHandlers.add(handler); // Aggiunge il gestore alla lista di gestori.
                        handler.start(); // Avvia il thread del gestore.
                    }
                } catch (IOException e) {
                    textArea.append("Server interrotto.\n"); // Logga l'interruzione del server.
                }
            }).start(); // Avvia il thread.
        } catch (IOException e) {
            textArea.append("Non è possibile avviare il server: " + e.getMessage() + "\n"); // Logga l'errore se il server non può essere avviato.
        }
    }

    private void stopServer() { // Metodo per fermare il server.
        try {
            if (serverSocket != null && !serverSocket.isClosed()) { // Controlla se il ServerSocket esiste ed è aperto.
                serverSocket.close(); // Chiude il ServerSocket.
                textArea.append("Server fermato.\n"); // Logga la fermata del server.
            }
        } catch (IOException e) {
            textArea.append("Errore durante la fermata del server: " + e.getMessage() + "\n"); // Logga gli errori durante la fermata del server.
        }
    }

    private void sendMessageToAllClients(String message) { // Metodo per inviare un messaggio a tutti i client connessi.
        for (ClientHandler handler : clientHandlers) { // Itera su tutti i gestori client.
            handler.sendMessage(message); // Invia il messaggio tramite il gestore.
        }
        chatArea.append("Server: " + message + "\n"); // Aggiunge il messaggio inviato nell'area di chat del server.
        chatInput.setText(""); // Pulisce il campo di input della chat.
    }

    private class ClientHandler extends Thread { // Classe interna per gestire ogni connessione client.
        private Socket socket; // Socket per la connessione con il client.
        private PrintWriter out; // Flusso per inviare dati al client.

        public ClientHandler(Socket socket) { // Costruttore del gestore client.
            this.socket = socket; // Imposta il socket ricevuto come socket di connessione.
            try {
                out = new PrintWriter(socket.getOutputStream(), true); // Inizializza il flusso di uscita con il flusso di output del socket.
            } catch (IOException e) {
                e.printStackTrace(); // Stampa lo stack trace se c'è un errore durante l'inizializzazione del flusso.
            }
        }

        public void sendMessage(String message) { // Metodo per inviare un messaggio al client.
            out.println(message); // Scrive il messaggio nel flusso di uscita.
        }

        public void run() { // Metodo eseguito quando il thread viene avviato.
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) { // Crea un BufferedReader per leggere i dati in entrata dal socket.
                String inputLine;
                while ((inputLine = in.readLine()) != null) { // Continua a leggere finché ci sono dati.
                    chatArea.append(socket.getInetAddress().getHostAddress() + ": " + inputLine + "\n"); // Aggiunge i messaggi ricevuti nell'area di chat.
                    out.println("Server: " + inputLine); // Optional: Echo the message back to the client.
                }
            } catch (IOException e) {
                System.out.println("Client disconnected."); // Stampa a console che il client è disconnesso.
            } finally {
                try {
                    socket.close(); // Chiude il socket.
                    clientHandlers.remove(this); // Rimuove il gestore dalla lista dei gestori.
                } catch (IOException e) {
                    e.printStackTrace(); // Stampa lo stack trace se c'è un errore durante la chiusura del socket.
                }
            }
        }
    }

    public static void main(String[] args) { // Metodo principale per eseguire il server.
        SwingUtilities.invokeLater(ServerGUI::new); // Invoca il costruttore di ServerGUI nel thread di dispatching degli eventi di Swing.
    }
}
