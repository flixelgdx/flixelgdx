/**
 * File and resource access for FlixelGDX.
 *
 * <p>This package is the platform-neutral seam FlixelGDX uses to open files. Game code never
 * touches a file library directly; it asks for a root through
 * {@link org.flixelgdx.Flixel#files Flixel.files}, gets back a
 * {@link org.flixelgdx.file.FlixelFile FlixelFile} handle, and reads from it. The active backend
 * wires in the real implementation before
 * {@link org.flixelgdx.Flixel#start(org.flixelgdx.FlixelGame, org.flixelgdx.backend.FlixelGameRunner) Flixel.start(...)}
 * runs, so the same game code works across every platform FlixelGDX supports.
 *
 * <h2>Key types</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.file.FlixelFiles FlixelFiles} - the file system entry point, accessed
 *       through {@link org.flixelgdx.Flixel#files Flixel.files}. Call one of its factory methods to
 *       get a handle rooted at the right location for your use case.</li>
 *   <li>{@link org.flixelgdx.file.FlixelFile FlixelFile} - a lightweight pointer to a single file or
 *       directory. It does nothing until you query or read it; handles are cheap to create.</li>
 *   <li>{@link org.flixelgdx.file.FlixelNoopFiles FlixelNoopFiles} and
 *       {@link org.flixelgdx.file.FlixelNoopFile FlixelNoopFile} - safe no-op defaults installed
 *       before a backend is ready. All reads return empty content and {@code exists()} returns
 *       {@code false}, so headless tests and early startup code never crash.</li>
 * </ul>
 *
 * <h2>File roots</h2>
 *
 * <p>{@link org.flixelgdx.file.FlixelFiles FlixelFiles} exposes five roots and one
 * preference-aware root. Pick the one that matches where the data lives:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.file.FlixelFiles#internal(String) internal} - assets bundled with
 *       the game (the {@code assets/} folder). This is the right root for images, sounds, fonts,
 *       and data files that ship with the build.</li>
 *   <li>{@link org.flixelgdx.file.FlixelFiles#classpath(String) classpath} - resources embedded on
 *       the Java classpath. Used as a fallback on desktop when assets are packaged inside a JAR.</li>
 *   <li>{@link org.flixelgdx.file.FlixelFiles#external(String) external} - files under the user's
 *       home directory. Use this for save data that must survive game reinstalls.</li>
 *   <li>{@link org.flixelgdx.file.FlixelFiles#local(String) local} - files relative to the working
 *       directory where the application was started. Handy for developer tooling and debug output.</li>
 *   <li>{@link org.flixelgdx.file.FlixelFiles#absolute(String) absolute} - a file named by its
 *       full OS path. Use sparingly; prefer the other roots when the location is predictable.</li>
 *   <li>{@link org.flixelgdx.file.FlixelFiles#pref(String, String, String) pref} - resolves a path
 *       inside the OS-appropriate preferences directory for a given organization and application name
 *       ({@code %APPDATA%} on Windows, {@code ~/Library/Application Support} on macOS,
 *       {@code $XDG_DATA_HOME} on Linux). Use this for persistent save data in
 *       production-shipping games instead of {@code external}. This is rarely used in game code, as the
 *       {@link org.flixelgdx.util.save.FlixelSave FlixelSave} system uses this under the hood.</li>
 * </ul>
 *
 * <h2>Reading a file</h2>
 *
 * <pre>{@code
 * // Reading a bundled level file:
 * FlixelFile level = Flixel.files.internal("levels/world1.json");
 * if (level.exists()) {
 *   String json = level.readString();
 *   // parse json...
 * }
 * }</pre>
 *
 * <h2>Listing a directory</h2>
 *
 * <pre>{@code
 * // Discovering every PNG in a bundled atlas folder:
 * FlixelFile[] frames = Flixel.files.internal("sprites/hero").list("png");
 * for (int i = 0; i < frames.length; i++) {
 *   sheet.addFrame(frames[i].readBytes());
 * }
 * }</pre>
 *
 * <h2>Web platform note</h2>
 *
 * <p>On the HTML5 backend, {@link org.flixelgdx.file.FlixelFile#readBytes() readBytes()} returns
 * the raw encoded bytes from the server (PNG, JPEG, OGG, and so on), not decoded pixel or PCM
 * data. Code that needs decoded image data must go through
 * {@link org.flixelgdx.asset.FlixelAssetManager FlixelAssetManager} so the browser's async decode
 * path runs correctly. Preload all images with {@code Flixel.assets.load()} and poll
 * {@code Flixel.assets.update()} in a loading state before reading them.
 *
 * @see org.flixelgdx.file.FlixelFiles
 * @see org.flixelgdx.file.FlixelFile
 * @see org.flixelgdx.Flixel#files
 */
package org.flixelgdx.file;
