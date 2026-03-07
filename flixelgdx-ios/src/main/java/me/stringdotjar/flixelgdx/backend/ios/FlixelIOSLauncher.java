package me.stringdotjar.flixelgdx.backend.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.ios.alert.FlixelIOSAlerter;
import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;

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
   * Launches the iOS version of the game with the given game instance.
   *
   * <p>Use this from your {@link com.badlogic.gdx.backends.iosrobovm.IOSApplication.Delegate}
   * implementation:
   *
   * <pre>{@code
   * public class MyIOSLauncher extends IOSApplication.Delegate {
   *   @Override
   *   protected IOSApplication createApplication() {
   *     return FlixelIOSLauncher.launch(new MyGame("My Game", 800, 600, new InitialState()));
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
   * @return The configured {@link IOSApplication} to return from {@code createApplication()}.
   */
  public static IOSApplication launch(FlixelGame game) {
    Flixel.initialize(game, new FlixelIOSAlerter(), new FlixelDefaultStackTraceProvider());

    IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
    configuration.preventScreenDimming = false;

    return new IOSApplication(game, configuration);
  }
}
