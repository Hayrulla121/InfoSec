package uz.infosec.risk.error;

/**
 * Thrown when a request is valid in isolation but clashes with existing data
 * (duplicate username, duplicate asset+threat pair). Mapped to 409.
 */
public class ConflictException extends RuntimeException implements LocalizedException {

    private final String messageCode;
    private final transient Object[] messageArgs;

    /** Literal message; used where no translation exists yet. */
    public ConflictException(String message) {
        super(message);
        this.messageCode = null;
        this.messageArgs = new Object[0];
    }

    private ConflictException(String code, Object[] args) {
        // The code doubles as the exception message, so logs stay greppable.
        super(code);
        this.messageCode = code;
        this.messageArgs = args;
    }

    /**
     * Preferred form: a message key resolved against the caller's locale.
     *
     * @param code key in messages.properties
     * @param args values for the {0}, {1}… placeholders
     */
    public static ConflictException of(String code, Object... args) {
        return new ConflictException(code, args);
    }

    @Override
    public String getMessageCode() {
        return messageCode;
    }

    @Override
    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
