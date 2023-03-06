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
    private volatile int connectionStatus = 0;
    private volatile Event<Connection> onConnectionEnd;
    private volatile Event<Packet> onReceiveNewPacket;
    private volatile boolean keepPackets = false;
    private volatile boolean endFlag = false;
    public Client(String ip, int port) {
        this.ip = ip;
        this.port = port;
        onConnectionEnd = e->{};
        incomingPackets = Collections.synchronizedList(new LinkedList<>());
        outgoingPackets = Collections.synchronizedList(new LinkedList<>());
    }
    private static long time() {
        return Clock.systemUTC().millis();
    }
    public synchronized void flush() {
        while (hasNextPacket()) {}
    }
    public synchronized DefaultPacket getNextPacket() {
        String packet = incomingPackets.get(0);
        incomingPackets.remove(0);
        return new DefaultPacket(this, packet);
    }
    public synchronized DefaultPacket peekNextPacket() {
        String packet = incomingPackets.get(0);
        return new DefaultPacket(this, packet);
    }
    @Override
    public synchronized DefaultPacket nextPacket() {
        while (!hasNextPacket()) {}
        return getNextPacket();
    }
    public synchronized boolean hasNextPacket() {
        return incomingPackets.size() > 0;
    }
    public synchronized void sendPacket(int type, String s) {
        if (PacketType.isInternalCommand(type))
            throw new RuntimeException("Tried to sent internal only");
        outgoingPackets.add("[" + type + "]" + s);
    }
    public synchronized void sendPacket(String s) {
        sendPacket(PacketType.NULLTYPE, s);
    }
    private synchronized void sendInternalPacket(int type, String s) {
        outgoingPackets.add("[" + type + "]" + s);
    }
    public synchronized void endConnection() {
        if (connectionStatus != -1) {
            sendInternalPacket(PacketType.TERMINATE, "");
            flush();
            connectionStatus = -1;
        }
    }

    /**
     * trigger an event when you get a new packet
     * @param e event to trigger
     * @param clearPackets keep new packets after event trigger?
     */
    public synchronized void onReceiveNewPacket(Event<Packet> e, boolean clearPackets) {
        if (e == null)
            throw new RuntimeException("Cant have null event");
        onReceiveNewPacket = e;
        keepPackets = clearPackets;
    }
    /**
     * trigger an event when you get a new packet. clears packets out of queue when set
     * @param e event to trigger
     */
    public synchronized void onReceiveNewPacket(Event<Packet> e) {
        onReceiveNewPacket(e, false);
    }
    public synchronized void onConnectionEnd(Event<Connection> e) {
        if (e == null)
            throw new RuntimeException("Cant have null event");
        onConnectionEnd = e;
    }

    @Override
    public long getConnectionID() {
        return -1;
    }
    public synchronized Connection getConnection() {
        return this;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public void run() {
        try {
            try (Socket server = new Socket(ip, port)) {
                PrintWriter out = new PrintWriter(server.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
                long timeSentKeepalive = time();
                long timeReceiveKeepalive = time();
                while (connectionStatus != -1) {
                    if (in.ready()) {
                        if (DefaultPacket.deconstructRawPacket(in.readLine()).getType() == PacketType.INITIALIZE) {
                            connectionStatus = 1;
                            sendInternalPacket(PacketType.INITIALIZE, "");
                            break;
                        }
                    }
                    if (time() - timeReceiveKeepalive > 10000) {
                        connectionStatus = -1;
                        break;
                    }
                    sleep(100);
                }
                while (connectionStatus == 1) {
                    if (time() - timeReceiveKeepalive > 10000)
                        break;
                    if (time() - timeSentKeepalive > 5000) {
                        timeSentKeepalive = time();
                        sendInternalPacket(PacketType.KEEPALIVE, "");
                    }
                    if (in.ready()) {
                        String s = in.readLine();
                        if (DefaultPacket.deconstructRawPacket(s).getType() == PacketType.KEEPALIVE) {
                            timeReceiveKeepalive = time();
                        }
                        else if (DefaultPacket.deconstructRawPacket(s).getType() == PacketType.TERMINATE) {
                            connectionStatus = -1;
                        }
                        else
                            incomingPackets.add(s);
                    }
                    if (outgoingPackets.size() > 0) {
                        out.println(outgoingPackets.remove(0));
                    }
                    if (incomingPackets.size() > 0 && onReceiveNewPacket != null) {
                        onReceiveNewPacket.trigger(peekNextPacket());
                        if (!keepPackets)
                            getNextPacket();
                    }
                    sleep(100);
                }
            }
        } catch (Exception ignored) {
//            throw new RuntimeException(ignored);
        }
        connectionStatus = -1;
        onConnectionEnd.trigger(this);
    }
    public static void main(String[] args) {
        try {
            Client client = new Client(args[0], Integer.parseInt(args[1]));
            BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
            int type;
            if (args.length > 2)
                type = Integer.parseInt(args[3]);
            else
                type = PacketType.NULLTYPE;
            client.start();

            while (true) {
                sleep(100);
                if (client.hasNextPacket()) {
                    Packet s = client.getNextPacket();
                    System.out.println(s.getData());
                }
                if (scanner.ready()) {
                    String s = scanner.readLine();
                    if (s.equals("exit"))
                        break;
                    client.sendPacket(type, s);
                }
                if (!client.isAlive())
                    break;
            }
            client.endConnection();
        } catch (Exception ignored) {}
    }
}
