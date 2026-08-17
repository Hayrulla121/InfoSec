package uz.infosec.risk.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.*;
import uz.infosec.risk.error.ConflictException;
import uz.infosec.risk.error.NotFoundException;
import uz.infosec.risk.repository.UserModulePermissionRepository;
import uz.infosec.risk.repository.UserRepository;
import uz.infosec.risk.security.PermissionService;
import uz.infosec.risk.web.dto.AuthDtos.ModulePermissionDto;
import uz.infosec.risk.web.dto.AuthDtos.UserDto;
import uz.infosec.risk.web.dto.UserAdminDtos.CreateUserRequest;
import uz.infosec.risk.web.dto.UserAdminDtos.UpdateUserRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * User administration. All business rules live here; the controller only maps
 * HTTP to method calls.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserModulePermissionRepository permissionRepository;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserModulePermissionRepository permissionRepository,
                       PermissionService permissionService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(UserService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return userRepository.findById(id).map(UserService::toDto)
                .orElseThrow(() -> NotFoundException.of("entity.user", id));
    }

    /**
     * Creates the account and its default grants: READ on every module, no
     * write access anywhere - the "default deny" posture from the spec.
     */
    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw ConflictException.of("user.usernameTaken", request.username());
        }
        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmail(request.email())) {
            throw ConflictException.of("user.emailTaken", request.email());
        }

        User user = new User();
        user.setUsername(request.username());
        // The plaintext password exists only in this local variable and is
        // never stored, logged, or returned.
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email() == null || request.email().isBlank() ? null : request.email());
        user.setRole(request.role());
        user.setActive(true);
        User saved = userRepository.save(user);

        if (saved.getRole() == Role.USER) {
            List<UserModulePermission> defaults = new ArrayList<>();
            for (AppModule module : AppModule.values()) {
                UserModulePermission p = new UserModulePermission();
                p.setUser(saved);
                p.setModule(module);
                p.setCanRead(true);
                p.setCanCreate(false);
                p.setCanUpdate(false);
                p.setCanDelete(false);
                defaults.add(p);
            }
            permissionRepository.saveAll(defaults);
        }

        return toDto(saved);
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.user", id));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            String email = request.email().isBlank() ? null : request.email();
            if (email != null && !email.equals(user.getEmail())
                    && userRepository.existsByEmail(email)) {
                throw ConflictException.of("user.emailTaken", email);
            }
            user.setEmail(email);
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            // Guard rail: locking yourself out of the only admin account is
            // unrecoverable without database access.
            if (!request.active() && user.getRole() == Role.ADMIN && countActiveAdmins() <= 1) {
                throw ConflictException.of("user.lastAdmin");
            }
            user.setActive(request.active());
        }
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        // No explicit save() call needed: `user` is a managed entity inside a
        // transaction, so Hibernate detects the changes and flushes them on
        // commit. This is "dirty checking".
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public List<ModulePermissionDto> getPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("entity.user", userId));
        return permissionService.effectivePermissions(user.getId(), user.getRole()).stream()
                .map(p -> new ModulePermissionDto(p.module(), p.canCreate(), p.canRead(),
                        p.canUpdate(), p.canDelete()))
                .toList();
    }

    /** Bulk replace of the permission grid for one user. */
    @Transactional
    public List<ModulePermissionDto> replacePermissions(Long userId, List<ModulePermissionDto> requested) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("entity.user", userId));

        if (user.getRole() == Role.ADMIN) {
            throw ConflictException.of("user.adminGridNotEditable");
        }

        Map<AppModule, UserModulePermission> existing = permissionRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserModulePermission::getModule, Function.identity()));

        List<UserModulePermission> toSave = new ArrayList<>();
        for (ModulePermissionDto dto : requested) {
            // Update the row if present, otherwise create it. Avoids the
            // delete-all-then-insert pattern, which churns ids and breaks
            // any future foreign key onto this table.
            UserModulePermission p = existing.get(dto.module());
            if (p == null) {
                p = new UserModulePermission();
                p.setUser(user);
                p.setModule(dto.module());
            }
            p.setCanCreate(dto.canCreate());
            p.setCanRead(dto.canRead());
            p.setCanUpdate(dto.canUpdate());
            p.setCanDelete(dto.canDelete());
            toSave.add(p);
        }
        permissionRepository.saveAll(toSave);

        return getPermissions(userId);
    }

    private long countActiveAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isActive())
                .count();
    }

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(),
                user.getEmail(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
