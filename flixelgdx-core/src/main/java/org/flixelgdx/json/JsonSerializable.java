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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for automatic, reflection-free JSON mapping.
 *
 * <p>At compile time an annotation processor generates a companion class named
 * {@code <ClassName>JsonSerializer} in the same package, with two static methods:
 * <ul>
 *   <li>{@code String toJson(<ClassName> value)} - turns an instance into a JSON string.</li>
 *   <li>{@code <ClassName> fromJson(FlixelJsonValue json)} - fills a fresh instance from parsed JSON.</li>
 * </ul>
 *
 * <p>Because the mapping code is generated ahead of time, there is no runtime reflection: this both
 * avoids per-call allocation churn and works on platforms (such as the web target) where reflection
 * metadata is expensive or unavailable.
 *
 * <p>Requirements for an annotated class:
 * <ul>
 *   <li>It must have an accessible no-argument constructor.</li>
 *   <li>Mapped fields must be non-{@code private} (the generated class lives in the same package and
 *       reads and writes fields directly). {@code static}, {@code transient}, and {@code final}
 *       fields are skipped.</li>
 *   <li>Supported field types: the primitives, {@link String}, enums (mapped by
 *       {@link Enum#name()}), other {@code @JsonSerializable} types (which map through their own
 *       generated serializer), and one-dimensional arrays of any of those.</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * @JsonSerializable
 * public class SaveData {
 *   public int highScore;
 *   public String playerName;
 *   public boolean tutorialDone;
 * }
 *
 * // Elsewhere, no reflection involved:
 * String text = SaveDataJsonSerializer.toJson(save);
 * SaveData loaded = SaveDataJsonSerializer.fromJson(FlixelJson.parse(text));
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface JsonSerializable {
}
