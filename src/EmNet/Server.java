package EmNet;

import java.io.*;
import java.net.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Server extends Thread {
    public class ClientConnection extends Thread implements Connection {
        private final Socket socket;
        private final long connectionID;
        private int connectionStatus = 0;
        private final List<String> incomingPacketData, outgoingPacketData;
        private volatile boolean endFlag = false;
        public ClientConnection(Socket s, long connectionID) {
            socket = s;
            this.connectionID = connectionID;
            incomingPacketData = Collections.synchronizedList(new LinkedList<>());
            outgoingPacketData = Collections.synchronizedList(new LinkedList<>());
        }

        private long time() {
            return Clock.systemUTC().millis();
        }
        public synchronized long getConnectionID() {
            return connectionID;
        }
        public synchronized Connection getConnection() {
            return this;
        }
        public synchronized void flush() {
            while (hasNextPacket()) {}
        }
        public synchronized DefaultPacket nextPacket() {
            if (!hasNextPacket())
                return null;
            String packet = incomingPacketData.get(0);
            incomingPacketData.remove(0);
            return new DefaultPacket(connectionID, packet);
        }
        public synchronized DefaultPacket peekNextPacket() {
            if (!hasNextPacket())
                return null;
            return new DefaultPacket(connectionID, incomingPacketData.get(0));
        }
        @Override
        public synchronized int connectionStatus() {
            return connectionStatus;
        }
        public synchronized boolean hasNextPacket() {
            return incomingPacketData.size() > 0;
        }

        @Override
        public synchronized DefaultPacket getNextPacket() {
            while (!hasNextPacket()) {}
            return nextPacket();
        }

        public synchronized void sendPacket(String s) {
            if (s.equals("keepalive"))
                s = "keepalive\r";
            outgoingPacketData.add(s);
        }
        public synchronized void endConnection() {
            endFlag = true;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                long lastReceivedPacket = time();
                connectionStatus = 1;
                while (time() - lastReceivedPacket <= 10000) {
                    if (endFlag)
                        break;
                    if (in.ready()) {
                        String receivedData = in.readLine();
                        lastReceivedPacket = time();
                        if (receivedData.equals("keepalive")) {
                            out.println("keepalive");
                            continue;
                        }
                        incomingPacketData.add(receivedData);
                    }
                    if (outgoingPacketData.size() > 0) {
                        out.println(outgoingPacketData.remove(0));
                    }
                }
            } catch (IOException ignored) { }
            activeConnections.remove(this);
            if (connectionEndEvent != null)
                connectionEndEvent.trigger(this);
            connectionStatus = -1;
        }
    }

    private final List<ClientConnection> activeConnections, newConnections;
    private Event<Connection> connectionEndEvent;
    private final int port;
    private volatile boolean endFlag = false;

    public Server(int port) {
        this.port = port;
        activeConnections = Collections.synchronizedList(new ArrayList<>());
        newConnections = Collections.synchronizedList(new ArrayList<>());
    }

    public void onConnectionEnd(Event<Connection> e) {
        connectionEndEvent = e;
    }
    public synchronized boolean hasConnections() {
        return activeConnections.size() > 0;
    }
    public synchronized int activeConnections() {
        return activeConnections.size();
    }
    public synchronized List<Connection> getConnections() {
        ArrayList<Connection> connections = new ArrayList<>(activeConnections.size());
        connections.addAll(activeConnections);
        return connections;
    }
    public synchronized Connection getConnection(long connectionID) {
        for (Connection c: activeConnections) {
            if (c.getConnectionID() == connectionID)
                return c;
        }
        return null;
    }
    public synchronized boolean hasNewConnections() {
        return newConnections.size() > 0;
    }
    public synchronized List<Connection> getNewConnections() {
        ArrayList<Connection> connections = new ArrayList<>(newConnections.size());
        for (int i = 0; i < newConnections.size(); i++) {
            connections.add(newConnections.remove(0));
        }
        return connections;
    }
    public synchronized Connection getNewConnection() {
        return newConnections.remove(0);
    }
    public synchronized List<DefaultPacket> getUnprocessedPackets() {
        ArrayList<DefaultPacket> p = new ArrayList<>(activeConnections.size());
        for (Connection c: activeConnections) {
            while (c.hasNextPacket()) {
                p.add(c.nextPacket());
            }
        }
        return p;
    }
    public void stopServer() {
        endFlag = true;
        Client c = new Client("127.0.0.1", port);
        c.start();
        c.endConnection();
    }

    public void run() {
        try {
            try (ServerSocket server = new ServerSocket(port)) {
                long connectionIDs = 0;
                while (!endFlag) {
                    Socket newClientSocket = server.accept();
                    ClientConnection newClient = new ClientConnection(newClientSocket, connectionIDs);
                    activeConnections.add(newClient);
                    newConnections.add(newClient);
                    newClient.start();
                    connectionIDs++;
                }
            }
        } catch (IOException ignored) {}
        for (Connection c: activeConnections) {
            c.endConnection();
        }
    }

    public static void main(String[] args) {
        Server server = new Server(25567);
        server.onConnectionEnd(connection -> System.out.println("Connection Ended: " + connection.getConnectionID()));
        server.start();
        boolean running = true;
        while (running) {
            List<DefaultPacket> packets = server.getUnprocessedPackets();
            while (packets.size() > 0) {
                Packet p = packets.remove(0);
                Connection c = server.getConnection(p.getConnectionID());
                if (p.getData().equals("CHEAT-CODE12345CRASH")) {
                    running = false;
                    break;
                }
                System.out.println("<Server(" + server.activeConnections() + " Active Connection(s))> ID: " + p.getConnectionID() + ": " + p.getData());
                c.sendPacket("Received data [" + p.getData() + "]");
            }
            for (Connection c: server.getNewConnections()) {
                System.out.println("New Connection: " + c.getConnectionID());
            }
        }
        server.stopServer();
    }
}