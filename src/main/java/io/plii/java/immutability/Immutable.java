package io.plii.java.immutability;

/**
 * A class extending this interface is responsible for enforcing deep immutability prior
 * to the completion of each and every constructor.
 * <p>
 * This marker interface is analogous to the <code>@Immutable</code> annotation within the Open
 * Source package <a href="https://jcip.net/annotations/doc/index.html">net.jcip.annotations</a>
 * <p>
 * Paraphrased from said Open Source project...
 * <p>
 * The state from an instance of this class must not be observable by callers, which implies that
 * <ul>
 * <li> all {@code public} fields are {@code final}, </li>
 * <li> all {@code public final} reference fields refer to other immutable objects, and </li>
 * <li> constructors and methods do not publish references to any internal state
 *      which is potentially mutable by the implementation. </li>
 * </ul>
 * Immutable objects may still have internal mutable state for purposes of performance
 * optimization; some state variables may be lazily computed, so long as they are computed
 * from immutable state and that callers cannot tell the difference.
 * <p>
 * Immutable objects are inherently thread-safe; they may be passed between threads or
 * published without synchronization.
 */
public interface Immutable {}
