import javax.swing.*;
import java.awt.*;
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
        bottomPanel.add(chatInput, BorderLayout.CENTER);

        cryptoOptions = new JComboBox<>(new String[]{"Plain Text", "Caesar Encrypt", "Caesar Decrypt", "Vigenère Encrypt", "Vigenère Decrypt"});
        bottomPanel.add(cryptoOptions, BorderLayout.WEST);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessageToAllClients(chatInput.getText()));
        bottomPanel.add(sendButton, BorderLayout.EAST);

        toggleButton = new JButton("Start Server");
        toggleButton.addActionListener(e -> toggleServer());
        add(toggleButton, BorderLayout.NORTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void toggleServer() {
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

    private class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
    
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Error setting up streams: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // Qui puoi decidere di processare i messaggi per cifrarli o decifrarli prima di inviarli agli altri client
                    String processedMessage = processInput(inputLine);
                    sendMessageToAllClients(processedMessage);  // Invia ai client collegati
                    chatArea.append("Client (" + socket.getInetAddress().getHostAddress() + "): " + processedMessage + "\n");
                }
            } catch (IOException e) {
                System.out.println("Error reading from client: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }
    
        private void closeConnection() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
                clientHandlers.remove(this);
                System.out.println("Connection closed with client: " + socket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    
        public void sendMessage(String message) {
            out.println(message);
        }
    
        private String processInput(String input) {
            // Implementa qui la logica di cifratura/decifratura basata su come vuoi processare i messaggi
            // Questo può includere il controllo di comandi specifici o altro.
            return input;  // Per ora, semplicemente inoltra il messaggio non modificato
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

    private void sendMessageToAllClients(String message) {
        String option = (String) cryptoOptions.getSelectedItem();
        final String finalMessage = applyCrypto(message, option);  // Creare una variabile finale
        clientHandlers.forEach(handler -> handler.sendMessage(finalMessage));  // Usa la variabile finale nella lambda
        SwingUtilities.invokeLater(() -> chatArea.append("Server: " + finalMessage + "\n"));
        chatInput.setText("");
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
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                result.append((char) ((c - 'a' + (key.charAt(j) - 'a')) % 26 + 'a'));
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
        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                result.append((char) ((c - 'a' - (key.charAt(j) - 'a') + 26) % 26 + 'a'));
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
}
