package io.cloudpivot.auth.security;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.BearerHttpAuthenticationFilter;

import jakarta.servlet.ServletRequest;

public class JwtAuthFilter extends BearerHttpAuthenticationFilter {

    @Override
    protected AuthenticationToken createBearerToken(String token, ServletRequest request) {
        return new JwtToken(token, request.getRemoteHost());
    }
}
