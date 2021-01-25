package EndPoint;

public class ChannelShall {
    private final BothConnectionSides bothConnectionSides;
    private final SocketChannelSide socketChannelSide;

    public ChannelShall(BothConnectionSides bothConnectionSides,  ChannelShall.SocketChannelSide socketChannelSide) {
        this.bothConnectionSides = bothConnectionSides;
        this.socketChannelSide = socketChannelSide;
    }

    public  BothConnectionSides getBothConnectionSides() {
        return bothConnectionSides;
    }

    public  ChannelShall.SocketChannelSide getSocketChannelSide() {
        return socketChannelSide;
    }

    public enum SocketChannelSide {
        CLIENT,
        DESTINATION;
    }
}