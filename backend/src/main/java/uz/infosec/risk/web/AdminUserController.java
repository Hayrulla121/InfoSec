package uz.infosec.risk.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.infosec.risk.service.UserService;
import uz.infosec.risk.web.dto.AuthDtos.ModulePermissionDto;
import uz.infosec.risk.web.dto.AuthDtos.UserDto;
import uz.infosec.risk.web.dto.UserAdminDtos.CreateUserRequest;
import uz.infosec.risk.web.dto.UserAdminDtos.UpdatePermissionsRequest;
import uz.infosec.risk.web.dto.UserAdminDtos.UpdateUserRequest;

import java.util.List;

/**
 * User administration. Guarded by role, not by the module permission grid:
 * managing users is an ADMIN-only capability that cannot be delegated.
 *
 * <p>@PreAuthorize on the class applies to every method. It is enabled by
 * @EnableMethodSecurity in MethodSecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.findById(id);
    }

    /** 201 Created is the correct status for a POST that creates a resource. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @GetMapping("/{id}/permissions")
    public List<ModulePermissionDto> getPermissions(@PathVariable Long id) {
        return userService.getPermissions(id);
    }

    @PutMapping("/{id}/permissions")
    public List<ModulePermissionDto> updatePermissions(@PathVariable Long id,
                                                       @Valid @RequestBody UpdatePermissionsRequest request) {
        return userService.replacePermissions(id, request.permissions());
    }
}
