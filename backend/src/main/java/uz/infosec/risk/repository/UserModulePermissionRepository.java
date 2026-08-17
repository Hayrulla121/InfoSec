package uz.infosec.risk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.infosec.risk.domain.UserModulePermission;

import java.util.List;

public interface UserModulePermissionRepository extends JpaRepository<UserModulePermission, Long> {

    /**
     * All grants for one user. We always load the whole set at once rather than
     * querying per module, because a request may check several modules and a
     * user has at most seven rows.
     */
    List<UserModulePermission> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
