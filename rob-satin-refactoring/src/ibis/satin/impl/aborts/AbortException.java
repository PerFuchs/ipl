/* $Id$ */

package ibis.satin.impl.aborts;

/**
 * Exception that gets thrown when an invocation is aborted.
 */
public final class AbortException extends RuntimeException {

    /**
     * Constructs an <code>AbortException</code>.
     */
    public AbortException() {
        super();
    }

    /**
     * Overrides the <code>fillInStackTrace</code> from <code>Throwable</code>.
     * This version does not actually create a stack trace, which are useless
     * in this case.
     */
    public Throwable fillInStackTrace() {
        return this;
    }
}
