package Msg;

import java.net.Inet4Address;
import java.net.Inet6Address;

public class Connection {

    private final byte socksVersion;
    private final ConnectionCommandCode command;
    private final AddressTypeCode addressTypeCode;
    private final Object address;
    private final int port;

    public Connection(byte socksVersion, ConnectionCommandCode command, AddressTypeCode addressTypeCode, Object address, int port) {
        this.socksVersion = socksVersion;
        this.command = command;
        this.addressTypeCode = addressTypeCode;
        this.address = address;
        this.port = port;
    }

    public static int addressCheck(AddressTypeCode addressTypeCode, Object address){
        switch (addressTypeCode) {
            case IPV4_ADDRESS:
                if (!(address instanceof Inet4Address)){
                    System.out.println("Invalid address for specified address type");
                    return -1;
                }
                break;
            case DOMAIN_NAME:
                if (!(address instanceof String)){
                    System.out.println("Invalid address for specified address type");
                    return -1;
                }
                break;
            case IPV6_ADDRESS:
                if (!(address instanceof Inet6Address)){
                    System.out.println("Invalid address for specified address type");
                    return -1;
                }
                break;
            default:
                System.out.println("Invalid Msg.AddressType");
                return -1;
        }
        return 0;
    }

    public byte getSocksVersion() {
        return socksVersion;
    }

    public ConnectionCommandCode getCommandNumber() {
        return command;
    }

    public AddressTypeCode getAddressTypeCode() {
        return addressTypeCode;
    }

    public Object getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}