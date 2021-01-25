package Msg;

public class HelloAnswer {
    private final byte socksVersion;
    private final AuthenticationMethodCode chosenAuthenticationMethodCode;

    public HelloAnswer(byte socksVersion, AuthenticationMethodCode chosenAuthenticationMethodCode) {
        this.socksVersion = socksVersion;
        this.chosenAuthenticationMethodCode = chosenAuthenticationMethodCode;
    }

    public byte[] toByteArray() {
        byte[] byteArray = new byte[2];
        byteArray[0] = socksVersion;
        byteArray[1] = chosenAuthenticationMethodCode.getValue();
        return byteArray;
    }
}