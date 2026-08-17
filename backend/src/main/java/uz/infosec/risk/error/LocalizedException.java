package uz.infosec.risk.error;

/**
 * Marker for exceptions whose message is a resource-bundle key rather than
 * finished text.
 *
 * <p>Business rules are explained to the user in their own language, so the
 * service layer must not build sentences. It raises a code and the arguments
 * that fill it; {@link GlobalExceptionHandler} resolves both against the
 * request's locale at the very edge of the application.
 *
 * <p>{@code getMessage()} still returns the code, which is what you want in a
 * stack trace: a log line reading {@code risk.duplicatePair} is greppable in a
 * way that a translated sentence is not.
 */
public interface LocalizedException {

    /** Key in messages.properties, e.g. {@code risk.duplicatePair}. */
    String getMessageCode();

    /** Values for the {0}, {1}… placeholders in that message. */
    Object[] getMessageArgs();
}
