import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ServerGUI extends JFrame {
    private JTextField messageField;
    private JTextArea chatArea;
    private PrintWriter out;
    private Socket clientSocket;
    private boolean isLoggedIn = false;
    private JPanel sendPanel;

    public ServerGUI() {
        setTitle("Chat Application - Server");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel authPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        JLabel usernameLabel = new JLabel("Username:");
        authPanel.add(usernameLabel);
        JTextField usernameField = new JTextField();
        authPanel.add(usernameField);
        JLabel passwordLabel = new JLabel("Password:");
        authPanel.add(passwordLabel);
        JPasswordField passwordField = new JPasswordField();
        authPanel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                // Thực hiện xác thực thông tin đăng nhập ở đây
                if (username.equals("admin") && password.equals("admin")) {
                    isLoggedIn = true;
                    panel.remove(authPanel);
                    panel.revalidate();
                    panel.repaint();
                } else {
                    JOptionPane.showMessageDialog(ServerGUI.this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        authPanel.add(loginButton);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isLoggedIn = false;
                panel.add(authPanel, BorderLayout.NORTH);
                panel.revalidate();
                panel.repaint();
            }
        });
        authPanel.add(logoutButton);

        panel.add(authPanel, BorderLayout.NORTH);

        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(chatPanel, BorderLayout.WEST);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send Message");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.CENTER);

        // Thêm JPanel để chứa ô gửi tin nhắn và các nút gửi file, ảnh
        sendPanel = new JPanel(new CardLayout());

        // Thêm ô gửi tin nhắn
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(new JLabel("Enter message:"), BorderLayout.WEST);
        JTextField messageField = new JTextField();
        messagePanel.add(messageField, BorderLayout.CENTER);
        JButton sendMsgButton = new JButton("Send");
        sendMsgButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        messagePanel.add(sendMsgButton, BorderLayout.EAST);
        sendPanel.add(messagePanel, "MESSAGE");

        // Thêm ô gửi file, ảnh
        JPanel filePanel = new JPanel(new BorderLayout());
        JButton sendFileButton = new JButton("Send File");
        sendFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });
        filePanel.add(sendFileButton, BorderLayout.WEST);
        JButton sendImageButton = new JButton("Send Image");
        sendImageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendImage();
            }
        });
        filePanel.add(sendImageButton, BorderLayout.CENTER);
        sendPanel.add(filePanel, "FILE");

        panel.add(sendPanel, BorderLayout.SOUTH);

        add(panel);
        setVisible(true);

        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            chatArea.append("Server started. Waiting for clients...\n");

            clientSocket = serverSocket.accept();
            chatArea.append("Client connected: " + clientSocket + "\n");
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            Thread readThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String message;
                        while ((message = in.readLine()) != null) {
                            chatArea.append("Client: " + message + "\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            readThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        if (!isLoggedIn) {
            JOptionPane.showMessageDialog(this, "Please login first.", "Not Logged In", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String message = messageField.getText();
        chatArea.append("Server: " + message + "\n");
        out.println(message);
        messageField.setText("");
    }

    private void sendImage() {
        if (!isLoggedIn) {
            JOptionPane.showMessageDialog(this, "Please login first.", "Not Logged In", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            chatArea.append("You sent an image: " + selectedFile.getName() + "\n");

            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                objectOutputStream.writeObject(selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendFile() {
        if (!isLoggedIn) {
            JOptionPane.showMessageDialog(this, "Please login first.", "Not Logged In", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            chatArea.append("You sent a file: " + selectedFile.getName() + "\n");
            out.println("FILE:" + selectedFile.getName());

            try {
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    clientSocket.getOutputStream().write(buffer, 0, bytesRead);
                }
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ServerGUI();
            }
        });
    }
}

