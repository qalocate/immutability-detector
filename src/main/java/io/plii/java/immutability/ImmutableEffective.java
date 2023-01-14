package io.plii.java.immutability;

/**
 * A class extending this interface is responsible for enforcing deep immutability <em>following
 * both the instance's construction and the permanently locking of the instance as irreversibly
 * immutable</em>; i.e. effectively immutable.
 * <p>
 * Permanently locking the instance as irreversibly immutable may be done directly and externally
 * via a <code>setAsIrreversiblyImmutable()</code> method, indirectly and internally via some other
 * instance method's implementation, or both.
 * <p>
 * While allowing mutation following construction and prior to the
 * <code>setAsIrreversiblyImmutable()</code> method returning <code>true</code>, the implementation
 * must enforce the same deep immutability contract as the
 * {@link io.plii.java.immutability.Immutable} interface.
 * <p>
 * Once the <code>setAsIrreversiblyImmutable()</code> method begins permanently returning
 * <code>true</code>, all methods directly or indirectly attempting to modify the state of the
 * given instance must begin throwing an {@link java.lang.UnsupportedOperationException}.
 * <p>
 * If the <code>setAsIrreversiblyImmutable()</code> method begins permanently returning
 * <code>true</code> prior to or at the conclusion of all the class's constructors,
 * strongly consider using the {@link io.plii.java.immutability.Immutable} instead.
 */
public interface ImmutableEffective {
  //TODO: integrate into Javadoc above and below, and then delete
  //By allowing post instantiation mutation, it enables a class to have instances that remain mutable for their lifetime, and others to be "effective" immutable by conditionally setting the flag
  //Great StackOverflow Answer detailing the pattern: https://stackoverflow.com/a/14148585/501113
  //Must consider multi-thread race around setting the state prior to, and including, setting the flag: https://stackoverflow.com/a/61537561/501113

  /**
   * Returns <tt>true</tt> when the instance become irreversibly immutable; a.k.a. permanently
   * read-only or calcified.
   * <p>
   * More formally, initially returns <tt>false</tt> after the instance completes execution of its
   * constructor and has become visible beyond the class's private scope. Once it begins returning
   * <tt>true</tt>, it remains permanently locked to continue returning <tt>true</tt>, making the
   * instance irreversibly immutable.
   * <p>
   * All implementing class methods directly or indirectly modifying the instance's observable
   * state must call this method as a precondition, and if the method returns <tt>true</tt>, the
   * implementing method must throw an {@link java.lang.UnsupportedOperationException}.
   *
   * @return Returns <tt>true</tt> once the instance becomes irreversibly immutable, thus making
   * the instance permanently read-only or calcified; i.e. effectively immutable.
   */
  boolean isIrreversiblyImmutable();
}
