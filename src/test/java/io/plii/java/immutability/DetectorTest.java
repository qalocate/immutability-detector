package io.plii.java.immutability;

import java.lang.constant.ClassDesc;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DetectorTest {

  private void testValue(Object object, Detector.ImmutabilityType immutabilityType) {
    testValue(object, immutabilityType, immutabilityType);
  }

  private void testValue(Object object, Detector.ImmutabilityType classIsImmutabilityType, Detector.ImmutabilityType instanceIsImmutabilityType) {
    var c = object.getClass();
    assertAll(
      //as class
      () -> assertEquals(classIsImmutabilityType, Detector.classIs(c)),
      () -> assertEquals(Detector.isClassATypeOf(c), classIsImmutabilityType != Detector.ImmutabilityType.UNDEFINED),
      () -> assertEquals(Detector.isClassNotATypeOf(c), classIsImmutabilityType == Detector.ImmutabilityType.UNDEFINED),

      //as instance
      () -> assertEquals(instanceIsImmutabilityType, Detector.instanceIs(object)),
      () -> assertEquals(Detector.isInstanceInStateOf(object), instanceIsImmutabilityType != Detector.ImmutabilityType.UNDEFINED),
      () -> assertEquals(Detector.isInstanceNotInStateOf(object), instanceIsImmutabilityType == Detector.ImmutabilityType.UNDEFINED)
    );
  }

  @Test
  @DisplayName("Object as ImmutabilityType.UNDEFINED")
  void objectAsUndefined() {
    testValue(new Object(), Detector.ImmutabilityType.UNDEFINED);
  }

  static class ImmutableEffectiveImpl implements ImmutableEffective {
    private boolean irreversiblyImmutable;

    public ImmutableEffectiveImpl(boolean irreversiblyImmutable) {
      this.irreversiblyImmutable = irreversiblyImmutable;
    }

    public boolean isIrreversiblyImmutable() {
      return irreversiblyImmutable;
    }

    public void setAsIrreversiblyImmutable() {
      irreversiblyImmutable = true;
    }
  }

  @Nested
  @DisplayName("ImmutableEffective")
  class ImmutableEffectiveAll {
    @Test
    @DisplayName("ImmutableEffective as ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE/ImmutabilityType.UNDEFINED")
    void immutableEffectiveIsIrreversiblyImmutableAsFalse() {
      var immutableEffectiveImplFalse = new ImmutableEffectiveImpl(false);
      testValue(immutableEffectiveImplFalse, Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE, Detector.ImmutabilityType.UNDEFINED);
    }

    @Test
    @DisplayName("ImmutableEffective ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE")
    void immutableEffectiveIsIrreversiblyImmutableAsTrue() {
      var immutableEffectiveImplTrue = new ImmutableEffectiveImpl(true);
      testValue(immutableEffectiveImplTrue, Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE);
    }

    @Test
    @DisplayName("ImmutableEffective before and after property is set true")
    void immutableEffective() {
      var immutableEffectiveImpl = new ImmutableEffectiveImpl(false);
      testValue(immutableEffectiveImpl, Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE, Detector.ImmutabilityType.UNDEFINED);
      immutableEffectiveImpl.setAsIrreversiblyImmutable();
      testValue(immutableEffectiveImpl, Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE);
    }

  }

  @Test
  @DisplayName("Immutable as ImmutabilityType.CLASS_CONSTRUCTOR")
  void immutableAsClassConstructor() {
    testValue(new Immutable() {}, Detector.ImmutabilityType.BEFORE_END_OF_CLASS_CONSTRUCTOR);
  }


  @net.jcip.annotations.Immutable
  public static class JcipAnnotationImmutable {}

  @Test
  @DisplayName("@Immutable as ImmutabilityType.CLASS_CONSTRUCTOR")
  void atImmutableAsClassConstructor() {
    testValue(new JcipAnnotationImmutable(), Detector.ImmutabilityType.BEFORE_END_OF_CLASS_CONSTRUCTOR);
  }

  @Nested
  @DisplayName("ImmutabilityTypes - Java Record")
  class ImmutabilityTypesJavaRecord {
    public record ImmutabilityTypeJavaConstantShallowPrimitives(
        boolean valueBoolean,
        byte valueByte,
        short valueShort,
        char valueChar,
        int valueInt,
        long valueLong,
        float valueFloat,
        double valueDouble
    ) {}
    public record ImmutabilityTypeJavaConstantShallowPrimitiveObjects(
        Boolean valueBoolean,
        Byte valueByte,
        Short valueShort,
        Character valueChar,
        Integer valueInt,
        Long valueLong,
        Float valueFloat,
        Double valueDouble
    ) {}
    public record ImmutabilityTypeClassConstructor(
      int valueInt,
      JcipAnnotationImmutable jcipAnnotationImmutable,
      Immutable immutable
    ) {}
    public record ImmutabilityTypePropertyIrreversiblyImmutable(
      int valueInt,
      ImmutableEffectiveImpl immutableEffectiveImpl
    ) {}
    public record ImmutabilityTypeUndefinedShallowArrayPrimitiveInt(int[] ints) {}
    public record ImmutabilityTypeUndefinedShallowArrayObjectInteger(Integer[] integers) {}

    @Test
    @DisplayName("Shallow")
    void recordShallow() {
      testValue(new ImmutabilityTypeJavaConstantShallowPrimitives(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D), Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(new ImmutabilityTypeJavaConstantShallowPrimitiveObjects(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D), Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(new ImmutabilityTypeClassConstructor(1, new JcipAnnotationImmutable(), new Immutable() {}), Detector.ImmutabilityType.BEFORE_END_OF_CLASS_CONSTRUCTOR);
      var immutabilityTypePropertyIrreversiblyImmutable = new ImmutabilityTypePropertyIrreversiblyImmutable(1, new ImmutableEffectiveImpl(false));
      testValue(immutabilityTypePropertyIrreversiblyImmutable, Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE, Detector.ImmutabilityType.UNDEFINED);
      immutabilityTypePropertyIrreversiblyImmutable.immutableEffectiveImpl.setAsIrreversiblyImmutable();
      testValue(immutabilityTypePropertyIrreversiblyImmutable, Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE);

      testValue(new ImmutabilityTypeUndefinedShallowArrayPrimitiveInt(new int[]{0, 1, 2}), Detector.ImmutabilityType.UNDEFINED);
      testValue(new ImmutabilityTypeUndefinedShallowArrayObjectInteger(new Integer[]{3, 4, 5}), Detector.ImmutabilityType.UNDEFINED);
    }

    public record ValidDeepPrimitives(
      ImmutabilityTypeJavaConstantShallowPrimitives validShallowPrimitives,
      ImmutabilityTypeJavaConstantShallowPrimitiveObjects validShallowPrimitiveObjects
    ) {}

    public record ValidDeepPrimitivesWithJcipAnnotationImmutable(
      ImmutabilityTypeJavaConstantShallowPrimitives validShallowPrimitives,
      JcipAnnotationImmutable jcipAnnotationImmutable
    ) {}

    public record ValidDeepPrimitivesWithInterfaceImmutable(
      ImmutabilityTypeJavaConstantShallowPrimitives validShallowPrimitives,
      Immutable immutable
    ) {}

    public record ValidDeepPrimitivesWithInterfaceImmutableEffective(
      ImmutabilityTypeJavaConstantShallowPrimitives validShallowPrimitives,
      ImmutableEffectiveImpl immutableEffectiveImpl
    ) {}

    public record InvalidDeepPrimitivesWithArray(
      ImmutabilityTypeJavaConstantShallowPrimitives validShallowPrimitives,
      ImmutabilityTypeUndefinedShallowArrayPrimitiveInt invalidShallowArrayPrimitiveInt
    ) {}

    @Test
    @DisplayName("Deep")
    void recordDeep() {
      testValue(
          new ValidDeepPrimitives(
              new ImmutabilityTypeJavaConstantShallowPrimitives(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D),
              new ImmutabilityTypeJavaConstantShallowPrimitiveObjects(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D)),
          Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(
        new ValidDeepPrimitivesWithJcipAnnotationImmutable(
          new ImmutabilityTypeJavaConstantShallowPrimitives(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D),
          new JcipAnnotationImmutable()),
        Detector.ImmutabilityType.BEFORE_END_OF_CLASS_CONSTRUCTOR);
      testValue(
        new ValidDeepPrimitivesWithInterfaceImmutable(
          new ImmutabilityTypeJavaConstantShallowPrimitives(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D),
          new Immutable() {}),
        Detector.ImmutabilityType.BEFORE_END_OF_CLASS_CONSTRUCTOR);
      var immutableEffectiveImpl = new ImmutableEffectiveImpl(false);
      testValue(
        new ValidDeepPrimitivesWithInterfaceImmutableEffective(
          new ImmutabilityTypeJavaConstantShallowPrimitives(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D),
          immutableEffectiveImpl),
        Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE, Detector.ImmutabilityType.UNDEFINED);
      immutableEffectiveImpl.setAsIrreversiblyImmutable();
      testValue(
        new ValidDeepPrimitivesWithInterfaceImmutableEffective(
          new ImmutabilityTypeJavaConstantShallowPrimitives(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D),
          immutableEffectiveImpl),
        Detector.ImmutabilityType.PROPERTY_IRREVERSIBLY_IMMUTABLE);
      testValue(
        new InvalidDeepPrimitivesWithArray(
          new ImmutabilityTypeJavaConstantShallowPrimitives(true, (byte) 1, (short) 2, '3', 4, 5L, 6.0F, 7.0D),
          new ImmutabilityTypeUndefinedShallowArrayPrimitiveInt(new int[]{0, 1, 2})),
        Detector.ImmutabilityType.UNDEFINED);
    }
  }
  @Nested
  @DisplayName("ImmutabilityType.JAVA_CONSTANT")
  class ImmutabilityTypeJavaConstant {
    @Test
    @DisplayName("Constable")
    void javaConstable() {
      testValue("String", Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(Integer.MAX_VALUE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(Long.MAX_VALUE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(Float.MAX_VALUE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(Double.MAX_VALUE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(String.class, Detector.ImmutabilityType.JAVA_CONSTANT); //Class
      //TODO: Add tests for MethodType and MethodHandle?
    }

    @Test
    @DisplayName("ConstantDesc")
    void javaConstantDesc() {
      //ConstantDesc

      testValue(ClassDesc.of("java.lang.String"), Detector.ImmutabilityType.JAVA_CONSTANT);
      //TODO: Add tests for MethodTypeDesc, MethodHandleDesc, and DynamicConstantDesc?
    }

    @Test
    @DisplayName("Extraneous Java Platform Classes not covered by Constable nor ConstantDesc")
    void javaExtraneousJavaPlatformClasses() {
      testValue(Boolean.TRUE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(Byte.MAX_VALUE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(Short.MAX_VALUE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue('x', Detector.ImmutabilityType.JAVA_CONSTANT); //Character
      testValue(BigInteger.ONE, Detector.ImmutabilityType.JAVA_CONSTANT);
      testValue(BigDecimal.ONE, Detector.ImmutabilityType.JAVA_CONSTANT);

      //enum
      testValue(Detector.ImmutabilityType.UNDEFINED, Detector.ImmutabilityType.JAVA_CONSTANT);
    }
  }
}