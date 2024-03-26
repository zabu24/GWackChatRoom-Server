import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class GWackClientNetworking {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected;
    private GWackClientGUI gui; // Reference to the GUI
    private String name;

    public GWackClientNetworking(String serverAddress, int serverPort, GWackClientGUI gui, String name) throws IOException {
        this.gui = gui; // Store the GUI reference
        this.name = name; // Set the name for this client
        this.socket = new Socket(serverAddress, serverPort); // Connect to the server
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); // Enable auto-flush
        this.connected = true;

        writer.println("SECRET");
        writer.println("3c3c4ac618656ae32b7f3431e75f7b26b1a14a87");
        writer.println("NAME");
        writer.println(name);

        new ReaderThread().start();
    }

    public synchronized void writeMessage(String message) throws IOException {
        if (socket != null && socket.isConnected()) {
            writer.println(message);
            writer.flush(); 
        }
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } 
            connected = false;
        }

    class ReaderThread extends Thread {
        public void run() {
            String clientName = "";
            boolean validName = false;
            while (connected) {
                try {
                    String message = reader.readLine();
                    if (message == null) {
                        disconnect();
                        break;
                    } else if (message.equals("START_CLIENT_LIST")) {
                        validName = true;
                    } else if (message.equals("END_CLIENT_LIST")) {
                        gui.updateClients(clientName);
                        validName = false;
                        clientName = "";
                    } else if (validName) {
                        clientName += message + "\n";
                    } else {
                        gui.newMessage(message);
                    }
                } catch (Exception e) {
                }
            }
        }
    }    
}