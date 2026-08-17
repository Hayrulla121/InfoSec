package uz.infosec.risk.web;

import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.infosec.risk.security.AppUserDetails;
import uz.infosec.risk.security.JwtService;
import uz.infosec.risk.security.PermissionService;
import uz.infosec.risk.web.dto.AuthDtos.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PermissionService permissionService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          PermissionService permissionService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.permissionService = permissionService;
    }

    /**
     * The only public endpoint.
     *
     * <p>authenticate() runs the full Spring Security check: load the user,
     * BCrypt-compare the password, and reject disabled accounts
     * (DisabledException). It throws on failure - we never write our own
     * password comparison, because a naive {@code hash.equals(other)} is both
     * wrong (no BCrypt salt handling) and timing-attack prone.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();

        return new LoginResponse(
                jwtService.generateToken(principal),
                toDto(principal),
                permissionsOf(principal));
    }

    /**
     * Lets a reloaded browser tab restore its session from a stored token
     * without asking for the password again.
     *
     * <p>@AuthenticationPrincipal injects whatever our JWT filter put into the
     * SecurityContext for this request.
     */
    @GetMapping("/me")
    public LoginResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return new LoginResponse(null, toDto(principal), permissionsOf(principal));
    }

    private List<ModulePermissionDto> permissionsOf(AppUserDetails principal) {
        return permissionService.effectivePermissions(principal.getId(), principal.getRole()).stream()
                .map(p -> new ModulePermissionDto(p.module(), p.canCreate(), p.canRead(),
                        p.canUpdate(), p.canDelete()))
                .toList();
    }

    private UserDto toDto(AppUserDetails principal) {
        return new UserDto(principal.getId(), principal.getUsername(), principal.getFullName(),
                null, principal.getRole(), principal.isActive(), null);
    }
}
