# FlixelGDX Assets Plugin

Turn your `assets/` folder into Java code, so a typo in an asset path breaks the build instead of the game.

> This plugin is being built in steps. This first step only sets up the module and the `assetsClass` settings block;
> the class generation comes next.

## Usage

```groovy
// core/build.gradle
plugins {
  id 'java'
  id 'org.flixelgdx.assets' version '<flixel-version>'
}

assetsClass {
  packageName = 'com.mygame'
}
```
