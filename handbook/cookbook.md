# "How Do I...?" Cookbook

Short answers to common questions, grouped by topic. Each answer links to the guide that explains it fully.

## Table of contents

1. [Game and states](#game-and-states)
2. [Sprites and drawing](#sprites-and-drawing)
3. [Movement and collisions](#movement-and-collisions)
4. [Input](#input)
5. [Text and HUD](#text-and-hud)
6. [Audio](#audio)
7. [Cameras and effects](#cameras-and-effects)
8. [Time](#time)
9. [Data and saving](#data-and-saving)
10. [Window and platform](#window-and-platform)
11. [Debugging and performance](#debugging-and-performance)

## Game and states

**...set the window title, size, or frame rate?**
Set them on the `FlixelConfig.Builder` in your game class: `.size(640, 360)`, `.framerate(60)`, `.vsync(true)`. Use
`Flixel.window` to change them while the game runs. ([Getting Started](getting-started.md))

**...go to another screen?**
`Flixel.switchState(NextState::new);`, or `Flixel.switchState(() -> new GameOverState(score));` to pass data.
([States and Substates](states-and-substates.md))

**...restart the level?**
`Flixel.resetState();`

**...pause the game?**
`openSubState(new PauseSubState());`, and `close()` inside the substate to resume.
([States and Substates](states-and-substates.md#substates-pause-menus-and-pop-ups))

**...keep the world moving behind a menu?**
Set `persistentUpdate = true` on the parent state before opening the substate.

**...pause automatically when the player tabs out?**
Override `onFocusLost()` in your state and open the pause substate. ([The Game Loop](game-loop.md#focus-minimize-suspend-and-close))

**...keep running in the background?**
`Flixel.autoPause = false;`

**...quit?**
`Flixel.quit();` (desktop only). ([The Game Loop](game-loop.md#quitting-the-game))

**...keep a value across states?**
A constructor argument for the next state, a field on your game class, or `Flixel.save`.
([States and Substates](states-and-substates.md#passing-data-between-states))

**...show something on every screen, like an FPS counter?**
`Flixel.game.add(fpsText); Flixel.game.enableGlobalOverlay(true);` ([The Game Loop](game-loop.md#things-that-live-across-states))

## Sprites and drawing

**...show an image?**
`add(new FlixelSprite(x, y, Flixel.files.internal("images/thing.png")));` ([Basics, Objects, and Sprites](sprites-and-objects.md))

**...make a placeholder rectangle?**
`sprite.makeGraphic(32, 32, FlixelColor.RED);`

**...center something?**
`sprite.screenCenter();` or `sprite.screenCenter(FlixelAxes.X);`

**...hide something without deleting it?**
`sprite.visible = false;` (keeps updating) or `sprite.kill();` (switched off, reusable).

**...flip a character to face left?**
`sprite.setFacing(FlixelDirectionFlags.LEFT);` or `sprite.setFlipX(true);`

**...make the hitbox smaller than the picture?**
`sprite.setSize(w, h); sprite.setOffset(ox, oy);` ([Basics, Objects, and Sprites](sprites-and-objects.md#the-hitbox-and-the-picture-are-separate))

**...draw something in front of something else?**
Add it later, or use separate groups as layers. ([Groups and Recycling](groups-and-recycling.md#nesting-groups))

**...make pixel art crisp?**
Keep antialiasing off (the default), and use `renderResolution(w, h, false)` in the config.
([Graphics and Shaders](graphics-and-shaders.md#crisp-pixel-art))

**...play an animation?**
`sprite.loadGraphic(file, 32, 32); sprite.ensureAnimation().addAnimation("walk", new int[] {0, 1, 2, 3}, 0.1f); sprite.animation.play("walk", true);`
([Animation](animation.md))

**...use a Sparrow atlas?**
`sprite.ensureAnimation().addSparrowFrames(Flixel.files.internal("images/hero")); sprite.animation.addByPrefix("idle", "Hero Idle", 24, true);`

**...add a glow effect?**
`sprite.setBlendMode(FlixelBlendMode.ADD);` ([Graphics and Shaders](graphics-and-shaders.md#blend-modes))

**...apply a shader to the whole screen?**
`Flixel.graphics.addGlobalShader(FlixelShader.load("crt"));` ([Graphics and Shaders](graphics-and-shaders.md#three-places-to-put-a-shader))

**...make sparks or smoke?**
A `FlixelEmitter`. ([Particles](particles.md))

## Movement and collisions

**...move something at a steady speed?**
`sprite.setVelocity(150f, 0f);` ([Physics and Collisions](physics-and-collisions.md))

**...make something fall?**
`sprite.setAccelerationY(900f);` (positive Y is down).

**...make the player jump?**
`if (justPressed && player.isTouching(FlixelDirectionFlags.FLOOR)) player.setVelocityY(-350f);`

**...stop the player from walking through walls?**
Make walls immovable (`wall.setImmovable(true)`), put them in a group, and call `Flixel.collide(player, walls);` after
`super.update(elapsed)`.

**...make a platform you can jump up through?**
`platform.setImmovable(true); platform.setAllowCollisions(FlixelDirectionFlags.UP);`

**...pick up coins?**
`Flixel.overlap(player, coins, onCoin, null);` with `onCoin` stored in a field.
([Physics and Collisions](physics-and-collisions.md#collision-callbacks))

**...know when the player lands?**
`player.justTouched(FlixelDirectionFlags.FLOOR)`

**...fire bullets without garbage?**
A `FlixelBasicGroup` with a size cap and `createMemberForRecycle()`, then `recycle()`.
([Groups and Recycling](groups-and-recycling.md#recycling-the-toy-box-pattern))

**...wrap around the screen edges?**
`FlixelSpriteUtil.screenWrap(ship, true, true, true, true);`

**...move along a path?**
`FlixelTween.linearPath(...)` or `quadPath(...)`. ([Tweens and Timers](tweens-and-timers.md#motion-paths))

## Input

**...react to a single key press?**
`Flixel.keys.justPressed(FlixelKey.SPACE)`; use `pressed(...)` for held keys. ([Input](input.md))

**...accept several keys for one action?**
`Flixel.keys.anyJustPressed(FlixelKey.ENTER, FlixelKey.Z, FlixelKey.SPACE)`, or an input action.
([Input Actions](input-actions.md))

**...support keyboard and gamepad with the same code?**
Define a `FlixelActionSet` with bindings for both. ([Input Actions](input-actions.md))

**...know where the mouse is in the level?**
`Flixel.mouse.getWorldX()` and `getWorldY()`. Use `getScreenX()` for HUD positions.

**...detect a click on a sprite?**
`Flixel.mouse.overlap(sprite) && Flixel.mouse.justPressed(FlixelMouseButton.LEFT)`

**...support touch on desktop?**
`Flixel.touches.enabled = true;`

**...read a gamepad stick?**
`Flixel.gamepads.getAxis(0, FlixelGamepadAxis.LEFT_X)`, with `Flixel.gamepads.globalDeadZone = 0.15f;`

**...make a controller rumble?**
`Flixel.gamepads.vibrate(0, 0.3f);`

**...let the player type a name?**
`Flixel.input.startTextInput();` and a `FlixelKeyboardListener` with `keyTyped(char)`.
([Input](input.md#typed-text-and-the-raw-input-layer))

**...let players rebind keys?**
Wait for `Flixel.keys.firstJustPressed()`, then `action.addBinding("kb", FlixelDigitalBinding.key(key))`.
([Input Actions](input-actions.md#building-a-rebinding-screen))

## Text and HUD

**...show text?**
`add(new FlixelText(x, y, 0f, "Hello", 16));` ([Text, HUDs, and Bars](text-and-ui.md))

**...show a score that changes every frame?**
A reused `FlixelString`: `buffer.set("Score: ").concat(score); text.setText(buffer);`

**...use a custom font?**
`FlixelFontRegistry.register("ui", Flixel.files.internal("fonts/Nunito.ttf")); text.setFont("ui");`

**...outline text?**
`text.setBorderStyle(FlixelText.BorderStyle.OUTLINE, FlixelColor.BLACK);`

**...wrap a paragraph?**
Pass a field width to the constructor and `setWordWrap(true)`.

**...show a health bar?**
`new FlixelBar(x, y, w, h).setRange(0f, 100f).setTrack(player::getHealth);`

**...keep the HUD from scrolling?**
A HUD camera with `defaultDrawTarget = false`, or `setScrollFactor(0f, 0f)` for a quick fix.
([Cameras](cameras.md#hud-cameras))

## Audio

**...play a sound effect?**
`Flixel.sound.play(Flixel.files.internal("sounds/jump.ogg"));` ([Audio](audio.md))

**...play music?**
`Flixel.sound.playMusic(Flixel.files.internal("music/theme.ogg"));`

**...keep music playing across states?**
`Flixel.sound.music.setPersist(true);`

**...fade music out?**
`Flixel.sound.music.fadeOut(1f);`

**...offer separate music and effects volume?**
Two sound groups, one per slider. ([Audio](audio.md#sound-groups-for-options-menus))

**...play a sound many times per second?**
`create(...)` it once and call `play()` each time. ([Audio](audio.md#reusing-sounds-you-play-often))

## Cameras and effects

**...follow the player?**
`Flixel.cameras.first().follow(player, FlixelCamera.FollowStyle.PLATFORMER);` ([Cameras](cameras.md))

**...stop the camera at the level edges?**
`camera.setScrollBounds(0f, levelWidth, 0f, levelHeight);`

**...zoom?**
`camera.setZoom(2f);`

**...shake the screen?**
`Flixel.cameras.first().shake(0.01f, 0.25f);`

**...flash the screen?**
`camera.flash(FlixelColor.WHITE, 0.2f);`

**...fade between screens?**
`camera.fade(FlixelColor.BLACK, 0.5f, false, () -> Flixel.switchState(Next::new), false);`

**...make a parallax background?**
`background.setScrollFactor(0.3f, 0.3f);` ([Cameras](cameras.md#parallax-with-scroll-factors))

**...make split screen?**
Two cameras with `setScreenRegionNormalized(...)`. ([Cameras](cameras.md#split-screen-and-picture-in-picture))

## Time

**...run code after a delay?**
`FlixelTimer.wait(2f, timer -> doSomething());` ([Tweens and Timers](tweens-and-timers.md#timers))

**...run code repeatedly?**
`FlixelTimer.loop(0.5f, timer -> spawnEnemy(), 0);` (`0` means forever).

**...animate a value smoothly?**
`FlixelTween.tween(obj, new FlixelTweenSettings().addGoal(obj::getX, 300f, obj::setX).setDuration(1f));`

**...make something bob forever?**
A `PINGPONG` tween with a `sineInOut` ease. ([Tweens and Timers](tweens-and-timers.md#tween-types-once-loop-ping-pong-backward-persist))

**...make the player blink after being hit?**
`FlixelTween.flicker(player, new FlixelTweenSettings().setDuration(1f));`

**...add slow motion?**
`Flixel.timeScale = 0.25f;` ([The Game Loop](game-loop.md#slow-motion-with-timescale))

## Data and saving

**...save a high score?**
`Flixel.save.bind("MyGame", null);` once, then `Flixel.save.data.put("highScore", score); Flixel.save.flush();`
([Saving Progress](saving.md))

**...make sure saves survive a closed browser tab?**
Flush when progress changes, and `Flixel.Signals.gameSuspended.add(_ -> Flixel.save.flush());`

**...read a JSON file?**
`FlixelJsonValue root = FlixelJson.parse(Flixel.files.internal("data/level.json").readString());`
([JSON and XML](data-serialization.md))

**...turn my own class into JSON?**
Annotate it with `@JsonSerializable` and use the generated `...JsonSerializer.toJson(...)` and `fromJson(...)`.

**...read an XML file?**
`FlixelXmlElement root = FlixelXml.parse(text);` then `getChild(...)` and `getAttribute(...)`.

**...pick something at random?**
`Flixel.random.nextInt(1, 6)`, `Flixel.random.pick(array)`, `Flixel.random.nextBool(0.25f)`.
([Math and Utilities](math-and-utilities.md#randomness-with-flixelrandom))

**...get the same random level every time?**
`new FlixelRandom(seed)`. ([Math and Utilities](math-and-utilities.md#seeds-making-randomness-repeatable))

**...preload assets with a loading bar?**
`Flixel.assets.load(...)` in `create()`, `Flixel.assets.update(10)` each frame, and a `FlixelBar` tracking
`Flixel.assets::getProgress`. ([Files and Assets](files-and-assets.md#building-a-loading-screen))

## Window and platform

**...toggle fullscreen?**
`Flixel.window.toggleFullscreen();` ([Window and Platform Services](window-and-platform-services.md))

**...copy text to the clipboard?**
`Flixel.host.copyToClipboard(text);`

**...let the player choose a file?**
`Flixel.host.pickFile(files -> ..., "json");`

**...vibrate a phone?**
`Flixel.haptics.vibrate(30);`

**...know which platform I am on?**
`Flixel.host.getPlatform()`, but prefer the `supports...()` checks.

**...handle the Android back button?**
`Flixel.keys.justPressed(FlixelKey.BACK)` ([Platform Guide](platforms.md#touch-and-the-back-button))

## Debugging and performance

**...print a message?**
`Flixel.info("x is {}", x);` ([Logging](logging.md))

**...watch a value live?**
`Flixel.debug.watches.addFloat("Player X", player::getX);` ([Extending the Debugger](debugger-extending.md))

**...add a cheat command?**
`Flixel.debug.console.register("god", args -> player.invincible = true);`

**...see hitboxes?**
Run in debug mode and press F2. ([Debugging](debugging.md))

**...pause and step frame by frame?**
F3 to pause, then the step button or the `step` console command.

**...find out why the game stutters?**
Watch the memory graph in the debugger's performance panel; a sawtooth means garbage.
([Optimization](optimization.md))

**...ship the game?**
packagr for desktop, `:html5:package` for the web, and a signed release build for Android.
([Building and Shipping](building-and-shipping.md))

**Back to the [index](README.md).**
