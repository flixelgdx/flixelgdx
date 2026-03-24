package me.stringdotjar.flixelgdx.backend.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.ios.alert.FlixelIOSAlerter;
import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelDefaultReflectionHandler;
import me.stringdotjar.flixelgdx.backend.runtime.FlixelRuntimeMode;

/**
 * Launches the iOS (RoboVM) version of the FlixelGDX game.
 *
 * <p>The developer creates a subclass of {@link FlixelGame} and an iOS launcher class that
 * extends {@link com.badlogic.gdx.backends.iosrobovm.IOSApplication.Delegate}. In
 * {@link com.badlogic.gdx.backends.iosrobovm.IOSApplication.Delegate#createApplication()},
 * create the game instance and return {@link #launch(FlixelGame)}.
 */
public class FlixelIOSLauncher {

  /**
   * Launches the iOS version of the game in {@link FlixelRuntimeMode#RELEASE RELEASE} mode.
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @return The configured {@link IOSApplication} to return from {@code createApplication()}.
   */
  public static IOSApplication launch(FlixelGame game) {
    return launch(game, FlixelRuntimeMode.RELEASE);
  }

  /**
   * Launches the iOS version of the game with the given runtime mode.
   *
   * <p>Use this from your {@link com.badlogic.gdx.backends.iosrobovm.IOSApplication.Delegate}
   * implementation:
   *
   * <pre>{@code
   * public class MyIOSLauncher extends IOSApplication.Delegate {
   *   @Override
   *   protected IOSApplication createApplication() {
   *     return FlixelIOSLauncher.launch(
   *         new MyGame("My Game", 800, 600, new InitialState()),
   *         FlixelRuntimeMode.DEBUG
   *     );
   *   }
   *   public static void main(String[] argv) {
   *     NSAutoreleasePool pool = new NSAutoreleasePool();
   *     UIApplication.main(argv, null, MyIOSLauncher.class);
   *     pool.close();
   *   }
   * }
   * }</pre>
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @return The configured {@link IOSApplication} to return from {@code createApplication()}.
   */
  public static IOSApplication launch(FlixelGame game, FlixelRuntimeMode runtimeMode) {
    Flixel.initialize(game, new FlixelIOSAlerter(), new FlixelDefaultStackTraceProvider());
    Flixel.setReflection(new FlixelDefaultReflectionHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);

    IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
    configuration.preventScreenDimming = false;

    return new IOSApplication(game, configuration);
  }
}
