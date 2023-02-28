package EmNet;

public class DefaultPacket implements Packet {
    private final long connectionID;
    private final String data;

    public DefaultPacket(long connectionID, String data) {
        this.connectionID = connectionID;
        this.data = data;
    }
    @Override
    public long getConnectionID() {
        return connectionID;
    }

    @Override
    public String getType() {
        //TODO: Implement
        return null;
    }

    @Override
    public String getData() {
        return data;
    }
    @Override
    public String toString() {
        return connectionID + ": " + data;
    }
}
