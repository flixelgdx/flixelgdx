/**
 * The heart of FlixelGDX, and a complete beginner's tutorial for building your first game with it.
 *
 * <p>This page is written like a handbook. If you are brand new, read it from top to bottom once:
 * it walks you through setting up a project, explains the handful of ideas the whole framework is
 * built on, and then shows you how to do the everyday things almost every game needs (moving a
 * character, playing a sound, showing a score, switching screens, saving progress, and so on).
 * If you already know the basics, skip straight to the "How do I...?" cookbook near the end.
 *
 * <p>Every code example on this page uses real FlixelGDX APIs, so you can copy them into your own
 * project and adapt them.
 *
 * <h2>Table of contents</h2>
 * <ol>
 *   <li>What FlixelGDX is (and the one analogy to remember)</li>
 *   <li>Setting up a project</li>
 *   <li>Your first game, step by step</li>
 *   <li>The game loop: create, update, draw</li>
 *   <li>The building blocks: basics, objects, and sprites</li>
 *   <li>The {@code Flixel} class: your toolbox of global services</li>
 *   <li>States and substates: organizing your game into screens</li>
 *   <li>Sprites in depth</li>
 *   <li>Animation</li>
 *   <li>Movement, physics, and collisions</li>
 *   <li>Groups and recycling</li>
 *   <li>Input: keyboard, mouse, touch, and gamepads</li>
 *   <li>Text and HUDs</li>
 *   <li>Sound and music</li>
 *   <li>Files and assets</li>
 *   <li>Cameras</li>
 *   <li>Tweens and timers</li>
 *   <li>Signals (events)</li>
 *   <li>Saving progress</li>
 *   <li>Logging and debugging</li>
 *   <li>Writing fast code: the framework's performance habits</li>
 *   <li>"How do I...?" cookbook</li>
 *   <li>Where to go next</li>
 * </ol>
 *
 * <h2>1. What FlixelGDX is (and the one analogy to remember)</h2>
 *
 * <p>FlixelGDX is a 2D game framework for Java. It handles the boring, platform-specific parts of
 * making a game (opening a window, talking to the graphics card, reading the keyboard, playing
 * audio, saving files) so you can spend your time on the fun part: the game itself. The same game
 * code runs on desktop, in a web browser, and on Android.
 *
 * <p>The easiest way to understand how a FlixelGDX game is organized is to picture a
 * <b>theater</b>:
 *
 * <ul>
 *   <li>The <b>theater building</b> is your {@link org.flixelgdx.FlixelGame FlixelGame}. There is
 *       exactly one, it is open for as long as the game runs, and it owns the stage, the lights,
 *       and the sound system.</li>
 *   <li>Each <b>play</b> performed on the stage is a {@link org.flixelgdx.FlixelState FlixelState}:
 *       the title screen, the main menu, level one, the game over screen. Only one play runs at a
 *       time, and when it ends the stage is cleared for the next one.</li>
 *   <li>The <b>actors and props</b> are your objects: mostly
 *       {@link org.flixelgdx.FlixelSprite FlixelSprite} instances (the player, enemies, bullets,
 *       backgrounds) plus text, groups, and particle emitters. You add them to the current state
 *       and the state takes care of updating and drawing them.</li>
 *   <li>The <b>stage crew</b> is the {@link org.flixelgdx.Flixel Flixel} class. It is always
 *       available backstage, and you call on it whenever you need something: read input, play a
 *       sound, switch to another play, log a message.</li>
 *   <li>The <b>audience's view</b> is a {@link org.flixelgdx.FlixelCamera FlixelCamera}. It decides
 *       which part of the stage is visible, and it can follow an actor, shake, zoom, or fade to
 *       black.</li>
 * </ul>
 *
 * <p>Keep this picture in mind. Almost every question about "where does this code go?" has an
 * answer in it: global things live on the theater ({@code FlixelGame}) or the crew
 * ({@code Flixel}), and everything about a particular screen lives in its play
 * ({@code FlixelState}).
 *
 * <h2>2. Setting up a project</h2>
 *
 * <h3>What you need</h3>
 * <ul>
 *   <li><b>A Java 25 JDK.</b> Eclipse Temurin is the recommended distribution. You usually do not
 *       even need to install it yourself: the generated project asks Gradle to download a matching
 *       JDK on the first build.</li>
 *   <li><b>An IDE</b> such as IntelliJ IDEA, Eclipse, or VS Code with the Java extensions. Any
 *       editor that understands Gradle projects will work.</li>
 *   <li><b>(Optional) The Android SDK</b>, only if you want to build for Android.</li>
 * </ul>
 *
 * <h3>Generating the project</h3>
 *
 * <p>The recommended way to start is the project generator on the FlixelGDX website
 * ({@code https://flixelgdx.org/getting-started}). Fill in your game's name, an id, a Java package
 * name (such as {@code com.example.mygame}), pick Java as the language, choose the platforms you
 * want, and pick the "Blank play state" template. Download the zip and unzip it anywhere you like.
 * You will get a Gradle project with several modules:
 *
 * <pre>
 * my-game/
 *   assets/           Images, sounds, fonts, and data files shared by every platform.
 *   core/             Your actual game code. This is where you will spend almost all your time.
 *   desktop/          A tiny launcher that starts the game in a desktop window.
 *   html5/            A tiny launcher that starts the game in a web browser.
 *   android/          A tiny launcher that starts the game on Android (if you chose it).
 *   build.gradle      The root build file.
 *   settings.gradle   Lists the modules above.
 *   gradlew           The Gradle wrapper (gradlew.bat on Windows).
 * </pre>
 *
 * <p>The important idea here is the split between {@code core} and the launchers. Your game is
 * written once, in {@code core}, and it only depends on {@code flixelgdx-core}. Each platform
 * module is a thin "plug" that connects that shared game to one kind of device. You almost never
 * need to edit the launchers after the first day.
 *
 * <h3>Running the game</h3>
 *
 * <p>Open a terminal in the project folder and run one of these:
 *
 * <pre>
 * ./gradlew :desktop:run           Desktop window.
 * ./gradlew :html5:run             Web browser.
 * ./gradlew :android:installDebug  Android device or emulator.
 * </pre>
 *
 * <p>On Windows, use {@code gradlew.bat} instead of {@code ./gradlew}. The first build takes a little
 * longer because Gradle downloads the JDK and the framework. After that you should see an empty
 * game window, which means everything is wired correctly.
 *
 * <h3>The launchers</h3>
 *
 * <p>Each platform has a one-line launcher. It installs everything that platform needs (window,
 * graphics, audio, input, files, saving, logging) and then starts your game. You never start the
 * framework by hand.
 *
 * <pre>{@code
 * // desktop/src/main/java/.../DesktopLauncher.java
 * public final class DesktopLauncher {
 *   public static void main(String[] args) {
 *     FlixelDesktopLauncher.launch(new MyGame());
 *   }
 *
 *   private DesktopLauncher() {}
 * }
 *
 * // html5/src/main/java/.../WebLauncher.java
 * public final class WebLauncher {
 *   public static void main(String[] args) {
 *     FlixelHtml5Launcher.launch(new MyGame());
 *   }
 *
 *   private WebLauncher() {}
 * }
 *
 * // android/src/main/java/.../GameActivity.java
 * public final class GameActivity extends Activity {
 *   @Override
 *   protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     FlixelAndroidLauncher.launch(this, new MyGame());
 *   }
 * }
 * }</pre>
 *
 * <p>The desktop launcher can also take window icon paths, listed from smallest to largest, for
 * example {@code FlixelDesktopLauncher.launch(new MyGame(), "icons/icon16.png", "icons/icon32.png")}.
 *
 * <h3>Runtime modes</h3>
 *
 * <p>A game runs in one of three modes: release (the default), debug, or test. Debug mode turns on
 * extra checks and connects the game to the remote debugger app (see section 20). Pick it without
 * rebuilding:
 *
 * <ul>
 *   <li>Desktop: pass {@code -Dflixel.mode=debug} to the JVM.</li>
 *   <li>Web: add {@code ?flixel.mode=debug} to the page address.</li>
 *   <li>Android: any debuggable build runs in debug mode.</li>
 * </ul>
 *
 * <p>Your code can ask which mode it is in with
 * {@link org.flixelgdx.Flixel#isDebugMode() Flixel.isDebugMode()}.
 *
 * <h2>3. Your first game, step by step</h2>
 *
 * <p>A FlixelGDX game needs exactly two classes of your own to get going: a game class and a
 * first state. Let's build a tiny game where you move a square around with the arrow keys.
 *
 * <h3>Step 1: the game class</h3>
 *
 * <p>Subclass {@link org.flixelgdx.FlixelGame FlixelGame} and hand its constructor two things: a
 * {@link org.flixelgdx.FlixelConfig FlixelConfig} describing the window, and a "recipe" for the
 * first state to show.
 *
 * <pre>{@code
 * package com.example.mygame;
 *
 * import org.flixelgdx.FlixelConfig;
 * import org.flixelgdx.FlixelGame;
 *
 * public class MyGame extends FlixelGame {
 *
 *   public MyGame() {
 *     super(
 *         new FlixelConfig.Builder("My Game")
 *             .company("My Studio")    // Used to pick the save folder.
 *             .version("1.0.0")
 *             .size(640, 360)          // The design resolution, in game pixels.
 *             .framerate(60)
 *             .vsync(true)
 *             .build(),
 *         PlayState::new);             // The first state to show.
 *   }
 * }
 * }</pre>
 *
 * <p>Two details are worth understanding right away:
 *
 * <ul>
 *   <li><b>The size is a design resolution.</b> You write all your positions as if the screen is
 *       always 640 by 360 pixels. When the real window is bigger or smaller, the framework scales
 *       the picture for you, so your coordinates never change. Ask for the design size at runtime
 *       with {@link org.flixelgdx.Flixel#getDesignWidth() Flixel.getDesignWidth()} and
 *       {@link org.flixelgdx.Flixel#getDesignHeight() Flixel.getDesignHeight()}.</li>
 *   <li><b>{@code PlayState::new} is a recipe, not a state.</b> The framework asks for a
 *       {@code Supplier<FlixelState>}, a small function that builds a fresh state when called. This
 *       way the framework decides <em>when</em> the state is created (after the window and graphics
 *       are ready), and it can rebuild the state from scratch later, for example when you call
 *       {@link org.flixelgdx.Flixel#resetState() Flixel.resetState()}.</li>
 * </ul>
 *
 * <p>If you do not need a company name or version yet, there are shorter constructors:
 * {@code super("My Game", PlayState::new)} and {@code super("My Game", 640, 360, PlayState::new)}.
 *
 * <h3>Step 2: the first state</h3>
 *
 * <p>Subclass {@link org.flixelgdx.FlixelState FlixelState}. Build your scene in
 * {@link org.flixelgdx.FlixelState#create() create()} and run your game logic in
 * {@link org.flixelgdx.FlixelState#update(float) update(float)}.
 *
 * <pre>{@code
 * package com.example.mygame;
 *
 * import org.flixelgdx.Flixel;
 * import org.flixelgdx.FlixelSprite;
 * import org.flixelgdx.FlixelState;
 * import org.flixelgdx.input.keyboard.FlixelKey;
 * import org.flixelgdx.util.FlixelColor;
 *
 * public class PlayState extends FlixelState {
 *
 *   private FlixelSprite player;
 *
 *   @Override
 *   public void create() {
 *     super.create();
 *
 *     player = new FlixelSprite();
 *     player.makeGraphic(16, 16, FlixelColor.WHITE); // A plain white square.
 *     player.screenCenter();
 *     add(player); // Without add(), the sprite is never updated or drawn.
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed); // Updates every member that was add()-ed. Do not forget it!
 *
 *     float speed = 200f * elapsed; // 200 pixels per second.
 *     if (Flixel.keys.pressed(FlixelKey.LEFT)) {
 *       player.changeX(-speed);
 *     }
 *     if (Flixel.keys.pressed(FlixelKey.RIGHT)) {
 *       player.changeX(speed);
 *     }
 *     if (Flixel.keys.pressed(FlixelKey.UP)) {
 *       player.changeY(-speed);
 *     }
 *     if (Flixel.keys.pressed(FlixelKey.DOWN)) {
 *       player.changeY(speed);
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>Step 3: run it</h3>
 *
 * <p>Make sure your launcher creates {@code new MyGame()} and run {@code ./gradlew :desktop:run}.
 * You now have a white square you can move with the arrow keys. Everything else in this tutorial
 * builds on these two classes.
 *
 * <h3>The coordinate system</h3>
 *
 * <p>The origin {@code (0, 0)} is the <b>top-left</b> corner of the screen. X grows to the right
 * and Y grows <b>downward</b>, the same as most 2D drawing tools. So "move up" means making Y
 * smaller, and "gravity" means a positive Y acceleration. A sprite's {@code (x, y)} is its own
 * top-left corner.
 *
 * <h2>4. The game loop: create, update, draw</h2>
 *
 * <p>A game is a loop that runs many times per second. Each pass through the loop is a
 * <b>frame</b>, and every frame does two things in order:
 *
 * <ol>
 *   <li><b>Update.</b> Read input, move things, check collisions, run timers and tweens. Your code
 *       runs here, in {@code update(float elapsed)}.</li>
 *   <li><b>Draw.</b> Render everything that is visible. You rarely touch this: every sprite you
 *       {@code add()} draws itself.</li>
 * </ol>
 *
 * <p>Before the loop starts, the framework calls {@code create()} once on your state. When the
 * state is replaced, it calls {@code destroy()} once.
 *
 * <h3>What is {@code elapsed}?</h3>
 *
 * <p>{@code elapsed} is the number of seconds since the previous frame. At 60 frames per second it
 * is about {@code 0.0167}. Multiply speeds by it so the game runs at the same speed on a fast
 * computer and a slow one. Think of it like the difference between "walk 3 steps per frame" (the
 * walker goes faster on a faster machine) and "walk 180 steps per second" (always the same pace).
 *
 * <pre>{@code
 * // Bad: moves 5 pixels per frame, so speed depends on the frame rate.
 * player.changeX(5f);
 *
 * // Good: moves 300 pixels per second, no matter the frame rate.
 * player.changeX(300f * elapsed);
 * }</pre>
 *
 * <p>You can also read the current frame's elapsed time anywhere with
 * {@link org.flixelgdx.Flixel#getElapsed() Flixel.getElapsed()}. Built-in movement (velocity and
 * acceleration, see section 10) already uses it for you.
 *
 * <h3>Slow motion and pausing</h3>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.Flixel#timeScale Flixel.timeScale} scales the time that timers see.
 *       Set it to {@code 0.5f} for half-speed timers.</li>
 *   <li>{@link org.flixelgdx.Flixel#autoPause Flixel.autoPause} (on by default) pauses updates
 *       and audio when the window loses focus, and resumes them when it comes back. Set it to
 *       {@code false} to keep the game running in the background.</li>
 * </ul>
 *
 * <h3>Always call {@code super}</h3>
 *
 * <p>When you override {@code create()}, {@code update(float)}, {@code draw(...)}, or
 * {@code destroy()}, call the {@code super} version too. The parent class does real work in each
 * of them: {@code FlixelState.update(...)} updates every member, {@code FlixelSprite.update(...)}
 * moves the sprite and advances its animation, and {@code destroy()} releases memory. Forgetting
 * {@code super.update(elapsed)} is the number one reason a beginner's sprites "stop moving."
 *
 * <h2>5. The building blocks: basics, objects, and sprites</h2>
 *
 * <p>Nearly everything you put on screen comes from a small family of classes. Each one adds a
 * layer on top of the previous one, like Russian nesting dolls:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.FlixelBasic FlixelBasic} - the smallest doll. It can be updated and
 *       drawn, and it carries four on/off flags (below). It has no position. Use it directly for
 *       invisible "manager" objects that need to tick every frame.</li>
 *   <li>{@link org.flixelgdx.FlixelObject FlixelObject} - adds a position, a size (its hitbox),
 *       an angle, and built-in physics: velocity, acceleration, drag, and collisions. It still has
 *       no picture. Use it for invisible trigger zones and walls.</li>
 *   <li>{@link org.flixelgdx.FlixelSprite FlixelSprite} - adds a picture: an image, animations,
 *       scale, color tint, transparency, flipping, and blend modes. This is the class you will use
 *       the most by far.</li>
 * </ul>
 *
 * <p>Other visual classes build on top of {@code FlixelSprite}:
 * {@link org.flixelgdx.text.FlixelText FlixelText} (text),
 * {@link org.flixelgdx.group.FlixelSpriteGroup FlixelSpriteGroup} (several sprites moved as one),
 * and {@link org.flixelgdx.particle.FlixelParticle FlixelParticle} (particles). Because they are
 * all sprites, everything you learn about sprites also works for them.
 *
 * <h3>The four lifecycle flags</h3>
 *
 * <p>Every {@code FlixelBasic} has four public flags that control what the framework does with it
 * each frame:
 *
 * <ul>
 *   <li>{@code active} - when {@code false}, {@code update(...)} is skipped (the object freezes in
 *       place but is still drawn).</li>
 *   <li>{@code visible} - when {@code false}, {@code draw(...)} is skipped (the object is
 *       invisible but keeps updating).</li>
 *   <li>{@code alive} - a gameplay flag meaning "this thing has not been defeated." It is what
 *       {@code kill()} and {@code revive()} flip.</li>
 *   <li>{@code exists} - when {@code false}, the object is neither updated nor drawn. It is
 *       effectively switched off but kept in memory for reuse.</li>
 * </ul>
 *
 * <h3>kill(), revive(), and destroy()</h3>
 *
 * <p>These three methods sound similar but mean very different things:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.FlixelBasic#kill() kill()} switches an object off
 *       ({@code alive = false}, {@code exists = false}) <b>without</b> freeing it. It is like
 *       putting a toy back in the toy box: it is out of the game, but you can take it out again
 *       later.</li>
 *   <li>{@link org.flixelgdx.FlixelBasic#revive() revive()} takes it back out of the toy box.</li>
 *   <li>{@link org.flixelgdx.FlixelBasic#destroy() destroy()} throws the toy away for good and
 *       releases its memory. Never use an object after destroying it.</li>
 * </ul>
 *
 * <p>The golden rule: for things that come and go often (bullets, enemies, coins, particles),
 * <b>kill and revive</b> instead of creating and destroying. Section 11 shows the recycling
 * pattern that makes this easy. You rarely need to call {@code destroy()} yourself, because a
 * state destroys all of its members when it is replaced.
 *
 * <h2>6. The {@code Flixel} class: your toolbox of global services</h2>
 *
 * <p>{@link org.flixelgdx.Flixel Flixel} is a static class: you never create it, you just use its
 * fields and methods from anywhere. It is the "stage crew" from the theater analogy. These are
 * the members you will use the most:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.Flixel#switchState(java.util.function.Supplier) Flixel.switchState(...)}
 *       - move to another state (screen).</li>
 *   <li>{@link org.flixelgdx.Flixel#keys Flixel.keys} - the keyboard.</li>
 *   <li>{@link org.flixelgdx.Flixel#mouse Flixel.mouse} - the mouse.</li>
 *   <li>{@link org.flixelgdx.Flixel#touches Flixel.touches} - touch screens.</li>
 *   <li>{@link org.flixelgdx.Flixel#gamepads Flixel.gamepads} - controllers.</li>
 *   <li>{@link org.flixelgdx.Flixel#sound Flixel.sound} - sound effects and music.</li>
 *   <li>{@link org.flixelgdx.Flixel#files Flixel.files} - open files from the assets folder and
 *       other locations.</li>
 *   <li>{@link org.flixelgdx.Flixel#assets Flixel.assets} - preload and cache images, sounds, and
 *       data.</li>
 *   <li>{@link org.flixelgdx.Flixel#cameras Flixel.cameras} - the list of active cameras.</li>
 *   <li>{@link org.flixelgdx.Flixel#save Flixel.save} - a ready-to-use save file.</li>
 *   <li>{@link org.flixelgdx.Flixel#random Flixel.random} - random numbers, booleans, and
 *       picks.</li>
 *   <li>{@link org.flixelgdx.Flixel#log Flixel.log}, plus the shortcuts
 *       {@code Flixel.info(...)}, {@code Flixel.warn(...)}, {@code Flixel.error(...)}, and
 *       {@code Flixel.debug(...)} - logging.</li>
 *   <li>{@link org.flixelgdx.Flixel#debug Flixel.debug} - watches and console commands for the
 *       remote debugger.</li>
 *   <li>{@link org.flixelgdx.Flixel#window Flixel.window} - the game window (title, fullscreen,
 *       and so on).</li>
 *   <li>{@link org.flixelgdx.Flixel.Signals Flixel.Signals} - global events such as "a state is
 *       about to switch" or "the game is being suspended."</li>
 *   <li>{@link org.flixelgdx.Flixel#collide(org.flixelgdx.FlixelBasic, org.flixelgdx.FlixelBasic)
 *       Flixel.collide(...)} and
 *       {@link org.flixelgdx.Flixel#overlap(org.flixelgdx.FlixelBasic, org.flixelgdx.FlixelBasic)
 *       Flixel.overlap(...)} - collision checks.</li>
 *   <li>{@link org.flixelgdx.Flixel#quit() Flixel.quit()} - close the game.</li>
 * </ul>
 *
 * <p>Three things to know about these services:
 *
 * <ul>
 *   <li><b>They are ready once your first state is created.</b> Use them from {@code create()},
 *       {@code update(...)}, and later. Do not use them in a static initializer or in your
 *       {@code FlixelGame} constructor, because the platform has not installed them yet.</li>
 *   <li><b>They are replaceable.</b> Each service is an interface with a platform implementation
 *       behind it, so advanced users can swap in their own logger, save handler, or asset
 *       manager without forking the framework.</li>
 *   <li><b>Use them from the main thread.</b> Unless a method's documentation says otherwise,
 *       call framework APIs from your game code (your state's methods), not from your own
 *       background threads.</li>
 * </ul>
 *
 * <h2>7. States and substates: organizing your game into screens</h2>
 *
 * <h3>Switching states</h3>
 *
 * <p>Each screen of your game should be its own {@code FlixelState} subclass: {@code TitleState},
 * {@code MenuState}, {@code PlayState}, {@code GameOverState}. Move between them with
 * {@code Flixel.switchState(...)}, passing a recipe for the next state:
 *
 * <pre>{@code
 * if (Flixel.keys.justPressed(FlixelKey.ENTER)) {
 *   Flixel.switchState(PlayState::new);
 * }
 *
 * // A state that needs data can use a lambda instead.
 * Flixel.switchState(() -> new GameOverState(score));
 * }</pre>
 *
 * <p>When you switch, the old state and everything you {@code add()}-ed to it is destroyed, active
 * tweens and timers are cleared, and the new state's {@code create()} runs. That makes a state the
 * natural "unit of cleanup": if something only matters on one screen, put it in that state and you
 * never have to remember to delete it. To restart the current state from scratch (for example,
 * after the player dies), call {@link org.flixelgdx.Flixel#resetState() Flixel.resetState()}.
 *
 * <p>Carry data between states through constructor arguments (as above), through a field on your
 * game class, or through {@code Flixel.save} if it must survive a restart.
 *
 * <h3>Background color</h3>
 *
 * <p>Set the color behind everything with
 * {@link org.flixelgdx.FlixelState#setBgColor(org.flixelgdx.util.FlixelColor) setBgColor(...)} in
 * your state's {@code create()}:
 *
 * <pre>{@code
 * setBgColor(new FlixelColor(0.1f, 0.1f, 0.2f, 1f)); // A dark blue night sky.
 * }</pre>
 *
 * <h3>Substates: pause menus and pop-ups</h3>
 *
 * <p>A {@link org.flixelgdx.FlixelSubState FlixelSubState} is a smaller state that opens
 * <em>on top of</em> the current one without replacing it. It is perfect for pause menus,
 * dialog boxes, and inventories. Think of it like a sheet of glass held in front of the stage: the
 * play behind it is still there and still visible, but it is frozen until you take the glass
 * away.
 *
 * <pre>{@code
 * public class PauseSubState extends FlixelSubState {
 *
 *   public PauseSubState() {
 *     super(new FlixelColor(0f, 0f, 0f, 0.5f)); // Dim the game behind it by 50%.
 *   }
 *
 *   @Override
 *   public void create() {
 *     super.create();
 *     FlixelText label = new FlixelText(0f, 0f, 0f, "PAUSED", 32);
 *     label.screenCenter();
 *     add(label);
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
 *       close(); // Go back to the game.
 *     }
 *   }
 * }
 *
 * // In PlayState.update(...):
 * if (Flixel.keys.justPressed(FlixelKey.ESCAPE)) {
 *   openSubState(new PauseSubState());
 * }
 * }</pre>
 *
 * <p>While a substate is open, the parent state keeps drawing but stops updating. Change that with
 * the parent's {@link org.flixelgdx.FlixelState#persistentDraw persistentDraw} and
 * {@link org.flixelgdx.FlixelState#persistentUpdate persistentUpdate} flags. Substates can open
 * their own substates too, which is handy for "Are you sure you want to quit?" prompts on top of
 * a pause menu.
 *
 * <h3>Focus and window events</h3>
 *
 * <p>States can react to the window losing or regaining focus by overriding
 * {@link org.flixelgdx.FlixelState#onFocusLost() onFocusLost()} and
 * {@link org.flixelgdx.FlixelState#onFocusGained() onFocusGained()}. A common use is opening the
 * pause menu automatically when the player tabs out.
 *
 * <h2>8. Sprites in depth</h2>
 *
 * <h3>Giving a sprite a picture</h3>
 *
 * <pre>{@code
 * // From an image in the assets folder.
 * FlixelSprite player = new FlixelSprite(100f, 50f);
 * player.loadGraphic(Flixel.files.internal("images/player.png"));
 *
 * // Or in one line, through the constructor.
 * FlixelSprite tree = new FlixelSprite(300f, 80f, Flixel.files.internal("images/tree.png"));
 *
 * // A solid colored rectangle, great for prototypes, walls, and fade overlays.
 * FlixelSprite floor = new FlixelSprite(0f, 340f);
 * floor.makeGraphic(640, 20, FlixelColor.GRAY);
 * }</pre>
 *
 * <p>{@code loadGraphic(...)} resizes the sprite (and its hitbox) to match the image. To cut an
 * image into equally sized frames for animation, pass the frame size:
 * {@code loadGraphic(file, 32, 32)}.
 *
 * <h3>Position, size, and movement helpers</h3>
 *
 * <pre>{@code
 * player.setPosition(120f, 200f);
 * player.setX(10f);
 * player.changeX(5f);          // Move 5 pixels to the right.
 * player.screenCenter();       // Center on the screen on both axes.
 * player.screenCenter(FlixelAxes.X); // Center horizontally only.
 * float centerX = player.getMidpointX();
 * }</pre>
 *
 * <h3>Looks: scale, rotation, color, alpha, and flipping</h3>
 *
 * <pre>{@code
 * player.setScale(2f);                       // Twice as big (does not change the hitbox).
 * player.updateHitbox();                     // Make the hitbox match the new size.
 * player.setGraphicSize(64, 64);             // Stretch to an exact pixel size.
 * player.setAngle(45f);                      // Rotate 45 degrees clockwise.
 * player.setOriginCenter();                  // Rotate and scale around the center.
 * player.setAlpha(0.5f);                     // Half transparent.
 * player.setColor(FlixelColor.RED);          // Tint red.
 * player.setFlipX(true);                     // Mirror horizontally.
 * player.setFacing(FlixelDirectionFlags.LEFT); // Face left (flips automatically).
 * player.setBlendMode(FlixelBlendMode.ADD);  // Glowing, additive blending.
 * player.setAntialiasing(false);             // Crisp pixels for pixel art.
 * }</pre>
 *
 * <p>A key idea: <b>the hitbox and the picture are separate.</b> The hitbox
 * ({@code getX()}, {@code getY()}, {@code getWidth()}, {@code getHeight()}) is what collisions use.
 * The picture can be scaled, rotated, or shifted with
 * {@link org.flixelgdx.FlixelSprite#setOffset(float, float) setOffset(...)} without moving the
 * hitbox. This is how you give a character a hitbox smaller than its drawing (for example, so
 * that its hair does not bump into ceilings):
 *
 * <pre>{@code
 * player.loadGraphic(Flixel.files.internal("images/player.png")); // A 32 by 48 image.
 * player.setSize(20f, 40f);   // A slimmer hitbox.
 * player.setOffset(6f, 8f);   // Shift the picture so it lines up with the hitbox.
 * }</pre>
 *
 * <h3>Draw order</h3>
 *
 * <p>Objects are drawn in the order you {@code add()} them: the first added is at the back, the
 * last added is in front. Add the background first and the HUD last. If you need to reorder later,
 * groups have helpers such as {@code bringToFront(...)} and {@code sendToBack(...)}.
 *
 * <h2>9. Animation</h2>
 *
 * <p>A sprite's {@link org.flixelgdx.FlixelSprite#animation animation} controller plays named
 * clips. It is created on demand, so call
 * {@link org.flixelgdx.FlixelSprite#ensureAnimation() ensureAnimation()} once before you use it.
 *
 * <h3>From a grid spritesheet</h3>
 *
 * <p>If your frames are laid out on a grid of equally sized cells, load the sheet with a frame size
 * and list the frame indices for each clip. Indices count from the top-left cell, left to right,
 * then top to bottom.
 *
 * <pre>{@code
 * player.loadGraphic(Flixel.files.internal("images/hero.png"), 32, 32);
 * player.ensureAnimation();
 * player.animation.addAnimation("idle", new int[] {0, 1}, 0.25f);       // 4 frames per second.
 * player.animation.addAnimation("walk", new int[] {2, 3, 4, 5}, 0.1f);  // 10 frames per second.
 * player.animation.play("idle", true); // true = loop.
 *
 * // Later, in update(...):
 * player.animation.play(moving ? "walk" : "idle", true);
 * }</pre>
 *
 * <p>Calling {@code play(...)} with the clip that is already playing does nothing, so it is safe
 * to call every frame. Pass {@code forceRestart = true} through
 * {@code play(name, loop, true)} to restart a clip from its first frame.
 *
 * <h3>From a Sparrow atlas</h3>
 *
 * <p>A Sparrow atlas is a packed PNG plus an XML file naming every frame. Pass the shared base path
 * without an extension, then build clips from frame name prefixes:
 *
 * <pre>{@code
 * FlixelSprite hero = new FlixelSprite();
 * hero.ensureAnimation().addSparrowFrames(Flixel.files.internal("images/hero"));
 * hero.animation.addByPrefix("idle", "Hero Idle", 24, true);
 * hero.animation.addByPrefix("jump", "Hero Jump", 24, false);
 * hero.animation.play("idle");
 * }</pre>
 *
 * <p>For Adobe Animate rigs and animation state machines, see the
 * {@link org.flixelgdx.animation animation} package.
 *
 * <h2>10. Movement, physics, and collisions</h2>
 *
 * <h3>Built-in motion</h3>
 *
 * <p>Instead of moving objects by hand every frame, you can describe <em>how</em> they move and
 * let the framework do the math. Every {@code FlixelObject} has:
 *
 * <ul>
 *   <li><b>Velocity</b> - speed in pixels per second. The object moves by itself.</li>
 *   <li><b>Acceleration</b> - change in velocity per second. Gravity is just a constant downward
 *       acceleration.</li>
 *   <li><b>Drag</b> - slows the object down when it is not accelerating, like friction.</li>
 *   <li><b>Max velocity</b> - a speed limit.</li>
 * </ul>
 *
 * <p>A simple platformer character, using all four:
 *
 * <pre>{@code
 * // In create():
 * player.setAcceleration(0f, 900f);   // Gravity pulls down (positive Y is down).
 * player.setMaxVelocity(200f, 600f);  // Horizontal and falling speed limits.
 * player.setDrag(800f, 0f);           // Stop quickly when no key is held.
 *
 * // In update(...), before super.update(elapsed):
 * player.setAccelerationX(0f);
 * if (Flixel.keys.pressed(FlixelKey.LEFT)) {
 *   player.setAccelerationX(-1000f);
 * } else if (Flixel.keys.pressed(FlixelKey.RIGHT)) {
 *   player.setAccelerationX(1000f);
 * }
 * if (Flixel.keys.justPressed(FlixelKey.SPACE) && player.isTouching(FlixelDirectionFlags.FLOOR)) {
 *   player.setVelocityY(-350f); // Jump: a quick upward push.
 * }
 * }</pre>
 *
 * <p>There are rotation equivalents too: {@code setAngularVelocity(...)},
 * {@code setAngularAcceleration(...)}, and {@code setAngularDrag(...)}. Turn built-in motion off
 * for one object with {@code setMoves(false)}.
 *
 * <h3>Collisions</h3>
 *
 * <p>There are two kinds of collision checks, and choosing the right one matters:
 *
 * <ul>
 *   <li>{@code Flixel.overlap(a, b)} only <b>asks</b> whether things touch. Nothing is moved. Use it
 *       for pickups, damage zones, and triggers.</li>
 *   <li>{@code Flixel.collide(a, b)} touches <b>and pushes apart</b>. Use it for solid things such
 *       as walls, floors, and crates.</li>
 * </ul>
 *
 * <p>Either argument can be a single object or a whole group, so one call can check the player
 * against every wall at once. Both return {@code true} if anything touched, and both accept an
 * optional callback that runs once for every touching pair:
 *
 * <pre>{@code
 * // Walls and floors: push the player out.
 * Flixel.collide(player, walls);
 *
 * // Coins: just detect it, then react.
 * Flixel.overlap(player, coins, (p, coin) -> {
 *   coin.kill();
 *   score++;
 * }, null);
 * }</pre>
 *
 * <p>Some settings change how collisions resolve:
 *
 * <ul>
 *   <li>{@code setImmovable(true)} - the object never gets pushed. Floors and moving platforms
 *       should be immovable.</li>
 *   <li>{@code setAllowCollisions(FlixelDirectionFlags.UP)} - only collide on certain sides, which
 *       is how you make a one-way platform you can jump up through.</li>
 *   <li>{@code setElasticity(0.8f)} - bounciness, from {@code 0} (no bounce) to {@code 1}.</li>
 *   <li>{@code setMass(...)} - heavier objects push lighter ones more.</li>
 * </ul>
 *
 * <p>After a collision, {@link org.flixelgdx.FlixelObject#isTouching(int) isTouching(...)} tells you
 * which sides are touching something this frame, and
 * {@link org.flixelgdx.FlixelObject#justTouched(int) justTouched(...)} tells you if that contact
 * just started (good for landing sounds). Use the constants in
 * {@link org.flixelgdx.util.FlixelDirectionFlags FlixelDirectionFlags}, such as {@code FLOOR},
 * {@code CEILING}, {@code LEFT}, {@code RIGHT}, and {@code ANY}.
 *
 * <p>Collision callbacks are lambdas. If you check collisions every frame, store the lambda in a
 * field once instead of writing it inline, so a new one is not created each frame (see section
 * 21).
 *
 * <h2>11. Groups and recycling</h2>
 *
 * <h3>Why groups?</h3>
 *
 * <p>A group is a container of objects that is itself an object. Adding a group to a state updates
 * and draws every member. Groups let you treat many things as one: collide the player against
 * "all walls" in one call, kill "all enemies" when a level ends, or put "all bullets" on one layer.
 * A {@code FlixelState} is itself a group, which is why {@code add(...)} works in it.
 *
 * <p>There are three group types, from simplest to fanciest:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.group.FlixelGroup FlixelGroup} - just an organized list. You drive it
 *       yourself.</li>
 *   <li>{@link org.flixelgdx.group.FlixelBasicGroup FlixelBasicGroup} - a list of game objects that
 *       updates and draws its members. This is the everyday choice.</li>
 *   <li>{@link org.flixelgdx.group.FlixelSpriteGroup FlixelSpriteGroup} - a group that is also a
 *       sprite: moving, fading, or scaling the group moves, fades, or scales every child. Use it for
 *       things built from several pieces, such as a character with a separate hat.</li>
 * </ul>
 *
 * <h3>Recycling: the toy box pattern</h3>
 *
 * <p>Creating a brand new object every time the player fires a bullet is slow and fills memory
 * with garbage that the computer then has to clean up, which can cause small stutters. Instead,
 * keep a group of bullets and <b>recycle</b> them: when a bullet leaves the screen, {@code kill()}
 * it, and when you need a new one, ask the group for a dead one to revive. A new bullet is only
 * created when no dead one is available.
 *
 * <pre>{@code
 * public class Bullet extends FlixelSprite {
 *
 *   public Bullet() {
 *     makeGraphic(4, 4, FlixelColor.YELLOW);
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     if (getY() < 0 - getHeight()) {
 *       kill(); // Off the top of the screen: back in the toy box.
 *     }
 *   }
 * }
 *
 * // In PlayState:
 * private FlixelBasicGroup<Bullet> bullets;
 *
 * @Override
 * public void create() {
 *   super.create();
 *   // Up to 32 bullets. The group calls createMemberForRecycle() when it needs a new one.
 *   bullets = new FlixelBasicGroup<>(Bullet[]::new, 32) {
 *     @Override
 *     protected Bullet createMemberForRecycle() {
 *       return new Bullet();
 *     }
 *   };
 *   add(bullets);
 * }
 *
 * private void shoot() {
 *   Bullet bullet = bullets.recycle(); // A revived old bullet, a new one, or null when full.
 *   if (bullet != null) {
 *     bullet.setPosition(player.getMidpointX(), player.getY());
 *     bullet.setVelocity(0f, -500f);
 *   }
 * }
 * }</pre>
 *
 * <p>Remember that {@code recycle()} hands back an <em>old</em> object, so always reset everything
 * that might still hold an old value (position, velocity, health, animation) right after you get
 * it. {@link org.flixelgdx.FlixelObject#reset(float, float) reset(x, y)} is a convenient way to
 * revive an object and move it in one call.
 *
 * <p>Removing a member with {@code remove(...)} only takes it out of the group; it does not destroy
 * it. Destroying a group destroys all of its members.
 *
 * <h3>Particles</h3>
 *
 * <p>For sparks, smoke, dust, and confetti, use a
 * {@link org.flixelgdx.particle.FlixelEmitter FlixelEmitter}. It is a specialized recycling group
 * that launches many short-lived particles with random ranges of speed, angle, color, and so on,
 * without creating any objects while the game runs:
 *
 * <pre>{@code
 * FlixelEmitter<FlixelParticle> sparks = new FlixelEmitter<>(64, FlixelParticle::new);
 * sparks.loadGraphic(Flixel.files.internal("images/spark.png"));
 * sparks.speed.set(100f, 250f);
 * sparks.acceleration.set(0f, 400f); // Gravity.
 * add(sparks);
 *
 * // When something explodes:
 * sparks.setPosition(enemy.getMidpointX(), enemy.getMidpointY());
 * sparks.start(true, 0f, 20); // Burst 20 sparks at once.
 * }</pre>
 *
 * <h2>12. Input: keyboard, mouse, touch, and gamepads</h2>
 *
 * <p>Input in FlixelGDX is <b>polled</b>: instead of waiting for events, you ask "what is the state
 * of this key right now?" inside {@code update(...)}. Every device answers the same three
 * questions:
 *
 * <ul>
 *   <li>{@code pressed(...)} - is it held down this frame? Use it for continuous actions like
 *       walking.</li>
 *   <li>{@code justPressed(...)} - did it go down this exact frame? Use it for one-shot actions
 *       like jumping, shooting, or confirming a menu choice.</li>
 *   <li>{@code justReleased(...)} - did it come up this exact frame? Use it for charge-up
 *       attacks.</li>
 * </ul>
 *
 * <h3>Keyboard</h3>
 *
 * <pre>{@code
 * if (Flixel.keys.pressed(FlixelKey.D)) {
 *   player.changeX(150f * elapsed);
 * }
 * if (Flixel.keys.justPressed(FlixelKey.SPACE)) {
 *   jump();
 * }
 * // Check several keys at once (no array is created for up to six keys).
 * if (Flixel.keys.anyJustPressed(FlixelKey.ENTER, FlixelKey.Z, FlixelKey.SPACE)) {
 *   confirm();
 * }
 * }</pre>
 *
 * <p>Key codes live in {@link org.flixelgdx.input.keyboard.FlixelKey FlixelKey}.
 * {@code FlixelKey.ANY} matches any key, which is handy for "press any key to start."
 *
 * <h3>Mouse</h3>
 *
 * <pre>{@code
 * if (Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   // Where in the game world was the click? (This accounts for camera scroll and zoom.)
 *   spawnAt(Flixel.mouse.getWorldX(), Flixel.mouse.getWorldY());
 * }
 * if (Flixel.mouse.overlap(playButton) && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)) {
 *   Flixel.switchState(PlayState::new);
 * }
 * float wheel = Flixel.mouse.getScrollDeltaY();
 * }</pre>
 *
 * <p>Use the <b>world</b> position for things in the level and the <b>screen</b> position
 * ({@code getScreenX()}, {@code getScreenY()}) for HUD elements that do not scroll.
 *
 * <h3>Touch</h3>
 *
 * <p>Touch input is switched on automatically on Android. On desktop and the web it starts switched
 * off; turn it on with {@code Flixel.touches.enabled = true} if your game supports touchscreen
 * monitors or phone browsers.
 *
 * <pre>{@code
 * if (Flixel.touches.anyJustPressed()) {
 *   jump();
 * }
 * // Was the left half of the screen just tapped?
 * if (Flixel.touches.justTouchedScreen(0f, 0f, Flixel.getDesignWidth() / 2f, Flixel.getDesignHeight())) {
 *   moveLeft();
 * }
 * }</pre>
 *
 * <h3>Gamepads</h3>
 *
 * <pre>{@code
 * if (Flixel.gamepads.anyJustPressed(FlixelGamepadButton.A)) {
 *   jump();
 * }
 * FlixelGamepadDevice pad = Flixel.gamepads.getFirstActiveGamepad();
 * if (pad != null) {
 *   player.setVelocityX(pad.getXAxis() * 200f);
 * }
 * }</pre>
 *
 * <h3>Actions: one control scheme for every device</h3>
 *
 * <p>Checking the keyboard, the gamepad, and touch separately for every action gets repetitive. The
 * {@link org.flixelgdx.input.action action} package lets you define named actions such as "jump"
 * or "move" once, bind them to any number of keys, buttons, and sticks, and then ask only about the
 * action. It also makes rebindable controls much easier to build.
 *
 * <h2>13. Text and HUDs</h2>
 *
 * <p>{@link org.flixelgdx.text.FlixelText FlixelText} draws text. It is a sprite, so you can move,
 * tint, fade, scale, and tween it like any other sprite.
 *
 * <pre>{@code
 * // x, y, field width (0 = grow to fit), text, size in pixels.
 * FlixelText title = new FlixelText(0f, 40f, 0f, "My Awesome Game", 32);
 * title.setFormat(32, FlixelColor.YELLOW);
 * title.setBorderStyle(FlixelText.BorderStyle.OUTLINE, FlixelColor.BLACK);
 * title.screenCenter(FlixelAxes.X);
 * add(title);
 *
 * // A fixed-width, word-wrapped paragraph.
 * FlixelText dialog = new FlixelText(20f, 280f, 600f, "Welcome, traveler!", 16);
 * dialog.setWordWrap(true);
 * add(dialog);
 * }</pre>
 *
 * <p>For custom fonts, point {@code setFont(...)} at a {@code .ttf} or {@code .otf} file in your
 * assets, or register fonts once in {@link org.flixelgdx.text.FlixelFontRegistry FlixelFontRegistry}
 * and select them by id.
 *
 * <h3>Text that changes every frame</h3>
 *
 * <p>Building a new {@code String} like {@code "Score: " + score} every frame creates garbage. For
 * a score, timer, or FPS counter, keep one reusable
 * {@link org.flixelgdx.util.FlixelString FlixelString} and write numbers into it:
 *
 * <pre>{@code
 * private final FlixelString scoreBuffer = new FlixelString();
 * private FlixelText scoreText;
 *
 * // In update(...):
 * scoreBuffer.set("Score: ").concat(score);
 * scoreText.setText(scoreBuffer); // No new String is created.
 * }</pre>
 *
 * <p>To keep the HUD still while the world camera scrolls, give it its own camera (see section 16).
 * For bars such as health and loading progress, see
 * {@link org.flixelgdx.util.FlixelBar FlixelBar}.
 *
 * <h2>14. Sound and music</h2>
 *
 * <pre>{@code
 * // A one-shot sound effect.
 * Flixel.sound.play(Flixel.files.internal("sounds/jump.ogg"));
 *
 * // Quieter, at 50% volume.
 * Flixel.sound.play(Flixel.files.internal("sounds/coin.ogg"), 0.5f);
 *
 * // Background music, looping. Only one music track plays at a time.
 * Flixel.sound.playMusic(Flixel.files.internal("music/theme.ogg"), 0.8f, true);
 *
 * // Fade the music out over one second.
 * Flixel.sound.music.fadeOut(1f);
 *
 * // Overall volume for everything.
 * Flixel.sound.setMasterVolume(0.7f);
 * }</pre>
 *
 * <p>{@code play(...)} returns a {@link org.flixelgdx.audio.FlixelSound FlixelSound} you can keep
 * to control it later: {@code pause()}, {@code resume()}, {@code stop()}, {@code setVolume(...)},
 * {@code setPitch(...)}, {@code setPan(...)}, {@code fadeIn(...)}, and an {@code onComplete} signal
 * that fires when it finishes. Sound groups ({@link org.flixelgdx.audio.FlixelSoundGroup
 * FlixelSoundGroup}) let you control volumes by category, so you can offer separate "music" and
 * "effects" sliders in an options menu.
 *
 * <h2>15. Files and assets</h2>
 *
 * <h3>Files</h3>
 *
 * <p>{@link org.flixelgdx.Flixel#files Flixel.files} hands out
 * {@link org.flixelgdx.file.FlixelFile FlixelFile} handles. A handle is just an address: creating
 * one is cheap and does not touch the disk until you read from it. The root you pick says where to
 * look:
 *
 * <ul>
 *   <li>{@code Flixel.files.internal("images/player.png")} - inside your {@code assets/} folder.
 *       This is what you want 95% of the time.</li>
 *   <li>{@code Flixel.files.external(...)} - in the user's home directory.</li>
 *   <li>{@code Flixel.files.local(...)}, {@code absolute(...)}, and {@code classpath(...)} - for
 *       less common cases.</li>
 * </ul>
 *
 * <pre>{@code
 * FlixelFile levelFile = Flixel.files.internal("data/level1.json");
 * if (levelFile.exists()) {
 *   String json = levelFile.readString();
 * }
 * }</pre>
 *
 * <p>Use forward slashes in paths on every platform, and keep file names lowercase: the web and
 * Android are case-sensitive even when your desktop is not.
 *
 * <h3>Assets: preloading and caching</h3>
 *
 * <p>{@link org.flixelgdx.Flixel#assets Flixel.assets} loads files into memory once and shares them.
 * You do not have to use it directly, since {@code loadGraphic(...)} and {@code Flixel.sound} load
 * what they need. But loading lots of large files at the moment they are first used can cause a
 * hitch, so bigger games usually <b>preload</b> them on a loading screen:
 *
 * <pre>{@code
 * public class LoadingState extends FlixelState {
 *
 *   @Override
 *   public void create() {
 *     super.create();
 *     Flixel.assets.load("images/player.png");
 *     Flixel.assets.load("images/tiles.png");
 *     Flixel.assets.load("music/theme.ogg");
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     // update() loads a little each frame and returns true when everything is ready.
 *     if (Flixel.assets.update()) {
 *       Flixel.switchState(MenuState::new);
 *     }
 *     // Flixel.assets.getProgress() goes from 0 to 1 for a loading bar.
 *   }
 * }
 * }</pre>
 *
 * <p>Loaded assets stay in memory until you call {@code Flixel.assets.unload(path)}. Nothing is
 * thrown away automatically when you switch states, so you stay in control of when memory is
 * freed. See the {@link org.flixelgdx.asset asset} package for reference counting and custom asset
 * types.
 *
 * <h2>16. Cameras</h2>
 *
 * <p>The game starts with one camera that covers the whole window. Get it with
 * {@code Flixel.cameras.first()}. Moving a camera does not move the objects in the world; it moves
 * <em>the view</em>, the same way a film camera pans across a set.
 *
 * <pre>{@code
 * FlixelCamera camera = Flixel.cameras.first();
 *
 * // Follow the player, with a style suited to the genre.
 * camera.follow(player, FlixelCamera.FollowStyle.PLATFORMER);
 *
 * // Do not scroll past the edges of the level (minX, maxX, minY, maxY).
 * camera.setScrollBounds(0f, levelWidth, 0f, levelHeight);
 *
 * camera.setZoom(2f);                         // Zoom in.
 * camera.shake(0.01f, 0.3f);                  // Shake for 0.3 seconds, like an explosion hit.
 * camera.flash(FlixelColor.WHITE, 0.2f);      // A quick white flash.
 * camera.fade(FlixelColor.BLACK, 1f);         // Fade to black over one second.
 * }</pre>
 *
 * <p>A fade with a callback is the classic way to transition between states smoothly:
 *
 * <pre>{@code
 * camera.fade(FlixelColor.BLACK, 0.5f, false, () -> Flixel.switchState(NextState::new), false);
 * }</pre>
 *
 * <h3>Parallax and HUD cameras</h3>
 *
 * <p>{@code setScrollFactor(x, y)} controls how much an object moves when the camera scrolls.
 * {@code 1} is normal, {@code 0.5} moves at half speed (a distant background, for a parallax
 * effect), and {@code 0} never moves (a quick way to pin something to the screen).
 *
 * <p>For a real HUD, use a second camera that never scrolls. Turning off
 * {@code defaultDrawTarget} keeps world objects off it, and HUD objects opt in through their
 * {@code cameras} field:
 *
 * <pre>{@code
 * FlixelCamera hud = new FlixelCamera(Flixel.getDesignWidth(), Flixel.getDesignHeight());
 * hud.bgColor.setAlpha(0f);      // Transparent, so the world shows through.
 * hud.useBgAlphaBlending = true; // Don't forget this to allow the alpha value to render properly!
 * hud.defaultDrawTarget = false; // World objects stay on the main camera.
 * Flixel.cameras.add(hud);
 *
 * scoreText.cameras = new FlixelCamera[] {hud};
 * }</pre>
 *
 * <h2>17. Tweens and timers</h2>
 *
 * <h3>Tweens: smooth changes over time</h3>
 *
 * <p>A <b>tween</b> (short for "in-between") smoothly changes a value from where it is now to a
 * target over a duration. Fading in a title, sliding a menu onto the screen, or bouncing a coin
 * are all one-liners with tweens. An <b>ease</b> function shapes the motion: linear is robotic,
 * while "out" eases start fast and land gently.
 *
 * <pre>{@code
 * // Slide the title to y = 80 over 1 second, landing with a bounce.
 * FlixelTween.tween(title, new FlixelTweenSettings()
 *     .addGoal(title::getY, 80f, title::setY)
 *     .setDuration(1f)
 *     .setEase(FlixelEase::bounceOut));
 *
 * // Fade a sprite in, then run code when it finishes.
 * logo.setAlpha(0f);
 * FlixelTween.tween(logo, new FlixelTweenSettings()
 *     .addGoal(logo::getAlpha, 1f, logo::setAlpha)
 *     .setDuration(0.5f)
 *     .setOnComplete(tween -> Flixel.info("Logo is visible!")));
 *
 * // Make a coin bob up and down forever.
 * FlixelTween.tween(coin, new FlixelTweenSettings(FlixelTweenType.PINGPONG)
 *     .addGoal(coin::getY, coin.getY() - 6f, coin::setY)
 *     .setDuration(0.6f)
 *     .setEase(FlixelEase::sineInOut));
 *
 * // Ready-made effects.
 * FlixelTween.shake(player, FlixelAxes.XY, 0.02f, new FlixelTweenSettings().setDuration(0.2f));
 * FlixelTween.flicker(player, new FlixelTweenSettings().setDuration(1f)); // Blink after being hit.
 * }</pre>
 *
 * <p>Tween types control what happens at the end: {@code ONESHOT} (the default) runs once,
 * {@code LOOPING} restarts, {@code PINGPONG} goes back and forth, {@code BACKWARD} runs in reverse,
 * and {@code PERSIST} stays around so you can restart it. Cancel tweens on an object with
 * {@code FlixelTween.cancelTweensOf(object)}. All tweens are cleared when the state switches.
 *
 * <h3>Timers: do something later</h3>
 *
 * <pre>{@code
 * // Once, after 2 seconds.
 * FlixelTimer.wait(2f, timer -> spawnBoss());
 *
 * // Every 0.5 seconds, 10 times. Pass 0 to repeat forever.
 * FlixelTimer.loop(0.5f, timer -> spawnEnemy(), 10);
 * }</pre>
 *
 * <p>Both return the {@link org.flixelgdx.timer.FlixelTimer FlixelTimer}, which you can keep to
 * cancel it early. Timers are paused along with the game and cleared when the state switches, so
 * they never fire on the wrong screen. Prefer them over counting {@code elapsed} by hand.
 *
 * <h2>18. Signals (events)</h2>
 *
 * <p>A {@link org.flixelgdx.signal.FlixelSignal FlixelSignal} is a list of listeners you can notify
 * all at once. It is like a school bell: when it rings, every class that cares hears it, and the
 * bell does not need to know who is listening. Signals keep code decoupled: the player does not
 * need to know about the HUD, the sound system, and the achievement tracker; it just rings its
 * "died" bell.
 *
 * <pre>{@code
 * // In your Player class:
 * public final FlixelSignal<Integer> onHurt = new FlixelSignal<>();
 *
 * public void hurt(int damage) {
 *   health -= damage;
 *   onHurt.dispatch(health);
 * }
 *
 * // Somewhere else, such as the HUD:
 * player.onHurt.add(health -> healthBar.setValue(health));
 * }</pre>
 *
 * <p>Use {@code addOnce(...)} for a listener that should only run one time, and {@code remove(...)}
 * to stop listening. The framework's own events live in
 * {@link org.flixelgdx.Flixel.Signals Flixel.Signals}, for example:
 *
 * <pre>{@code
 * Flixel.Signals.preStateSwitch.add(data -> Flixel.info("Switching states..."));
 * Flixel.Signals.gameSuspended.add(data -> Flixel.save.flush()); // Save before a phone kills the app.
 * }</pre>
 *
 * <p>Listeners added to the global signals stay until you remove them, even across state
 * switches. Remove listeners that belong to a single state in that state's {@code destroy()}.
 *
 * <h2>19. Saving progress</h2>
 *
 * <p>{@link org.flixelgdx.Flixel#save Flixel.save} is a key-value store that survives restarts.
 * The same code works everywhere: it writes to the app data folder on desktop, private storage on
 * Android, and browser storage on the web.
 *
 * <pre>{@code
 * // Once, early (for example in your first state's create()):
 * Flixel.save.bind("MyGame", "slot1");
 *
 * // Read, with a default for the very first run.
 * int best = Flixel.save.getInt("highScore", 0);
 * boolean musicOn = Flixel.save.getBool("musicOn", true);
 *
 * // Write, then flush to actually store it.
 * if (score > best) {
 *   Flixel.save.data.put("highScore", score);
 *   Flixel.save.flush();
 * }
 * }</pre>
 *
 * <p>Supported values are strings, numbers, booleans, and nested
 * {@link org.flixelgdx.collections.FlixelMap FlixelMap} and
 * {@link org.flixelgdx.collections.FlixelArray FlixelArray} values. Flush as soon as something
 * important changes: browsers and phones often stop a game without warning, so waiting until the
 * game closes is not safe. Use a different slot name for each save slot, and {@code erase()} to
 * start over. For your own data classes, the {@link org.flixelgdx.json json} package can turn them
 * into JSON and back.
 *
 * <h2>20. Logging and debugging</h2>
 *
 * <h3>Logging</h3>
 *
 * <pre>{@code
 * Flixel.info("Level {} started.", levelNumber);
 * Flixel.warn("Player health is low: {}", health);
 * Flixel.error("Could not load the level file!");
 * Flixel.debug("Jump pressed."); // The lowest, most detailed level.
 *
 * // A tagged logger for a class that logs a lot, so you can tell where messages come from.
 * private static final FlixelLogger LOG = Flixel.log.tagged("PlayState");
 * }</pre>
 *
 * <p>The <code>{}</code> markers are filled in with the arguments, in order. Prefer them over string
 * concatenation: the logger fills them in without creating a new string for every call, and skips
 * the work entirely when that log level is turned off.
 *
 * <h3>The remote debugger</h3>
 *
 * <p>Run the game in debug mode (see section 2) and it connects to the FlixelGDX debugger app,
 * which shows frame times, memory, the log, a tree of every object in the state, an inspector that
 * edits values live, and more. Add your own values and commands with one line each:
 *
 * <pre>{@code
 * Flixel.debug.watches.addFloat("Player X", player::getX);
 * Flixel.debug.console.register("heal", args -> player.heal()); // heal() is your own method.
 * }</pre>
 *
 * <p>Nothing runs and nothing is sent in release builds, so you can leave these lines in.
 *
 * <h2>21. Writing fast code: the framework's performance habits</h2>
 *
 * <p>Games redo the same work 60 or more times per second, so small waste adds up fast. The most
 * common cause of stutter in Java games is <b>garbage</b>: objects created and thrown away every
 * frame, which the computer must periodically stop and clean up. FlixelGDX is designed to make
 * garbage-free code easy. Follow these habits and your game will stay smooth, even on phones and
 * in the browser:
 *
 * <ol>
 *   <li><b>Create objects in {@code create()}, not in {@code update(...)}.</b> If you need a
 *       temporary vector, rectangle, or color every frame, make it a field and reuse it.</li>
 *   <li><b>Recycle instead of creating and destroying.</b> Use {@code kill()}, {@code revive()},
 *       and {@code recycle()} (section 11), or a
 *       {@link org.flixelgdx.collections.FlixelPool FlixelPool} for plain objects.</li>
 *   <li><b>Use the framework's collections.</b> The
 *       {@link org.flixelgdx.collections collections} package
 *       ({@code FlixelArray}, {@code FlixelMap}, {@code FlixelIntArray}, and friends) is built to
 *       avoid hidden allocations. For example, a for-each loop over a {@code FlixelArray} reuses
 *       its iterator instead of making a new one.</li>
 *   <li><b>Use {@code FlixelString} for changing text</b> instead of {@code String}
 *       concatenation (section 13).</li>
 *   <li><b>Store lambdas you pass every frame in fields.</b> A capturing lambda written inline in
 *       {@code update(...)} can create a new object each frame:
 *       <pre>{@code
 * // Created once, reused every frame.
 * private final BiConsumer<FlixelObject, FlixelObject> onCoin = (p, coin) -> coin.kill();
 *
 * // In update(...):
 * Flixel.overlap(player, coins, onCoin, null);
 *       }</pre></li>
 *   <li><b>Prefer indexed {@code for} loops over streams</b> in per-frame code.</li>
 *   <li><b>Preload big assets</b> on a loading screen instead of loading them the moment they are
 *       first shown (section 15).</li>
 * </ol>
 *
 * <h2>22. "How do I...?" cookbook</h2>
 *
 * <dl>
 *   <dt>...change the window title, size, or frame rate?</dt>
 *   <dd>Set them on the {@code FlixelConfig.Builder} in your game class (section 3). Use
 *       {@link org.flixelgdx.Flixel#window Flixel.window} for changes while the game runs.</dd>
 *
 *   <dt>...go to another screen?</dt>
 *   <dd>{@code Flixel.switchState(NextState::new);}</dd>
 *
 *   <dt>...restart the level?</dt>
 *   <dd>{@code Flixel.resetState();}</dd>
 *
 *   <dt>...pause the game?</dt>
 *   <dd>Open a {@code FlixelSubState} with {@code openSubState(new PauseSubState())} (section
 *       7).</dd>
 *
 *   <dt>...quit the game?</dt>
 *   <dd>{@code Flixel.quit();}</dd>
 *
 *   <dt>...show an image?</dt>
 *   <dd>{@code add(new FlixelSprite(x, y, Flixel.files.internal("images/thing.png")));}</dd>
 *
 *   <dt>...make a placeholder rectangle?</dt>
 *   <dd>{@code sprite.makeGraphic(32, 32, FlixelColor.RED);}</dd>
 *
 *   <dt>...center something on the screen?</dt>
 *   <dd>{@code sprite.screenCenter();}</dd>
 *
 *   <dt>...hide something without deleting it?</dt>
 *   <dd>{@code sprite.setVisible(false);} to hide it while it keeps updating, or
 *       {@code sprite.kill();} to switch it off completely.</dd>
 *
 *   <dt>...remove an enemy that was defeated?</dt>
 *   <dd>{@code enemy.kill();} If it is in a group, the group can {@code recycle()} it later.</dd>
 *
 *   <dt>...make something fall?</dt>
 *   <dd>{@code sprite.setAccelerationY(900f);} (positive Y is down).</dd>
 *
 *   <dt>...stop the player from walking through walls?</dt>
 *   <dd>Make walls immovable with {@code wall.setImmovable(true)}, put them in a group, and call
 *       {@code Flixel.collide(player, walls);} every frame.</dd>
 *
 *   <dt>...know if the player is standing on the ground?</dt>
 *   <dd>{@code player.isTouching(FlixelDirectionFlags.FLOOR)}, after calling
 *       {@code Flixel.collide(...)}.</dd>
 *
 *   <dt>...pick up coins?</dt>
 *   <dd>{@code Flixel.overlap(player, coins, onCoin, null);} where {@code onCoin} kills the coin
 *       and adds to the score.</dd>
 *
 *   <dt>...react to a single key press?</dt>
 *   <dd>{@code Flixel.keys.justPressed(FlixelKey.SPACE)}. Use {@code pressed(...)} for keys that
 *       are held.</dd>
 *
 *   <dt>...know where the mouse is in the level?</dt>
 *   <dd>{@code Flixel.mouse.getWorldX()} and {@code Flixel.mouse.getWorldY()}.</dd>
 *
 *   <dt>...play a sound effect or music?</dt>
 *   <dd>{@code Flixel.sound.play(file);} and {@code Flixel.sound.playMusic(file, 1f, true);}</dd>
 *
 *   <dt>...show a score that updates?</dt>
 *   <dd>A {@code FlixelText} plus a reusable {@code FlixelString} (section 13).</dd>
 *
 *   <dt>...keep the HUD from scrolling?</dt>
 *   <dd>Put it on a HUD camera, or for a quick fix use {@code setScrollFactor(0f, 0f)}.</dd>
 *
 *   <dt>...make the camera follow the player?</dt>
 *   <dd>{@code Flixel.cameras.first().follow(player, FlixelCamera.FollowStyle.PLATFORMER);}</dd>
 *
 *   <dt>...shake the screen?</dt>
 *   <dd>{@code Flixel.cameras.first().shake(0.01f, 0.25f);}</dd>
 *
 *   <dt>...fade out between screens?</dt>
 *   <dd>A camera {@code fade(...)} with a callback that calls {@code Flixel.switchState(...)}
 *       (section 16).</dd>
 *
 *   <dt>...animate a value smoothly?</dt>
 *   <dd>{@code FlixelTween.tween(...)} with a {@code FlixelTweenSettings} goal (section 17).</dd>
 *
 *   <dt>...run code after a delay?</dt>
 *   <dd>{@code FlixelTimer.wait(seconds, timer -> doSomething());}</dd>
 *
 *   <dt>...pick something at random?</dt>
 *   <dd>Use {@link org.flixelgdx.Flixel#random Flixel.random}, for example for random spawn
 *       positions or loot rolls.</dd>
 *
 *   <dt>...save a high score?</dt>
 *   <dd>{@code Flixel.save.data.put("highScore", score); Flixel.save.flush();} after binding once
 *       (section 19).</dd>
 *
 *   <dt>...print a message for debugging?</dt>
 *   <dd>{@code Flixel.info("x is {}", x);}</dd>
 *
 *   <dt>...make pixel art look crisp?</dt>
 *   <dd>{@code sprite.setAntialiasing(false);}, or turn antialiasing off globally with
 *       {@link org.flixelgdx.Flixel#setAntialiasing(boolean) Flixel.setAntialiasing(false)}.</dd>
 *
 *   <dt>...give the game a background color?</dt>
 *   <dd>{@code setBgColor(...)} in your state's {@code create()}.</dd>
 * </dl>
 *
 * <h2>23. Where to go next</h2>
 *
 * <p>You now know enough to build a complete small game. When you want to go deeper, each package
 * has its own guide:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.animation animation} - spritesheets, Sparrow atlases, Adobe Animate
 *       rigs, and animation state machines.</li>
 *   <li>{@link org.flixelgdx.group group} - group types, iteration, sorting, and recycling.</li>
 *   <li>{@link org.flixelgdx.input input} - every input device and the action system.</li>
 *   <li>{@link org.flixelgdx.audio audio} - sounds, music, groups, and effects.</li>
 *   <li>{@link org.flixelgdx.asset asset} and {@link org.flixelgdx.file file} - loading, caching,
 *       and file locations.</li>
 *   <li>{@link org.flixelgdx.tween tween} and {@link org.flixelgdx.timer timer} - every tween type
 *       and ease.</li>
 *   <li>{@link org.flixelgdx.text text} - fonts and text layout.</li>
 *   <li>{@link org.flixelgdx.particle particle} - emitters and particles.</li>
 *   <li>{@link org.flixelgdx.graphics graphics} - shaders, blend modes, and rendering.</li>
 *   <li>{@link org.flixelgdx.math math} and {@link org.flixelgdx.util util} - vectors, rectangles,
 *       random numbers, colors, and helpers.</li>
 *   <li>{@link org.flixelgdx.collections collections} - the garbage-free collections.</li>
 *   <li>{@link org.flixelgdx.save save} and {@link org.flixelgdx.json json} - saving and
 *       serialization.</li>
 *   <li>{@link org.flixelgdx.signal signal} - events.</li>
 *   <li>{@link org.flixelgdx.debug debug} and {@link org.flixelgdx.logging logging} - the remote
 *       debugger and logging.</li>
 *   <li>{@link org.flixelgdx.backend backend} - for power users who want to replace a platform
 *       service or add a platform.</li>
 * </ul>
 *
 * <p>Have fun creating! Now go make something great. &lt;3
 *
 * @see org.flixelgdx.Flixel
 * @see org.flixelgdx.FlixelGame
 * @see org.flixelgdx.FlixelState
 * @see org.flixelgdx.FlixelSprite
 */
package org.flixelgdx;
