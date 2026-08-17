package uz.infosec.risk.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.infosec.risk.domain.Role;

import java.util.List;

/** DTOs for the admin user-management screens. */
public final class UserAdminDtos {

    private UserAdminDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 255) String fullName,
            @Email @Size(max = 255) String email,
            @NotNull Role role) {
    }

    /**
     * Every field is optional: null means "leave unchanged". This is what makes
     * one endpoint serve rename, role change, activate/deactivate and password
     * reset without four near-identical routes.
     */
    public record UpdateUserRequest(
            @Size(max = 255) String fullName,
            @Email @Size(max = 255) String email,
            Role role,
            Boolean active,
            @Size(min = 6, max = 100) String newPassword) {
    }

    /** Bulk replace of one user's permission grid: 7 modules x 4 checkboxes. */
    public record UpdatePermissionsRequest(
            @NotNull List<AuthDtos.ModulePermissionDto> permissions) {
    }
}
