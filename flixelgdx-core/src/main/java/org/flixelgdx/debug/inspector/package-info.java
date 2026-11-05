/**
 * The building blocks for rows of values in the remote debugger: the inspector's property sections and the rows
 * of custom panels.
 *
 * <p>A <em>property</em> is one labeled row (a name, a live value, and optionally a way to change it). Rows are
 * grouped into a <em>section</em>, and sections are created with a builder that has one method per value type,
 * such as {@code intProp(...)} and {@code floatProp(...)}.
 *
 * <p>Use {@link org.flixelgdx.debug.inspector.FlixelSectionBuilder} to describe what the inspector shows for one of
 * your classes, and {@link org.flixelgdx.debug.inspector.FlixelPanelBuilder} to describe the rows and buttons of a
 * custom panel. The panels that use them live in {@link org.flixelgdx.debug}.
 */
package org.flixelgdx.debug.inspector;
