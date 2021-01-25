package Msg;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum ConnectionCommandCode {
    ESTABLISH_STREAM_CONNECTION((byte)0x01),
    ESTABLISH_PORT_BINDING((byte)0x02),
    ASSOCIATE_UDP_PORT((byte)0x03);

    private final byte value;

    private static final Map<Byte, ConnectionCommandCode> valuesToCommands = Stream.of(values())
            .collect(toMap(ConnectionCommandCode::getValue, e -> e));

    public static ConnectionCommandCode getByValue(byte value) {
        return valuesToCommands.get(value);
    }

    ConnectionCommandCode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}