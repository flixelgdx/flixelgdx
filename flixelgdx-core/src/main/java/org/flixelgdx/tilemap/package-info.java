/**
 * Tilemap system for FlixelGDX.
 *
 * <p>This package includes the necessities for creating, manipulating and handling levels using
 * a looping ring-buffer system for tiles. Note that this doesn't wrap (let alone utilize) libGDX's
 * tilemap system at all. The framework's tilemap system is designed to be lightweight even for
 * large levels by recycling visible tiles based on the set width and height, rather than creating a
 * massive tilemap and culling the tiles the player can't see.
 *
 * @see org.flixelgdx.tilemap.FlixelTilemap
 * @see org.flixelgdx.tilemap.FlixelTilemapLayer
 * @see org.flixelgdx.tilemap.FlixelTileBehavior
 */
package org.flixelgdx.tilemap;
