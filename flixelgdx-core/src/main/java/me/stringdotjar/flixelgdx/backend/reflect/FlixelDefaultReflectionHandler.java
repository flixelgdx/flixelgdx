package me.stringdotjar.flixelgdx.backend.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default reflection handler used across platforms that rely on Java reflection.
 */
public class FlixelDefaultReflectionHandler implements FlixelReflection {

  private final Map<Class<?>, CachedClassMetadata> metadataCache = new ConcurrentHashMap<>();
  private final Map<String, Boolean> classFinalCache = new ConcurrentHashMap<>();

  @Override
  public boolean hasField(Object target, String fieldName) {
    return getFieldInternal(target, fieldName) != null;
  }

  @Override
  public Object field(Object target, String fieldName) {
    Field field = getRequiredField(target, fieldName);
    try {
      return field.get(target);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot read field \"" + fieldName + "\".", e);
    }
  }

  @Override
  public void setField(Object target, String fieldName, Object value) {
    Field field = getRequiredField(target, fieldName);
    int modifiers = field.getModifiers();
    if (Modifier.isFinal(modifiers)) {
      throw new IllegalStateException("Field \"" + fieldName + "\" is final and cannot be changed.");
    }
    if (Modifier.isStatic(modifiers)) {
      throw new IllegalStateException("Field \"" + fieldName + "\" is static and cannot be changed on an instance.");
    }

    Object converted = convertValue(field.getType(), value, "setField(" + fieldName + ")");
    try {
      field.set(target, converted);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot write field \"" + fieldName + "\".", e);
    }
  }

  @Override
  public Object getProperty(Object target, String propertyName) {
    CachedClassMetadata meta = getMetadata(requireTarget(target));
    Method getter = meta.gettersByProperty.get(propertyName);
    if (getter != null) {
      try {
        return getter.invoke(target);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException("Cannot read property \"" + propertyName + "\".", e);
      }
    }
    return field(target, propertyName);
  }

  @Override
  public void setProperty(Object target, String propertyName, Object value) {
    CachedClassMetadata meta = getMetadata(requireTarget(target));
    Method setter = meta.settersByProperty.get(propertyName);
    if (setter != null) {
      Class<?> paramType = setter.getParameterTypes()[0];
      Object converted = convertValue(paramType, value, "setProperty(" + propertyName + ")");
      try {
        setter.invoke(target, converted);
        return;
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException("Cannot write property \"" + propertyName + "\".", e);
      }
    }
    setField(target, propertyName, value);
  }

  @Override
  public List<String> fields(Object target) {
    CachedClassMetadata meta = getMetadata(requireTarget(target));
    return meta.fieldNames;
  }

  @Override
  public Object callMethod(Object target, String methodName, Object... args) {
    Objects.requireNonNull(methodName, "methodName");
    CachedClassMetadata meta = getMetadata(requireTarget(target));
    List<Method> methods = meta.methodsByName.get(methodName);
    if (methods == null || methods.isEmpty()) {
      throw new IllegalArgumentException("Method \"" + methodName + "\" does not exist.");
    }

    Object[] safeArgs = args == null ? new Object[0] : args;
    Method method = resolveMethod(methods, safeArgs);
    if (method == null) {
      throw new IllegalArgumentException("No compatible overload found for method \"" + methodName + "\".");
    }

    try {
      return method.invoke(target, adaptArgs(method.getParameterTypes(), safeArgs, methodName));
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Failed to invoke method \"" + methodName + "\".", e);
    }
  }

  @Override
  public boolean isObject(Object value) {
    if (value == null) return false;
    return !(value instanceof String
      || value instanceof Number
      || value instanceof Boolean
      || value instanceof Character
      || value instanceof Enum<?>);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T copy(T source) {
    if (source == null) return null;
    Class<?> type = source.getClass();
    CachedClassMetadata meta = getMetadata(type);
    Constructor<?> ctor = meta.noArgConstructor;
    if (ctor == null) {
      throw new IllegalStateException("Type \"" + type.getName() + "\" cannot be copied (missing no-arg constructor).");
    }
    try {
      Object instance = ctor.newInstance();
      for (Field field : meta.allFields) {
        if (Modifier.isStatic(field.getModifiers())) continue;
        field.set(instance, field.get(source));
      }
      return (T) instance;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Failed to copy instance of \"" + type.getName() + "\".", e);
    }
  }

  @Override
  public boolean compareMethods(Object methodA, Object methodB) {
    return methodA == methodB || (methodA != null && methodA.equals(methodB));
  }

  @Override
  public List<Field> getAllFields(Class<?> type) {
    return new ArrayList<>(getMetadata(type).allFields);
  }

  @Override
  public Field[] getAllFieldsAsArray(Class<?> type) {
    List<Field> all = getMetadata(type).allFields;
    return all.toArray(new Field[0]);
  }

  @Override
  public boolean isClassFinal(String classPath) {
    Objects.requireNonNull(classPath, "classPath");
    return classFinalCache.computeIfAbsent(classPath, path -> {
      try {
        Class<?> clazz = Class.forName(path);
        return Modifier.isFinal(clazz.getModifiers());
      } catch (ClassNotFoundException e) {
        return false;
      }
    });
  }

  private Field getRequiredField(Object target, String fieldName) {
    Field field = getFieldInternal(target, fieldName);
    if (field == null) {
      throw new IllegalArgumentException("Field \"" + fieldName + "\" does not exist.");
    }
    return field;
  }

  private Field getFieldInternal(Object target, String fieldName) {
    Objects.requireNonNull(fieldName, "fieldName");
    CachedClassMetadata meta = getMetadata(requireTarget(target));
    return meta.fieldsByName.get(fieldName);
  }

  private Class<?> requireTarget(Object target) {
    if (target == null) {
      throw new IllegalArgumentException("Target cannot be null.");
    }
    return target.getClass();
  }

  private CachedClassMetadata getMetadata(Class<?> type) {
    Objects.requireNonNull(type, "type");
    return metadataCache.computeIfAbsent(type, this::buildMetadata);
  }

  private CachedClassMetadata buildMetadata(Class<?> type) {
    List<Field> allFields = new ArrayList<>();
    Map<String, Field> fieldsByName = new ConcurrentHashMap<>();
    Map<String, List<Method>> methodsByName = new ConcurrentHashMap<>();
    Map<String, Method> gettersByProperty = new ConcurrentHashMap<>();
    Map<String, Method> settersByProperty = new ConcurrentHashMap<>();
    Constructor<?> noArgConstructor = null;

    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        field.trySetAccessible();
        allFields.add(field);
        fieldsByName.putIfAbsent(field.getName(), field);
      }
      for (Method method : current.getDeclaredMethods()) {
        method.trySetAccessible();
        methodsByName.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
        if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
          String property = getterPropertyName(method.getName());
          if (property != null) gettersByProperty.putIfAbsent(property, method);
        } else if (method.getParameterCount() == 1 && method.getName().startsWith("set")) {
          String property = normalizeProperty(method.getName().substring(3));
          if (property != null) settersByProperty.putIfAbsent(property, method);
        }
      }
    }

    try {
      noArgConstructor = type.getDeclaredConstructor();
      noArgConstructor.trySetAccessible();
    } catch (NoSuchMethodException ignored) {
      // Optional for copy().
    }

    List<String> fieldNames = new ArrayList<>(fieldsByName.keySet());
    Collections.sort(fieldNames);
    return new CachedClassMetadata(
      Collections.unmodifiableList(allFields),
      Collections.unmodifiableMap(fieldsByName),
      Collections.unmodifiableMap(methodsByName),
      Collections.unmodifiableMap(gettersByProperty),
      Collections.unmodifiableMap(settersByProperty),
      Collections.unmodifiableList(fieldNames),
      noArgConstructor
    );
  }

  private static Method resolveMethod(List<Method> methods, Object[] args) {
    for (Method method : methods) {
      Class<?>[] paramTypes = method.getParameterTypes();
      if (paramTypes.length != args.length) continue;
      if (isCompatible(paramTypes, args)) return method;
    }
    return null;
  }

  private static boolean isCompatible(Class<?>[] paramTypes, Object[] args) {
    for (int i = 0; i < paramTypes.length; i++) {
      Object arg = args[i];
      if (arg == null && paramTypes[i].isPrimitive()) return false;
      if (arg == null) continue;
      Class<?> wrapped = wrapPrimitive(paramTypes[i]);
      if (!wrapped.isAssignableFrom(arg.getClass())) {
        if (!canConvertNumeric(wrapped, arg.getClass())) return false;
      }
    }
    return true;
  }

  private static Object[] adaptArgs(Class<?>[] paramTypes, Object[] args, String operation) {
    Object[] adapted = Arrays.copyOf(args, args.length);
    for (int i = 0; i < adapted.length; i++) {
      adapted[i] = convertValue(paramTypes[i], adapted[i], operation);
    }
    return adapted;
  }

  private static Object convertValue(Class<?> targetType, Object value, String operation) {
    if (value == null) {
      if (targetType.isPrimitive()) {
        throw new IllegalArgumentException(operation + " cannot assign null to primitive " + targetType.getName());
      }
      return null;
    }
    Class<?> wrappedTarget = wrapPrimitive(targetType);
    if (wrappedTarget.isAssignableFrom(value.getClass())) {
      return value;
    }
    if (value instanceof Number number && Number.class.isAssignableFrom(wrappedTarget)) {
      if (wrappedTarget == Byte.class) return number.byteValue();
      if (wrappedTarget == Short.class) return number.shortValue();
      if (wrappedTarget == Integer.class) return number.intValue();
      if (wrappedTarget == Long.class) return number.longValue();
      if (wrappedTarget == Float.class) return number.floatValue();
      if (wrappedTarget == Double.class) return number.doubleValue();
    }
    if (wrappedTarget == Character.class && value instanceof String s && s.length() == 1) {
      return s.charAt(0);
    }
    throw new IllegalArgumentException(operation + " cannot convert " + value.getClass().getName() + " to " + targetType.getName());
  }

  private static boolean canConvertNumeric(Class<?> targetType, Class<?> sourceType) {
    return Number.class.isAssignableFrom(targetType) && Number.class.isAssignableFrom(sourceType);
  }

  private static Class<?> wrapPrimitive(Class<?> type) {
    if (!type.isPrimitive()) return type;
    if (type == boolean.class) return Boolean.class;
    if (type == byte.class) return Byte.class;
    if (type == short.class) return Short.class;
    if (type == int.class) return Integer.class;
    if (type == long.class) return Long.class;
    if (type == float.class) return Float.class;
    if (type == double.class) return Double.class;
    if (type == char.class) return Character.class;
    return type;
  }

  private static String getterPropertyName(String name) {
    if (name.startsWith("get") && name.length() > 3) {
      return normalizeProperty(name.substring(3));
    }
    if (name.startsWith("is") && name.length() > 2) {
      return normalizeProperty(name.substring(2));
    }
    return null;
  }

  private static String normalizeProperty(String segment) {
    if (segment == null || segment.isEmpty()) return null;
    if (segment.length() == 1) return segment.toLowerCase(Locale.ROOT);
    return Character.toLowerCase(segment.charAt(0)) + segment.substring(1);
  }

  private record CachedClassMetadata(
    List<Field> allFields,
    Map<String, Field> fieldsByName,
    Map<String, List<Method>> methodsByName,
    Map<String, Method> gettersByProperty,
    Map<String, Method> settersByProperty,
    List<String> fieldNames,
    Constructor<?> noArgConstructor
  ) {}
}
