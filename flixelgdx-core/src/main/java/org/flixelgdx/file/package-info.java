/**
 * File and resource access for FlixelGDX.
 *
 * <p>This package provides the seam FlixelGDX uses to read files without naming a specific file
 * library, so the same game code works across platforms and backends. It is the plumbing beneath
 * asset loading, save data, fonts, and any other content a game reads from disk or the classpath.
 *
 * <h2>Where things live</h2>
 *
 * <ul>
 *   <li><b>File system roots:</b> {@link org.flixelgdx.file.FlixelFiles FlixelFiles} on
 *       {@link org.flixelgdx.Flixel#files Flixel.files}, with {@code internal}, {@code classpath},
 *       {@code external}, {@code local}, and {@code absolute} roots.</li>
 *   <li><b>A single file:</b> {@link org.flixelgdx.file.FlixelFile FlixelFile}, a lightweight handle
 *       you query ({@code exists}, {@code isDirectory}) and read ({@code readString},
 *       {@code readBytes}).</li>
 *   <li><b>Safe defaults:</b> {@link org.flixelgdx.file.FlixelNoopFiles FlixelNoopFiles} and
 *       {@link org.flixelgdx.file.FlixelNoopFile FlixelNoopFile}, installed until a backend provides
 *       a real file system, so lookups and reads never crash on headless sessions.</li>
 * </ul>
 *
 * <p>The active backend installs a real {@link org.flixelgdx.file.FlixelFiles FlixelFiles} on
 * {@link org.flixelgdx.Flixel#files Flixel.files} before
 * {@link org.flixelgdx.Flixel#start(org.flixelgdx.FlixelGame, org.flixelgdx.backend.FlixelGameRunner) Flixel.start(...)} runs.
 */
package org.flixelgdx.file;
