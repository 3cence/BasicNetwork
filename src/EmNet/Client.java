package EmNet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Clock;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Client extends Thread implements Connection {
    private final String ip;
    private final int port;
    private final List<String> incomingPackets, outgoingPackets;
    private int connectionEstablished = 0;
    private volatile boolean endFlag = false;
    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
        incomingPackets = Collections.synchronizedList(new LinkedList<>());
        outgoingPackets = Collections.synchronizedList(new LinkedList<>());
    }
    private static long time() {
        return Clock.systemUTC().millis();
    }
    public synchronized void flush() {
        while (hasNextPacket()) {}
    }
    public synchronized DefaultPacket nextPacket() {
        String packet = incomingPackets.get(0);
        incomingPackets.remove(0);
        return new DefaultPacket(-1, packet);
    }
    public synchronized DefaultPacket peekNextPacket() {
        String packet = incomingPackets.get(0);
        return new DefaultPacket(-1, packet);
    }
    @Override
    public synchronized DefaultPacket getNextPacket() {
        while (!hasNextPacket()) {}
        return nextPacket();
    }
    public synchronized boolean hasNextPacket() {
        return incomingPackets.size() > 0;
    }
    public synchronized void sendPacket(String s) {
        if (s.equals("keepalive"))
            s = "keepalive\r";
        outgoingPackets.add(s);
    }
    public synchronized void endConnection() {
        endFlag = true;
    }

    @Override
    public long getConnectionID() {
        return -1;
    }
    public synchronized Connection getConnection() {
        return this;
    }

    /**
     * Will return 1 for connected, 0 for connecting, and -1 for connection failure
     * @return Connection Status
     */
    public int connectionStatus() {
        return connectionEstablished;
    }

    @Override
    public void run() {
        try {
            try (Socket server = new Socket(ip, port)) {
                PrintWriter out = new PrintWriter(server.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
                long timeSentKeepalive = time();
                long timeReceiveKeepalive = time();
                connectionEstablished = 1;
                while (true) {
                    if (time() - timeReceiveKeepalive > 10000)
                        break;
                    if (time() - timeSentKeepalive > 5000) {
                        timeSentKeepalive = time();
                        out.println("keepalive");
                    }
                    if (in.ready()) {
                        String s = in.readLine();
                        if (s.equals("keepalive")) {
                            timeReceiveKeepalive = time();
                        }
                        else
                            incomingPackets.add(s);
                    }
                    if (outgoingPackets.size() > 0) {
                        out.println(outgoingPackets.remove(0));
                    }
                    if (endFlag)
                        break;
                }
            }
        } catch (Exception ignored) {}
        connectionEstablished = -1;
    }
    public static void main(String[] args) {
        try {
            Client client = new Client(args[0], Integer.parseInt(args[1]));
            BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
            client.start();

            while (true) {
                if (client.hasNextPacket()) {
                    Packet s = client.nextPacket();
                    System.out.println(s.getData());
                }
                if (scanner.ready()) {
                    String s = scanner.readLine();
                    if (s.equals("exit"))
                        break;
                    client.sendPacket(s);
                }
                if (!client.isAlive())
                    break;
            }
            client.endConnection();
        } catch (Exception ignored) {}
    }
}
