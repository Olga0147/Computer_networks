package Msg;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum AddressTypeCode {
    IPV4_ADDRESS((byte)0x01),
    DOMAIN_NAME((byte)0x03),
    IPV6_ADDRESS((byte)0x04);

    private final byte value;

    private static final Map<Byte, AddressTypeCode> valuesToCommands = Stream.of(values())
            .collect(toMap(AddressTypeCode::getValue, e -> e));

    public static AddressTypeCode getByValue(byte value) {
        return valuesToCommands.get(value);
    }

    AddressTypeCode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}