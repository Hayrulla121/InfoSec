package uz.infosec.risk.error;

/** Thrown by services when a requested entity does not exist. Mapped to 404. */
public class NotFoundException extends RuntimeException implements LocalizedException {

    private final String messageCode;
    private final transient Object[] messageArgs;

    public NotFoundException(String message) {
        super(message);
        this.messageCode = null;
        this.messageArgs = new Object[0];
    }

    private NotFoundException(String code, Object[] args) {
        super(code);
        this.messageCode = code;
        this.messageArgs = args;
    }

    public static NotFoundException code(String code, Object... args) {
        return new NotFoundException(code, args);
    }

    /**
     * "Asset 42 not found", localised.
     *
     * <p>The id is converted to a String on purpose. MessageFormat applies
     * locale-aware number formatting to numeric arguments, so a raw Long turns
     * id 999999 into "999 999" - a grouping separator inside an identifier the
     * user is about to search for.
     *
     * @param entityCode key naming the entity type, e.g. {@code entity.asset}
     */
    public static NotFoundException of(String entityCode, Object id) {
        return new NotFoundException("error.notFound",
                new Object[]{entityCode, String.valueOf(id)});
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
