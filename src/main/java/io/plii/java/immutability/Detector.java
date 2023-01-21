package io.plii.java.immutability;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

//import net.jcip.annotations.Immutable;
import org.jetbrains.annotations.NotNull;

/**
 * A lightweight white-list based recursive Java class and instance immutability detector.
 * <p>
 * Designed for use at runtime as part of a pre-condition verifier strategy to promote deep
 * immutability, it might be used to permit/deny the adding of an item into a deeply
 * immutable collection.
 * <p>
 * When the class hasn't been already {@link Detector#registered}, to remain lightweight, the
 * recursive detection strategy does not deep dive into Java reflection class implementation
 * patterns. For example, the
 * <a href="https://github.com/MutabilityDetector/MutabilityDetector">MutabilityDetector</a>
 * Java library implements detecting if all the fields in a class have been marked
 * <code>final</code>.
 * Instead, this enables a minimum overhead runtime checking of structures and collections to
 * validate the degree to which they are (thread) safely immutable.
 * <p>
 * By providing a registration ability for a class to be associated with its
 * {@link ImmutabilityType}, the association is found in O(1) time. Thus, bypassing the
 * slower, and still as lightweight as possible, recursive immutability detector.
 * <p>
 * Class libraries can pre-register their classes to bypass the slower, deeper, and recursive
 * detector algorithm. Another useful strategy might be to run a class through the detector, and then register it's
 * <p>
 * All ImmutabilityTypes are allowed excluding {@link ImmutabilityType#JAVA_CONSTANT}. However, the
 * registration is UNVERIFIED. IOW, the {@link ImmutabilityType} returned entirely depends upon the
 * clients' trustworthiness.
 * <p>
 * Registration allows {@link ImmutabilityType#UNDEFINED} to be submitted to efficiently short
 * circuit all the other checks for known highly utilized mutable classes.
 * <p>
 * A simplified/reduce version of this class+interface(x2) pattern has been posted as this
 * <a href="https://stackoverflow.com/a/75043881/501113">StackOverflow Answer</a>.
 */
public final class Detector {
  /**
   * Various types of deep-immutability detected based on the general notion of the {@link io.plii.java.immutability.Immutable Immutable} contract.
   * <p>
   * {@link #UNDEFINED}
   * <p>
   * {@link #PROPERTY_IRREVERSIBLY_IMMUTABLE}
   * <p>
   * {@link #BEFORE_END_OF_CLASS_CONSTRUCTOR}
   * <p>
   * {@link #JAVA_CONSTANT}
   */
  public enum ImmutabilityType {
    /**
     * Class/Instance owner not trusted to have enforced of any level of immutability
     */
    UNDEFINED,

    /**
     * Class/Instance owner trusted to have properly enforced {@link io.plii.java.immutability.ImmutableEffective ImmutableEffective} contract
     */
    PROPERTY_IRREVERSIBLY_IMMUTABLE,

    /**
     * Class/Instance owner trusted to have properly enforced {@link io.plii.java.immutability.Immutable Immutable} contract
     */
    BEFORE_END_OF_CLASS_CONSTRUCTOR,

    /**
     * Java platform and compiler trusted to have properly enforced the equivalent of the {@link io.plii.java.immutability.Immutable Immutable} contract
     */
    JAVA_CONSTANT
  }

  private record Tuple2SR(ImmutabilityType immutabilityType, boolean isRestricted) {}

  private static final Map<Class<?>, Tuple2SR> REGISTERED_TUPLE2SR_BY_CLASS = new ConcurrentHashMap<>();

  //Injecting concrete members of JAVA_PLATFORM_CLASSES
  static {
    Set.of(
      //Meta-classes
      Class.class, MethodType.class,
      //primitive class wrappers
      Boolean.class, Byte.class, Short.class, Character.class, Integer.class, Long.class, Float.class, Double.class,
      //classes
      String.class, BigInteger.class, BigDecimal.class
      //TODO: insert more well known instances in base module
    ).forEach(class___ -> REGISTERED_TUPLE2SR_BY_CLASS.put(class___, new Tuple2SR(ImmutabilityType.JAVA_CONSTANT, true)));
  }

  /**
   * Enables verifying a class has been registered.
   *
   * @param class___ class to check
   * @return an {@link Optional} describing the associated {@link ImmutabilityType}
   */
  @NotNull
  public static Optional<ImmutabilityType> registered(@NotNull Class<?> class___)  {
    //TODO: Write test cases
    return Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
        .map(Tuple2SR::immutabilityType);
  }

  /**
   * Provides the full Set of the associations between a given class and its associated
   * {@link ImmutabilityType}.
   *
   * @return a Set of the class to {@link ImmutabilityType} associations
   */
  @NotNull
  public static Set<Map.Entry<Class<?>, ImmutabilityType>> registeredAll() {
    //TODO: Write test cases
    return REGISTERED_TUPLE2SR_BY_CLASS
      .entrySet()
      .stream()
      //due to loss of type inference with .map(), prefixing a type witness as in .<...>map();
      //  details here: https://stackoverflow.com/a/73281848/501113
      .<Map.Entry<Class<?>, ImmutabilityType>> map(mapEntry -> Map.entry(mapEntry.getKey(), mapEntry.getValue().immutabilityType))
      .collect(Collectors.toUnmodifiableSet());
  }

  private static boolean isRegistrable(Class<?> class___) {
    return Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
      .filter(Tuple2SR::isRestricted)
      .isEmpty();
  }

  /**
   * Ensures the class is registered with its automatically detected {@link ImmutabilityType}.
   *
   * @param class___ class whose registration is to be ensured
   * @return {@code true} if the class was registered with the automatically detected
   *         {@link ImmutabilityType}
   */
  public static boolean registerAutoDetect(@NotNull Class<?> class___) {
    //TODO: Write test cases
    if (isRegistrable(class___)) {
      var immutabilityTypeDetected = internalClassIs(class___);
      REGISTERED_TUPLE2SR_BY_CLASS.put(class___, new Tuple2SR(immutabilityTypeDetected, false));
      return true;
    }

    return false;
  }

  /**
   * Ensures the class is registered with its provided {@link ImmutabilityType}, overriding any prior
   * registration.
   *
   * @param class___ class whose registration is to be ensured
   * @param immutabilityType {@link ImmutabilityType} to be associated with the class, overriding any
   *                         prior registration association
   * @return {@code true} if the class was registered with the provided {@link ImmutabilityType}
   */
  public static boolean registerOrOverride(@NotNull Class<?> class___, @NotNull Detector.ImmutabilityType immutabilityType) {
    //TODO: Write test cases
    if (isRegistrable(class___)) {
      REGISTERED_TUPLE2SR_BY_CLASS.put(class___, new Tuple2SR(immutabilityType, false));
      return true;
    }

    return false;
  }

  /**
   * Ensures the class is registered with its provided {@link ImmutabilityType}, if and only if the
   * detected Immutability type matches the immutabilityType parameter.
   *
   * @param class___ class whose registration is to be ensured
   * @param immutabilityType {@link ImmutabilityType} to be associated with the class, if and only if
   *                         the detected Immutability type matches the immutabilityType parameter
   * @return {@code true} if the class was registered with the provided {@link ImmutabilityType}
   */
  public static boolean registerWithVerify(@NotNull Class<?> class___, @NotNull Detector.ImmutabilityType immutabilityType) {
    //TODO: Write test cases
    if (isRegistrable(class___)) {
      var immutabilityTypeDetected = internalClassIs(class___);
      if (immutabilityType != immutabilityTypeDetected)
        return false;
      REGISTERED_TUPLE2SR_BY_CLASS.put(class___, new Tuple2SR(immutabilityType, false));
      return true;
    }

    return false;
  }

  /**
   * Deregisters (i.e. removes) the class and its {@link ImmutabilityType}
   *
   * @param class___ class whose removal is to be ensured
   * @return {@code true} if the class was deregistered
   */
  public static boolean deregister(@NotNull Class<?> class___) {
    //TODO: Write test cases
    if (Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
      .filter(tuple2SR -> !tuple2SR.isRestricted())
      .isPresent()) {
      REGISTERED_TUPLE2SR_BY_CLASS.remove(class___);
      return true;
    }

    return false;
  }

  private record Tuple2CB(Class<?> class___, boolean isNotProcessed) {};

  /**
   * Ensures the classes are registered, each with their own automatically detected
   * {@link ImmutabilityType}.
   *
   * @param class___s classes whose registration is to be ensured
   * @return list of classes which failed to register; i.e. could have already been added, or were
   *         prohibited from being registered
   */
  @NotNull
  public static List<Class<?>> registerMultipleAutoDetect(@NotNull Set<Class<?>> class___s) {
    //TODO: Write test cases
    if (class___s.isEmpty())
      return List.of();
    var tuple2CBs = class___s.stream()
      .map(class___ -> new Tuple2CB(class___, !registerAutoDetect(class___)))
      .toList();

    return tuple2CBs.stream()
      .filter(Tuple2CB::isNotProcessed)
      //due to loss of type inference with .map(), prefixing a type witness as in .<...>map();
      //  details here: https://stackoverflow.com/a/73281848/501113
      .<Class<?>> map(Tuple2CB::class___)
      .toList();
  }

  /**
   * Ensures the classes are registered, each with their own automatically detected
   * {@link ImmutabilityType}.
   *
   * @param class___s classes whose registration is to be ensured
   * @return list of classes which failed to register; i.e. could have already been added, or were
   *         prohibited from being registered
   */
  public static List<Class<?>> registerMultipleAutoDetect(Class<?>... class___s) {
    //TODO: Write test cases
    return registerMultipleAutoDetect(Arrays.stream(class___s).collect(Collectors.toUnmodifiableSet()));
  }

  /**
   * Ensures the classes are registered, each with its provided {@link ImmutabilityType}. When isVerifying
   * is true, then if and only if the detected Immutability type matches the immutabilityType
   * parameter is the class registered.
   *
   * @param immutabilityTypeByClass each class key is whose registration with its {@link ImmutabilityType}
   *                                value is to be ensured
   * @param isVerifying when {@code true}, ensures the provided {@link ImmutabilityType} matches the
   *                    {@link ImmutabilityType} returned from the detector, otherwise indiscriminately
   *                    overrides the existing associated value
   * @return list of classes which failed to register; i.e. could have failed to have verified,
   *         already been added, or were prohibited from being registered
   */
  @NotNull
  public static List<Class<?>> registerMultiple(@NotNull Map<Class<?>, ImmutabilityType> immutabilityTypeByClass, boolean isVerifying) {
    //TODO: Write test cases
    if (immutabilityTypeByClass.isEmpty())
      return List.of();
    var tuple2CBs = immutabilityTypeByClass.entrySet().stream()
      .map(entry -> new Tuple2CB(entry.getKey(), !(isVerifying
          ? registerWithVerify(entry.getKey(), entry.getValue())
          : registerOrOverride(entry.getKey(), entry.getValue()))))
      .toList();

    return tuple2CBs.stream()
      .filter(Tuple2CB::isNotProcessed)
      //due to loss of type inference with .map(), prefixing a type witness as in .<...>map();
      //  details here: https://stackoverflow.com/a/73281848/501113
      .<Class<?>> map(Tuple2CB::class___)
      .collect(Collectors.toList()); //cannot use .toList() due to loss of type inference with .map(), details here: https://stackoverflow.com/a/73281848/501113
  }

  /**
   * Deregisters (i.e. removes) all the classes and their associated {@link ImmutabilityType}
   *
   * @param class___s classes whose removal is to be ensured
   * @return list of classes which failed to deregister; i.e. could have already been removed, or were
   *         prohibited from being deregistered
   */
  @NotNull
  public static List<Class<?>> deregisterMultiple(@NotNull Set<Class<?>> class___s) {
    //TODO: Write test cases
    if (class___s.isEmpty())
      return List.of();
    var tuple2CBs = class___s.stream()
      .map(class___ -> new Tuple2CB(class___, !deregister(class___)))
      .toList();

    return tuple2CBs.stream()
      .filter(Tuple2CB::isNotProcessed)
      //due to loss of type inference with .map(), prefixing a type witness as in .<...>map();
      //  details here: https://stackoverflow.com/a/73281848/501113
      .<Class<?>> map(Tuple2CB::class___)
      .toList();
  }

  /**
   * Deregisters (i.e. removes) all the classes and their associated {@link ImmutabilityType}
   *
   * @param class___s classes whose removal is to be ensured
   * @return list of classes which failed to deregister; i.e. could have already been removed, or were
   *         prohibited from being deregistered
   */
  @NotNull
  public static List<Class<?>> deregisterMultiple(Class<?>... class___s) {
    //TODO: Write test cases
    return deregisterMultiple(Arrays.stream(class___s).collect(Collectors.toUnmodifiableSet()));
  }

  private static ImmutabilityType internalClassIsRecord(Class<Record> classRecord) {
    var resolved = ImmutabilityType.JAVA_CONSTANT;
    var recordComponents = Arrays.stream(classRecord.getRecordComponents()).iterator();
    while ((resolved != ImmutabilityType.UNDEFINED) && recordComponents.hasNext()) {
      var recordComponent = recordComponents.next();
      if (!recordComponent.getType().isPrimitive()) { //within a Record, a primitive is always immutable
        var immutabilityType = classIs(recordComponent.getType());
        if (immutabilityType.ordinal() < resolved.ordinal())
          resolved = immutabilityType;
      }
    }

    return resolved;
  }

  /**
   * Detector for analyzing if the class is a Java platform defined constant as a form of deep
   * immutability.
   *
   * @param class___ class to check against the detector
   * @return an {@link Optional} describing the detected {@link ImmutabilityType}
   */
  @NotNull
  public static Optional<ImmutabilityType> classIsJavaConstant(@NotNull Class<?> class___) {
    return Constable.class.isAssignableFrom(class___)    //Covers: MethodHandle
        || ConstantDesc.class.isAssignableFrom(class___) //Covers: ClassDesc, MethodTypeDesc, MethodHandleDesc, and DynamicConstantDesc
        || class___.isEnum()
            ? Optional.of(ImmutabilityType.JAVA_CONSTANT)
            : Optional.empty();
  }

  /**
   * Detector for analyzing if the class is a Java Record with deep-immutability. A Java Record is
   * only guaranteed by the compiler to be shallow-immutable.
   *
   * @param class___ Java Record class to check against the detector
   * @return an {@link Optional} describing the detected {@link ImmutabilityType}
   */
  @NotNull
  public static Optional<ImmutabilityType> classIsRecord(@NotNull Class<?> class___) {
    //noinspection unchecked
    return class___.isRecord()
        ? Optional.of(internalClassIsRecord((Class<Record>) class___))
        : Optional.empty();
  }

  /**
   * Detector for analyzing if the class has the specific {@code interface} ensuring a deep-immutability contract.
   *
   * @param class___ class to check against the detector
   * @return an {@link Optional} describing the detected {@link ImmutabilityType}
   */
  @NotNull
  public static Optional<ImmutabilityType> classIsJcipAnnotationOrImmutableInterface(@NotNull Class<?> class___) {
    return class___.isAnnotationPresent(net.jcip.annotations.Immutable.class) || Immutable.class.isAssignableFrom(class___)
        ? Optional.of(ImmutabilityType.BEFORE_END_OF_CLASS_CONSTRUCTOR)
        : Optional.empty();
  }

  /**
   * Detector for analyzing if the class is the specific {@code interface} ensuring an effectively
   * deep-immutability contract.
   *
   * @param class___ class to check against the detector
   * @return an {@link Optional} describing the detected {@link ImmutabilityType}
   */
  @NotNull
  public static Optional<ImmutabilityType> classIsImmutableEffective(@NotNull Class<?> class___) {
    return ImmutableEffective.class.isAssignableFrom(class___)
        ? Optional.of(ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE)
        : Optional.empty();
  }

  private static Detector.ImmutabilityType internalClassIs(Class<?> class___) {
    return class___.isArray() //java.lang.Array is always mutable
        ? ImmutabilityType.UNDEFINED
        : classIsJavaConstant(class___)
            .or(() -> classIsRecord(class___))
            .or(() -> classIsJcipAnnotationOrImmutableInterface(class___))
            .or(() -> classIsImmutableEffective(class___))
            .orElse(ImmutabilityType.UNDEFINED);
  }

  /**
   * Detector for analyzing the class-level of deep-immutability; i.e. applies to all instances of
   * the class.
   * <p>
   * Scans across all the individual detectors in preferred order of likelihood.
   *
   * @param class___ class to check against the detector
   * @return the detected {@link ImmutabilityType}
   */
  @NotNull
  public static Detector.ImmutabilityType classIs(@NotNull Class<?> class___) {
    return Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
        .map(Tuple2SR::immutabilityType)
        .orElse(internalClassIs(class___));
  }

  /**
   * Maximum reduction on the detector to positively indicate immutability availability.
   * <p>
   * This is a convenience method provided to simplify stream filtering across the {@link ImmutabilityType}
   * enum values; i.e. reduce boilerplate for lambdas.
   *
   * @param class___ class to check against the simplified detector
   * @return {@code true} if the detected {@link ImmutabilityType} is any enum value but {@link ImmutabilityType#UNDEFINED}
   */
  public static boolean isClassATypeOf(@NotNull Class<?> class___) {
    return classIs(class___) != ImmutabilityType.UNDEFINED;
  }

  /**
   * Maximum reduction on the class-level detector to negatively indicate immutability
   * availability.
   * <p>
   * This is a convenience method provided to simplify stream filtering across the {@link ImmutabilityType}
   * enum values; i.e. reduce boilerplate for lambdas just needing negation.
   *
   * @param class___ class to check against the simplified detector
   * @return {@code true} if the detected {@link ImmutabilityType} is the enum value {@link ImmutabilityType#UNDEFINED}
   */
  public static boolean isClassNotATypeOf(@NotNull Class<?> class___) {
    return classIs(class___) == ImmutabilityType.UNDEFINED;
  }

  private static boolean internalRecordHasAtLeastOneIrreversiblyImmutableStillUnset(Record record) throws InvocationTargetException, IllegalAccessException {
    boolean result = false; //have hit a isIrreversiblyImmutable() which is still false

    var classRecord = record.getClass();
    var recordComponents = Arrays.stream(classRecord.getRecordComponents()).iterator();
    while (!result && recordComponents.hasNext()) {
      var recordComponent = recordComponents.next();
      if (ImmutableEffective.class.isAssignableFrom(recordComponent.getType())) {
        var object = recordComponent.getAccessor().invoke(record);
        result = !((ImmutableEffective)object).isIrreversiblyImmutable();
      } else if (recordComponent.getType().isRecord()) {
        result = internalRecordHasAtLeastOneIrreversiblyImmutableStillUnset((Record)recordComponent.getAccessor().invoke(record));
      }
    }

    return result;
  }

  /**
   * Detector for analyzing the instance-level of deep-immutability; i.e. applies only to some
   * instances of the object's parent class.
   *
   * @param object instance to check against the detector
   * @return the detected {@link ImmutabilityType}
   */
  @NotNull
  public static Detector.ImmutabilityType instanceIs(@NotNull Object object) {
    var result = classIs(object.getClass());

    if (result == ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE)
      if (object instanceof ImmutableEffective immutableEffective) {
        if (!immutableEffective.isIrreversiblyImmutable())
          result = ImmutabilityType.UNDEFINED;
      } else
        if (object instanceof Record record) {
          try {
            if (internalRecordHasAtLeastOneIrreversiblyImmutableStillUnset(record))
              result = ImmutabilityType.UNDEFINED;
          } catch (InvocationTargetException | IllegalAccessException ignored) {
            result = ImmutabilityType.UNDEFINED; //unable to resolve
          }
        } else
          result = ImmutabilityType.UNDEFINED; //unable to resolve

    return result;
  }

  /**
   * Maximum reduction on the instance-level detector to positively indicate immutability
   * availability.
   * <p>
   * This is a convenience method provided to simplify stream filtering across the {@link ImmutabilityType}
   * enum values; i.e. reduce boilerplate for lambdas.
   *
   * @param object instance to check against the simplified detector
   * @return {@code true} if the detected {@link ImmutabilityType} is any enum value but {@link ImmutabilityType#UNDEFINED}
   */
  public static boolean isInstanceInStateOf(@NotNull Object object) {
    return instanceIs(object) != ImmutabilityType.UNDEFINED;
  }

  /**
   * Maximum reduction on the instance-level detector to negatively indicate immutability
   * availability.
   * <p>
   * This is a convenience method provided to simplify stream filtering across the {@link ImmutabilityType}
   * enum values; i.e. reduce boilerplate for lambdas.
   *
   * @param object instance to check against the simplified detector
   * @return {@code true} if the detected {@link ImmutabilityType} is the enum value {@link ImmutabilityType#UNDEFINED}
   */
  public static boolean isInstanceNotInStateOf(@NotNull Object object) {
    return instanceIs(object) == ImmutabilityType.UNDEFINED;
  }
}
