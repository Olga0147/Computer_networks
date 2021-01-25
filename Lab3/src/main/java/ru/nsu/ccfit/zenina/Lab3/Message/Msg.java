package ru.nsu.ccfit.zenina.Lab3.Message;

import ru.nsu.ccfit.zenina.Lab3.TreeNode;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;

public abstract class Msg implements Serializable {
    UUID uuid;
    InetSocketAddress senderInetSocketAddress;

    Msg() {}

    Msg(InetSocketAddress inetSocketAddress) {
        this.senderInetSocketAddress = inetSocketAddress;
    }

    public void process(TreeNode treeNode) throws IOException, InterruptedException {
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public InetSocketAddress getSenderInetSocketAddress() {
        return senderInetSocketAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Msg msg = (Msg) o;

        return msg.uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (senderInetSocketAddress != null ? senderInetSocketAddress.hashCode() : 0);
        return result;
    }
}