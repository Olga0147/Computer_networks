package ru.nsu.ccfit.zenina.Lab3.Message;

import ru.nsu.ccfit.zenina.Lab3.TreeNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class DataMsg extends Msg {
    private String author;
    private String text;

    public DataMsg() {}

    public DataMsg(UUID uuid, String author, String text, InetSocketAddress inetSocketAddress) {
        this.uuid = uuid;
        this.author = author;
        this.text = text;
        this.senderInetSocketAddress = inetSocketAddress;
    }

    @Override
    public void process(TreeNode treeNode) throws IOException, InterruptedException {

        System.out.println("Received DataMessage from " + author + "pr at = "+ senderInetSocketAddress);
        System.out.println(author + " : " + text);
        InetSocketAddress previousAuthorAddress = senderInetSocketAddress;
        senderInetSocketAddress = treeNode.getMyInetSocketAddress();
        try {
          //send others
            treeNode.sendEveryoneExceptSender(this, previousAuthorAddress);
            treeNode.sendToSender(new AckMsg(uuid, treeNode.getMyInetSocketAddress()), previousAuthorAddress);

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