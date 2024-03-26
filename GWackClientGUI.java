import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class GWackClientGUI extends JFrame {

    private JTextArea messageDisplayArea;
    private JTextArea clientListArea;
    private JTextField messageInputField;
    private JButton toggleConnectionButton;
    private JTextField nameField;
    private JTextField hostField;
    private JTextField portField;
    private GWackClientNetworking clientNetworking;
    private static GWackClientGUI gui;


    public GWackClientGUI() {
        createGUI();
        updateConnectionStatus(false);
    }

    private void createGUI() {
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel membersOnlinePanel = new JPanel(new BorderLayout());
        JLabel membersOnlineLabel = new JLabel("Members Online");
        membersOnlinePanel.add(membersOnlineLabel, BorderLayout.NORTH);
        clientListArea = new JTextArea(10, 15);
        clientListArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(clientListArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(clientListArea.getPreferredSize().width, Integer.MAX_VALUE));

        membersOnlinePanel.add(scrollPane, BorderLayout.CENTER);
        add(membersOnlinePanel, BorderLayout.WEST);
        add(membersOnlinePanel, BorderLayout.WEST);

        JPanel messagesPanel = new JPanel(new BorderLayout());
        JLabel messagesLabel = new JLabel("Messages");
        messagesPanel.add(messagesLabel, BorderLayout.NORTH);
        messageDisplayArea = new JTextArea(10, 30);
        messageDisplayArea.setEditable(false);
        messagesPanel.add(new JScrollPane(messageDisplayArea), BorderLayout.CENTER);

        add(messagesPanel, BorderLayout.CENTER);

        JPanel composePanel = new JPanel(new BorderLayout());
        JLabel composeLabel = new JLabel("Compose");
        messageInputField = new JTextField(20);
        messageInputField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    sendMessage();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = messageInputField.getText();
                if (!message.isEmpty() && clientNetworking != null && clientNetworking.isConnected()) {
                    try {
                        clientNetworking.writeMessage(message);
                        messageInputField.setText(""); 
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(GWackClientGUI.this,
                                "Error sending message: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        composePanel.add(composeLabel, BorderLayout.WEST);
        composePanel.add(messageInputField, BorderLayout.CENTER);
        composePanel.add(sendButton, BorderLayout.EAST);
        add(composePanel, BorderLayout.SOUTH);

        toggleConnectionButton = new JButton("Connect");

        JPanel connectionPanel = new JPanel();
        connectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel nameLabel = new JLabel("Name:");
        nameField = new JTextField(10);
        JLabel ipLabel = new JLabel("IP Address:");
        hostField = new JTextField(10);
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField(5);

        connectionPanel.add(nameLabel);
        connectionPanel.add(nameField);
        connectionPanel.add(ipLabel);
        connectionPanel.add(hostField);
        connectionPanel.add(portLabel);
        connectionPanel.add(portField);
        connectionPanel.add(toggleConnectionButton);
        toggleConnectionButton.addActionListener(new ConnectActionListener());

        add(membersOnlinePanel, BorderLayout.WEST);
        add(connectionPanel, BorderLayout.NORTH);
    }

    private class ConnectActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (clientNetworking == null || !clientNetworking.isConnected()) {
                try {
                    String host = hostField.getText();
                    int port = Integer.parseInt(portField.getText());
                    clientNetworking = new GWackClientNetworking(host, port, GWackClientGUI.this, nameField.getText());
                    updateConnectionStatus(true);
    
                    toggleConnectionButton.setText("Disconnect");
                    toggleConnectionButton.removeActionListener(this);
                    toggleConnectionButton.addActionListener(new DisconnectActionListener());
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(GWackClientGUI.this,
                            "Invalid port number.",
                            "Connection Error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (IOException ioException) {
                    JOptionPane.showMessageDialog(GWackClientGUI.this,
                            "Cannot connect to server. Please check the host and port.",
                            "Connection Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    

    private class DisconnectActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (clientNetworking != null && clientNetworking.isConnected()) {
                clientNetworking.disconnect();
                updateConnectionStatus(false);
    
                // Change button text and listener back to "Connect"
                toggleConnectionButton.setText("Connect");
                toggleConnectionButton.removeActionListener(this);
                toggleConnectionButton.addActionListener(new ConnectActionListener());
                messageDisplayArea.setText("");
                clientListArea.setText("");
            }
        }
    }
    
    

    public void updateConnectionStatus(boolean isConnected) {
        if (isConnected) {
            setTitle("GWack -- GW Slack Simulator (connected)");
        } else {
            setTitle("GWack -- Disconnected");
        }
        nameField.setEnabled(!isConnected);
        hostField.setEnabled(!isConnected);
        portField.setEnabled(!isConnected);
    }
    

    public void updateClients(String clients) {
        clientListArea.setText(""); // Clear the current display
        String[] members = clients.split(","); // Assuming the clients string is comma-separated
        for (String member : members) {
            clientListArea.append(member + "\n"); // Append each client name
        }
    }

    public void sendMessage() {
        String message = messageInputField.getText().trim();
        if (!message.isEmpty() && clientNetworking != null && clientNetworking.isConnected()) {
            try {
                clientNetworking.writeMessage(message);
                messageInputField.setText(""); // Clear the input field
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Error sending message: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    

public void newMessage(String message) {
    messageDisplayArea.append(message + "\n");
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui = new GWackClientGUI();
                gui.setVisible(true);
            }
        });
    }
}
