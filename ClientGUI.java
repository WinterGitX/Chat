// Importazione dei pacchetti necessari per la creazione dell'interfaccia grafica, la gestione degli eventi,
// la lettura/scrittura di flussi di dati e la gestione delle connessioni di rete.
import javax.swing.*; // Importa le classi per l'interfaccia grafica Swing.
import java.awt.*; // Importa classi per utilizzare componenti dell'interfaccia grafica.
import java.awt.event.ActionEvent; // Importa le classi per la gestione degli eventi su componenti GUI.
import java.io.BufferedReader; // Importa la classe per la lettura bufferizzata di stream di input.
import java.io.IOException; // Importa la classe per la gestione delle eccezioni di I/O.
import java.io.InputStreamReader; // Importa la classe per convertire un byte stream in un character stream.
import java.io.PrintWriter; // Importa la classe per stampare formati di testo.
import java.net.Socket; // Importa la classe per la creazione di socket.

// Definizione della classe ClientGUI, che estende JFrame per creare la finestra dell'applicazione.
public class ClientGUI extends JFrame {
    // Dichiarazione delle componenti dell'interfaccia grafica: aree di testo, campo di input e bottone.
    private JTextArea textArea; // Area per le notifiche e i messaggi del server.
    private JTextArea chatArea; // Area per i messaggi della chat.
    private JTextField chatInput; // Campo di input per scrivere messaggi nella chat.
    private JButton toggleConnectionButton; // Bottone per connettersi o disconnettersi dal server.
    private Socket socket; // Socket per la connessione al server.
    private PrintWriter out; // Flusso di output per inviare messaggi al server.

    // Costruttore della classe ClientGUI.
    public ClientGUI() {
        super("ClientGUI"); // Imposta il titolo della finestra.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Imposta l'operazione di chiusura di default.
        setSize(800, 600); // Imposta le dimensioni della finestra.
        setLocationRelativeTo(null); // Centra la finestra nello schermo.

        // Inizializzazione delle aree di testo e configurazione per renderle non modificabili.
        textArea = new JTextArea();
        textArea.setEditable(false);
        chatArea = new JTextArea();
        chatArea.setEditable(false);

        // Creazione di un JSplitPane per dividere la finestra in due aree di testo separate.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(chatArea));
<<<<<<< HEAD
        splitPane.setDividerLocation(200); // Imposta la posizione del divisore.
=======
        splitPane.setDividerLocation(250); // Imposta la posizione del divisore.
>>>>>>> 6243587b21e200f2b0eece4f59a6d4ba48da4b8b
        add(splitPane, BorderLayout.CENTER); // Aggiunge il JSplitPane al frame.

        // Inizializzazione del campo di input per i messaggi e configurazione dell'azione da eseguire all'invio.
        chatInput = new JTextField();
        chatInput.addActionListener(this::sendChatMessage);

        // Inizializzazione e configurazione del bottone per la connessione.
        toggleConnectionButton = new JButton("Connetti");
        toggleConnectionButton.addActionListener(e -> toggleConnection());

        // Creazione di un pannello inferiore che include il campo di input e il bottone.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(chatInput, BorderLayout.CENTER);
        bottomPanel.add(toggleConnectionButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH); // Aggiunge il pannello inferiore al frame.

        setVisible(true); // Rende visibile la finestra.
    }

    // Metodo per gestire il cambio di stato della connessione.
    private void toggleConnection() {
        if (socket == null || socket.isClosed()) {
            connectToServer("localhost", 12345); // Tenta di connettersi al server se non già connesso.
        } else {
            disconnect(); // Disconnette dal server se già connesso.
        }
    }

    // Metodo per connettersi al server.
    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> { // Utilizza un thread separato per non bloccare l'interfaccia grafica.
            try {
                socket = new Socket(serverAddress, port); // Crea una nuova connessione al server.
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Connesso al Server\n"); // Mostra una notifica di connessione riuscita.
                    toggleConnectionButton.setText("Disconnettiti"); // Cambia il testo del bottone.
                });
                out = new PrintWriter(socket.getOutputStream(), true); // Inizializza il flusso di output.

                // Legge i messaggi in arrivo dal server.
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String fromServer;
                while ((fromServer = in.readLine()) != null) { // Continua a leggere finché ci sono messaggi.
                    String message = fromServer;
                    SwingUtilities.invokeLater(() -> chatArea.append("Server: " + message + "\n")); // Mostra i messaggi nella chatArea.
                }
            } catch (IOException e) {
                // Mostra un messaggio di attesa se la connessione fallisce.
                SwingUtilities.invokeLater(() -> textArea.append("In attesa della connessione con il server...\n"));
            }
        }).start(); // Avvia il thread.
    }

    // Metodo per inviare messaggi al server attraverso il campo di input della chat.
    private void sendChatMessage(ActionEvent event) {
        String message = chatInput.getText(); // Ottiene il testo dal campo di input.
        if (!message.isEmpty() && out != null) {
            out.println(message); // Invia il messaggio al server.
            chatArea.append("Tu: " + message + "\n"); // Mostra il messaggio inviato nella chatArea.
            chatInput.setText(""); // Pulisce il campo di input dopo l'invio.
        }
    }

    // Metodo per disconnettersi dal server.
    private void disconnect() {
        try {
            if (socket != null) {
                socket.close(); // Chiude la connessione.
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Disconnesso dal Server\n"); // Mostra una notifica di disconnessione.
                    toggleConnectionButton.setText("Connettiti"); // Cambia il testo del bottone.
                });
            }
        } catch (IOException e) {
            e.printStackTrace(); // Stampa l'errore se qualcosa va storto durante la disconnessione.
        }
    }

    // Metodo principale per eseguire l'applicazione.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new); // Avvia l'interfaccia grafica del client nell'Event Dispatch Thread.
    }
}
