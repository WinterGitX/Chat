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

// Classe principale del client GUI che estende JFrame per fornire una interfaccia grafica.
public class ClientGUI extends JFrame {
    private JTextArea textArea; // Area di testo per mostrare eventi di sistema o messaggi del server.
    private JTextArea chatArea; // Area di testo per la conversazione dell'utente.
    private JTextField chatInput; // Campo di testo per inserire messaggi da inviare.
    private JButton toggleConnectionButton; // Bottone per connettersi/disconnettersi dal server.
    private Socket socket; // Socket per la connessione al server.
    private PrintWriter out; // Strumento per inviare messaggi al server.
    private BufferedReader in;

    // Costruttore della classe ClientGUI.
    public ClientGUI() {
        super("Client Chat"); // Imposta il titolo del JFrame.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Operazione di default alla chiusura della finestra.
        setSize(800, 600); // Dimensioni della finestra.
        setLocationRelativeTo(null); // Centra la finestra sullo schermo.

        // Inizializzazione delle aree di testo.
        textArea = new JTextArea();
        textArea.setEditable(false); // Impedisce la modifica dell'area di testo.
        chatArea = new JTextArea();
        chatArea.setEditable(false); // Impedisce la modifica dell'area di chat.

        // Setup dello split pane per dividere le due aree di testo.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(chatArea));
        splitPane.setDividerLocation(250); // Posizione del divisore tra le due aree.
        add(splitPane, BorderLayout.CENTER); // Aggiunge il pannello diviso al frame.

        // Campo di input con ascoltatore per inviare messaggi quando si preme Enter.
        chatInput = new JTextField();
        chatInput.addActionListener(this::sendChatMessage); // Metodo che gestisce l'invio dei messaggi.

        // Bottone per connessione/disconnessione.
        toggleConnectionButton = new JButton("Connetti");
        toggleConnectionButton.addActionListener(e -> toggleConnection()); // Metodo che gestisce la connessione.

        // Pannello inferiore che contiene il campo di input e il bottone.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(chatInput, BorderLayout.CENTER);
        bottomPanel.add(toggleConnectionButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH); // Aggiunge il pannello al frame.

        setVisible(true); // Rende visibile il frame.
    }

    // Metodo per alternare la connessione e disconnessione.
    private void toggleConnection() {
        if (socket == null || socket.isClosed()) {
            connectToServer("localhost", 12345); // Tenta di connettersi se non connesso o se il socket è chiuso.
        } else {
            disconnect(); // Disconnette se il socket è già connesso.
        }
    }

    // Metodo per connettersi al server.
    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> { // Utilizza un thread separato per la connessione per non bloccare l'UI.
            try {
                socket = new Socket(serverAddress, port); // Crea un nuovo socket.
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Connesso al Server\n"); // Aggiorna l'area di testo sulla UI.
                    toggleConnectionButton.setText("Disconnettiti"); // Cambia il testo del bottone.
                });
                out = new PrintWriter(socket.getOutputStream(), true); // Inizializza il PrintWriter.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Inizializza il BufferedReader.

                // Ascoltatore per i messaggi in arrivo dal server.
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    final String finalFromServer = fromServer; // Crea una variabile finale per uso nella lambda
                    SwingUtilities.invokeLater(() -> chatArea.append("Server: " + finalFromServer + "\n")); // Aggiorna la chat area nel thread dell'UI.
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> textArea.append("In attesa della connessione con il server...\n"));
            }
        }).start(); // Avvia il thread.
    }
    
    // Metodo per disconettersi al server.
    private void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                // Close the output stream
                if (out != null) {
                    out.close();
                }
                // Close the socket
                socket.close();
                // Update UI components or state as necessary
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Disconnected from the server.\n"); // Log to UI
                    toggleConnectionButton.setText("Connect"); // Change button text to "Connect"
                });
            } catch (IOException e) {
                // Log to UI or console in case of an error during disconnect
                SwingUtilities.invokeLater(() -> textArea.append("Error disconnecting: " + e.getMessage() + "\n"));
            } finally {
                // Ensure resources are nullified or reset state as necessary
                out = null;
                socket = null;
            }
        }
    }

    // Metodo per inviare messaggi al server.
    private void sendChatMessage(ActionEvent event) {
        String input = chatInput.getText().trim(); // Ottiene il messaggio dal campo di input.
        if (!input.isEmpty() && out != null) {
            if (input.startsWith("/")) { // Controlla se il messaggio è un comando.
                processCommand(input); // Elabora il comando.
            } else {
                out.println(input); // Invia il messaggio normale.
                chatArea.append("Tu: " + input + "\n"); // Mostra il messaggio nella chat area.
            }
            chatInput.setText(""); // Pulisce il campo di input.
        }
    }

    // Metodo per elaborare i comandi di cifratura e decifratura.
    private void processCommand(String input) {
        try {
            String[] parts = input.split(" ", 3); // Divide il comando dalle sue parti.
            String command = parts[0]; // Il comando stesso.
            String key = parts[1]; // La chiave di cifratura.
            String message = parts[2]; // Il messaggio da cifrare o decifrare.

            switch (command.toLowerCase()) {
                case "/ce":
                    message = encryptCaesar(message, Integer.parseInt(key));
                    break;
                case "/vi":
                    message = encryptVigenere(message, key);
                    break;
                case "/dece":
                    message = decryptCaesar(message, Integer.parseInt(key));
                    break;
                case "/devi":
                    message = decryptVigenere(message, key);
                    break;
                default:
                    chatArea.append("Comando non riconosciuto.\n");
                    return;
            }
            out.println(message); // Invia il messaggio cifrato/decifrato.
            chatArea.append("Tu (" + command.substring(1) + "): " + message + "\n"); // Mostra il messaggio nella chat area.
        } catch (Exception e) {
            chatArea.append("Errore nel processare il comando. Assicurati di usare il formato corretto.\n");
        }
    }

    // Metodo per cifrare un testo con la cifratura di Cesare.
    private String encryptCaesar(String text, int shift) {
        shift = shift % 26 + 26; // Normalizza il valore di shift.
        StringBuilder encrypted = new StringBuilder();
        for (char i : text.toCharArray()) {
            if (Character.isLetter(i)) {
                if (Character.isUpperCase(i)) {
                    encrypted.append((char) ('A' + (i - 'A' + shift) % 26));
                } else {
                    encrypted.append((char) ('a' + (i - 'a' + shift) % 26));
                }
            } else {
                encrypted.append(i); // Lascia inalterati i caratteri non alfabetici.
            }
        }
        return encrypted.toString(); // Ritorna il testo cifrato.
    }

    // Metodo per decifrare un testo con la cifratura di Cesare.
    private String decryptCaesar(String text, int shift) {
        return encryptCaesar(text, -shift); // Utilizza la cifratura di Cesare con uno shift negativo per decifrare.
    }

    // Metodo per cifrare un testo con la cifratura di Vigenère.
    private String encryptVigenere(String text, String key) {
        StringBuilder result = new StringBuilder();
        key = key.toLowerCase(); // Normalizza la chiave a minuscolo.
        int j = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                int shift = key.charAt(j) - 'a'; // Calcola il valore di shift basato sulla chiave.
                if (Character.isUpperCase(c)) {
                    result.append((char) ('A' + (c - 'A' + shift) % 26));
                } else {
                    result.append((char) ('a' + (c - 'a' + shift) % 26));
                }
                j = ++j % key.length(); // Incrementa j e lo resetta se necessario.
            } else {
                result.append(c); // Lascia inalterati i caratteri non alfabetici.
            }
        }
        return result.toString(); // Ritorna il testo cifrato.
    }

    // Metodo per decifrare un testo con la cifratura di Vigenère.
    private String decryptVigenere(String text, String key) {
        StringBuilder result = new StringBuilder();
        key = key.toLowerCase(); // Normalizza la chiave a minuscolo.
        int j = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                int shift = -(key.charAt(j) - 'a'); // Calcola il valore negativo di shift basato sulla chiave.
                if (Character.isUpperCase(c)) {
                    result.append((char) ('A' + (c - 'A' + shift + 26) % 26));
                } else {
                    result.append((char) ('a' + (c - 'a' + shift + 26) % 26));
                }
                j = ++j % key.length(); // Incrementa j e lo resetta se necessario.
            } else {
                result.append(c); // Lascia inalterati i caratteri non alfabetici.
            }
        }
        return result.toString(); // Ritorna il testo decifrato.
    }
    
    // Metodo principale per eseguire l'applicazione.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new); // Avvia l'interfaccia grafica del client nell'Event Dispatch Thread.
    }
}
