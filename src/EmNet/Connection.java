package EmNet;

public interface Connection {
    /**
     * Returns the next packet, null if empty
     * @return Next Packet
     */
    DefaultPacket nextPacket();
    /**
     * Blocks until has next packet, unlike nextPacket()
     * @return the next packet
     */
    DefaultPacket getNextPacket();

    /**
     * Returns the next packet, does not remove it;
     * @return Next Packet
     */
    DefaultPacket peekNextPacket();

    /**
     * Wait until send queue is empty
     */
    void flush();

    /**
     * Returns an int, 1 = connected, 0 = connecting, -1 = not connected
     * @return status
     */
    int getConnectionStatus();

    /**
     * Returns whether there is a new packet waiting
     * @return has packet
     */
    boolean hasNextPacket();

    /**
     * Send packet data
     * @param s data
     */
    void sendPacket(int type, String s);

    /**
     * Send packet data
     * @param s data
     */
    void sendPacket(String s);

    /**
     * End the connection thread cleanly
     * TODO: its not clean yet lol
     */
    void endConnection();

    /**
     * Returns connection ID
     * @return id
     */
    long getConnectionID();

    /**
     * Returns a reference to this object
     * (I'm not sure why this exists, but its used somewhere)
     * @return Reference to this
     */
    Connection getConnection();
}
