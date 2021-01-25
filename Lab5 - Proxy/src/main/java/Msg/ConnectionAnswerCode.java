package Msg;

public enum ConnectionAnswerCode {
    REQUEST_GRANTED((byte)0x00),
    HOST_UNREACHABLE((byte)0x04),
    CAD_NOT_SUPPORTED((byte)0x07),
    ADDR_TYPE_NOT_SUPPORTED((byte)0x08);

    private final byte value;

    ConnectionAnswerCode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
