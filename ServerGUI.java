import javax.swing.*;  // Importa le classi per l'interfaccia grafica Swing.
import java.awt.*;  // Importa le classi per la grafica e i layout di AWT.
import java.awt.event.ActionEvent;  // Importa le classi per la gestione degli eventi di azione.
import java.io.*;  // Importa le classi per input/output di file e flussi di dati.
import java.net.ServerSocket;  // Importa la classe ServerSocket per creare socket di server.
import java.net.Socket;  // Importa la classe Socket per le connessioni di rete.
import java.util.concurrent.CopyOnWriteArrayList;  // Importa la classe per una lista thread-safe.

public class ServerGUI extends JFrame {  // Definisce la classe ServerGUI che estende JFrame.
    private JTextArea textArea; // Area di testo per i log di sistema e le informazioni.
    private JTextArea chatArea; // Area di testo per la chat tra client e server.
    private JTextField chatInput; // Campo di testo per inserire messaggi da inviare ai client.
    private JTextField keyField; // Campo per la chiave di cifratura/decifratura.
    private JButton toggleButton; // Pulsante per avviare o fermare il server.
    private JComboBox<String> cryptoOptions; // Menu a tendina per selezionare il tipo di cifratura/decifratura.
    private boolean isRunning; // Stato del server, true se in esecuzione.
    private ServerSocket serverSocket; // Socket del server per accettare connessioni.
    private CopyOnWriteArrayList<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>(); // Lista di gestori client per la gestione concorrente.

    public ServerGUI() {  // Costruttore della classe ServerGUI.
        super("Server GUI");  // Imposta il titolo della finestra.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Imposta l'azione di chiusura della finestra.
        setSize(800, 600);  // Imposta le dimensioni della finestra.
        setLocationRelativeTo(null);  // Centra la finestra sullo schermo.
        setupComponents();  // Chiama il metodo per configurare i componenti dell'interfaccia.
        setVisible(true);  // Rende visibile la finestra.
    }

    private void setupComponents() {  // Metodo per configurare i componenti dell'interfaccia.
        textArea = new JTextArea();  // Crea l'area di testo per i log.
        textArea.setEditable(false);  // Impedisce la modifica dell'area di testo.
        chatArea = new JTextArea();  // Crea l'area di testo per la chat.
        chatArea.setEditable(false);  // Impedisce la modifica dell'area di chat.

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              new JScrollPane(textArea),
                                              new JScrollPane(chatArea));  // Divide la finestra in due pannelli scrollabili.
        splitPane.setDividerLocation(250);  // Imposta la posizione del divisore.
        add(splitPane, BorderLayout.CENTER);  // Aggiunge il pannello diviso al centro della finestra.

        JPanel bottomPanel = new JPanel(new BorderLayout());  // Crea un pannello per gli elementi inferiori.
        chatInput = new JTextField();  // Crea il campo di testo per l'input della chat.
        chatInput.addActionListener(this::sendMessageToAllClients);  // Associa l'invio di messaggi all'azione di invio.
        bottomPanel.add(chatInput, BorderLayout.CENTER);  // Aggiunge il campo di testo al centro del pannello inferiore.

        cryptoOptions = new JComboBox<>(new String[]{"Plain Text", "Caesar Encrypt", "Caesar Decrypt", "Vigenère Encrypt", "Vigenère Decrypt"});  // Crea il menu a tendina per le opzioni di cifratura.
        bottomPanel.add(cryptoOptions, BorderLayout.WEST);  // Aggiunge il menu a tendina al lato ovest del pannello.

        keyField = new JTextField();  // Crea il campo di testo per la chiave di cifratura.
        keyField.setVisible(true);  // Rende visibile il campo chiave.
        bottomPanel.add(keyField, BorderLayout.NORTH);  // Aggiunge il campo chiave nella parte superiore del pannello.

        toggleButton = new JButton("Start Server");  // Crea il pulsante per avviare o fermare il server.
        toggleButton.addActionListener(this::toggleServer);  // Associa l'azione di toggle del server al pulsante.
        bottomPanel.add(toggleButton, BorderLayout.EAST);  // Aggiunge il pulsante al lato est del pannello.

        add(bottomPanel, BorderLayout.SOUTH);  // Aggiunge il pannello inferiore alla parte inferiore della finestra.
    }

    private void toggleServer(ActionEvent event) {  // Metodo per avviare o fermare il server.
        if (!isRunning) {  // Se il server non è in esecuzione, avvialo.
            startServer(12345);  // Chiama il metodo per avviare il server.
            toggleButton.setText("Stop Server");  // Cambia il testo del pulsante in "Stop Server".
            isRunning = true;  // Imposta lo stato del server su in esecuzione.
        } else {
            stopServer();  // Chiama il metodo per fermare il server.
            toggleButton.setText("Start Server");  // Cambia il testo del pulsante in "Start Server".
            isRunning = false;  // Imposta lo stato del server su non in esecuzione.
        }
    }

    private void startServer(int port) {  // Metodo per avviare il server.
        try {
            serverSocket = new ServerSocket(port);  // Crea un nuovo socket di server.
            textArea.append("Server connected on port " + port + "\n");  // Visualizza la connessione nel log.
            new Thread(() -> {  // Crea un nuovo thread per gestire le connessioni in arrivo.
                try {
                    while (!serverSocket.isClosed()) {  // Continua a eseguire finché il socket non viene chiuso.
                        Socket socket = serverSocket.accept();  // Accetta una connessione in entrata.
                        String currentKey = keyField.getText();  // Prende la chiave attuale dal campo di testo.
                        ClientHandler handler = new ClientHandler(socket, currentKey);  // Crea un nuovo gestore per il client connesso.
                        clientHandlers.add(handler);  // Aggiunge il gestore alla lista di gestori.
                        handler.start();  // Avvia il thread del gestore.
                    }
                } catch (IOException e) {  // Gestisce le eccezioni di input/output.
                    textArea.append("Server interrupted.\n");  // Visualizza l'interruzione nel log.
                }
            }).start();  // Avvia il thread.
        } catch (IOException e) {  // Gestisce le eccezioni di input/output.
            textArea.append("Unable to start server: " + e.getMessage() + "\n");  // Visualizza l'errore di avvio nel log.
        }
    }

    private void stopServer() {  // Metodo per fermare il server.
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {  // Se il socket di server esiste e non è chiuso, chiudilo.
                serverSocket.close();  // Chiude il socket di server.
                textArea.append("Server stopped.\n");  // Visualizza la fermata nel log.
            }
        } catch (IOException e) {  // Gestisce le eccezioni di input/output.
            textArea.append("Error stopping server: " + e.getMessage() + "\n");  // Visualizza l'errore di fermata nel log.
        }
    }

    private void sendMessageToAllClients(ActionEvent event) {  // Metodo per inviare messaggi a tutti i client connessi.
        String message = chatInput.getText();  // Prende il messaggio dal campo di input.
        if (!message.isEmpty()) {  // Se il messaggio non è vuoto, procedi.
            String option = (String) cryptoOptions.getSelectedItem();  // Prende l'opzione di cifratura selezionata.
            String key = keyField.getText();  // Prende la chiave dal campo di testo.
    
            if (keyRequired(option) && key.isEmpty()) {  // Se è necessaria una chiave e non è stata fornita, mostra un errore.
                JOptionPane.showMessageDialog(this, "A key is required for " + option, "Key Error", JOptionPane.ERROR_MESSAGE);
            } else {
                String finalMessage = applyCrypto(message, option, key);  // Applica la cifratura al messaggio.
                clientHandlers.forEach(handler -> handler.sendMessage(finalMessage));  // Invia il messaggio cifrato a tutti i client.
                chatArea.append("You: " + finalMessage + "\n");  // Aggiunge il messaggio cifrato all'area di chat.
                chatInput.setText("");  // Pulisce il campo di input dopo l'invio.
            }
        }
    }

    private boolean keyRequired(String option) {  // Metodo per determinare se una chiave è necessaria per la cifratura selezionata.
        return option.contains("Caesar") || option.contains("Vigenère");  // Restituisce vero se l'opzione contiene "Caesar" o "Vigenère".
    }
    
    private String applyCrypto(String message, String option, String key) {  // Metodo per applicare la cifratura o decifratura al messaggio.
        try {
            switch (option) {  // Seleziona l'opzione di cifratura.
                case "Caesar Encrypt":
                case "Caesar Decrypt":
                    int shift = Integer.parseInt(key);  // Converti la chiave in un intero per la cifratura di Cesare.
                    return option.contains("Encrypt") ? encryptCaesar(message, shift) : decryptCaesar(message, shift);  // Applica la cifratura o decifratura di Cesare.
                case "Vigenère Encrypt":
                case "Vigenère Decrypt":
                    return option.contains("Encrypt") ? encryptVigenere(message, key) : decryptVigenere(message, key);  // Applica la cifratura o decifratura di Vigenère.
                default:
                    return message;  // Restituisce il messaggio originale se non è applicata alcuna cifratura/decifratura.
            }
        } catch (NumberFormatException e) {  // Gestisce le eccezioni per formati di chiave non validi.
            JOptionPane.showMessageDialog(this, "Invalid key format for Caesar cipher. Please enter a valid number.", "Key Error", JOptionPane.ERROR_MESSAGE);  // Mostra un messaggio di errore se il formato della chiave non è valido.
            return null;  // Restituisce null in caso di formato chiave non valido.
        }
    }

    private String encryptCaesar(String text, int shift) {  // Metodo per cifrare un testo con la cifratura di Cesare.
        StringBuilder encrypted = new StringBuilder();  // Crea un StringBuilder per costruire il testo cifrato.
        shift = shift % 26;  // Normalizza lo shift a un valore tra 0 e 25.
        for (char c : text.toCharArray()) {  // Itera su ogni carattere del testo.
            if (Character.isLetter(c)) {  // Se il carattere è una lettera, applica la cifratura.
                int base = (Character.isLowerCase(c) ? 'a' : 'A');  // Determina la base ASCII per lettere minuscole o maiuscole.
                encrypted.append((char) ((c - base + shift) % 26 + base));  // Cifra il carattere e aggiungilo al risultato.
            } else {
                encrypted.append(c);  // Se non è una lettera, aggiungilo inalterato al risultato.
            }
        }
        return encrypted.toString();  // Restituisce il testo cifrato.
    }

    private String decryptCaesar(String text, int shift) {  // Metodo per decifrare un testo con la cifratura di Cesare.
        return encryptCaesar(text, 26 - (shift % 26));  // Utilizza il metodo di cifratura con uno shift inverso per decifrare.
    }

    private String encryptVigenere(String text, String key) {  // Metodo per cifrare un testo con la cifratura di Vigenère.
        StringBuilder result = new StringBuilder();  // Crea un StringBuilder per costruire il testo cifrato.
        key = key.toLowerCase();  // Converte la chiave in minuscolo.
        int j = 0;  // Inizializza un indice per la chiave.
        for (char c : text.toCharArray()) {  // Itera su ogni carattere del testo.
            if (Character.isLetter(c)) {  // Se il carattere è una lettera, applica la cifratura.
                int base = (Character.isLowerCase(c) ? 'a' : 'A');  // Determina la base ASCII per lettere minuscole o maiuscole.
                result.append((char) ((c - base + (key.charAt(j) - 'a')) % 26 + base));  // Cifra il carattere e aggiungilo al risultato.
                j = (j + 1) % key.length();  // Aggiorna l'indice della chiave, ripetendo la chiave se necessario.
            } else {
                result.append(c);  // Se non è una lettera, aggiungilo inalterato al risultato.
            }
        }
        return result.toString();  // Restituisce il testo cifrato.
    }

    private String decryptVigenere(String text, String key) {  // Metodo per decifrare un testo con la cifratura di Vigenère.
        StringBuilder result = new StringBuilder();  // Crea un StringBuilder per costruire il testo decifrato.
        key = key.toLowerCase();  // Converte la chiave in minuscolo.
        int j = 0;  // Inizializza un indice per la chiave.
        for (char c : text.toCharArray()) {  // Itera su ogni carattere del testo.
            if (Character.isLetter(c)) {  // Se il carattere è una lettera, applica la decifratura.
                int base = (Character.isLowerCase(c) ? 'a' : 'A');  // Determina la base ASCII per lettere minuscole o maiuscole.
                result.append((char) ((c - base - (key.charAt(j) - 'a') + 26) % 26 + base));  // Decifra il carattere e aggiungilo al risultato.
                j = (j + 1) % key.length();  // Aggiorna l'indice della chiave, ripetendo la chiave se necessario.
            } else {
                result.append(c);  // Se non è una lettera, aggiungilo inalterato al risultato.
            }
        }
        return result.toString();  // Restituisce il testo decifrato.
    }

    public static void main(String[] args) {  // Metodo principale per avviare il server.
        SwingUtilities.invokeLater(ServerGUI::new);  // Crea e mostra l'interfaccia grafica utilizzando il thread di dispatch degli eventi di Swing.
    }

    class ClientHandler extends Thread {  // Classe interna per gestire le connessioni client.
        private Socket socket;  // Socket per la connessione con il client.
        private PrintWriter out;  // Strumento di output per inviare messaggi al client.
        private BufferedReader in;  // Strumento di input per ricevere messaggi dal client.
        private String key;  // Chiave per la cifratura/decifratura dei messaggi.

        ClientHandler(Socket socket, String key) {  // Costruttore del gestore client.
            this.socket = socket;  // Imposta il socket del client.
            this.key = key;  // Imposta la chiave di cifratura/decifratura.
            try {
                out = new PrintWriter(socket.getOutputStream(), true);  // Inizializza lo strumento di output.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // Inizializza lo strumento di input.
            } catch (IOException e) {  // Gestisce le eccezioni di input/output.
                e.printStackTrace();  // Stampa le informazioni di eccezione.
            }
        }

        public void run() {  // Metodo run del thread.
            try {
                String line;
                while ((line = in.readLine()) != null) {  // Legge i messaggi dal client fino alla chiusura della connessione.
                    String processedMessage = applyCrypto(line, (String) cryptoOptions.getSelectedItem(), key);  // Applica la cifratura/decifratura al messaggio ricevuto.
                    for (ClientHandler client : clientHandlers) {  // Inoltra il messaggio a tutti gli altri client.
                        if (client != this) client.out.println(processedMessage);  // Evita di inviare il messaggio al mittente originale.
                    }
                    SwingUtilities.invokeLater(() -> chatArea.append("Client: " + processedMessage + "\n"));  // Aggiunge il messaggio alla chat area.
                }
            } catch (IOException e) {  // Gestisce le eccezioni di input/output.
                e.printStackTrace();  // Stampa le informazioni di eccezione.
            } finally {
                try {
                    in.close();  // Chiude il flusso di input.
                    out.close();  // Chiude il flusso di output.
                    socket.close();  // Chiude il socket.
                } catch (IOException e) {  // Gestisce le eccezioni di input/output durante la chiusura delle risorse.
                    e.printStackTrace();  // Stampa le informazioni di eccezione.
                }
                clientHandlers.remove(this);  // Rimuove il gestore dalla lista di gestori.
            }
        }

        public void sendMessage(String message) {  // Metodo per inviare un messaggio al client.
            out.println(message);  // Invia il messaggio attraverso il flusso di output.
        }
    }
}
