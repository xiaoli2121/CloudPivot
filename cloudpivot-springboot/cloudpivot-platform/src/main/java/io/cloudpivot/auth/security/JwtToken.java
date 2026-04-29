package io.cloudpivot.auth.security;

import org.apache.shiro.authc.BearerToken;

public class JwtToken extends BearerToken {

    public JwtToken(String token, String host) {
        super(token, host);
    }
}
