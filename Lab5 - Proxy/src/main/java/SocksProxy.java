
import EndPoint.*;
import Msg.*;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import static java.nio.channels.SelectionKey.*;

public class SocksProxy implements Runnable {
    private static final String DNS_SERVER_ADDR = "8.8.8.8";
    private static final int DNS_SERVER_PORT = 53;

    private final SimpleResolver simpleResolver;
    {
        try {
            simpleResolver = new SimpleResolver(DNS_SERVER_ADDR);
        } catch (UnknownHostException e) {
            throw new ExceptionInInitializerError();
        }
        simpleResolver.setPort(DNS_SERVER_PORT);
    }

    private static final byte VERSION = 0x05;
    private static final int BACKLOG = 10;

    private final int proxyPort;

    public SocksProxy(int port) {
        proxyPort = port;
    }

    @Override
    public void run() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open();
             Selector selector = Selector.open()) {

            serverSocket.bind(new InetSocketAddress(proxyPort), BACKLOG);
            serverSocket.configureBlocking(false);

            serverSocket.register(selector, OP_ACCEPT);

            while (!Thread.currentThread().isInterrupted()) {
                int selCount = selector.select();

                if(selCount == 0){continue;}

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isValid() && key.isAcceptable()) {
                        try{ doAccept(key);}
                        catch (IOException e){
                            System.err.println("Unable to accept channel");
                            e.printStackTrace();
                            key.cancel();
                        }
                    }
                    if (key.isValid() && key.isConnectable()) {
                        try{ doConnect(key);}
                        catch (IOException e){
                            System.err.println("Unable to connect channel");
                            e.printStackTrace();
                            key.cancel();
                        }
                    }
                    if (key.isValid() && key.isReadable()) {
                        try{doRead(key);}
                        catch(IOException e){
                            System.err.println("Unable to read channel");
                            e.printStackTrace();
                            key.cancel();
                        }
                    }
                    if (key.isValid() && key.isWritable()) {
                        try{doWrite(key);}
                        catch(IOException e){
                            System.err.println("Unable to write channel");
                            e.printStackTrace();
                            key.cancel();
                        }
                    }

                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to setup environment");
        }
    }

    private void doAccept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel clientSocketChannel = serverSocketChannel.accept();

        System.out.println("--------------------------------------ACCEPTED NEW CLIENT: " + clientSocketChannel.getRemoteAddress());

        clientSocketChannel.configureBlocking(false);
        SelectionKey clientSelectionKey = clientSocketChannel.register(selectionKey.selector(), OP_READ);

        BothConnectionSides bothConnectionSides = new BothConnectionSides(clientSocketChannel, clientSelectionKey);
        clientSelectionKey.attach(new ChannelShall(bothConnectionSides, ChannelShall.SocketChannelSide.CLIENT));
    }

    private void doConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel destSocketChannel = (SocketChannel) selectionKey.channel();
        ChannelShall channelShall = (ChannelShall) selectionKey.attachment();
        BothConnectionSides bothConnectionSides = channelShall.getBothConnectionSides();

        SocketAddress addr = destSocketChannel.getRemoteAddress();

        try {
            if (!destSocketChannel.finishConnect()) {
                throw new AssertionError("Unexpected behaviour: true or exception were expected");
            }

            System.out.println("Connected to " + destSocketChannel.getRemoteAddress());

            selectionKey.interestOps(0);
            bothConnectionSides.getClientSelectionKey().interestOps(OP_WRITE);
            bothConnectionSides.setSideState(SideState.SEND_CONNECTION_ANSWER);

            ConnectionAnswer response = new ConnectionAnswer(VERSION, ConnectionAnswerCode.REQUEST_GRANTED,
                    bothConnectionSides.getType(), bothConnectionSides.getDestAddress().getAddress(), bothConnectionSides.getDestAddress().getPort());

            bothConnectionSides.getDestToClientBuffer().put(response.toByteArray());

        } catch (IOException e) {
            System.out.println("Unable to connect to " + addr);

            ConnectionAnswer response = new ConnectionAnswer(VERSION, ConnectionAnswerCode.HOST_UNREACHABLE,
                    bothConnectionSides.getType(), bothConnectionSides.getDestAddress().getAddress(), bothConnectionSides.getDestAddress().getPort());
            bothConnectionSides.getDestToClientBuffer().put(response.toByteArray());

            bothConnectionSides.setCloseUponSending(true);
            bothConnectionSides.getClientSelectionKey().interestOps(OP_WRITE);
            bothConnectionSides.setSideState(SideState.SEND_CONNECTION_ANSWER);
        }
    }

    private void doRead(SelectionKey selectionKey) throws IOException {
        ChannelShall channelShall = (ChannelShall) selectionKey.attachment();
        BothConnectionSides bothConnectionSides = channelShall.getBothConnectionSides();

        switch (channelShall.getSocketChannelSide()) {
            case CLIENT: {
                readFromClient(bothConnectionSides, selectionKey);
                break;
            }
            case DESTINATION: {
                readFromDestination(bothConnectionSides, selectionKey);
                break;
            }
            default:
                throw new AssertionError("Unexpected socket channel side received");
        }
    }

    private void doWrite(SelectionKey selectionKey) throws IOException {
        ChannelShall channelShall = (ChannelShall) selectionKey.attachment();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        BothConnectionSides bothConnectionSides = channelShall.getBothConnectionSides();

        switch (channelShall.getSocketChannelSide()) {
            case CLIENT: {
                writeToClient(socketChannel, bothConnectionSides, selectionKey);
                break;
            }
            case DESTINATION: {
                bothConnectionSides.getClientToDestBuffer().flip();

                long writeNum = socketChannel.write(bothConnectionSides.getClientToDestBuffer());
                System.out.println("Write " + writeNum + " bytes to DESTIN (" + socketChannel.getRemoteAddress()+")");

                // wrote him all the data
                if (bothConnectionSides.getClientToDestBuffer().remaining() == 0) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~OP_WRITE);

                    if (bothConnectionSides.getSideState() == SideState.CLOSED) {
                        bothConnectionSides.closeDestSide();
                        return;
                    }
                }

                // if we write something to dest socket channel, we can read something from client socket channel
                if (writeNum > 0) {
                    bothConnectionSides.getClientSelectionKey().interestOps(
                            bothConnectionSides.getClientSelectionKey().interestOps() | OP_READ);
                }

                bothConnectionSides.getClientToDestBuffer().compact();
                break;
            }
            default:
                throw new AssertionError("Unexpected socket channel side received");
        }
    }

    private void readFromClient(BothConnectionSides bothConnectionSides, SelectionKey selectionKey) throws IOException {

        SocketChannel socketChannelClient = (SocketChannel) selectionKey.channel();

        if (bothConnectionSides.getSideState() == SideState.CLOSED) {
            bothConnectionSides.closeClientSide();
            return;
        }

        long recvNum;
        try {
            recvNum = socketChannelClient.read(bothConnectionSides.getClientToDestBuffer());
        } catch (IOException e) {
            return;
        }

        if (recvNum == -1) {
            bothConnectionSides.closeClientSide();
            if (bothConnectionSides.getDestSelectionKey() != null) {
                bothConnectionSides.getDestSelectionKey().interestOps(bothConnectionSides.getDestSelectionKey().interestOps() & ~OP_READ);
            }
            return;
        }

        System.out.println("Read " + recvNum + " bytes from CLIENT (" + socketChannelClient.getRemoteAddress() + ")");

        switch (bothConnectionSides.getSideState()) {
            case RECV_HELLO:
                sendHelloAnswer(socketChannelClient, bothConnectionSides);
                break;
            case RECV_CONNECTION:
                sendConnectionToDestOrBadConnectionToClient(socketChannelClient, bothConnectionSides);
                break;
            case RECV_FROM_DEST_SEND_TO_CLIENT:
                // buf is full,cant read something from client socket channel
                if (bothConnectionSides.getClientToDestBuffer().remaining() == 0) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~OP_READ);
                }

                // if we have read something from client, we can write it to destination socket channel
                if (recvNum > 0) {
                    bothConnectionSides.getDestSelectionKey().interestOps(
                            bothConnectionSides.getDestSelectionKey().interestOps() | OP_WRITE);
                }
                break;

            case SEND_HELLO_ANSWER:
            case CONNECTING_TO_DEST:
            case SEND_CONNECTION_ANSWER:
            case CLOSED:
            default:
                throw new AssertionError("Invalid State " + bothConnectionSides.getSideState());
        }
    }

    private void readFromDestination(BothConnectionSides bothConnectionSides, SelectionKey selectionKey)throws IOException {

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        if (bothConnectionSides.getSideState() == SideState.CLOSED) {
            bothConnectionSides.closeDestSide();
            return;
        }


        long recvNum;
        try {
            recvNum = socketChannel.read(bothConnectionSides.getDestToClientBuffer());
        } catch (IOException e) {
            return;
        }

        System.out.println("Read " + recvNum + " bytes from " + socketChannel.getRemoteAddress());

        if (recvNum == -1) { // socket was closed by destination side
            System.out.println(socketChannel.getRemoteAddress() + " DESTIN closed connection");
            bothConnectionSides.closeDestSide();
            bothConnectionSides.getClientSelectionKey().interestOps(bothConnectionSides.getClientSelectionKey().interestOps() & ~OP_READ);
            return;
        }

        // buf is full,cant read something from destination socket channel
        if (bothConnectionSides.getDestToClientBuffer().remaining() == 0) {
            selectionKey.interestOps(selectionKey.interestOps() & ~OP_READ);
        }

        // if we have read something from dest, we can write it to client socket channel
        if (recvNum > 0) {
            bothConnectionSides.getClientSelectionKey().interestOps(
                    bothConnectionSides.getClientSelectionKey().interestOps() | OP_WRITE);
        }
    }

    private void connectToDestination(BothConnectionSides bothConnectionSides, InetSocketAddress address) throws IOException {
        SocketChannel destSocketChannel = SocketChannel.open();
        destSocketChannel.configureBlocking(false);
        destSocketChannel.connect(address);

        SelectionKey destSelectionKey = destSocketChannel.register(bothConnectionSides.getClientSelectionKey().selector(), OP_CONNECT);
        destSelectionKey.attach(new ChannelShall(bothConnectionSides, ChannelShall.SocketChannelSide.DESTINATION));

        bothConnectionSides.setDestSelectionKey(destSelectionKey);
        bothConnectionSides.setDestSocketChannel(destSocketChannel);
    }

    private void sendHelloAnswer(SocketChannel socketChannel, BothConnectionSides bothConnectionSides) throws IOException {

        Hello hello = bothConnectionSides.helloParser();

        if(hello == null){
            System.out.println("Invalid greeting message received from " + socketChannel.getRemoteAddress() +
                    ". Closing connection.");
            bothConnectionSides.closeClientSide();
            return;
       }


        if (hello.getSocksVersion() != VERSION) {
            System.out.println("Unsupported version of socks protocol from client " + socketChannel.getRemoteAddress());
            bothConnectionSides.closeClientSide();
            return;
        }

        HelloAnswer answer;
        if (hello.hasAuthMethod(AuthenticationMethodCode.NO_AUTHENTICATION)) {
            answer = new HelloAnswer(VERSION, AuthenticationMethodCode.NO_AUTHENTICATION);

        } else {
            System.out.println("Client doesn't supports required auth method " + socketChannel.getRemoteAddress());
            answer = new HelloAnswer(VERSION, AuthenticationMethodCode.NO_ACCEPTABLE_METHOD);
            bothConnectionSides.setCloseUponSending(true);
        }

        bothConnectionSides.getDestToClientBuffer().put(answer.toByteArray());
        bothConnectionSides.getClientSelectionKey().interestOps(OP_WRITE);
        bothConnectionSides.setSideState(SideState.SEND_HELLO_ANSWER);
    }

    private void sendConnectionToDestOrBadConnectionToClient(SocketChannel socketChannel, BothConnectionSides bothConnectionSides) throws IOException {

        Connection request = bothConnectionSides.connectionParser();

        if(request == null){
            System.out.println("Invalid connection request received from " + socketChannel.getRemoteAddress());
            bothConnectionSides.closeClientSide();
            return;
        }


        if (request.getSocksVersion() != VERSION) {
            System.out.println("Unsupported version of socks protocol from client " + socketChannel.getRemoteAddress());
            bothConnectionSides.closeClientSide();
            return;
        }

        switch (request.getCommandNumber()) {
            case ESTABLISH_STREAM_CONNECTION:
                InetAddress address;
                switch (request.getAddressTypeCode()) {
                    case IPV4_ADDRESS: {
                        address = (Inet4Address) request.getAddress();
                        break;
                    }
                    case DOMAIN_NAME: {
                        address = resolveDomainName(request);
                        break;
                    }
                    case IPV6_ADDRESS: {
                        address = (Inet6Address) request.getAddress();
                        break;
                    }
                    default:
                        throw new AssertionError("Invalid address type received");
                }

                if (address == null) {
                    System.out.println("Unable to resolve hostname " + request.getAddress());

                    ConnectionAnswer response = new ConnectionAnswer(VERSION, ConnectionAnswerCode.HOST_UNREACHABLE,
                            AddressTypeCode.IPV4_ADDRESS, InetAddress.getLocalHost(), proxyPort);

                    bothConnectionSides.setCloseUponSending(true);
                    bothConnectionSides.getClientSelectionKey().interestOps(OP_WRITE);
                    bothConnectionSides.setSideState(SideState.SEND_CONNECTION_ANSWER);

                    bothConnectionSides.getDestToClientBuffer().put(response.toByteArray());
                    break;
                }

                if(Connection.addressCheck(request.getAddressTypeCode(),request.getAddress()) == -1){
                    System.out.println("Address type and address do not match " + request.getAddress());

                    ConnectionAnswer response = new ConnectionAnswer(VERSION, ConnectionAnswerCode.ADDR_TYPE_NOT_SUPPORTED,
                            AddressTypeCode.IPV4_ADDRESS, InetAddress.getLocalHost(), proxyPort);

                    bothConnectionSides.setCloseUponSending(true);
                    bothConnectionSides.getClientSelectionKey().interestOps(OP_WRITE);
                    bothConnectionSides.setSideState(SideState.SEND_CONNECTION_ANSWER);

                    bothConnectionSides.getDestToClientBuffer().put(response.toByteArray());
                    break;
                }

                bothConnectionSides.getClientSelectionKey().interestOps(0);
                bothConnectionSides.setSideState(SideState.CONNECTING_TO_DEST);

                InetSocketAddress inetSocketAddress = new InetSocketAddress(address, request.getPort());
                bothConnectionSides.setDestAddress(inetSocketAddress);
                connectToDestination(bothConnectionSides, inetSocketAddress);

                break;

            case ASSOCIATE_UDP_PORT:
            case ESTABLISH_PORT_BINDING:
            default:
                System.out.println("Unsupported method received from " + socketChannel.getRemoteAddress());

                ConnectionAnswer response = new ConnectionAnswer(VERSION, ConnectionAnswerCode.CAD_NOT_SUPPORTED,
                        AddressTypeCode.IPV4_ADDRESS, InetAddress.getLocalHost(), proxyPort);

                bothConnectionSides.setCloseUponSending(true);
                bothConnectionSides.getClientSelectionKey().interestOps(OP_WRITE);
                bothConnectionSides.setSideState(SideState.SEND_CONNECTION_ANSWER);

                bothConnectionSides.getDestToClientBuffer().put(response.toByteArray());
        }
    }

    private InetAddress resolveDomainName(Connection connection) {
        try {
            Lookup lookup = new Lookup((String) connection.getAddress(), Type.A);
            lookup.setResolver(simpleResolver);

            Record[] result = lookup.run();
            if (result.length > 0) {
                return ((ARecord) result[0]).getAddress();
            } else {
                return null;
            }

        } catch (TextParseException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private void writeToClient(SocketChannel socketChannel, BothConnectionSides bothConnectionSides, SelectionKey selectionKey) throws IOException {
        bothConnectionSides.getDestToClientBuffer().flip();

        long writeNum = socketChannel.write(bothConnectionSides.getDestToClientBuffer());
        System.out.println("Write " + writeNum + " bytes to CLIENT (" + socketChannel.getRemoteAddress() + ")");

        switch (bothConnectionSides.getSideState()) {
            case SEND_HELLO_ANSWER:
                if (bothConnectionSides.getDestToClientBuffer().remaining() == 0) {
                    if (bothConnectionSides.isCloseUponSending()) {
                        bothConnectionSides.closeClientSide();
                    } else {
                        bothConnectionSides.setSideState(SideState.RECV_CONNECTION);
                        bothConnectionSides.getClientSelectionKey().interestOps(OP_READ);
                    }
                }
                break;
            case SEND_CONNECTION_ANSWER:
                if (bothConnectionSides.getDestToClientBuffer().remaining() == 0) {
                    if (bothConnectionSides.isCloseUponSending()) {
                        bothConnectionSides.closeClientSide();
                    } else {
                        bothConnectionSides.setSideState(SideState.RECV_FROM_DEST_SEND_TO_CLIENT);

                        bothConnectionSides.getClientSelectionKey().interestOps(OP_READ);
                        bothConnectionSides.getDestSelectionKey().interestOps(OP_READ);
                    }
                }
                break;
            case RECV_FROM_DEST_SEND_TO_CLIENT:
                // wrote him all the data
                if (bothConnectionSides.getDestToClientBuffer().remaining() == 0) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~OP_WRITE);

                    if (bothConnectionSides.getSideState() == SideState.CLOSED) {
                        bothConnectionSides.closeDestSide();
                        return;
                    }
                }

                // if we write something to client socket channel, we can read something from dest socket channel
                if (writeNum > 0) {
                    bothConnectionSides.getDestSelectionKey().interestOps(
                            bothConnectionSides.getDestSelectionKey().interestOps() | OP_READ);
                }
                break;

            case CLOSED: {
                if (bothConnectionSides.getDestToClientBuffer().remaining() == 0) {
                    bothConnectionSides.closeClientSide();
                }
                break;
            }

            case RECV_HELLO:
            case RECV_CONNECTION:
            case CONNECTING_TO_DEST:
            default:
                throw new AssertionError("Invalid State");
        }

        bothConnectionSides.getDestToClientBuffer().compact();
    }
}