/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.json;

import org.flixelgdx.collections.FlixelArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One node of a parsed JSON document: an object, array, string, number, boolean, or null.
 *
 * <p>Produced by {@link FlixelJson#parse(String)}. The API is navigation-friendly for game
 * data: children are reachable by name or index, and every typed getter has an overload with a
 * default so missing fields read cleanly:
 *
 * <pre>{@code
 * FlixelJsonValue root = FlixelJson.parse(file.readString());
 * int width = root.getInt("frameWidth", 32);
 * for (FlixelJsonValue frame : root.get("frames")) {
 *   String name = frame.getString("name", "");
 * }
 * }</pre>
 *
 * <p>The DOM is deliberately reflection-free so it works identically on TeaVM and native
 * images with no configuration.
 */
public final class FlixelJsonValue implements Iterable<FlixelJsonValue> {

  /** The number payload when {@link #getKind() kind} is {@link Kind#NUMBER}. */
  private double numberValue;

  @NotNull
  private final Kind kind;

  /** The string payload when {@link #getKind() kind} is {@link Kind#STRING}. */
  @Nullable
  private String stringValue;

  /** The field name of this value inside its parent object, or {@code null}. */
  @Nullable
  private String name;

  /** Child values for objects and arrays. */
  @Nullable
  private FlixelArray<FlixelJsonValue> children;

  /** The boolean payload when {@link #getKind() kind} is {@link Kind#BOOL}. */
  private boolean boolValue;

  FlixelJsonValue(@NotNull Kind kind) {
    this.kind = kind;
  }

  /** Creates a string value. */
  static FlixelJsonValue ofString(@Nullable String value) {
    FlixelJsonValue v = new FlixelJsonValue(Kind.STRING);
    v.stringValue = value;
    return v;
  }

  /** Creates a number value. */
  static FlixelJsonValue ofNumber(double value) {
    FlixelJsonValue v = new FlixelJsonValue(Kind.NUMBER);
    v.numberValue = value;
    return v;
  }

  /** Creates a boolean value. */
  static FlixelJsonValue ofBool(boolean value) {
    FlixelJsonValue v = new FlixelJsonValue(Kind.BOOL);
    v.boolValue = value;
    return v;
  }

  /** Adds a child to an object or array value. */
  void addChild(@NotNull FlixelJsonValue child) {
    if (children == null) {
      children = new FlixelArray<>(8);
    }
    children.add(child);
  }

  void setName(@Nullable String name) {
    this.name = name;
  }

  /**
   * Returns what kind of JSON node this is.
   *
   * @return The {@link Kind} enum constant identifying this node's type.
   */
  @NotNull
  public Kind getKind() {
    return kind;
  }

  /**
   * Returns this value's field name inside its parent object, or {@code null} for array
   * elements and the root.
   *
   * @return The field name, or {@code null} when this node is an array element or the root.
   */
  @Nullable
  public String getName() {
    return name;
  }

  /**
   * Returns {@code true} for object nodes.
   *
   * @return {@code true} when this node holds a JSON object.
   */
  public boolean isObject() {
    return kind == Kind.OBJECT;
  }

  /**
   * Returns {@code true} for array nodes.
   *
   * @return {@code true} when this node holds a JSON array.
   */
  public boolean isArray() {
    return kind == Kind.ARRAY;
  }

  /**
   * Returns {@code true} for JSON {@code null} nodes.
   *
   * @return {@code true} when this node represents a JSON {@code null} value.
   */
  public boolean isNull() {
    return kind == Kind.NULL;
  }

  /**
   * Returns the number of children for objects and arrays, or {@code 0} otherwise.
   *
   * @return The child count, or {@code 0} when this is not a container node.
   */
  public int getSize() {
    return children != null ? children.getSize() : 0;
  }

  /**
   * Looks a child up by field name.
   *
   * @param childName The field name.
   * @return The child, or {@code null} when absent.
   */
  @Nullable
  public FlixelJsonValue get(@NotNull String childName) {
    if (children == null) {
      return null;
    }
    for (int i = 0; i < children.getSize(); i++) {
      FlixelJsonValue child = children.get(i);
      if (childName.equals(child.name)) {
        return child;
      }
    }
    return null;
  }

  /**
   * Looks a child up by index.
   *
   * @param index The child index.
   * @return The child, or {@code null} when out of range.
   */
  @Nullable
  public FlixelJsonValue get(int index) {
    if (children == null || index < 0 || index >= children.getSize()) {
      return null;
    }
    return children.get(index);
  }

  /**
   * Returns whether an object child with the given name exists.
   *
   * @param childName The field name.
   * @return {@code true} when present.
   */
  public boolean has(@NotNull String childName) {
    return get(childName) != null;
  }

  /**
   * Returns this node's string payload, or a stringified number/boolean, or {@code null} for
   * any other node type.
   *
   * @return The string representation of this node's value, or {@code null} for object, array, and null nodes.
   */
  @Nullable
  public String asString() {
    return switch (kind) {
      case STRING -> stringValue;
      case NUMBER -> String.valueOf(numberValue);
      case BOOL -> String.valueOf(boolValue);
      default -> null;
    };
  }

  /**
   * Returns this node's numeric payload; strings are parsed, booleans map to 0/1, and every
   * other node type returns {@code 0}.
   *
   * @return The numeric value of this node, or {@code 0} for non-numeric types.
   */
  public double asDouble() {
    return switch (kind) {
      case NUMBER -> numberValue;
      case STRING -> parseDoubleSafe(stringValue);
      case BOOL -> boolValue ? 1 : 0;
      default -> 0;
    };
  }

  /**
   * Returns {@link #asDouble()} narrowed to a float.
   *
   * @return The numeric value of this node cast to {@code float}.
   */
  public float asFloat() {
    return (float) asDouble();
  }

  /**
   * Returns {@link #asDouble()} narrowed to an int.
   *
   * @return The numeric value of this node cast to {@code int}.
   */
  public int asInt() {
    return (int) asDouble();
  }

  /**
   * Returns this node's boolean payload; strings compare against {@code "true"} and numbers are
   * {@code true} when nonzero.
   *
   * @return The boolean interpretation of this node's value.
   */
  public boolean asBool() {
    return switch (kind) {
      case BOOL -> boolValue;
      case STRING -> "true".equalsIgnoreCase(stringValue);
      case NUMBER -> numberValue != 0;
      default -> false;
    };
  }

  /**
   * Reads a named string field.
   *
   * @param childName The field name.
   * @param defaultValue Returned when the field is absent or null.
   * @return The field's string value or the default.
   */
  public String getString(@NotNull String childName, @Nullable String defaultValue) {
    FlixelJsonValue child = get(childName);
    if (child == null || child.isNull()) {
      return defaultValue;
    }
    String s = child.asString();
    return s != null ? s : defaultValue;
  }

  /**
   * Reads a named float field.
   *
   * @param childName The field name.
   * @param defaultValue Returned when the field is absent.
   * @return The field's float value or the default.
   */
  public float getFloat(@NotNull String childName, float defaultValue) {
    FlixelJsonValue child = get(childName);
    return child != null && !child.isNull() ? child.asFloat() : defaultValue;
  }

  /**
   * Reads a named int field.
   *
   * @param childName The field name.
   * @param defaultValue Returned when the field is absent.
   * @return The field's int value or the default.
   */
  public int getInt(@NotNull String childName, int defaultValue) {
    FlixelJsonValue child = get(childName);
    return child != null && !child.isNull() ? child.asInt() : defaultValue;
  }

  /**
   * Reads a named boolean field.
   *
   * @param childName The field name.
   * @param defaultValue Returned when the field is absent.
   * @return The field's boolean value or the default.
   */
  public boolean getBool(@NotNull String childName, boolean defaultValue) {
    FlixelJsonValue child = get(childName);
    return child != null && !child.isNull() ? child.asBool() : defaultValue;
  }

  /**
   * Iterates this node's children (empty for non-container nodes).
   *
   * <p>Note that this allocates an iterator; hot paths should index with {@link #get(int)} and
   * {@link #getSize()} instead.
   */
  @NotNull
  @Override
  public java.util.Iterator<FlixelJsonValue> iterator() {
    if (children == null) {
      children = new FlixelArray<>(0);
    }
    return children.iterator();
  }

  private static double parseDoubleSafe(@Nullable String value) {
    if (value == null) {
      return 0;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /** The JSON node kinds. */
  public enum Kind {
    OBJECT,
    ARRAY,
    STRING,
    NUMBER,
    BOOL,
    NULL
  }
}
