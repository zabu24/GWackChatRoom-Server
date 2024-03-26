import java.util.*;
import java.net.*;
import java.io.*;

public class GWackChannel {

    private ServerSocket serverSock;
    private List<GWackConnectedClient> clients;
    private Queue<String> messageQueue;

    public GWackChannel(int port) {
        clients = new ArrayList<>();
        messageQueue = new LinkedList<>();
        try {
            serverSock = new ServerSocket(port);
        } catch (Exception e) {
            System.err.println("Cannot establish server socket on port " + port);
            System.exit(1);
        }
    }

    public void serve() {
        while (true) {
            try {
                Socket clientSock = serverSock.accept();
                GWackConnectedClient clientHandler = new GWackConnectedClient(clientSock);
                clientHandler.start();
            } catch (Exception e) {
                System.err.println("An error occurred while accepting a new connection.");
                e.printStackTrace();
            }
        }
    }

    public synchronized void addClient(GWackConnectedClient client) {
        clients.add(client);
        enqueueMessage(getClientList());
    }

    public synchronized void removeClient(GWackConnectedClient client) {
        clients.remove(client);
        enqueueMessage(getClientList());
    }

    public synchronized void enqueueMessage(String message) {
        messageQueue.add(message);
        dequeueAll(); // Make sure to call dequeueAll from a synchronized method to maintain thread safety
    }

    public synchronized void dequeueAll() {
        while (!messageQueue.isEmpty()) {
            String message = messageQueue.poll();
            broadcastMessage(message);
        }
    }

    private synchronized void broadcastMessage(String message) {
        for (GWackConnectedClient client : clients) {
            if (client.isValid()) {
                client.sendMessage(message);
            }
        }
    }

    public synchronized String getClientList() {
        List<String> validClientNames = new ArrayList<>();
        for (GWackConnectedClient client : clients) {
            if (client.isValid()) {
                validClientNames.add(client.getClientName());
            }
        }
        
        String clientListStr = "START_CLIENT_LIST\n";
        for (String name : validClientNames) {
            clientListStr += name + "\n";
        }
        clientListStr += "END_CLIENT_LIST";
    
        return clientListStr;
    }
    
    

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        GWackChannel server = new GWackChannel(port);
        server.serve();
    }

    private class GWackConnectedClient extends Thread {
        private Socket sock;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        GWackConnectedClient(Socket socket) {
            this.sock = socket;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                closeEverything();
            }
        }

        public boolean isValid() {
            return !sock.isClosed();
        }

        public String getClientName() {
            return clientName;
        }

        public void sendMessage(String message) {
            out.println(message);
            out.flush();
        }

        public void run() {
            try {
                // Secret Handshake
                String secretKeyword = in.readLine(); // Should be "SECRET"
                String secretValue = in.readLine(); // The actual secret
                String nameKeyword = in.readLine(); // Should be "NAME"
                clientName = in.readLine(); // The client's username

                if ("SECRET".equals(secretKeyword) &&
                    "3c3c4ac618656ae32b7f3431e75f7b26b1a14a87".equals(secretValue) &&
                    "NAME".equals(nameKeyword) && 
                    clientName != null && !clientName.isEmpty()) {

                    GWackChannel.this.addClient(this);
                    broadcastMessage(clientName + " has joined the chat.");

                    String incomingMessage;
                    while ((incomingMessage = in.readLine()) != null) {
                        enqueueMessage(clientName + ": " + incomingMessage);
                    }
                } else {
                    closeEverything();
                }
            } catch (IOException e) {
                System.err.println("Error in communication with client " + getClientName());
                e.printStackTrace();
            } 
                closeEverything();
            }

        private void closeEverything() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (sock != null) sock.close();
            } catch (IOException e) {
                System.err.println("Could not close the client's socket and streams.");
                e.printStackTrace();
            } 
                GWackChannel.this.removeClient(this);
            }
        }
    }
