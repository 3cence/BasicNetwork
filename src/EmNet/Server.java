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
        private volatile Event<Connection> onConnectionEnd;
        private volatile boolean endFlag = false;
        public ClientConnection(Socket s, long connectionID) {
            socket = s;
            this.connectionID = connectionID;
            onConnectionEnd = e->{};
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
        public synchronized DefaultPacket getNextPacket() {
            if (!hasNextPacket())
                return null;
            String packet = incomingPacketData.get(0);
            incomingPacketData.remove(0);
            return new DefaultPacket(this, packet);
        }
        public synchronized DefaultPacket peekNextPacket() {
            if (!hasNextPacket())
                return null;
            return new DefaultPacket(this, incomingPacketData.get(0));
        }
        @Override
        public synchronized int getConnectionStatus() {
            return connectionStatus;
        }
        public synchronized boolean hasNextPacket() {
            return incomingPacketData.size() > 0;
        }

        @Override
        public synchronized DefaultPacket nextPacket() {
            while (!hasNextPacket()) {}
            return getNextPacket();
        }

        public synchronized void sendPacket(int type, String s) {
            if (PacketType.isInternalCommand(type))
                throw new RuntimeException();
            outgoingPacketData.add("[" + type + "]" + s);
        }
        public synchronized void sendPacket(String s) {
            sendPacket(PacketType.NULLTYPE, s);
        }
        private synchronized void sendInternalPacket(int type, String s) {
            outgoingPacketData.add("[" + type + "]" + s);
        }
        public synchronized void endConnection() {
            if (connectionStatus != -1) {
                sendInternalPacket(PacketType.TERMINATE, "");
                flush();
                connectionStatus = -1;
            }
        }
        public void onConnectionEnd(Event<Connection> e) {
            if (e == null)
                throw new RuntimeException("Cant have null event");
            onConnectionEnd = e;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                long lastReceivedPacket = time();
                out.println(String.format("[%d]", PacketType.INITIALIZE));
                while (connectionStatus != -1) {
                    if (in.ready()) {
                        if (DefaultPacket.deconstructRawPacket(in.readLine()).getType() == PacketType.INITIALIZE) {
                            connectionStatus = 1;
                            break;
                        }
                    }
                    if (time() - lastReceivedPacket > 10000) {
                        connectionStatus = -1;
                        break;
                    }
                    sleep(100);
                }
                while (time() - lastReceivedPacket <= 10000 && connectionStatus == 1) {
                    if (endFlag)
                        break;
                    if (in.ready()) {
                        String receivedData = in.readLine();
                        lastReceivedPacket = time();
                        if (DefaultPacket.deconstructRawPacket(receivedData).getType() == PacketType.KEEPALIVE) {
                            sendInternalPacket(PacketType.KEEPALIVE, "");
                        }
                        else if (DefaultPacket.deconstructRawPacket(receivedData).getType() == PacketType.TERMINATE) {
                            connectionStatus = -1;
                        }
                        else
                        incomingPacketData.add(receivedData);
                    }
                    if (outgoingPacketData.size() > 0) {
                        out.println(outgoingPacketData.remove(0));
                    }
                    sleep(100);
                }
            } catch (IOException | InterruptedException ignored) { }
            activeConnections.remove(this);
            connectionStatus = -1;
            onConnectionEnd.trigger(this);
        }
    }

    private final List<ClientConnection> activeConnections, newConnections;
    private final int port;
    private volatile boolean endFlag = false;

    public Server(int port) {
        this.port = port;
        activeConnections = Collections.synchronizedList(new ArrayList<>());
        newConnections = Collections.synchronizedList(new ArrayList<>());
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
                p.add(c.getNextPacket());
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
                    sleep(100);
                }
            } catch (InterruptedException ignored) { }
        } catch (IOException ignored) {}
        for (Connection c: activeConnections) {
            c.endConnection();
        }
    }

    public static void main(String[] args) {
        Server server = new Server(25567);
        server.start();
        boolean running = true;
        while (running) {
            List<DefaultPacket> packets = server.getUnprocessedPackets();
            while (packets.size() > 0) {
                Packet p = packets.remove(0);
                Connection c = p.getConnection();
                if (p.getData().equals("CHEAT-CODE12345CRASH")) {
                    running = false;
                    break;
                }
                System.out.println("<Server(" + server.activeConnections() + " Active Connection(s))> ID: " + p.getConnection() + ": " + p.getData());
                c.sendPacket(PacketType.NULLTYPE, "Received data [" + p.getData() + "]");
            }
            for (Connection c: server.getNewConnections()) {
                System.out.println("New Connection: " + c.getConnectionID());
            }
            try {
                sleep(100);
            } catch (InterruptedException ignored) { }
        }
        server.stopServer();
    }
}
