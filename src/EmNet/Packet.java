package EmNet;

/**
 * Packet Interface
 */
public interface Packet {
    /**
     * Gets the reference to the connection that sent the packet.
     * For use on Servers
     * @return Connection ID
     */
    Connection getConnection();

    /**
     * Returns the type of packet
     * ex: request, response, information, nulltype
     * Note: Some types are used in the Client & Server implementations,
     * do not use the following:
     * keepalive, initialize, and terminate
     * @return The value in the Type Field
     */
    int getType();

    /**
     * Returns the data sent by the packet as a string
     * @return Data
     */
    String getData();
}
