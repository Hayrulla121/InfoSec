package uz.infosec.risk;

/**
 * The seeded administrator's credentials, in one place.
 *
 * <p>These were written out as {@code login("admin", "admin")} at roughly
 * twenty call sites across nine test classes. That was survivable while the
 * password was the word "admin"; it stopped being survivable the moment the
 * password became a twenty-character random string, because changing it again
 * would mean another twenty-site sweep and any site missed would fail as an
 * unhelpful 401 rather than as an obvious compile error.
 *
 * <p>Now the password lives here and in {@code V6__admin_password.sql}, and
 * nowhere else. Rotating it is: generate a hash, edit the migration, edit the
 * constant below.
 */
public final class TestCredentials {

    private TestCredentials() {
    }

    public static final String ADMIN_USERNAME = "admin";

    /** Must match the hash seeded by V6__admin_password.sql. */
    public static final String ADMIN_PASSWORD = "p@MZ7!q7vfYMGH478^B#";
}
