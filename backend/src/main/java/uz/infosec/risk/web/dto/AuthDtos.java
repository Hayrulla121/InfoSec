package uz.infosec.risk.web.dto;

import jakarta.validation.constraints.NotBlank;
import uz.infosec.risk.domain.AppModule;
import uz.infosec.risk.domain.Role;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for authentication.
 *
 * <p>Why DTOs at all, when we already have entities? Three reasons:
 * an entity would serialise its BCrypt hash straight to the client; entities
 * carry lazy associations that blow up during JSON rendering; and a change to
 * the database schema would silently change the public API contract.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username,
                               @NotBlank String password) {
    }

    public record LoginResponse(String token,
                                UserDto user,
                                List<ModulePermissionDto> permissions) {
    }

    /** Note the absence of any password field. That is the point. */
    public record UserDto(Long id,
                          String username,
                          String fullName,
                          String email,
                          Role role,
                          boolean active,
                          Instant createdAt) {
    }

    public record ModulePermissionDto(AppModule module,
                                      boolean canCreate,
                                      boolean canRead,
                                      boolean canUpdate,
                                      boolean canDelete) {
    }
}
