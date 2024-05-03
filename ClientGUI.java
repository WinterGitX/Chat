import javax.swing.*;  // Importa le classi Swing per la creazione dell'interfaccia grafica.
import java.awt.*;  // Importa le classi AWT per il layout e la grafica.
import java.awt.event.ActionEvent;  // Importa le classi per la gestione degli eventi di azione.
import java.io.*;  // Importa le classi per l'input/output di file e flussi di dati.
import java.net.Socket;  // Importa la classe Socket per la comunicazione di rete.

public class ClientGUI extends JFrame {  // Definisce la classe ClientGUI che estende JFrame.
    private JTextArea textArea; // Area di testo per visualizzare eventi di sistema o messaggi dal server.
    private JTextArea chatArea; // Area di testo per la chat dell'utente.
    private JTextField chatInput; // Campo di testo per inserire i messaggi da inviare.
    private JTextField keyField; // Campo di testo per inserire la chiave di cifratura/decifratura.
    private JButton toggleConnectionButton; // Pulsante per connettersi/disconnettersi dal server.
    private JComboBox<String> cryptoOptions; // Menu a tendina per selezionare l'opzione di cifratura/decifratura.
    private Socket socket; // Socket per la connessione al server.
    private PrintWriter out; // Strumento per inviare messaggi al server.
    private BufferedReader in; // Lettore per ricevere messaggi dal server.

    public ClientGUI() {  // Costruttore della classe.
        super("Client Chat");  // Titolo della finestra del client.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Imposta l'operazione di chiusura della finestra.
        setSize(800, 600);  // Imposta le dimensioni della finestra.
        setLocationRelativeTo(null);  // Centra la finestra sullo schermo.
        initializeComponents();  // Chiama il metodo per inizializzare i componenti dell'interfaccia.
        setVisible(true);  // Rende la finestra visibile.
    }

    private void initializeComponents() {  // Metodo per inizializzare i componenti dell'interfaccia.
        textArea = new JTextArea();  // Crea un'area di testo per il textArea.
        textArea.setEditable(false);  // Impedisce la modifica dell'area di testo.
        JScrollPane scrollPane1 = new JScrollPane(textArea);  // Aggiunge lo scorrimento all'area di testo.

        chatArea = new JTextArea();  // Crea un'area di testo per la chatArea.
        chatArea.setEditable(false);  // Impedisce la modifica dell'area di testo.
        JScrollPane scrollPane2 = new JScrollPane(chatArea);  // Aggiunge lo scorrimento all'area di testo.

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane1, scrollPane2);  // Divide la finestra in due aree scorrevoli.
        splitPane.setDividerLocation(250); // Imposta la posizione del divisore tra le due aree.
        add(splitPane, BorderLayout.CENTER);  // Aggiunge il pannello diviso al centro della finestra.

        chatInput = new JTextField();  // Crea un campo di testo per l'input della chat.
        chatInput.addActionListener(this::sendChatMessage); // Associa l'azione di invio del messaggio al premere Enter.

        toggleConnectionButton = new JButton("Connect");  // Crea un pulsante per connettersi.
        toggleConnectionButton.addActionListener(this::toggleConnection);  // Associa l'azione di connessione/disconnessione al pulsante.

        cryptoOptions = new JComboBox<>(new String[]{  // Crea un menu a tendina con le opzioni di cifratura.
            "Plain Text", "Caesar Encrypt", "Caesar Decrypt", "Vigenère Encrypt", "Vigenère Decrypt"
        });
        cryptoOptions.addActionListener(this::updateCryptoOptions);  // Associa l'azione di aggiornamento delle opzioni al menu a tendina.

        keyField = new JTextField(); // Crea un campo di testo per la chiave.
        keyField.setVisible(true); // Rende il campo chiave sempre visibile.

        JPanel bottomPanel = new JPanel(new BorderLayout());  // Crea un pannello in basso per gli elementi di controllo.
        bottomPanel.add(cryptoOptions, BorderLayout.WEST);  // Aggiunge il menu a tendina a ovest.
        bottomPanel.add(chatInput, BorderLayout.CENTER);  // Aggiunge il campo di input della chat al centro.
        bottomPanel.add(toggleConnectionButton, BorderLayout.EAST);  // Aggiunge il pulsante di connessione a est.
        bottomPanel.add(keyField, BorderLayout.NORTH);  // Aggiunge il campo chiave a nord.

        add(bottomPanel, BorderLayout.SOUTH);  // Aggiunge il pannello inferiore al sud della finestra.
    }

    private void updateCryptoOptions(ActionEvent event) {  // Metodo per aggiornare le opzioni di cifratura.
        String selected = (String) cryptoOptions.getSelectedItem();  // Ottiene l'opzione selezionata dal menu a tendina.
        boolean isKeyNeeded = selected.endsWith("Encrypt") || selected.endsWith("Decrypt");  // Determina se è necessaria una chiave.
        keyField.setVisible(isKeyNeeded); // Mostra il campo chiave solo se necessario.
        revalidate(); // Aggiorna il layout per mostrare/nascondere il campo chiave.
    }

    private void toggleConnection(ActionEvent event) {  // Metodo per attivare/disattivare la connessione.
        if (socket == null || socket.isClosed()) {  // Se il socket è chiuso o non esiste, tenta di connettere.
            connectToServer("localhost", 12345);  // Chiama il metodo per connettersi al server.
            toggleConnectionButton.setText("Disconnect");  // Cambia il testo del pulsante in "Disconnetti".
        } else {
            disconnect();  // Chiama il metodo per disconnettersi.
            toggleConnectionButton.setText("Connect");  // Cambia il testo del pulsante in "Connetti".
        }
    }

    private void connectToServer(String serverAddress, int port) {  // Metodo per connettersi al server.
        new Thread(() -> {  // Crea un nuovo thread per la connessione.
            try {
                socket = new Socket(serverAddress, port);  // Crea un socket per connettersi al server.
                out = new PrintWriter(socket.getOutputStream(), true);  // Crea uno strumento di output per inviare messaggi.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  // Crea uno strumento di input per ricevere messaggi.
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Connesso al server: " + serverAddress + ":" + port + "\n"); // Mostra la connessione nel text area.
                    toggleConnectionButton.setText("Disconnettiti");  // Cambia il testo del pulsante in "Disconnetti".
                });
                receiveMessages(); // Chiama il metodo per ricevere i messaggi dal server.
            } catch (IOException e) {  // Gestisce le eccezioni di input/output.
                SwingUtilities.invokeLater(() -> textArea.append("Impossibile connettersi al server: " + e.getMessage() + "\n"));  // Mostra l'errore di connessione.
                cleanupResources(); // Pulisce le risorse in caso di errore.
            }
        }).start();  // Avvia il thread.
    }

    private void disconnect() {  // Metodo per disconnettersi dal server.
        if (socket != null && !socket.isClosed()) {  // Se il socket è attivo, tenta di disconnettersi.
            try {
                if (out != null) {
                    out.close();  // Chiude il flusso di output.
                }
                socket.close();  // Chiude il socket.
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Disconnesso.\n");  // Aggiorna l'area di testo per mostrare lo stato di disconnessione.
                    toggleConnectionButton.setText("Connetti");  // Cambia il testo del pulsante in "Connetti".
                });
            } catch (IOException e) {  // Gestisce le eccezioni di input/output.
                SwingUtilities.invokeLater(() -> textArea.append("Errore di disconnessione: " + e.getMessage() + "\n"));  // Mostra l'errore di disconnessione.
            } finally {
                cleanupResources();  // Chiama il metodo per pulire le risorse.
            }
        }
    }

    private void sendChatMessage(ActionEvent event) {  // Metodo per inviare messaggi di chat.
        String message = chatInput.getText();  // Prende il testo dal campo di input della chat.
        if (!message.isEmpty()) {  // Controlla se il messaggio non è vuoto.
            String selectedCrypto = (String) cryptoOptions.getSelectedItem();  // Prende l'opzione di cifratura selezionata.
            String key = keyField.getText();  // Prende la chiave dal campo chiave.
            if (keyRequired(selectedCrypto) && key.isEmpty()) {  // Controlla se la chiave è necessaria e se è vuota.
                JOptionPane.showMessageDialog(this, "Key is required for " + selectedCrypto, "Key Error", JOptionPane.ERROR_MESSAGE);  // Mostra un messaggio di errore se la chiave è necessaria ma non è stata inserita.
                return;  // Termina il metodo se non c'è la chiave necessaria.
            }
            String encryptedMessage = applyCrypto(message, selectedCrypto, key);  // Applica la cifratura al messaggio.
            out.println(encryptedMessage);  // Invia il messaggio cifrato al server.
            chatArea.append("Tu: " + encryptedMessage + "\n");  // Aggiunge il messaggio cifrato alla chat area.
            chatInput.setText("");  // Pulisce il campo di input della chat dopo l'invio.
        }
    }

    private void receiveMessages() {  // Metodo per ricevere messaggi dal server.
        try {
            String line;
            while ((line = in.readLine()) != null) {  // Legge i messaggi in arrivo finché la connessione è attiva.
                String finalMessage = applyDecryptionIfNeeded(line);  // Decifra il messaggio se necessario.
                SwingUtilities.invokeLater(() -> chatArea.append("Server: " + finalMessage + "\n"));  // Mostra il messaggio nella chat area.
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                textArea.append("Disconnesso dal server: " + e.getMessage() + "\n");
                toggleConnectionButton.setText("Connetti");  // Cambia il testo del bottone in "Connect" dopo la disconnessione.
            });
        } finally {
            disconnect();  // Pulisce le risorse quando il ciclo while termina.
        }
    }
    
    private String applyDecryptionIfNeeded(String message) {  // Metodo per applicare la decifratura se necessario.
        if (shouldDecrypt()) {  // Controlla se è necessario decifrare.
            return applyCrypto(message, (String) cryptoOptions.getSelectedItem(), keyField.getText());  // Applica la decifratura.
        }
        return message;  // Restituisce il messaggio se non è necessaria la decifratura.
    }
    
    private boolean shouldDecrypt() {  // Metodo per controllare se è necessario decifrare.
        String option = (String) cryptoOptions.getSelectedItem();  // Prende l'opzione di cifratura selezionata.
        return option != null && option.endsWith("Decrypt");  // Restituisce vero se l'opzione termina con "Decrypt".
    }
    
    private void cleanupResources() {  // Metodo per pulire le risorse.
        try {
            if (in != null) {
                in.close();  // Chiude il flusso di input.
                in = null;  // Imposta il flusso di input a null.
            }
            if (out != null) {
                out.close();  // Chiude il flusso di output.
                out = null;  // Imposta il flusso di output a null.
            }
            if (socket != null) {
                socket.close();  // Chiude il socket.
                socket = null;  // Imposta il socket a null.
            }
        } catch (IOException e) {  // Gestisce le eccezioni di input/output.
            SwingUtilities.invokeLater(() -> textArea.append("Error cleaning up resources: " + e.getMessage() + "\n"));  // Mostra l'errore di pulizia delle risorse.
        }
    }
    
    private boolean keyRequired(String option) {  // Metodo per determinare se una chiave è necessaria.
        return option.contains("Caesar") || option.contains("Vigenère");  // Restituisce vero se l'opzione contiene "Caesar" o "Vigenère".
    }

    private String applyCrypto(String message, String option, String key) {  // Metodo per applicare la cifratura o la decifratura.
        try {
            switch (option) {  // Seleziona l'opzione di cifratura.
                case "Caesar Encrypt":
                case "Caesar Decrypt":
                    int shift = Integer.parseInt(key); // Assumi che la chiave per la cifratura di Cesare sia un intero.
                    return option.contains("Encrypt") ? encryptCaesar(message, shift) : decryptCaesar(message, shift);  // Applica la cifratura o la decifratura di Cesare.
                case "Vigenère Encrypt":
                case "Vigenère Decrypt":
                    return option.contains("Encrypt") ? encryptVigenere(message, key) : decryptVigenere(message, key);  // Applica la cifratura o la decifratura di Vigenère.
                default:
                    return message; // Restituisce il messaggio originale se non è applicata alcuna cifratura/decifratura.
            }
        } catch (NumberFormatException e) {  // Gestisce le eccezioni di formato numerico non valido.
            JOptionPane.showMessageDialog(this, "Invalid key format for Caesar cipher. Please enter a valid number.", "Key Error", JOptionPane.ERROR_MESSAGE);  // Mostra un messaggio di errore se il formato della chiave non è valido.
            return message; // Restituisce il messaggio originale se il formato della chiave è errato.
        }
    }

    private String encryptCaesar(String text, int shift) {  // Metodo per cifrare un testo con la cifratura di Cesare.
        shift = shift % 26 + 26; // Normalizza il valore di shift.
        StringBuilder encrypted = new StringBuilder();  // Crea un oggetto StringBuilder per costruire il testo cifrato.
        for (char i : text.toCharArray()) {  // Itera su ogni carattere del testo.
            if (Character.isLetter(i)) {  // Controlla se il carattere è una lettera.
                if (Character.isUpperCase(i)) {  // Controlla se la lettera è maiuscola.
                    encrypted.append((char) ('A' + (i - 'A' + shift) % 26));  // Cifra la lettera maiuscola.
                } else {
                    encrypted.append((char) ('a' + (i - 'a' + shift) % 26));  // Cifra la lettera minuscola.
                }
            } else {
                encrypted.append(i); // Lascia inalterati i caratteri non alfabetici.
            }
        }
        return encrypted.toString(); // Ritorna il testo cifrato.
    }

    private String decryptCaesar(String text, int shift) {  // Metodo per decifrare un testo con la cifratura di Cesare.
        return encryptCaesar(text, -shift); // Utilizza la cifratura di Cesare con uno shift negativo per decifrare.
    }
    

    private String encryptVigenere(String text, String key) {  // Metodo per cifrare un testo con la cifratura di Vigenère.
        StringBuilder result = new StringBuilder();  // Crea un oggetto StringBuilder per costruire il testo cifrato.
        key = key.toLowerCase();  // Converte la chiave in minuscolo.
        int j = 0;  // Inizializza un indice per la chiave.
        for (char c : text.toCharArray()) {  // Itera su ogni carattere del testo.
            if (Character.isLetter(c)) {  // Controlla se il carattere è una lettera.
                int base = (Character.isLowerCase(c) ? 'a' : 'A');  // Determina la base ASCII per lettere minuscole o maiuscole.
                result.append((char) ((c - base + (key.charAt(j) - 'a')) % 26 + base));  // Cifra il carattere con la chiave di Vigenère.
                j = (j + 1) % key.length();  // Aggiorna l'indice della chiave, ripetendo la chiave se necessario.
            } else {
                result.append(c);  // Lascia inalterati i caratteri non alfabetici.
            }
        }
        return result.toString();  // Ritorna il testo cifrato.
    }

    private String decryptVigenere(String text, String key) {  // Metodo per decifrare un testo con la cifratura di Vigenère.
        StringBuilder result = new StringBuilder();  // Crea un oggetto StringBuilder per costruire il testo decifrato.
        key = key.toLowerCase();  // Converte la chiave in minuscolo.
        int j = 0;  // Inizializza un indice per la chiave.
        for (char c : text.toCharArray()) {  // Itera su ogni carattere del testo.
            if (Character.isLetter(c)) {  // Controlla se il carattere è una lettera.
                int base = (Character.isLowerCase(c) ? 'a' : 'A');  // Determina la base ASCII per lettere minuscole o maiuscole.
                result.append((char) ((c - base - (key.charAt(j) - 'a') + 26) % 26 + base));  // Decifra il carattere con la chiave di Vigenère.
                j = (j + 1) % key.length();  // Aggiorna l'indice della chiave, ripetendo la chiave se necessario.
            } else {
                result.append(c);  // Lascia inalterati i caratteri non alfabetici.
            }
        }
        return result.toString();  // Ritorna il testo decifrato.
    }

    public static void main(String[] args) {  // Metodo principale per avviare il client.
        SwingUtilities.invokeLater(ClientGUI::new);  // Crea e mostra l'interfaccia grafica utilizzando il thread di dispatch degli eventi di Swing.
    }
}
