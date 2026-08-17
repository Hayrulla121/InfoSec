package uz.infosec.risk.domain;

/**
 * Coarse-grained role. ADMIN bypasses the per-module permission table entirely;
 * USER is governed by it.
 *
 * <p>Must match CHECK (role IN ('ADMIN','USER')) in V1__users.sql.
 */
public enum Role {
    ADMIN,
    USER
}
