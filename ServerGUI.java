import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerGUI extends JFrame {
    private JTextArea textArea; // Area di testo per i log di sistema e le informazioni.
    private JTextArea chatArea; // Area di testo per la chat tra client e server.
    private JTextField chatInput; // Campo di testo per inserire messaggi da inviare ai client.
    private JTextField keyField; // Campo per la chiave di cifratura/decifratura.
    private JButton toggleButton; // Pulsante per avviare o fermare il server.
    private JComboBox<String> cryptoOptions; // Menu a tendina per selezionare il tipo di cifratura/decifratura.
    private boolean isRunning; // Stato del server, true se in esecuzione.
    private ServerSocket serverSocket; // Server socket per accettare connessioni.
    private CopyOnWriteArrayList<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>(); // Lista di gestori client.

    public ServerGUI() {
        super("Server GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setupComponents();
        setVisible(true);
    }

    private void setupComponents() {
        textArea = new JTextArea();
        textArea.setEditable(false);
        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              new JScrollPane(textArea),
                                              new JScrollPane(chatArea));
        splitPane.setDividerLocation(250);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        chatInput.addActionListener(this::sendMessageToAllClients);
        bottomPanel.add(chatInput, BorderLayout.CENTER);

        cryptoOptions = new JComboBox<>(new String[]{"Plain Text", "Caesar Encrypt", "Caesar Decrypt", "Vigenère Encrypt", "Vigenère Decrypt"});
        bottomPanel.add(cryptoOptions, BorderLayout.WEST);

        keyField = new JTextField();
        keyField.setVisible(false);
        cryptoOptions.addActionListener(e -> keyField.setVisible(!"Plain Text".equals(cryptoOptions.getSelectedItem())));
        bottomPanel.add(keyField, BorderLayout.NORTH);

        toggleButton = new JButton("Start Server");
        toggleButton.addActionListener(this::toggleServer);
        bottomPanel.add(toggleButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void toggleServer(ActionEvent event) {
        if (!isRunning) {
            startServer(12345);
            toggleButton.setText("Stop Server");
            isRunning = true;
        } else {
            stopServer();
            toggleButton.setText("Start Server");
            isRunning = false;
        }
    }

    private void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            textArea.append("Server connected on port " + port + "\n");
            new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        Socket socket = serverSocket.accept();
                        String currentKey = keyField.getText();
                        ClientHandler handler = new ClientHandler(socket, currentKey);
                        clientHandlers.add(handler);
                        handler.start();
                    }
                } catch (IOException e) {
                    textArea.append("Server interrupted.\n");
                }
            }).start();
        } catch (IOException e) {
            textArea.append("Unable to start server: " + e.getMessage() + "\n");
        }
    }

    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                textArea.append("Server stopped.\n");
            }
        } catch (IOException e) {
            textArea.append("Error stopping server: " + e.getMessage() + "\n");
        }
    }

    private void sendMessageToAllClients(ActionEvent event) {
        String message = chatInput.getText();
        if (!message.isEmpty()) {
            String option = (String) cryptoOptions.getSelectedItem();
            String key = keyField.getText();
    
            if (keyRequired(option) && key.isEmpty()) {
                JOptionPane.showMessageDialog(this, "A key is required for " + option, "Key Error", JOptionPane.ERROR_MESSAGE);
            } else {
                String finalMessage = applyCrypto(message, option, key);
                clientHandlers.forEach(handler -> handler.sendMessage(finalMessage));
                chatArea.append("You: " + finalMessage + "\n");
                chatInput.setText("");
            }
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
                    int shift = Integer.parseInt(key);
                    return option.contains("Encrypt") ? encryptCaesar(message, shift) : decryptCaesar(message, shift);
                case "Vigenère Encrypt":
                case "Vigenère Decrypt":
                    return option.contains("Encrypt") ? encryptVigenere(message, key) : decryptVigenere(message, key);
                default:
                    return message;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid key format for Caesar cipher. Please enter a valid number.", "Key Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private String encryptCaesar(String text, int shift) {
        StringBuilder encrypted = new StringBuilder();
        shift = shift % 26;
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
        return encryptCaesar(text, 26 - (shift % 26));
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
        SwingUtilities.invokeLater(ServerGUI::new);
    }

    class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String key;  // Key for encryption/decryption

        ClientHandler(Socket socket, String key) {
            this.socket = socket;
            this.key = key;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String processedMessage = applyCrypto(line, (String) cryptoOptions.getSelectedItem(), key);
                    for (ClientHandler client : clientHandlers) {
                        if (client != this) client.out.println(processedMessage);
                    }
                    SwingUtilities.invokeLater(() -> chatArea.append("Client: " + processedMessage + "\n"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientHandlers.remove(this);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
