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
        chatInput.addActionListener(this::sendMessageToAllClients); // Send message on pressing Enter
        bottomPanel.add(chatInput, BorderLayout.CENTER);

        cryptoOptions = new JComboBox<>(new String[]{"Plain Text", "Caesar Encrypt", "Caesar Decrypt", "Vigenère Encrypt", "Vigenère Decrypt"});
        bottomPanel.add(cryptoOptions, BorderLayout.WEST);

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
                        textArea.append("Client connected: " + socket.getInetAddress().getHostAddress() + "\n");
                        ClientHandler handler = new ClientHandler(socket);
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
            final String finalMessage = applyCrypto(message, option);  // Create a final variable to use inside the lambda
            clientHandlers.forEach(handler -> handler.sendMessage(finalMessage));
            chatInput.setText("");
            chatArea.append("Server: " + finalMessage + "\n");
        }
    }
    

    private String applyCrypto(String message, String option) {
        switch (option) {
            case "Caesar Encrypt":
                return encryptCaesar(message, 3); // Example shift
            case "Caesar Decrypt":
                return decryptCaesar(message, 3);
            case "Vigenère Encrypt":
                return encryptVigenere(message, "key"); // Example key
            case "Vigenère Decrypt":
                return decryptVigenere(message, "key");
            default:
                return message; // No encryption/decryption
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
        return encryptCaesar(text, -shift);
    }

    private String encryptVigenere(String text, String key) {
        StringBuilder result = new StringBuilder();
        key = key.toLowerCase();
        int keyIndex = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                int base = (Character.isLowerCase(c) ? 'a' : 'A');
                result.append((char) ((c - base + (key.charAt(keyIndex) - 'a')) % 26 + base));
                keyIndex = (keyIndex + 1) % key.length();
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String decryptVigenere(String text, String key) {
        StringBuilder result = new StringBuilder();
        key = key.toLowerCase();
        int keyIndex = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                int base = (Character.isLowerCase(c) ? 'a' : 'A');
                result.append((char) ((c - base - (key.charAt(keyIndex) - 'a') + 26) % 26 + base));
                keyIndex = (keyIndex + 1) % key.length();
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

        ClientHandler(Socket socket) {
            this.socket = socket;
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
                    String processedMessage = applyCrypto(line, (String) cryptoOptions.getSelectedItem());
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
