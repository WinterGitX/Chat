import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private JTextArea textArea; // Area for displaying system events or server messages.
    private JTextArea chatArea; // Area for user chat.
    private JTextField chatInput; // Text field for entering messages to send.
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

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(cryptoOptions, BorderLayout.WEST);
        bottomPanel.add(chatInput, BorderLayout.CENTER);
        bottomPanel.add(toggleConnectionButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
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

    private void sendChatMessage(ActionEvent event) {
        String message = chatInput.getText();
        if (!message.isEmpty()) {
            message = applyCrypto(message, (String) cryptoOptions.getSelectedItem());
            out.println(message);
            chatArea.append("You: " + message + "\n");
            chatInput.setText("");
        }
    }

    private String applyCrypto(String message, String option) {
        switch (option) {
            case "Caesar Encrypt":
                return encryptCaesar(message, 3);
            case "Caesar Decrypt":
                return decryptCaesar(message, 3);
            case "Vigenère Encrypt":
                return encryptVigenere(message, "key");
            case "Vigenère Decrypt":
                return decryptVigenere(message, "key");
            default:
                return message; // No encryption/decryption applied
        }
    }

    private String encryptCaesar(String text, int shift) {
        StringBuilder encrypted = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                int base = (Character.isLowerCase(c) ? 'a' : 'A');
                encrypted.append((char) ((c - base + shift) % 26 + base));
            } else {
                encrypted.append(c);
            }
        }
        return encrypted.toString();
    }

    private String decryptCaesar(String text, int shift) {
        return encryptCaesar(text, -shift);
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
