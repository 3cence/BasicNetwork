package EmNet;

public class DefaultPacket implements Packet {
    private final Connection connection;
    private final String data;
    private final int type;

    /**
     * Extracts type and data from rawdata. "[<type>]<data>"
     * @param connection Connection that received packet
     * @param rawdata data in format detailed above
     */
    public DefaultPacket(Connection connection, String rawdata) {
        int closeBracket = rawdata.indexOf(']');
        if (closeBracket == -1) {
            throw new RuntimeException("Invalid Data" + rawdata);
        }
        String typeData = rawdata.substring(1, closeBracket);
        this.connection = connection;
        this.type = Integer.parseInt(typeData);
        this.data = rawdata.substring(closeBracket + 1);
    }
    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getData() {
        return data;
    }
    public static DefaultPacket deconstructRawPacket(String s) {
        return new DefaultPacket(null, s);
    }
    @Override
    public String toString() {
        return connection.getConnectionID() + ": " + data;
    }
}
