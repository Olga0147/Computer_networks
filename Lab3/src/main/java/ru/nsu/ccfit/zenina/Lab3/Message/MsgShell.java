package ru.nsu.ccfit.zenina.Lab3.Message;

import java.net.DatagramPacket;
import java.util.UUID;

public class MsgShell {
    public static final long TIME_OUT = 500;
    private int ATTEMPTS;
    private UUID uuid;
    private DatagramPacket datagramPacket;
    private long lastAttempt;
    private boolean isAck;
    private boolean isSent;

    MsgShell(UUID uuid) {
        this.uuid = uuid;
        this.lastAttempt = System.currentTimeMillis();
        this.isAck = false;
        this.isSent = false;
        this.ATTEMPTS = 10;
    }

    public MsgShell(UUID uuid, DatagramPacket datagramPacket) {
        this.uuid = uuid;
        this.datagramPacket = datagramPacket;
        this.lastAttempt = System.currentTimeMillis();
        this.isAck = false;
        this.isSent = false;
        this.ATTEMPTS = 10;
    }

    public UUID getUuid() {
        return uuid;
    }

    public DatagramPacket getDatagramPacket() {
        return datagramPacket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MsgShell that = (MsgShell) o;

        return that.uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (datagramPacket != null ? datagramPacket.hashCode() : 0);
        return result;
    }

    public long getLastAttempt() {
        return lastAttempt;
    }

    public void setLastAttempt(long lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    public boolean isAck() {
        return isAck;
    }

    public void setAck(boolean ack) {
        isAck = ack;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public int getATTEMPTS() {
        return ATTEMPTS;
    }

    public void setATTEMPTS(int ATTEMPTS) {
        this.ATTEMPTS = ATTEMPTS;
    }
}