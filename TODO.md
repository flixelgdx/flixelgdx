# TODO for the Framework

> [!IMPORTANT]
> This TODO list will be constantly changing and updated, so what you might see now will have a high
> chance of being different tomorrow!

- [x] Create base project structure (components and core classes such as the game loop, `FlixelBasic`/`FlixelObject`/`FlixelSprite` hierarchy, asset system, group system, tweening engine, utility classes such as `FlixelSave` and `FlixelSpriteUtil`, etc.)
- [x] Add unit tests for verifying logic works in the `flixelgdx-core` module.
- [x] Implement GitHub configs, such as issue templates, build checks, etc. (**NOTE**: this might change over time!)
- [x] Create a logo for the framework, including a large and small one
- [ ] Add support for Android and iOS
  - [ ] Find someone to help me test iOS ***or*** get a Macbook with Xcode and test it on there
- [ ] Create a Discord server for people to find help and for collaborators/contributors to easily contact me
- [x] Develop comprehensive and modernized website
  - [x] Create homepage
  - [x] Create "Getting Started" page to allow users to automatically generate a FlixelGDX project with pre-written  Gradle configs and their specified settings
  - [x] Create beginner-friendly and easy-to-navigate API documentation pages (use Docusaurus and DocletMD, our own custom plugin)
  - [ ] Use the created demos, compile them to web, and create a page for those demos (with GitHub links to the source code)
  - [x] Create multiple documentation pages teaching core concepts, using the debugger, etc.
  - [x] Create a "Your First Game" tutorial page for complete beginners wanting to code a game with FlixelGDX
- [x] Develop an ahead-of-time compiling plugin using Kotlin to solve the reflection problem for Java by compiling
      reflection at build time and catching errors before a game (or program) starts, with a `Reflect` class modeled after [Haxe's `Reflect` class](https://api.haxe.org/Reflect.html)
  - [x] Remove the current reflection system (which would break for mobile) from the core framework (use [ReflectAOT](https://github.com/flixelgdx/ReflectAOT) in game projects instead).
- [ ] Buy Twitter verification and post about the framework to gain traction
- [ ] Post on Hacker News(...?)
