package Msg;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public enum AuthenticationMethodCode {
    NO_AUTHENTICATION((byte)0x00),
    GSSAPI((byte)0x01),
    USERNAME_PASSWORD((byte)0x02),
    NO_ACCEPTABLE_METHOD((byte)0xFF);

    private final byte value;

    private static final Map<Byte, AuthenticationMethodCode> valuesToCommands = Stream.of(values())
            .collect(toMap(AuthenticationMethodCode::getValue, e -> e));

    public static AuthenticationMethodCode getByValue(byte value) {
        return valuesToCommands.get(value);
    }

    AuthenticationMethodCode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}