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

//StackOverflow Questions and Answers for inspiration:
//  - https://stackoverflow.com/q/203475/501113
//  - https://stackoverflow.com/q/37087809/501113
//Also where it was posted as an answer

public final class Detector {
  //TODO: turn these lines into Javadoc
  //Allows classes to be registered/deregistered to be treated as part of the immutability check
  //It's useful as a bypass for all other libraries like Vavr
  //All Styles are allowed excluding JAVA_CONSTANT, and are UNVERIFIED - entirely depends upon the clients' trustworthiness
  //Allows UNDEFINED to be submitted to efficiently short circuit all the other checks for known highly utilized mutable classes

  public enum ImmutabilityType {
    //TODO: move each of the comments to the right into a Javadoc for that specific enum
    UNDEFINED,          //unable to assert any level of immutability
    PROPERTY_IRREVERSIBLY_IMMUTABLE, //likely post-instantiation window of mutability requiring an ImmutableEffective.isIrreversiblyImmutable() style of check
    BEFORE_END_OF_CLASS_CONSTRUCTOR, //trusting the implementor to properly enforce the {@link io.plii.java.immutability.Immutable Immutable} contract
    JAVA_CONSTANT       //trusting the Java compiler and platform

    //TODO: capture these ideas in the above Javadoc
    /*
    public enum TrustLevel {
      UNDEFINED, //No reliable assertion(s) upon which to base trust the instance has entered and will remain in an observably deeply immutable state
      WEAK,    //trusting the implementor to accurately reflect post-instantiation deep immutability via the "set-once" getter, `isIrreversiblyImmutable()`, a.k.a. "post constructor effectively immutable"
      STRONG,   //trusting the Java platform or the implementor's contract to prohibit all mutation and ensuring deep immutability by the end of the execution of the final constructor
    }
  */
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
    ).forEach(class___ -> REGISTERED_TUPLE2SR_BY_CLASS.put(class___, new Tuple2SR(ImmutabilityType.JAVA_CONSTANT, true)));
  }

  //TODO: fill in the javadoc
  /** <-- type this, and then hit Return, and it generates the Javadoc outline
   *
   * @param class___
   * @return
   */
  @NotNull
  public static Optional<ImmutabilityType> registered(@NotNull Class<?> class___)  {
    //TODO: Write test cases
    return Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
        .map(Tuple2SR::immutabilityType);
  }

  //TODO: add javadoc
  public static boolean register(@NotNull Class<?> class___, @NotNull Detector.ImmutabilityType immutabilityType) {
    //TODO: Write test cases
    if (Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
        .filter(Tuple2SR::isRestricted)
        .isEmpty()) {
      REGISTERED_TUPLE2SR_BY_CLASS.put(class___, new Tuple2SR(immutabilityType, false));
      return true;
    }

    return false;
  }

  //TODO: add javadoc
  public static boolean deregister(Class<?> class___) {
    if (Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
      .filter(tuple2SR -> !tuple2SR.isRestricted())
      .isPresent()) {
      REGISTERED_TUPLE2SR_BY_CLASS.remove(class___);
      return true;
    }

    return false;
  }

  private record Tuple2CB(Class<?> class___, boolean isNotProcessed) {};

  //TODO: add javadoc
  public static List<Class<?>> registerMultiple(@NotNull Map<Class<?>, ImmutabilityType> styleByClass) {
    if (styleByClass.isEmpty())
      return List.of();
    var tuple2CBs = styleByClass.entrySet().stream()
      .map(entry -> new Tuple2CB(entry.getKey(), !register(entry.getKey(), entry.getValue())))
      .toList();

    return tuple2CBs.stream()
      .filter(Tuple2CB::isNotProcessed)
      .map(Tuple2CB::class___)
      .collect(Collectors.toList()); //cannot use .toList() due to loss of type inference with .map(), details here: https://stackoverflow.com/a/73281848/501113
  }

  //TODO: add javadoc
  public static List<Class<?>> deregisterMultiple(@NotNull Set<Class<?>> class___s) {
    //TODO: Write test cases
    if (class___s.isEmpty())
      return List.of();
    var tuple2CBs = class___s.stream()
      .map(class___ -> new Tuple2CB(class___, !deregister(class___)))
      .toList();

    return tuple2CBs.stream()
      .filter(Tuple2CB::isNotProcessed)
      .map(Tuple2CB::class___)
      .collect(Collectors.toList()); //cannot use .toList() due to loss of type inference with .map(), details here: https://stackoverflow.com/a/73281848/501113
  }

  //TODO: add javadoc
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
        var style = classIs(recordComponent.getType());
        if (style.ordinal() < resolved.ordinal())
          resolved = style;
      }
    }

    return resolved;
  }

  //TODO: add javadoc
  public static Optional<ImmutabilityType> classIsJavaConstant(Class<?> class___) {
    return Constable.class.isAssignableFrom(class___)    //Covers: MethodHandle
        || ConstantDesc.class.isAssignableFrom(class___) //Covers: ClassDesc, MethodTypeDesc, MethodHandleDesc, and DynamicConstantDesc
        || class___.isEnum()
            ? Optional.of(ImmutabilityType.JAVA_CONSTANT)
            : Optional.empty();
  }

  public static Optional<ImmutabilityType> classIsRecord(Class<?> class___) {
    //noinspection unchecked
    return class___.isRecord()
        ? Optional.of(internalClassIsRecord((Class<Record>) class___))
        : Optional.empty();
  }

  //TODO: add javadoc
  public static Optional<ImmutabilityType> classIsJcipAnnotationOrImmutableInterface(Class<?> class___) {
    return class___.isAnnotationPresent(net.jcip.annotations.Immutable.class) || Immutable.class.isAssignableFrom(class___)
        ? Optional.of(ImmutabilityType.BEFORE_END_OF_CLASS_CONSTRUCTOR)
        : Optional.empty();
  }

  //TODO: add javadoc
  public static Optional<ImmutabilityType> classIsImmutableEffective(Class<?> class___) {
    return ImmutableEffective.class.isAssignableFrom(class___)
        ? Optional.of(ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE)
        : Optional.empty();
  }

  //TODO: add javadoc
  @NotNull
  public static Detector.ImmutabilityType classIs(@NotNull Class<?> class___) {
    return class___.isArray() //java.lang.Array is always mutable
        ? ImmutabilityType.UNDEFINED
        : Optional.ofNullable(REGISTERED_TUPLE2SR_BY_CLASS.get(class___))
            .map(Tuple2SR::immutabilityType)
            .or(() -> classIsJavaConstant(class___))
            .or(() -> classIsRecord(class___))
            .or(() -> classIsJcipAnnotationOrImmutableInterface(class___))
            .or(() -> classIsImmutableEffective(class___))
            .orElse(ImmutabilityType.UNDEFINED);
  }

  //TODO: add javadoc
  public static boolean isClassATypeOf(@NotNull Class<?> c) {
    return classIs(c) != ImmutabilityType.UNDEFINED;
  }

  //TODO: add javadoc
  //providing helper to reduce boilerplate for lambdas just needing negation
  public static boolean isClassNotATypeOf(@NotNull Class<?> c) {
    return classIs(c) == ImmutabilityType.UNDEFINED;
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

  @NotNull
  //TODO: add javadoc
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

  //TODO: add javadoc
  public static boolean isInstanceInStateOf(@NotNull Object object) {
    return instanceIs(object) != ImmutabilityType.UNDEFINED;
  }

  //TODO: add javadoc
  //providing helper to reduce boilerplate for lambdas just needing negation
  public static boolean isInstanceNotInStateOf(@NotNull Object object) {
    return instanceIs(object) == ImmutabilityType.UNDEFINED;
  }
}
