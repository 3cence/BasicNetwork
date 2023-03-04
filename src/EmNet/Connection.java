package EmNet;

public interface Connection {
    /**
     * Returns the next packet, null if empty
     * @return Next Packet
     */
    DefaultPacket getNextPacket();
    /**
     * Blocks until has next packet, unlike getNextPacket()
     * @return the next packet
     */
    DefaultPacket nextPacket();

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
     */
    void endConnection();

    /**
     * Set an event to be triggered upon the connection closing
     * @param e Event object w/method
     */
    void onConnectionEnd(Event<Connection> e);

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
