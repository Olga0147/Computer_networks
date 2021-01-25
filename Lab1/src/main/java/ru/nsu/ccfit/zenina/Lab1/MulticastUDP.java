package ru.nsu.ccfit.zenina.Lab1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

class MulticastUDP {
    private static final long TIME = 1000;
    private static final int PORT = 4321;
    private static final int EXIST_ATTEMPTS = 4;
    private HashMap<String, Integer> ipAddresses = new HashMap<>();
    private MulticastSocket multicastSocket;
    private InetAddress groupAddress;
    private String address;

    MulticastUDP(String address) {
        this.address = address;
    }

    void work() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        try {
            multicastSocket = new MulticastSocket(PORT);
            groupAddress = InetAddress.getByName(address);
            multicastSocket.joinGroup(groupAddress);

            while (!Thread.interrupted()) {

                DatagramPacket msg= new DatagramPacket("Hello".getBytes(), "Hello".length(), groupAddress, PORT);
                multicastSocket.send(msg);
                long sendTime = System.currentTimeMillis();

                for (String key : ipAddresses.keySet()) {
                    ipAddresses.replace(key, ipAddresses.get(key), ipAddresses.get(key) - 1);
                }

                while (System.currentTimeMillis() - sendTime < TIME) {
                    try {
                        multicastSocket.setSoTimeout((int) TIME);
                        DatagramPacket recv_msg = new DatagramPacket(new byte[16], 16);
                        multicastSocket.receive(recv_msg);
                        updateHashMap(recv_msg);
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
                if (ipAddresses.entrySet().removeIf(e -> e.getValue() == 0)) {
                    System.out.println(ipAddresses.keySet().toString());
                    System.out.println("------------");
                }
                Thread.sleep(TIME);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }

    }

    private void updateHashMap(DatagramPacket datagramPacket) {
        if (!ipAddresses.containsKey(datagramPacket.getAddress().getHostAddress())) {
            ipAddresses.put(datagramPacket.getAddress().getHostAddress(), EXIST_ATTEMPTS);
            System.out.println("------------\n NEW: " + datagramPacket.getAddress().getHostAddress());
            System.out.println(ipAddresses.keySet().toString());
        } else {
            ipAddresses.replace(datagramPacket.getAddress().getHostAddress(), EXIST_ATTEMPTS);
        }
    }

    private void stop() {
        try {
            multicastSocket.leaveGroup(groupAddress);
            multicastSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
