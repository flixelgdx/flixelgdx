/**
 * Mouse and pointer input for FlixelGDX.
 *
 * <p>{@link org.flixelgdx.input.mouse.FlixelMouseManager FlixelMouseManager} is the global entry
 * point, accessible via {@link org.flixelgdx.Flixel#mouse Flixel.mouse}. It exposes pressed,
 * justPressed, and justReleased queries for each {@link org.flixelgdx.input.mouse.FlixelMouseButton
 * FlixelMouseButton}, along with screen-space and world-space coordinate reads.
 *
 * <p>{@link org.flixelgdx.input.mouse.FlixelMouseIconManager FlixelMouseIconManager} is a
 * pluggable interface for changing the cursor icon at runtime. Each backend installs its own
 * implementation: desktop uses {@code FlixelLwjgl3MouseIconManager} (GLFW cursor API) and the web
 * backend uses a CSS cursor override. The no-op fallback
 * {@link org.flixelgdx.input.mouse.FlixelNoopMouseIconManager FlixelNoopMouseIconManager} is used
 * until a backend installs its own.
 *
 * <p>{@link org.flixelgdx.input.mouse.FlixelMouseCursor FlixelMouseCursor} enumerates
 * the system cursor shapes (arrow, hand, crosshair, resize handles, and so on) that
 * {@link org.flixelgdx.input.mouse.FlixelMouseIconManager#setCursor(org.flixelgdx.input.mouse.FlixelMouseCursor)
 * FlixelMouseCursor} accepts.
 */
package org.flixelgdx.input.mouse;
