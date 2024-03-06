import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerGUI extends JFrame {
    private JTextArea textArea;
    private JTextArea chatArea;
    private JButton toggleButton;
    private boolean isRunning;
    private ServerSocket serverSocket;

    public ServerGUI() {
        super("ServerGUI");
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

        toggleButton = new JButton("Start Server");
        toggleButton.addActionListener(e -> toggleServer());
        add(toggleButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void toggleServer() {
        if (!isRunning) {
            startServer(12345);
            toggleButton.setText("Stop Server");
        } else {
            stopServer();
            toggleButton.setText("Start Server");
        }
        isRunning = !isRunning;
    }

    private void startServer(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                textArea.append("Server started on port " + port + "\n");
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    textArea.append("Client connected: " + socket.getInetAddress().getHostAddress() + "\n");
                    new ClientHandler(socket).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                textArea.append("Server exception: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                textArea.append("Server stopped.\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            String clientAddress = socket.getInetAddress().getHostAddress();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    chatArea.append(clientAddress + ": " + inputLine + "\n");
                    // Echo the message back to the client
                    out.println("Server: " + inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}
