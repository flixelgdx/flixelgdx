package me.stringdotjar.flixelgdx.backend.reflect;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Runtime reflection service contract used by {@code Flixel.reflect}.
 *
 * <p>This interface defines the reflection operations that Flixel uses across all platforms.
 * Platform modules may provide optimized implementations, but each implementation should preserve
 * the same behavior contract so gameplay code behaves consistently.
 *
 * <p>Implementations are expected to cache metadata aggressively. Reflection is often used in
 * frequently executed paths such as tween setup or scripting systems, so repeated class scanning
 * should be avoided whenever possible.
 */
public interface FlixelReflection {

  /**
   * Checks whether a field with the given name exists on the target instance.
   *
   * <p>Implementations should include inherited fields from superclasses. Returning {@code false}
   * means the field cannot be resolved for read or write operations.
   *
   * @param target The object instance to inspect.
   * @param fieldName The field name to search for.
   * @return {@code true} if the field exists on the target type, otherwise {@code false}.
   */
  boolean hasField(Object target, String fieldName);

  /**
   * Reads a field value from the target instance.
   *
   * <p>Implementations should resolve inherited fields and may allow non-public access depending on
   * platform constraints. If the field cannot be resolved or read, an exception should be thrown.
   *
   * @param target The object instance to read from.
   * @param fieldName the field name to read
   * @return the current field value
   */
  Object field(Object target, String fieldName);

  /**
   * Writes a field value to the target instance.
   *
   * <p>If a field is immutable, final, unsupported, or cannot be written safely, implementations
   * should throw an explicit exception instead of silently failing.
   *
   * @param target The object instance to modify.
   * @param fieldName The field name to write.
   * @param value The new value to store.
   */
  void setField(Object target, String fieldName, Object value);

  /**
   * Reads a property value from the target.
   *
   * <p>Implementations may resolve JavaBean getters when available and fall back to direct field
   * access when appropriate.
   *
   * @param target The object instance to inspect.
   * @param propertyName The property name to resolve.
   * @return The resolved property value.
   */
  Object getProperty(Object target, String propertyName);

  /**
   * Writes a property value on the target.
   *
   * <p>Implementations may resolve JavaBean setters when available and fall back to direct field
   * writes when appropriate. Unsupported writes should throw an explicit exception.
   *
   * @param target The object instance to modify.
   * @param propertyName The property name to resolve.
   * @param value The new property value.
   */
  void setProperty(Object target, String propertyName, Object value);

  /**
   * Returns a list of available field names for the target.
   *
   * <p>The result should include inherited fields. Implementations may return a cached immutable
   * list for performance.
   *
   * @param target The object instance to inspect.
   * @return Field names available on the target type.
   */
  List<String> fields(Object target);

  /**
   * Invokes a method by name on the target.
   *
   * <p>Implementations should resolve overloads using argument arity and type compatibility where
   * supported by the platform runtime.
   *
   * @param target The object instance to invoke on.
   * @param methodName The method name.
   * @param args Arguments for invocation.
   * @return The invocation result, or {@code null} for void methods.
   */
  Object callMethod(Object target, String methodName, Object... args);

  /**
   * Returns whether a value should be treated as an object value in reflection contexts.
   *
   * <p>This is useful for parity with dynamic reflection APIs and for differentiating scalar values
   * from richer object structures.
   *
   * @param value Value to classify.
   * @return {@code true} if the value is treated as an object value.
   */
  boolean isObject(Object value);

  /**
   * Creates a shallow copy of the source object.
   *
   * <p>Implementations typically require a no-argument constructor and then copy non-static field
   * values from source to destination.
   *
   * @param source Source instance.
   * @param <T> Source type.
   * @return Copied instance.
   */
  <T> T copy(T source);

  /**
   * Compares two method-like references for logical equality.
   *
   * <p>Implementations may use identity and equality checks depending on how method references are
   * represented on the current platform.
   *
   * @param methodA First method reference.
   * @param methodB Second method reference.
   * @return {@code true} if both represent the same method target
   */
  boolean compareMethods(Object methodA, Object methodB);

  /**
   * Returns all fields declared on a type and its superclasses.
   *
   * <p>Implementations should cache the result per class to avoid repeated hierarchy traversal.
   *
   * @param type Class to inspect.
   * @return All resolved fields.
   */
  List<Field> getAllFields(Class<?> type);

  /**
   * Returns all fields declared on a type and its superclasses as an array.
   *
   * @param type Class to inspect.
   * @return All resolved fields as an array.
   */
  Field[] getAllFieldsAsArray(Class<?> type);

  /**
   * Checks whether the class at the given class path is declared as final.
   *
   * <p>When a class cannot be resolved, implementations may return {@code false} to preserve
   * compatibility with existing behavior.
   *
   * @param classPath Fully qualified class name.
   * @return {@code true} when the resolved class is final.
   */
  boolean isClassFinal(String classPath);
}
