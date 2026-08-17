package uz.infosec.risk.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uz.infosec.risk.domain.Role;
import uz.infosec.risk.domain.User;

import java.util.Collection;
import java.util.List;

/**
 * Adapter between our {@link User} entity and Spring Security's {@link UserDetails}
 * contract. Spring Security knows nothing about our tables; it only knows this
 * interface. Keeping the adapter separate stops security concerns from leaking
 * into the entity.
 */
@Getter
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;   // the BCrypt hash
    private final String fullName;
    private final Role role;
    private final boolean active;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.active = user.isActive();
    }

    /**
     * Spring Security's convention: role authorities are prefixed "ROLE_", so
     * hasRole("ADMIN") matches the authority "ROLE_ADMIN". Forgetting the
     * prefix is the classic reason hasRole() silently never matches.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returning false makes DaoAuthenticationProvider throw DisabledException
     * during login - this is what implements "deactivated users cannot log in",
     * without a single if-statement in our controller.
     */
    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
