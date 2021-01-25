import com.google.protobuf.InvalidProtocolBufferException;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkManager {
    private static final int MULTICAST_PORT = 9192;
    private static final String MULTICAST_IP = "239.192.0.4";
    private Integer seqId = 0;

    DatagramSocket unicastSocket = null;
    MulticastSocket multicastSocket = null;

    private HashMap<String, Master> masters = new HashMap<>();

    public class Master{
        String masterIP;
        Integer masterPort;
        Long lastTime;
        SnakesProto.GameMessage msg;


        public Master(String lastIP, Integer lastPort, long currentTimeMillis, SnakesProto.GameMessage lastMsg) {
            this.masterIP = lastIP;
            this.masterPort = lastPort;
            this.lastTime=currentTimeMillis;
            this.msg = lastMsg;
        }
    }

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

    public DatagramPacket recvUnicastMsg() throws IOException {
        if(unicastSocket == null) {
            try {
                unicastSocket = new DatagramSocket();
                unicastSocket.setSoTimeout(1000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        byte[] recvData = new byte[65506];
        DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
        try {
            unicastSocket.receive(recvPacket);
        } catch (SocketTimeoutException e) {
            return  null;
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

    public HashMap<String, Master> getAvailableGames() {
        return new HashMap<>(masters);
    }


    class MulticastListener implements Runnable {
        @Override
        public void run() {
            while(true) {
                DatagramPacket p = recvMulticastMsg();
                String lastIP = p.getAddress().getHostAddress();
                Integer lastPort = p.getPort();
                //System.out.println("FOUND ip= "+ lastIP+" port= "+lastPort);
                SnakesProto.GameMessage lastMsg = null;
                ByteBuffer buf = ByteBuffer.wrap(p.getData(), 0, p.getLength());
                try {
                    lastMsg = SnakesProto.GameMessage.parseFrom(buf);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }

                String key = lastIP + ":" + lastPort;
                Master m= new Master(lastIP,lastPort,System.currentTimeMillis(),lastMsg);

                if(!masters.containsKey(key)){masters.put(key,m);}
                else{
                    masters.get(key).msg=lastMsg;
                    masters.get(key).lastTime=System.currentTimeMillis();
                }

                List<String> killMasters = new ArrayList<>();
                // determine alive masters
                Long currMilliseconds = System.currentTimeMillis();
                for(String k : masters.keySet()) {
                    if(Math.abs(masters.get(k).lastTime - currMilliseconds) > 3000) {
                        killMasters.add(k);
                    }
                }

                for (String k :killMasters) {
                    masters.remove(k);
                }

            }
        }
    }//сами себя слишим тоже
}
