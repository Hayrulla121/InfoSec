package uz.infosec.risk.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.repository.UserRepository;

/**
 * Spring Security's hook for "given a username, fetch the account".
 * Used by DaoAuthenticationProvider during login and by our JWT filter on
 * every authenticated request.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(AppUserDetails::new)
                // Deliberately vague message: telling an attacker that a
                // username exists but the password was wrong is an account
                // enumeration leak. Spring turns this into a generic
                // "Bad credentials" response anyway.
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
