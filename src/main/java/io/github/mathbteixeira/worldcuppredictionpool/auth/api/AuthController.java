package io.github.mathbteixeira.worldcuppredictionpool.auth.api;

import io.github.mathbteixeira.worldcuppredictionpool.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, token issuance, and current-user lookup.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates a user account and returns a bearer token for the new user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered and token issued"),
            @ApiResponse(responseCode = "400", description = "Invalid registration request")
    })
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/token")
    @Operation(summary = "Issue token", description = "Authenticates an existing user and returns a bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued"),
            @ApiResponse(responseCode = "400", description = "Invalid authentication request"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return authService.authenticate(request);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user", description = "Returns the identity and role represented by the bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public TokenResponse me(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }
}
