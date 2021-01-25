package ru.nsu.ccfit.zenina.Lab3.Message;

import ru.nsu.ccfit.zenina.Lab3.TreeNode;

import java.net.InetSocketAddress;
import java.util.UUID;

public class AckMsg extends Msg {
    public AckMsg() {}

    AckMsg(UUID uuid, InetSocketAddress inetSocketAddress) {
        this.uuid = uuid;
        this.senderInetSocketAddress = inetSocketAddress;
    }

    @Override

    public void process(TreeNode treeNode) {
        System.out.println("Received AckMessage");
        treeNode.getAlreadySentMsgs().remove(uuid);
        treeNode.getNeedToSendMsgs().remove(new MsgShell(uuid));
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