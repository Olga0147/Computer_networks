import com.google.protobuf.InvalidProtocolBufferException;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class NetworkManager {
    private static final int MULTICAST_PORT = 9192;
    private static final String MULTICAST_IP = "239.192.0.4";
    private Integer seqId = 0;

    DatagramSocket unicastSocket = null;
    MulticastSocket multicastSocket = null;

    private HashMap<String, Long> masterIPs = new HashMap<>();
    private HashMap<String, Integer> masterPorts = new HashMap<String, Integer>();
    private HashMap<String, SnakesProto.GameMessage> masterMsgs = new HashMap<String, SnakesProto.GameMessage>();

    public NetworkManager() {
        new Thread(new MulticastListener()).start();
    }

    public String getUnicastIp() {
        return unicastSocket.getInetAddress().getHostAddress();
    }

    public int getUnicastPort() {
        return unicastSocket.getPort();
    }

    public void sendUnicastMsg(SnakesProto.GameMessage gameMessage, String addr, int udpPort) {
        if (unicastSocket == null) {
            try {
                unicastSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        byte[] sendData = gameMessage.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, udpPort);
        try {
            unicastSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public DatagramPacket recvUnicastMsg() {
        if(unicastSocket == null) {
            try {
                unicastSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        byte[] recvData = new byte[65506];
        DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
        try {
            unicastSocket.receive(recvPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recvPacket;
    }

    public int getSeq() {
        return seqId++;
    }

    public void sendMulticastMsg(SnakesProto.GameMessage gameMessage) {
        if(unicastSocket == null) {
            try {
                unicastSocket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName(MULTICAST_IP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        byte[] sendData = gameMessage.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, MULTICAST_PORT);
        try {
            unicastSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DatagramPacket recvMulticastMsg() {
        if(multicastSocket == null) {
            try {
                InetAddress IPAddress = null;
                try {
                    IPAddress = InetAddress.getByName(MULTICAST_IP);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                multicastSocket = new MulticastSocket(MULTICAST_PORT);
                multicastSocket.joinGroup(IPAddress);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        byte[] recvData = new byte[1024];
        DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
        try {
            multicastSocket.receive(recvPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recvPacket;
    }

    public HashMap<String, SnakesProto.GameMessage> getAvailableGames() {
        return new HashMap<String, SnakesProto.GameMessage>(masterMsgs);
    }

    public HashMap<String, Integer> getMasterPorts() {
        return new HashMap<String, Integer>(masterPorts);
    }

    class MulticastListener implements Runnable {
        @Override
        public void run() {
            while(true) {
                DatagramPacket p = recvMulticastMsg();
                String lastIP = p.getAddress().getHostAddress();
                Integer lastPort = p.getPort();
                SnakesProto.GameMessage lastMsg = null;
                ByteBuffer buf = ByteBuffer.wrap(p.getData(), 0, p.getLength());
                try {
                    lastMsg = SnakesProto.GameMessage.parseFrom(buf);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                masterIPs.put(lastIP, System.currentTimeMillis());
                masterPorts.put(lastIP, lastPort);
                masterMsgs.put(lastIP, lastMsg);
                // determine alive masters
                Long currMilliseconds = System.currentTimeMillis();
                for(Map.Entry<String, Long> e : masterIPs.entrySet()) {
                    if(Math.abs(e.getValue() - currMilliseconds) > 3000) {
                        masterIPs.remove(e.getKey());
                        masterMsgs.remove(e.getKey());
                        masterPorts.remove(e.getKey());
                    }
                }
            }
        }
    }
}
