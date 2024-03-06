import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private JTextArea textArea; // Area for notifications and server messages
    private JTextArea chatArea; // Area for chat messages
    private JTextField chatInput; // Input field for chat
    private JButton toggleConnectionButton; // Button to toggle connection state
    private Socket socket;
    private PrintWriter out;

    public ClientGUI() {
        super("ClientGUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        textArea = new JTextArea();
        textArea.setEditable(false);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(textArea), new JScrollPane(chatArea));
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);

        chatInput = new JTextField();
        chatInput.addActionListener(this::sendChatMessage);

        toggleConnectionButton = new JButton("Connect");
        toggleConnectionButton.addActionListener(e -> toggleConnection());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(chatInput, BorderLayout.CENTER);
        bottomPanel.add(toggleConnectionButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void toggleConnection() {
        if (socket == null || socket.isClosed()) {
            connectToServer("localhost", 12345);
        } else {
            disconnect();
        }
    }

    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, port);
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Connected to Server\n");
                    toggleConnectionButton.setText("Disconnect");
                });
                out = new PrintWriter(socket.getOutputStream(), true);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    String message = fromServer;
                    SwingUtilities.invokeLater(() -> chatArea.append("Server: " + message + "\n"));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> textArea.append("Waiting for connection with the server...\n"));
                // Retry connection or handle failed connection attempt
            }
        }).start();
    }

    private void sendChatMessage(ActionEvent event) {
        String message = chatInput.getText();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            chatArea.append("You: " + message + "\n");
            chatInput.setText("");
        }
    }

    private void disconnect() {
        try {
            if (socket != null) {
                socket.close();
                SwingUtilities.invokeLater(() -> {
                    textArea.append("Disconnected from Server\n");
                    toggleConnectionButton.setText("Connect");
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
