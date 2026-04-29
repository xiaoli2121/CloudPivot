package io.cloudpivot.auth.security;

import java.util.HashSet;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import io.cloudpivot.auth.service.AuthService;

public class JwtBearerRealm extends AuthorizingRealm {

    private final AuthService authService;

    public JwtBearerRealm(AuthService authService) {
        this.authService = authService;
        setCredentialsMatcher(new SimpleCredentialsMatcher());
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = String.valueOf(token.getCredentials());
        UserPrincipal principal = authService.findByAccessToken(accessToken)
                .orElseThrow(() -> new AuthenticationException("Invalid bearer token."));
        return new SimpleAuthenticationInfo(principal, accessToken, getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        UserPrincipal principal = (UserPrincipal) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(new HashSet<>(principal.roles()));
        info.setStringPermissions(new HashSet<>(principal.permissions()));
        return info;
    }
}
