// Importazione dei pacchetti necessari per l'interfaccia grafica (Swing), la gestione di componenti grafici (AWT), 
// la gestione dell'input/output e le connessioni di rete.
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// Definizione della classe ServerGUI che estende JFrame per creare un'interfaccia grafica per il server.
public class ServerGUI extends JFrame {
    // Dichiarazione delle variabili d'istanza per i componenti dell'interfaccia grafica e la gestione del server.
    private JTextArea textArea; // Area di testo per visualizzare informazioni di sistema e log.
    private JTextArea chatArea; // Area di testo dedicata alla chat tra client e server.
    private JButton toggleButton; // Pulsante per avviare/fermare il server.
    private boolean isRunning; // Flag per tenere traccia dello stato del server (in esecuzione o meno).
    private ServerSocket serverSocket; // Socket del server per ascoltare le connessioni in entrata.

    // Costruttore della classe ServerGUI per inizializzare l'interfaccia grafica del server.
    public ServerGUI() {
        super("ServerGUI"); // Titolo della finestra del server.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Operazione di chiusura predefinita.
        setSize(800, 600); // Dimensioni della finestra.
        setLocationRelativeTo(null); // Posizionamento della finestra al centro dello schermo.

        // Inizializzazione e configurazione delle aree di testo.
        textArea = new JTextArea();
        textArea.setEditable(false); // Impedisce la modifica diretta dell'area di testo.
        
        chatArea = new JTextArea();
        chatArea.setEditable(false); // Impedisce la modifica diretta dell'area di chat.

        // Creazione e configurazione di un JSplitPane per dividere la finestra in due aree di testo.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(chatArea));
        splitPane.setDividerLocation(400); // Posizione del divisore tra le due aree.

        add(splitPane, BorderLayout.CENTER); // Aggiunta del JSplitPane al frame.

        // Inizializzazione e configurazione del pulsante per avviare/fermare il server.
        toggleButton = new JButton("Avvia Server");
        toggleButton.addActionListener(e -> toggleServer()); // Azione associata al pulsante.
        add(toggleButton, BorderLayout.SOUTH); // Posizionamento del pulsante nella parte inferiore.

        setVisible(true); // Rende visibile la finestra del server.
    }

    // Metodo per gestire lo stato del server (avvio/fermata) al clic del pulsante.
    private void toggleServer() {
        if (!isRunning) {
            startServer(12345); // Avvia il server sulla porta specificata.
            toggleButton.setText("Ferma Server"); // Aggiorna il testo del pulsante.
        } else {
            stopServer(); // Ferma il server.
            toggleButton.setText("Avvia Server"); // Aggiorna il testo del pulsante.
        }
        isRunning = !isRunning; // Aggiorna il flag dello stato del server.
    }

    // Metodo per avviare il server.
    private void startServer(int port) {
        new Thread(() -> { // Utilizza un thread per non bloccare l'interfaccia grafica.
            try {
                serverSocket = new ServerSocket(port); // Crea il ServerSocket sulla porta specificata.
                textArea.append("Server connesso sulla porta " + port + "\n"); // Log di avvio.
                while (!serverSocket.isClosed()) { // Ciclo per accettare connessioni multiple finché il server è attivo.
                    Socket socket = serverSocket.accept(); // Accetta una connessione in entrata.
                    textArea.append("Client connesso: " + socket.getInetAddress().getHostAddress() + "\n"); // Log di connessione client.
                    new ClientHandler(socket).start(); // Crea e avvia un thread per gestire la connessione client.
                }
            } catch (IOException e) {
                e.printStackTrace(); // Gestione delle eccezioni.
                textArea.append("Server info: " + e.getMessage() + "\n"); // Log dell'errore.
            }
        }).start(); // Avvia il thread.
    }

    // Metodo per fermare il server.
    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Chiude il ServerSocket, terminando il ciclo di accettazione delle connessioni.
                textArea.append("Chiusura Server.\n"); // Log di chiusura server.
            }
        } catch (IOException e) {
            e.printStackTrace(); // Gestione delle eccezioni in caso di errore durante la chiusura.
        }
    }

    // Classe interna per gestire ogni connessione client in un thread separato.
    private class ClientHandler extends Thread {
        private Socket socket; // Socket per la comunicazione con il client.

        public ClientHandler(Socket socket) {
            this.socket = socket; // Assegna il socket della connessione client.
        }

        public void run() {
            String clientAddress = socket.getInetAddress().getHostAddress(); // Ottiene l'indirizzo IP del client.
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Stream di input per ricevere messaggi.
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) { // Stream di output per inviare messaggi.
                String inputLine;
                while ((inputLine = in.readLine()) != null) { // Legge i messaggi dal client finché la connessione è attiva.
                    chatArea.append(clientAddress + ": " + inputLine + "\n"); // Visualizza il messaggio nella chat.
                    out.println("Server: " + inputLine); // Invia una risposta (eco) al client.
                }
            } catch (IOException e) {
                e.printStackTrace(); // Gestione delle eccezioni.
            } finally {
                try {
                    socket.close(); // Chiude la connessione con il client.
                } catch (IOException e) {
                    e.printStackTrace(); // Gestione delle eccezioni in caso di errore durante la chiusura.
                }
            }
        }
    }

    // Metodo principale per eseguire l'applicazione.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new); // Avvia l'interfaccia grafica del server nell'Event Dispatch Thread.
    }
}
