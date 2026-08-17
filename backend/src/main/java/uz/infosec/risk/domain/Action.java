package uz.infosec.risk.domain;

/**
 * The four operations a permission grant can allow, one per column in
 * user_module_permissions (can_create / can_read / can_update / can_delete).
 */
public enum Action {
    CREATE,
    READ,
    UPDATE,
    DELETE
}
