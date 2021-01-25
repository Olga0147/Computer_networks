package EndPoint;

import Msg.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BothConnectionSides {
    private static final int BUF_SIZE = 1024;

    private InetSocketAddress destAddress;

    private SideState sideState;

    private final SocketChannel clientSocketChannel;
    private SocketChannel destSocketChannel;

    private final SelectionKey clientSelectionKey;
    private SelectionKey destSelectionKey;

    private AddressTypeCode type;

    private final ByteBuffer clientToDestBuffer = ByteBuffer.allocate(BUF_SIZE);
    private final ByteBuffer destToClientBuffer = ByteBuffer.allocate(BUF_SIZE);

    private boolean closeUponSending;

    public BothConnectionSides(SocketChannel clientSocketChannel, SelectionKey clientSelectionKey) {
        this.clientSocketChannel = clientSocketChannel;
        this.clientSelectionKey = clientSelectionKey;
        sideState = SideState.RECV_HELLO;
        closeUponSending = false;
    }

    public ByteBuffer getClientToDestBuffer() {
        return clientToDestBuffer;
    }

    public ByteBuffer getDestToClientBuffer() {
        return destToClientBuffer;
    }

    public SelectionKey getClientSelectionKey() {
        return clientSelectionKey;
    }

    public SelectionKey getDestSelectionKey() {
        return destSelectionKey;
    }

    public void setDestSocketChannel(SocketChannel destSocketChannel) {
        this.destSocketChannel = destSocketChannel;
    }

    public void setDestSelectionKey(SelectionKey destSelectionKey) {
        this.destSelectionKey = destSelectionKey;
    }

    public SideState getSideState() {
        return sideState;
    }

    public void setSideState(SideState sideState) {
        this.sideState = sideState;
    }

    public Hello helloParser()  {

        clientToDestBuffer.mark();
        clientToDestBuffer.flip();

        byte socksVersion = clientToDestBuffer.get();
        byte authMethodsNum = clientToDestBuffer.get();
        if(authMethodsNum<0){return null;}
        Set<AuthenticationMethodCode> authenticationMethodCodes = new HashSet<>();

        for (int i = 0; i < authMethodsNum; ++i) {
            AuthenticationMethodCode authenticationMethodCode = AuthenticationMethodCode.getByValue(clientToDestBuffer.get());

            if (authenticationMethodCode != null) {
                authenticationMethodCodes.add(authenticationMethodCode);
            }
        }

        clientToDestBuffer.compact();
        return new Hello(socksVersion, authenticationMethodCodes);

    }

    public  Connection connectionParser(){

        clientToDestBuffer.mark();
        clientToDestBuffer.flip();

        final byte socksVersion = clientToDestBuffer.get();

        final ConnectionCommandCode command = ConnectionCommandCode.getByValue(clientToDestBuffer.get());
        if (command == null) { System.out.println("Invalid command");return  null; }

        byte reserved = clientToDestBuffer.get();
        if(reserved != (byte) 0x00){System.out.println("Invalid reserved byte");return null;}

        final AddressTypeCode addressTypeCode = AddressTypeCode.getByValue(clientToDestBuffer.get());
        if (addressTypeCode == null) { System.out.println("Invalid address type");return null;}

        type = addressTypeCode;

        final Object address;

        switch (addressTypeCode) {
            case IPV4_ADDRESS:
            case IPV6_ADDRESS: {
                int size = (addressTypeCode == AddressTypeCode.IPV4_ADDRESS) ? 4 : 16;
                byte[] bytes = new byte[size];
                clientToDestBuffer.get(bytes);

                try {
                    address = InetAddress.getByAddress(bytes);
                } catch (UnknownHostException uhe) {
                    System.out.println("Invalid address");
                    return null;
                }

                break;
            }
            case DOMAIN_NAME: {
                byte domainNameLength = clientToDestBuffer.get();
                if(domainNameLength <0){System.out.println("Invalid domain length");return null;}

                byte[] domainName = new byte[domainNameLength];
                clientToDestBuffer.get(domainName);

                address = new String(domainName, UTF_8);
                break;
            }
            default:
                System.out.println("Invalid address type number");
                return null;
        }

        int port = clientToDestBuffer.getShort();

        clientToDestBuffer.compact();

        return new Connection(socksVersion, command, addressTypeCode, address, port);

    }

    public void closeClientSide() throws IOException {
        System.out.println("Closed CLIENT: (" + clientSocketChannel.getRemoteAddress()+")");

        clientSelectionKey.cancel();
        clientSocketChannel.close();

        setSideState(SideState.CLOSED);
    }

    public void closeDestSide() throws IOException {
        System.out.println("Closed DESTIN: (" + destSocketChannel.getRemoteAddress()+")");

        destSelectionKey.cancel();
        destSocketChannel.close();

        setSideState(SideState.CLOSED);

    }

    public boolean isCloseUponSending() {
        return closeUponSending;
    }

    public void setCloseUponSending(boolean closeUponSending) {
        this.closeUponSending = closeUponSending;
    }

    public InetSocketAddress getDestAddress() {
        return destAddress;
    }

    public void setDestAddress(InetSocketAddress destAddress) {
        this.destAddress = destAddress;
    }

    public AddressTypeCode getType() {
        return type;
    }
}