package Msg;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConnectionAnswer {
    private static final byte RESERVED = (byte) 0x00;

    private final byte socksVersion;
    private final ConnectionAnswerCode responseCode;
    private final AddressTypeCode addressTypeCode;
    private final Object address;
    private final int port;


    public ConnectionAnswer(byte socksVersion, ConnectionAnswerCode responseCode, AddressTypeCode addressTypeCode, Object address, int port) {
        this.socksVersion = socksVersion;
        this.responseCode = responseCode;
        this.addressTypeCode = addressTypeCode;
        this.address = address;
        this.port = port;
    }

    public byte[] toByteArray() {
        int size;
        switch (addressTypeCode) {
            case IPV4_ADDRESS:
                size = 10;
                break;
            case IPV6_ADDRESS:
                size = 22;
                break;
            case DOMAIN_NAME:
                size = 7 + ((String)address).length();
                break;
            default:
                throw new AssertionError("Invalid address type");
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put(socksVersion);
        buffer.put(responseCode.getValue());
        buffer.put(RESERVED);
        buffer.put(addressTypeCode.getValue());

        switch (addressTypeCode) {
            case IPV4_ADDRESS:
                assert address instanceof Inet4Address;
                buffer.put(((Inet4Address)address).getAddress());
                break;
            case IPV6_ADDRESS:
                assert address instanceof Inet6Address;
                buffer.put(((Inet6Address)address).getAddress());
                break;
            case DOMAIN_NAME:
                buffer.put(((String)address).getBytes(UTF_8));
                break;
            default:
                throw new AssertionError("Invalid address type");
        }

        buffer.putShort((short) port);
        return buffer.array();
    }

}
