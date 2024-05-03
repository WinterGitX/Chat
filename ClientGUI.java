import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private JTextArea textArea; // Area for displaying system events or server messages.
    private JTextArea chatArea; // Area for user chat.
    private JTextField chatInput; // Text field for entering messages to send.
    private JTextField keyField;
    private JButton toggleConnectionButton; // Button to connect/disconnect from the server.
    private JComboBox<String> cryptoOptions; // Dropdown menu for selecting encryption/decryption.
    private Socket socket; // Socket for connecting to the server.
    private PrintWriter out; // Tool for sending messages to the server.
    private BufferedReader in; // Reader for receiving messages from the server.

    public ClientGUI() {
        super("Client Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        initializeComponents();
        setVisible(true);
    }

    private void initializeComponents() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane1 = new JScrollPane(textArea);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane2 = new JScrollPane(chatArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane1, scrollPane2);
        splitPane.setDividerLocation(250); // Posizione del divisore tra le due aree.
        add(splitPane, BorderLayout.CENTER);

        chatInput = new JTextField();
        chatInput.addActionListener(this::sendChatMessage); // Send message when pressing Enter
        
        toggleConnectionButton = new JButton("Connect");
        toggleConnectionButton.addActionListener(this::toggleConnection);

        cryptoOptions = new JComboBox<>(new String[]{
            "Plain Text", "Caesar Encrypt", "Caesar Decrypt", "Vigenère Encrypt", "Vigenère Decrypt"
        });
        cryptoOptions.addActionListener(this::updateCryptoOptions);

        keyField = new JTextField(); // Campo di testo per la chiave.
        keyField.setVisible(true); // Sempre visibile.

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(cryptoOptions, BorderLayout.WEST);
        bottomPanel.add(chatInput, BorderLayout.CENTER);
        bottomPanel.add(toggleConnectionButton, BorderLayout.EAST);
        bottomPanel.add(keyField, BorderLayout.NORTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateCryptoOptions(ActionEvent event) {
        String selected = (String) cryptoOptions.getSelectedItem();
        boolean isKeyNeeded = selected.endsWith("Encrypt") || selected.endsWith("Decrypt");
        keyField.setVisible(isKeyNeeded); // Mostra il campo chiave solo per le opzioni che richiedono una chiave.
        revalidate(); // Aggiorna il layout per mostrare/nascondere il campo chiave.
    }

    private void toggleConnection(ActionEvent event) {
        if (socket == null || socket.isClosed()) {
            connectToServer("localhost", 12345);
            toggleConnectionButton.setText("Disconnect");
        } else {
            disconnect();
            toggleConnectionButton.setText("Connect");
        }
    }

    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Connesso al server: " + serverAddress + ":" + port + "\n"); // Mostra la connessione nel text area
                    toggleConnectionButton.setText("Disconnettiti");
                });
                receiveMessages(); // Chiama il metodo per ricevere i messaggi
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> textArea.append("Impossibile connettersi al server: " + e.getMessage() + "\n"));
                cleanupResources(); // Pulizia delle risorse in caso di eccezione
            }
        }).start();
    }

    // Metodo per disconettersi al server.
    private void disconnect() {
        if (socket != null && !socket.isClosed()) {
            try {
                if (out != null) {
                    out.close();
                }
                // Close the socket
                socket.close();
                // Clear UI components or state as necessary
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Disconnesso.\n");
                    toggleConnectionButton.setText("Connetti"); // Change button text to "Connect"
                });
            } catch (IOException e) {
                // Log to UI or console in case of an error during disconnect
                SwingUtilities.invokeLater(() -> textArea.append("Errore di disconnessione: " + e.getMessage() + "\n"));
            } finally {
                // Ensure resources are nullified or reset state as necessary
                cleanupResources();
            }
        }
    }

    private void sendChatMessage(ActionEvent event) {
        String message = chatInput.getText();
        if (!message.isEmpty()) {
            String selectedCrypto = (String) cryptoOptions.getSelectedItem();
            String key = keyField.getText();
            if (keyRequired(selectedCrypto) && key.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Key is required for " + selectedCrypto, "Key Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String encryptedMessage = applyCrypto(message, selectedCrypto, key);
            out.println(encryptedMessage);
            chatArea.append("Tu: " + encryptedMessage + "\n");
            chatInput.setText("");
        }
    }

    private void receiveMessages() {
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
    
    private String applyDecryptionIfNeeded(String message) {
        if (shouldDecrypt()) {
            return applyCrypto(message, (String) cryptoOptions.getSelectedItem(), keyField.getText());
        }
        return message;
    }
    
    private boolean shouldDecrypt() {
        String option = (String) cryptoOptions.getSelectedItem();
        return option != null && option.endsWith("Decrypt");
    }
    
    private void cleanupResources() {
        try {
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> textArea.append("Error cleaning up resources: " + e.getMessage() + "\n"));
        }
    }
    
    private boolean keyRequired(String option) {
        return option.contains("Caesar") || option.contains("Vigenère");
    }

    private String applyCrypto(String message, String option, String key) {
        try {
            switch (option) {
                case "Caesar Encrypt":
                case "Caesar Decrypt":
                    int shift = Integer.parseInt(key); // Assume that key for Caesar must be an integer
                    return option.contains("Encrypt") ? encryptCaesar(message, shift) : decryptCaesar(message, shift);
                case "Vigenère Encrypt":
                case "Vigenère Decrypt":
                    return option.contains("Encrypt") ? encryptVigenere(message, key) : decryptVigenere(message, key);
                default:
                    return message; // No encryption/decryption applied
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid key format for Caesar cipher. Please enter a valid number.", "Key Error", JOptionPane.ERROR_MESSAGE);
            return message; // Return the original message if the key format is wrong
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
    

    private String encryptVigenere(String text, String key) {
        StringBuilder result = new StringBuilder();
        key = key.toLowerCase();
        int j = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                int base = (Character.isLowerCase(c) ? 'a' : 'A');
                result.append((char) ((c - base + (key.charAt(j) - 'a')) % 26 + base));
                j = (j + 1) % key.length();
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String decryptVigenere(String text, String key) {
        StringBuilder result = new StringBuilder();
        key = key.toLowerCase();
        int j = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                int base = (Character.isLowerCase(c) ? 'a' : 'A');
                result.append((char) ((c - base - (key.charAt(j) - 'a') + 26) % 26 + base));
                j = (j + 1) % key.length();
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}