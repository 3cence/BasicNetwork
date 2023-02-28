package EmNet;

/**
 * Packet Interface
 */
public interface Packet {
    /**
     * Gets the ID of the connection that sent the packet.
     * For use on Servers
     * @return Connection ID
     */
    long getConnectionID();

    /**
     * Returns the type of packet
     * ex: request, information, response
     * Note: Some types are used in the Client & Server implementations,
     * so be careful while using the following:
     * keepalive, initialize, and terminate
     * @return The value in the Type Field
     */
    String getType();

    /**
     * Returns the data sent by the packet as a string
     * @return Data
     */
    String getData();
}
