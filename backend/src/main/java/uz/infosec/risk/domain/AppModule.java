package uz.infosec.risk.domain;

/**
 * The seven permission-controlled areas of the application.
 *
 * <p>Named {@code AppModule} and not {@code Module} on purpose: {@code java.lang.Module}
 * exists and is auto-imported into every Java file, so a class called {@code Module}
 * compiles but silently shadows a JDK type - exactly the sort of ambiguity that
 * produces baffling errors later.
 *
 * <p>The names here must match the CHECK constraint in V2__permissions.sql.
 * The enum is the single source of truth; the CHECK constraint is the database
 * refusing to store anything the enum does not know about.
 */
public enum AppModule {
    ASSETS,
    THREATS,
    RISKS,
    CONTROLS,
    RISK_CONTROLS,
    DICTIONARIES,
    INFO_SYSTEMS
}
