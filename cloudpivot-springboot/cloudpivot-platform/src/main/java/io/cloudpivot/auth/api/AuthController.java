package io.cloudpivot.auth.api;

import org.apache.shiro.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cloudpivot.auth.api.dto.ChangePasswordRequest;
import io.cloudpivot.auth.api.dto.CurrentUserResponse;
import io.cloudpivot.auth.api.dto.LoginRequest;
import io.cloudpivot.auth.api.dto.LoginResponse;
import io.cloudpivot.auth.api.dto.RefreshTokenRequest;
import io.cloudpivot.auth.api.dto.TokenRefreshResponse;
import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.auth.service.AuthService;
import io.cloudpivot.auth.service.UnauthorizedException;
import io.cloudpivot.common.api.ApiResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(extractBearerToken(authorization));
        return ApiResponse.success(null);
    }

    @GetMapping("/current-user")
    public ApiResponse<CurrentUserResponse> currentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityUtils.getSubject().getPrincipal();
        return ApiResponse.success(authService.currentUser(principal));
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody ChangePasswordRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityUtils.getSubject().getPrincipal();
        authService.changePassword(principal.userId(), request, extractBearerToken(authorization));
        return ApiResponse.success(null);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization header is invalid.");
        }
        return authorization.substring("Bearer ".length());
    }
}
