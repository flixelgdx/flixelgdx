/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.reflect;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Default placeholder reflection service until a platform handler is assigned.
 */
public final class FlixelUnsupportedReflectionHandler implements FlixelReflection {

  private static UnsupportedOperationException unsupported(String operation) {
    return new UnsupportedOperationException(
      "Flixel.reflect is not configured yet. Call Flixel.setReflection(...) with a "
          + "FlixelReflection implementation. Unsupported operation: " + operation);
  }

  @Override
  public boolean hasField(Object target, String fieldName) {
    throw unsupported("hasField");
  }

  @Override
  public Object field(Object target, String fieldName) {
    throw unsupported("field");
  }

  @Override
  public void setField(Object target, String fieldName, Object value) {
    throw unsupported("setField");
  }

  @Override
  public Object property(Object target, String propertyName) {
    throw unsupported("getProperty");
  }

  @Override
  public void setProperty(Object target, String propertyName, Object value) {
    throw unsupported("setProperty");
  }

  @Override
  public List<String> fields(Object target) {
    throw unsupported("fields");
  }

  @Override
  public Object callMethod(Object target, String methodName, Object... args) {
    throw unsupported("callMethod");
  }

  @Override
  public boolean isObject(Object value) {
    throw unsupported("isObject");
  }

  @Override
  public <T> T copy(T source) {
    throw unsupported("copy");
  }

  @Override
  public boolean compareMethods(Object methodA, Object methodB) {
    throw unsupported("compareMethods");
  }

  @Override
  public List<Field> objectFields(Class<?> type) {
    throw unsupported("getAllFields");
  }

  @Override
  public Field[] objectFieldsArray(Class<?> type) {
    throw unsupported("getAllFieldsAsArray");
  }

  @Override
  public boolean isClassFinal(String classPath) {
    throw unsupported("isClassFinal");
  }

  @Override
  public FlixelPropertyPath resolvePropertyPath(Object root, String dottedPath) {
    throw unsupported("resolvePropertyPath");
  }
}
