package ru.nsu.ccfit.zenina.Lab3.Message;

import ru.nsu.ccfit.zenina.Lab3.TreeNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class JoinAckMsg extends Msg {

    private InetSocketAddress substitute;
    public JoinAckMsg() {
        this.uuid = UUID.randomUUID();
    }

    public JoinAckMsg(UUID uuid, InetSocketAddress inetSocketAddress,InetSocketAddress substitute) {
        this.uuid = uuid;
        this.senderInetSocketAddress = inetSocketAddress;
        this.substitute=substitute;
    }

    @Override
    public void process(TreeNode treeNode) throws IOException, InterruptedException {

        System.out.println("Received JoinAckMessage");

        treeNode.setSubstituteInetSocketAddress(substitute);
        if(! treeNode.getNeighbourAddresses().contains(senderInetSocketAddress)){//first time connection
            treeNode.getAlreadySentMsgs().remove(uuid);
        }
        treeNode.getNeedToSendMsgs().remove(new MsgShell(uuid));
        treeNode.sendToSender(new AckMsg(uuid, treeNode.getMyInetSocketAddress()), senderInetSocketAddress);
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