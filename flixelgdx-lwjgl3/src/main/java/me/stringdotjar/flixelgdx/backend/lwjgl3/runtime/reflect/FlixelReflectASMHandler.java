/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3.runtime.reflect;

import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;

import me.stringdotjar.flixelgdx.backend.reflect.FlixelDefaultReflectionHandler;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelReflection;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReflectASM-first handler with Java reflection fallback.
 */
public final class FlixelReflectASMHandler implements FlixelReflection {

  private final FlixelDefaultReflectionHandler fallback;
  private final Map<Class<?>, FieldAccess> fieldAccessCache = new ConcurrentHashMap<>();
  private final Map<Class<?>, MethodAccess> methodAccessCache = new ConcurrentHashMap<>();
  private final Map<Class<?>, Map<String, Integer>> fieldIndexCache = new ConcurrentHashMap<>();
  private final Map<Class<?>, Map<String, Integer>> methodIndexCache = new ConcurrentHashMap<>();

  public FlixelReflectASMHandler() {
    this(new FlixelDefaultReflectionHandler());
  }

  public FlixelReflectASMHandler(FlixelDefaultReflectionHandler fallback) {
    this.fallback = Objects.requireNonNull(fallback, "fallback");
  }

  @Override
  public boolean hasField(Object target, String fieldName) {
    if (target == null) {
      return false;
    }
    Integer index = getFieldIndex(target.getClass(), fieldName);
    return index != null || fallback.hasField(target, fieldName);
  }

  @Override
  public Object field(Object target, String fieldName) {
    if (target == null) {
      throw new IllegalArgumentException("Target cannot be null.");
    }
    Integer index = getFieldIndex(target.getClass(), fieldName);
    if (index != null) {
      try {
        return getFieldAccess(target.getClass()).get(target, index);
      } catch (RuntimeException ignored) {
        // Fallback below.
      }
    }
    return fallback.field(target, fieldName);
  }

  @Override
  public void setField(Object target, String fieldName, Object value) {
    if (target == null) {
      throw new IllegalArgumentException("Target cannot be null.");
    }
    Integer index = getFieldIndex(target.getClass(), fieldName);
    if (index != null) {
      try {
        getFieldAccess(target.getClass()).set(target, index, value);
        return;
      } catch (RuntimeException ignored) {
        // Fallback below.
      }
    }
    fallback.setField(target, fieldName, value);
  }

  @Override
  public Object getProperty(Object target, String propertyName) {
    return fallback.getProperty(target, propertyName);
  }

  @Override
  public void setProperty(Object target, String propertyName, Object value) {
    fallback.setProperty(target, propertyName, value);
  }

  @Override
  public List<String> fields(Object target) {
    return fallback.fields(target);
  }

  @Override
  public Object callMethod(Object target, String methodName, Object... args) {
    if (target == null) {
      throw new IllegalArgumentException("Target cannot be null.");
    }
    Integer index = getMethodIndex(target.getClass(), methodName, args == null ? 0 : args.length);
    if (index != null) {
      try {
        Object[] safeArgs = args == null ? new Object[0] : args;
        return getMethodAccess(target.getClass()).invoke(target, index, safeArgs);
      } catch (RuntimeException ignored) {
        // Fallback below.
      }
    }
    return fallback.callMethod(target, methodName, args);
  }

  @Override
  public boolean isObject(Object value) {
    return fallback.isObject(value);
  }

  @Override
  public <T> T copy(T source) {
    return fallback.copy(source);
  }

  @Override
  public boolean compareMethods(Object methodA, Object methodB) {
    return fallback.compareMethods(methodA, methodB);
  }

  @Override
  public List<Field> getAllFields(Class<?> type) {
    return fallback.getAllFields(type);
  }

  @Override
  public Field[] getAllFieldsAsArray(Class<?> type) {
    return fallback.getAllFieldsAsArray(type);
  }

  @Override
  public boolean isClassFinal(String classPath) {
    return fallback.isClassFinal(classPath);
  }

  private FieldAccess getFieldAccess(Class<?> type) {
    return fieldAccessCache.computeIfAbsent(type, FieldAccess::get);
  }

  private MethodAccess getMethodAccess(Class<?> type) {
    return methodAccessCache.computeIfAbsent(type, MethodAccess::get);
  }

  private Integer getFieldIndex(Class<?> type, String fieldName) {
    Map<String, Integer> indices = fieldIndexCache.computeIfAbsent(type, this::buildFieldIndices);
    return indices.get(fieldName);
  }

  private Integer getMethodIndex(Class<?> type, String methodName, int argCount) {
    Map<String, Integer> indices = methodIndexCache.computeIfAbsent(type, this::buildMethodIndices);
    return indices.get(methodName + "#" + argCount);
  }

  private Map<String, Integer> buildFieldIndices(Class<?> type) {
    try {
      FieldAccess access = getFieldAccess(type);
      String[] names = access.getFieldNames();
      Map<String, Integer> map = new HashMap<>(names.length);
      for (int i = 0; i < names.length; i++) {
        map.put(names[i], i);
      }
      return map;
    } catch (RuntimeException e) {
      return Map.of();
    }
  }

  private Map<String, Integer> buildMethodIndices(Class<?> type) {
    try {
      MethodAccess access = getMethodAccess(type);
      String[] names = access.getMethodNames();
      Class<?>[][] paramTypes = access.getParameterTypes();
      Map<String, Integer> map = new HashMap<>(names.length);
      for (int i = 0; i < names.length; i++) {
        map.putIfAbsent(names[i] + "#" + paramTypes[i].length, i);
      }
      return map;
    } catch (RuntimeException e) {
      return Map.of();
    }
  }
}
