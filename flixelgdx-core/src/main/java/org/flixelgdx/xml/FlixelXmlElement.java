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
package org.flixelgdx.xml;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One element of a parsed XML document: its tag name, attributes, text, and child elements.
 *
 * <p>Produced by {@link FlixelXml#parse(String)}. The API mirrors what atlas formats need:
 * attribute reads with defaults and name-filtered child iteration.
 *
 * <pre>{@code
 * FlixelXmlElement atlas = FlixelXml.parse(file.readString());
 * for (int i = 0; i < atlas.getChildCount(); i++) {
 *   FlixelXmlElement sub = atlas.getChild(i);
 *   if ("SubTexture".equals(sub.getName())) {
 *     int x = sub.getIntAttribute("x", 0);
 *   }
 * }
 * }</pre>
 */
public final class FlixelXmlElement {

  @NotNull
  private final String name;

  @NotNull
  private final FlixelMap<String, String> attributes = new FlixelMap<>();

  @NotNull
  private final FlixelArray<FlixelXmlElement> children = new FlixelArray<>(4);

  @Nullable
  private String text;

  FlixelXmlElement(@NotNull String name) {
    this.name = name;
  }

  void setAttribute(@NotNull String key, @NotNull String value) {
    attributes.put(key, value);
  }

  void addChild(@NotNull FlixelXmlElement child) {
    children.add(child);
  }

  void setText(@Nullable String text) {
    this.text = text;
  }

  /**
   * @return This element's tag name.
   */
  @NotNull
  public String getName() {
    return name;
  }

  /**
   * @return This element's text content, or {@code null} when it has none.
   */
  @Nullable
  public String getText() {
    return text;
  }

  /**
   * @return The number of direct child elements.
   */
  public int getChildCount() {
    return children.getSize();
  }

  /**
   * Returns a direct child by index.
   *
   * @param index The child index.
   * @return The child element.
   */
  @NotNull
  public FlixelXmlElement getChild(int index) {
    return children.get(index);
  }

  /**
   * Collects all direct children with the given tag name into a fresh list.
   *
   * @param childName The tag name to match.
   * @return A new list of matching children, possibly empty; never {@code null}.
   */
  @NotNull
  public FlixelArray<FlixelXmlElement> getChildrenByName(@NotNull String childName) {
    FlixelArray<FlixelXmlElement> out = new FlixelArray<>(8);
    for (int i = 0; i < children.getSize(); i++) {
      FlixelXmlElement child = children.get(i);
      if (childName.equals(child.name)) {
        out.add(child);
      }
    }
    return out;
  }

  /**
   * Reads a string attribute.
   *
   * @param key The attribute name.
   * @param defaultValue Returned when the attribute is absent.
   * @return The attribute's value or the default.
   */
  public String getAttribute(@NotNull String key, @Nullable String defaultValue) {
    String value = attributes.get(key);
    return value != null ? value : defaultValue;
  }

  /**
   * Reads an integer attribute.
   *
   * @param key The attribute name.
   * @param defaultValue Returned when the attribute is absent or not a number.
   * @return The attribute's value or the default.
   */
  public int getIntAttribute(@NotNull String key, int defaultValue) {
    String value = attributes.get(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Reads a float attribute.
   *
   * @param key The attribute name.
   * @param defaultValue Returned when the attribute is absent or not a number.
   * @return The attribute's value or the default.
   */
  public float getFloatAttribute(@NotNull String key, float defaultValue) {
    String value = attributes.get(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Float.parseFloat(value.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Reads a boolean attribute.
   *
   * @param key The attribute name.
   * @param defaultValue Returned when the attribute is absent.
   * @return {@code true} when the attribute equals {@code "true"} (case-insensitive).
   */
  public boolean getBooleanAttribute(@NotNull String key, boolean defaultValue) {
    String value = attributes.get(key);
    return value != null ? "true".equalsIgnoreCase(value.trim()) : defaultValue;
  }

  /**
   * Returns whether an attribute exists.
   *
   * @param key The attribute name.
   * @return {@code true} when present.
   */
  public boolean hasAttribute(@NotNull String key) {
    return attributes.containsKey(key);
  }
}
