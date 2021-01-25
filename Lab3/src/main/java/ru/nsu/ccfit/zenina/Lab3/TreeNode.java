package ru.nsu.ccfit.zenina.Lab3;

import ru.nsu.ccfit.zenina.Lab3.Message.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class TreeNode {
    private static final int BUF_SIZE = 2048;
    private static final int QUEUE_SIZE = 100;

    private String name;
    private InetSocketAddress myInetSocketAddress;
    private int percentageLoss;
    private boolean isRoot;
    private InetSocketAddress parentInetSocketAddress;

    private InetSocketAddress substituteInetSocketAddress;///не просто адрес, а адрес+порт
    private boolean sub;

    private DatagramSocket socket;

    private CopyOnWriteArrayList<InetSocketAddress> neighbourAddresses = new CopyOnWriteArrayList<>();

    private ArrayBlockingQueue<MsgShell> needToSendMsgs = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private CopyOnWriteArrayList<UUID> alreadySentMsgs = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Msg> msgHistory = new CopyOnWriteArrayList<>();

    TreeNode(String name, int percentageLoss, String address, int port) {
        this.name = name;
        this.percentageLoss = percentageLoss;
        this.myInetSocketAddress = new InetSocketAddress(address, port);
        this.isRoot = true;
        this.sub=false;
    }
    TreeNode(String name, int percentageLoss, int port, String address, String parentAddress, int parentPort) {
        this(name, percentageLoss, address, port);
        this.parentInetSocketAddress = new InetSocketAddress(parentAddress, parentPort);
        this.isRoot = false;
        this.sub=false;
    }

    void start() {
        try {
            socket = new DatagramSocket(myInetSocketAddress.getPort(),myInetSocketAddress.getAddress());
            System.out.println("start: MYsocket="+socket.getLocalAddress()+" "+socket.getLocalPort());

            Thread inThread = new Thread(new Receiver());
            Thread outThread = new Thread(new Sender());

            inThread.start();
            outThread.start();
            joinChat();

            Scanner scanner = new Scanner(System.in);
            while (!Thread.interrupted()) {
                String message = scanner.nextLine();
                try {
                    sendEveryoneExceptSender(new DataMsg(UUID.randomUUID(), name, message, myInetSocketAddress));
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public CopyOnWriteArrayList<InetSocketAddress> getNeighbourAddresses() {
        return neighbourAddresses;
    }

    public InetSocketAddress giveChildSubstituteInetSocketAddress() {

        if(!isRoot || (!neighbourAddresses.isEmpty() ) ){
            return neighbourAddresses.get(0);
        }
        else{
            return myInetSocketAddress;
        }
    }
    public ArrayBlockingQueue<MsgShell> getNeedToSendMsgs() {
        return needToSendMsgs;
    }

    public String getName() {
        return name;
    }

    public InetSocketAddress getMyInetSocketAddress() {
        return myInetSocketAddress;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public InetSocketAddress getParentInetSocketAddress() {
        return parentInetSocketAddress;
    }

    public void setSubstituteInetSocketAddress(InetSocketAddress substituteInetSocketAddress) {
        this.substituteInetSocketAddress = substituteInetSocketAddress;
        if(!myInetSocketAddress.equals(substituteInetSocketAddress)){
        this.sub=true;}
    }

    public void setParentInetSocketAddress(InetSocketAddress parentInetSocketAddress) {
        this.parentInetSocketAddress = parentInetSocketAddress;
    }

    public CopyOnWriteArrayList<UUID> getAlreadySentMsgs() {
        return alreadySentMsgs;
    }

    private void joinChat() throws IOException, InterruptedException {
        if (!isRoot) {
            neighbourAddresses.add(parentInetSocketAddress);
            sendToSender(new JoinMsg(myInetSocketAddress),parentInetSocketAddress);
        }
        System.out.println(name + " joined chat");
    }

    private void sendEveryoneExceptSender(Msg msg) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(msg);
        for (InetSocketAddress inetSocketAddress : getNeighbourAddresses()) {
            if (isNotAuthor(inetSocketAddress, msg)) {
                needToSendMsgs.put(new MsgShell(msg.getUUID(), new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress)));
            }
        }
        alreadySentMsgs.add(msg.getUUID());
        addToHistory(msg);
    }

    public void sendEveryoneExceptSender(Msg msg, InetSocketAddress previousAuthor) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(msg);
        for (InetSocketAddress inetSocketAddress : getNeighbourAddresses()) {
            if ((isNotAuthor(inetSocketAddress, msg)) && (!inetSocketAddress.equals(previousAuthor))) {
                needToSendMsgs.put(new MsgShell(msg.getUUID(), new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress)));

            }
        }
        alreadySentMsgs.add(msg.getUUID());
        addToHistory(msg);

    }

    public void sendToSender(Msg msg, InetSocketAddress receiver) throws IOException, InterruptedException {
        byte[] messageBytes = Serializer.serialize(msg);
        //System.out.println("(SendToSender) Receiver is " + receiver);
        MsgShell msgShell = new MsgShell(msg.getUUID(), new DatagramPacket(messageBytes, messageBytes.length, receiver));
        if (msg.getClass().getSimpleName().equals("AckMsg") || msg.getClass().getSimpleName().equals("JoinAckMsg")) {
            msgShell.setAck(true);
        }
        else{
            addToHistory(msg);
        }
        needToSendMsgs.put(msgShell);
        alreadySentMsgs.add(msg.getUUID());
    }

    private boolean isNotAuthor(InetSocketAddress inetSocketAddress, Msg msg) {
        return (!msg.getSenderInetSocketAddress().equals(inetSocketAddress));
    }

    private void addToHistory(Msg msg) {
        if (!msgHistory.contains(msg)) {
            msgHistory.add(msg);
        }
    }

    class Sender implements Runnable {


        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    MsgShell msgShell = needToSendMsgs.take();
                    //System.out.println("NeedSend " + msgShell.getDatagramPacket().getData()+" att="+msgShell.getATTEMPTS());
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - msgShell.getLastAttempt() > MsgShell.TIME_OUT ) {

                        DatagramPacket datagramPacket = msgShell.getDatagramPacket();
                        InetSocketAddress s = new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort());
                        //System.out.println(msgShell.getATTEMPTS());

                        if( msgShell.getATTEMPTS()>0){
                            System.out.println(msgShell.getATTEMPTS());
                            socket.send(datagramPacket);
                            msgShell.setLastAttempt(currentTime);
                            msgShell.setATTEMPTS(msgShell.getATTEMPTS()-1);
                            if (!msgShell.isAck()) {
                                needToSendMsgs.put(msgShell);
                            }
                        }
                        else{
                            System.out.println("Removed: "+s);
                            neighbourAddresses.remove(s);
                            if(sub && s.equals(parentInetSocketAddress)) {
                                System.out.println("Need redo: sb = "+substituteInetSocketAddress);
                                parentInetSocketAddress = substituteInetSocketAddress;
                                neighbourAddresses.add(substituteInetSocketAddress);
                                sendToSender(new JoinMsg(myInetSocketAddress), parentInetSocketAddress);
                                sendEveryoneExceptSender(new JoinAckMsg(UUID.randomUUID(), myInetSocketAddress, parentInetSocketAddress));
                            }

                        }
                    } else {
                        needToSendMsgs.put(msgShell);
                        Thread.sleep(currentTime - msgShell.getLastAttempt());
                    }

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                }
            }
        }
    }
    class Receiver implements Runnable {


        @Override
        public void run() {
            byte[] buffer = new byte[BUF_SIZE];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, BUF_SIZE);
            while (!Thread.interrupted()) {
                try {
                    socket.receive(datagramPacket);
                    //System.out.println("Got msg from "+datagramPacket.getSocketAddress());
                    if ( ThreadLocalRandom.current().nextInt(0, 100) >= percentageLoss ) {
                        Msg msg = Serializer.deserialize(datagramPacket.getData());
                        if (!msgHistory.contains(msg)) {

                            InetSocketAddress s = new InetSocketAddress(datagramPacket.getAddress(), datagramPacket.getPort());
                            if( !neighbourAddresses.contains(s) && !msg.getClass().getSimpleName().equals("JoinMsg")){

                                continue;
                            }
                            //System.out.println("Received " + msg);
                            msg.process(TreeNode.this);
                        }
                       // System.out.println("Have in History " + msg);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                }
            }
        }
    }
}