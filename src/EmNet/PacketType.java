package EmNet;

public class PacketType {
    /**
     * If you don't want to use a default type
     */
    public static final int NULLTYPE = -1;
    /**
     * Keepalive packet, used to detect unintentional disconnections
     * WARNING: DO NOT USE EXTERNALLY
     */
    public static final int KEEPALIVE = 0;
    /**
     * First packet sent by server/client
     * WARNING: DO NOT USE EXTERNALLY
     */
    public static final int INITIALIZE = 1;
    /**
     * Last packet sent by server/client to initiate graceful shutdown
     * WARNING: DO NOT USE EXTERNALLY
     */
    public static final int TERMINATE = 2;
    /**
     * Request packet: Used to request information from destination
     */
    public static final int REQUEST = 3;
    /**
     * Response: Response packet to Request
     */
    public static final int RESPONSE = 4;
    /**
     * Information: Used for sending information
     */
    public static final int INFORMATION = 5;

    /**
     * Checks if the given type can only be used internally
     * @param c Type
     * @return is Internal
     */
    public static boolean isInternalCommand(int c) {
        return c == 0 || c == 1 || c == 2;
    }
}
