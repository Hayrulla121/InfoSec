package uz.infosec.risk.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Locale;

/**
 * Turns exceptions into the {@link ApiError} contract, in the caller's language.
 *
 * <p>@RestControllerAdvice registers these handlers for every controller, so
 * no controller needs a try/catch. Without it, Spring's default error page
 * leaks stack traces and class names to the client.
 *
 * <p>This is the only place that turns a message code into a sentence. Services
 * raise codes; translation happens once, here, against the locale the request
 * asked for.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Args with this prefix are themselves message keys and get resolved first. */
    private static final List<String> NESTED_PREFIXES = List.of("entity.", "dict.", "permission.");

    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    /** Bean Validation failures on @Valid @RequestBody -> 400 with per-field detail. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                // Hibernate Validator has already localised these via the
                // MessageSource wired up in ValidationMessagesConfig.
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, msg("error.validationFailed"), fieldErrors));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, resolve(ex)));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, resolve(ex)));
    }

    /** A database UNIQUE/FK constraint we did not check first. Still a 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, msg("error.dataIntegrity")));
    }

    /**
     * Authenticated, but not allowed. Our own subclass names the missing grant;
     * a plain AccessDeniedException (from @PreAuthorize, say) stays generic.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        String message = ex instanceof LocalizedException localized
                ? resolve(localized)
                : msg("error.accessDenied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(403, message));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, msg("error.accountDisabled")));
    }

    /**
     * Bad username OR bad password, deliberately indistinguishable: telling the
     * caller which half was wrong lets an attacker enumerate valid usernames.
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, msg("error.badCredentials")));
    }

    /**
     * A URL that matches no controller. Spring reaches this point having already
     * failed to find a static file, so the exception is named after the resource
     * lookup rather than the routing - which is why it is easy to miss.
     *
     * <p>Without this handler it fell through to the catch-all below and became
     * a 500 with a full stack trace in the log. That is wrong twice over: the
     * client is told the server broke when in fact the client asked for
     * something that does not exist, and every scanner probing for /api/admin
     * or /wp-login.php writes a stack trace into the log, burying real faults.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleNoEndpoint(Exception ex) {
        // debug, not error: this is routine client behaviour, not a server fault.
        log.debug("No endpoint for request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, msg("error.endpointNotFound")));
    }

    /** Right path, wrong verb - e.g. POST to an endpoint that only accepts GET. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.debug("Method not supported: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of(405, msg("error.methodNotAllowed")));
    }

    /** Last resort. Log the detail server-side, tell the client nothing useful. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, msg("error.internal")));
    }

    // ------------------------------------------------------------ helpers

    /**
     * Resolves a localised exception, or falls back to its literal message when
     * it carries no code. A missing translation must never turn a 409 into a
     * 500, so the code itself is the last-resort default.
     */
    private String resolve(LocalizedException ex) {
        String code = ex.getMessageCode();
        if (code == null) {
            return ((RuntimeException) ex).getMessage();
        }
        Locale locale = LocaleContextHolder.getLocale();
        Object[] args = ex.getMessageArgs();

        // An argument may itself be a key - "entity.asset" becomes "Актив"
        // before it is substituted into "{0} {1} не найден".
        Object[] resolved = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            resolved[i] = (args[i] instanceof String s && isNestedKey(s))
                    ? messages.getMessage(s, null, s, locale)
                    : args[i];
        }
        return messages.getMessage(code, resolved, code, locale);
    }

    private boolean isNestedKey(String value) {
        return NESTED_PREFIXES.stream().anyMatch(value::startsWith);
    }

    private String msg(String code) {
        return messages.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }
}
