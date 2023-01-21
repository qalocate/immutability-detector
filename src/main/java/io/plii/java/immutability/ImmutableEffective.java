package io.plii.java.immutability;

/**
 * A class extending this interface is responsible for enforcing deep immutability <em>following
 * both the instance's construction and the permanently locking of the instance as irreversibly
 * immutable</em>; i.e. effectively immutable.
 * <p>
 * Permanently locking the instance as irreversibly immutable may be done directly as a side effect
 * of an internal method call. It may also be implemented to do so externally via something similar
 * to a <code>setAsIrreversiblyImmutable()</code> method (not provided by this interface).
 * <p>
 * While allowing mutation following construction and prior to the
 * <code>isIrreversiblyImmutable()</code> method returning {@code true}, the implementation
 * must enforce a similar deep immutability contract as the
 * {@link io.plii.java.immutability.Immutable} interface.
 * <p>
 * Once the <code>isIrreversiblyImmutable()</code> method begins permanently returning
 * {@code true}, all methods directly or indirectly attempting to modify the state of the
 * given instance must implement throwing an {@link java.lang.UnsupportedOperationException}.
 * <p>
 * If the class/interface implementing the <code>isIrreversiblyImmutable()</code> method begins
 * permanently returning {@code true} prior to or at the conclusion of all the class's
 * constructors, strongly consider refactoring to use the
 * {@link io.plii.java.immutability.Immutable} instead.
 */
public interface ImmutableEffective {
  /**
   * Returns {@code true} when the instance become irreversibly immutable; a.k.a. permanently
   * read-only or calcified.
   * <p>
   * More formally, initially returns {@code false} after the instance completes execution of its
   * constructor and has become visible beyond the class's private scope. Once it begins returning
   * {@code true}, it remains permanently locked to continue returning {@code true}, making the
   * instance irreversibly immutable.
   * <p>
   * All implementing class methods directly or indirectly modifying the instance's observable
   * state must call this method as a precondition, and if the method returns {@code true}, the
   * implementing method must throw an {@link java.lang.UnsupportedOperationException}.
   * <p>
   * An implementation must consider issues around multithreading race conditions around correctly
   * setting the flag. A good pattern for implementing this is found in this
   * <a href="https://stackoverflow.com/a/61537561/501113">StackOverflow Answer</a>.
   *
   * @return {@code true} once the instance becomes irreversibly immutable, thus making
   * the instance permanently read-only or calcified; i.e. effectively immutable
   */
  boolean isIrreversiblyImmutable();
}
