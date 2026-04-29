package io.cloudpivot.auth.api;

import org.apache.shiro.authc.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.cloudpivot.auth.service.UnauthorizedException;
import io.cloudpivot.common.api.ApiResponse;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("UNAUTHORIZED", exception.getMessage(), null));
    }
}
