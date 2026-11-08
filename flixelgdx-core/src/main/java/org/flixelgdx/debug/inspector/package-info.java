/**
 * The building blocks for rows of values in the debugger: properties, sections, and the builders that create them.
 *
 * <p>Much of what a debugger shows is a list of labeled values: a player's health and speed, a sprite's position and
 * scale, the aggression of an enemy AI. Writing that list by hand for every class would be tedious and would allocate
 * new strings every frame. This package lets you <em>describe</em> the rows once, with one getter (and optionally one
 * setter) per row, and then the debugger reads them, sends only what changed, and turns edits in the app back into
 * setter calls for you.
 *
 * <p>Think of a character sheet in a tabletop game. Each line on the sheet is a <em>property</em>: a label ("Health"),
 * the current number, and for some lines a pencil to change it. Lines are grouped into boxes such as "Movement" or
 * "Weapon": those boxes are <em>sections</em>. You fill in the sheet's layout once with a builder, and from then on
 * the debugger keeps the numbers up to date.
 *
 * <h2>Where to start</h2>
 *
 * <p>There are two builders, one for each place rows appear:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.inspector.FlixelSectionBuilder FlixelSectionBuilder} describes what the inspector
 *       shows for one of <em>your classes</em>. Its getters take the selected object, so one description works for
 *       every instance. Register it with
 *       {@link org.flixelgdx.debug.feeds.FlixelDebugInspector#register(Class, String, java.util.function.Consumer) Flixel.debug.inspector.register(...)}.
 *       </li>
 *   <li>{@link org.flixelgdx.debug.inspector.FlixelPropertyBuilder FlixelPropertyBuilder} describes the live rows of
 *       one section of a {@link org.flixelgdx.debug.ui.FlixelDebugProperties FlixelDebugProperties} property grid in a
 *       panel. Its getters take no arguments and read whatever the lambda captured.</li>
 * </ul>
 *
 * <p>Both builders create the same kind of row, a
 * {@link org.flixelgdx.debug.inspector.FlixelDebugProperty FlixelDebugProperty}. Inspector rows are grouped into a
 * {@link org.flixelgdx.debug.inspector.FlixelDebugSection FlixelDebugSection}, and an edit sent by the app arrives as
 * a {@link org.flixelgdx.debug.inspector.FlixelDebugValue FlixelDebugValue}. You rarely touch these three directly.
 *
 * <h2>Showing your class in the inspector</h2>
 *
 * <p>When an object is selected, the inspector shows one section for every registered class in its family tree, most
 * specific first. A {@code Player} that extends {@code FlixelSprite} shows a "Player" section, then the sections the
 * framework registers for sprites, objects, and the rest. You only describe what your own class adds:
 *
 * <pre>{@code
 * Flixel.debug.inspector.register(Player.class, "Player", b -> b
 *     .intProp("Health", Player::getHealth, Player::setHealth)  // With a setter, the row can be edited.
 *     .floatProp("Speed", Player::getSpeed)                     // Without one, it is read-only.
 *     .boolProp("On ground", Player::isOnGround)
 *     .stringProp("Name", (p, out) -> out.concat(p.getName())) // Text is appended into a reused buffer.
 *     .enumProp("State", Player::getState)
 *     .intProp("Id", Player::getId).once());                    // Read once per selection.
 *
 * Flixel.debug.inspector.select(player); // Or pick it in the app.
 * }</pre>
 *
 * <p>A section can also read from an object related to the selection, such as the player's weapon, with
 * {@link org.flixelgdx.debug.inspector.FlixelSectionBuilder#find(java.util.function.Function) find(...)}. Call it
 * before adding rows. When it returns {@code null}, the section is hidden for that selection:
 *
 * <pre>{@code
 * Flixel.debug.inspector.register(Player.class, "Weapon", b -> b
 *     .find(Player::getWeapon)
 *     .intProp("Ammo", Weapon::getAmmo, Weapon::setAmmo));
 * }</pre>
 *
 * <h2>Live rows in a panel</h2>
 *
 * <p>For values that are not about one selected object, such as the state of a whole system, put a property grid in
 * a panel and describe its sections with {@link org.flixelgdx.debug.inspector.FlixelPropertyBuilder
 * FlixelPropertyBuilder}. The grid reads every getter each frame while the panel is open and sends only the values
 * that changed:
 *
 * <pre>{@code
 * FlixelDebugProperties grid = new FlixelDebugProperties()
 *     .section("Enemy AI", s -> s
 *         .intProp("Active enemies", () -> enemies.countLiving())
 *         .floatProp("Aggression", () -> ai.aggression, v -> ai.aggression = v)
 *         .boolProp("Paused", () -> ai.paused, v -> ai.paused = v))
 *     .section("World", s -> s
 *         .doubleProp("Time", () -> world.time));
 *
 * Flixel.debug.panels.register(new FlixelDebugPanel("game.ai", "Enemy AI", grid));
 * }</pre>
 *
 * <h2>One method per type</h2>
 *
 * <p>Both builders have a separate method for each value type: {@code boolProp}, {@code intProp}, {@code longProp},
 * {@code floatProp}, {@code doubleProp}, {@code stringProp}, and {@code enumProp}. One overloaded {@code prop(...)}
 * method would look tidier, but the compiler could not always tell which getter type a lambda is meant to be. With
 * separate names a lambda always means exactly one thing, and numbers are read as primitives without boxing.
 *
 * <h2>Editing values</h2>
 *
 * <p>Giving a row a setter makes it editable in the app. When the person changes the value, the app sends it back,
 * and the row converts it to its own type: a numeric row accepts any number and converts it the way a Java cast
 * would, a boolean row only accepts a boolean, and a text row only accepts text. A value of the wrong kind is ignored
 * with a warning. Text setters receive a reused buffer that is only valid during the call, so call
 * {@code toString()} on it if you need to keep it.
 *
 * <h2>Threading and performance</h2>
 *
 * <ul>
 *   <li>Register sections and build grids on the game's main thread. Getters and setters are also called there.</li>
 *   <li>Reading a row never boxes or allocates. Every row keeps its latest reading in a primitive field or a reused
 *       {@link org.flixelgdx.util.FlixelString FlixelString}, so your getters must not allocate either.</li>
 *   <li>Rows marked with {@code once()} are read once instead of on every refresh (in the inspector, once per
 *       selection), and again after they are edited. That suits values that cannot change, such as an id.</li>
 *   <li>A getter that throws shows its message in place of the value, and the other rows keep working.</li>
 * </ul>
 *
 * @see org.flixelgdx.debug.inspector.FlixelSectionBuilder
 * @see org.flixelgdx.debug.inspector.FlixelPropertyBuilder
 * @see org.flixelgdx.debug.feeds.FlixelDebugInspector
 * @see org.flixelgdx.debug.ui.FlixelDebugProperties
 */
package org.flixelgdx.debug.inspector;
