package uz.infosec.risk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.infosec.risk.domain.User;

import java.util.Optional;

/**
 * You write the interface; Spring Data writes the implementation at startup.
 *
 * <p>JpaRepository&lt;User, Long&gt; already provides save, findById, findAll,
 * delete, count, paging and sorting. The methods below are <i>derived queries</i>:
 * Spring parses the method NAME ("findByUsername") and generates
 * "select u from User u where u.username = ?1". Misspell the property and the
 * application fails to start - a compile-time-ish safety net for queries.
 *
 * <p>Optional&lt;T&gt; as the return type forces the caller to handle "no such
 * user" explicitly instead of dereferencing a null.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
