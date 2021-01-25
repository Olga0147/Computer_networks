package Msg;

import java.util.Set;

public class Hello {
    private final byte socksVersion;
    private final Set<AuthenticationMethodCode> authenticationMethodCodes;

    public Hello(byte socksVersion, Set<AuthenticationMethodCode> authenticationMethodCodes) {
        this.socksVersion = socksVersion;
        this.authenticationMethodCodes = authenticationMethodCodes;
    }

    public byte getSocksVersion() {
        return socksVersion;
    }

    public boolean hasAuthMethod(AuthenticationMethodCode method) {
        return authenticationMethodCodes.contains(method);
    }
}