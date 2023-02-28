package EmNet;

public interface Connection {
    DefaultPacket nextPacket();
    /**
     * Blocks until has next packet, unlike nextPacket()
     * @return the next packet
     */
    DefaultPacket getNextPacket();
    DefaultPacket peekNextPacket();
    void flush();
    int connectionStatus();
    boolean hasNextPacket();
    void sendPacket(String s);
    void endConnection();
    long getConnectionID();
    Connection getConnection();
}
