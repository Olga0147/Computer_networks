package ru.nsu.ccfit.zenina.Lab3.Message;

import ru.nsu.ccfit.zenina.Lab3.TreeNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class JoinMsg extends Msg {
    public JoinMsg() {
        this.uuid = UUID.randomUUID();
    }

    public JoinMsg(InetSocketAddress inetSocketAddress) {
        super(inetSocketAddress);
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void process(TreeNode treeNode) throws IOException, InterruptedException {

        System.out.println("Received JoinMessage from " + senderInetSocketAddress);
        if (!treeNode.getNeighbourAddresses().contains(senderInetSocketAddress)) {
            treeNode.getNeighbourAddresses().add(senderInetSocketAddress);
            System.out.println("Added new child to " + treeNode.getName());
        }

        InetSocketAddress s = treeNode.giveChildSubstituteInetSocketAddress();
        JoinAckMsg joinAckMessage = new JoinAckMsg(uuid, treeNode.getMyInetSocketAddress(),s);
        try {
            treeNode.sendToSender(joinAckMessage, senderInetSocketAddress);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

}