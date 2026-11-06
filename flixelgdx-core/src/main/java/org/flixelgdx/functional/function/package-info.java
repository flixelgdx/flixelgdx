/**
 * Functional interfaces that read a primitive or text value out of an object.
 *
 * <p>Java ships {@link java.util.function.ToIntFunction} and friends for {@code int}, {@code long}, and
 * {@code double}. This package fills the gaps ({@code float} and {@code boolean}) and adds
 * {@link org.flixelgdx.functional.function.StringAppender}, which writes text into a reused buffer. None of them
 * box values, so they are safe to call every frame.
 */
package org.flixelgdx.functional.function;
