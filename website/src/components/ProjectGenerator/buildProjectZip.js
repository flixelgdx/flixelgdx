import JSZip from 'jszip';
import {saveAs} from 'file-saver';

const DEFAULT_GDX = '1.14.0';
const DEFAULT_GDX_TEAVM = '1.5.2';
const DEFAULT_MINIAUDIO = '0.7';
const DEFAULT_REFLECTASM = '1.11.9';
const DEFAULT_JANSI = '2.4.2';
const CONSTRUO_PLUGIN_VERSION = '2.1.0';

/** Same rules as the FlixelGDX repo root .editorconfig */
const EDITORCONFIG_FOR_ZIP = `# https://editorconfig.org
# http://editorconfig.org
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{java,scala,groovy,kt,kts}]
# 2-space indentation (Google Style Guide Section 4.2)
indent_style = space

max_line_length = 120

# Standard Java formatting rules.
ij_java_use_single_class_imports = true
ij_java_insert_inner_class_imports = false
ij_java_class_count_to_use_import_on_demand = 999
ij_java_names_count_to_use_import_on_demand = 999
ij_java_packages_to_use_import_on_demand = unset
ij_java_blank_lines_before_class_end = 0
ij_java_blank_lines_after_class_header = 1
ij_java_doc_align_param_comments = false
ij_java_doc_do_not_wrap_if_one_line = true
ij_any_keep_simple_blocks_in_one_line = true

ij_java_class_brace_style = end_of_line
ij_java_method_brace_style = end_of_line

ij_java_space_before_class_left_brace = true
ij_java_space_before_method_left_brace = true
ij_java_space_before_if_left_brace = true
ij_java_space_before_while_left_brace = true
ij_java_space_before_for_left_brace = true
ij_java_space_before_try_left_brace = true
ij_java_space_before_catch_left_brace = true
ij_java_space_before_switch_left_brace = true
ij_java_space_before_synchronized_left_brace = true

indent_size = 2

[*.gradle]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
`;

function sanitizeProjectName(name) {
  const s = (name || 'mygame')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return s || 'mygame';
}

function validatePackage(pkg) {
  const p = (pkg || '').trim();
  if (!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/.test(p)) {
    return 'com.example.game';
  }
  return p;
}

function pkgPath(pkg) {
  return pkg.replace(/\./g, '/');
}

function pluginManagementBlock() {
  return `pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
    maven { url 'https://jitpack.io' }
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == 'me.stringdotjar.flixelgdx.teavm') {
        useModule("com.github.stringdotjar.flixelgdx:flixelgdx-teavm-plugin:\${requested.version}")
      }
    }
  }
}

`;
}

function settingsGradle({projectName, desktop, android, ios, web}) {
  const mods = ["'core'"];
  if (desktop) {
    mods.push("'desktop'");
  }
  if (android) {
    mods.push("'android'");
  }
  if (ios) {
    mods.push("'ios'");
  }
  if (web) {
    mods.push("'html'");
  }
  return `${web ? pluginManagementBlock() : ''}rootProject.name = '${projectName}'
include ${mods.join(', ')}
`;
}

function rootConvenienceTasks({desktop, android, web, construto}) {
  const blocks = [];
  if (desktop) {
    blocks.push(`
tasks.register('run') {
  group = 'application'
  description = 'Runs the desktop game (alias for :desktop:run).'
  dependsOn ':desktop:run'
}`);
  }
  if (android) {
    blocks.push(`
tasks.register('installAndroid') {
  group = 'application'
  description = 'Installs debug APK (alias for :android:installDebug).'
  dependsOn ':android:installDebug'
}`);
  }
  if (web) {
    blocks.push(`
tasks.register('runHtml') {
  group = 'application'
  description = 'Runs TeaVM dev server (alias for :html:run).'
  dependsOn ':html:run'
}
tasks.register('buildHtml') {
  group = 'build'
  description = 'Compiles TeaVM JavaScript (alias for :html:generateJavaScript).'
  dependsOn ':html:generateJavaScript'
}`);
  }
  if (construto && desktop) {
    blocks.push(`
tasks.register('packageNativeAll') {
  group = 'build'
  description = 'Packages desktop for Linux, Windows, and macOS (Construo).'
  dependsOn ':desktop:packageLinuxX64', ':desktop:packageWinX64', ':desktop:packageMacX64', ':desktop:packageMacM1'
}`);
  }
  return blocks.join('\n');
}

function rootBuildGradle({android, web, flixelVersion, construto, desktop}) {
  let agp = '';
  if (android) {
    agp = `
buildscript {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies {
    classpath 'com.android.tools.build:gradle:8.7.3'
  }
}
`;
  }
  const teavmPlugin = web
    ? `\n  id 'me.stringdotjar.flixelgdx.teavm' version '${flixelVersion}' apply false`
    : '';
  const construoRoot =
    construto && desktop
      ? `\n  id 'io.github.fourlastor.construo' version '${CONSTRUO_PLUGIN_VERSION}' apply false`
      : '';
  const extraTasks = rootConvenienceTasks({desktop, android, web, construto});
  return `${agp}
plugins {
  id 'java-library' apply false
${android ? "  id 'com.android.application' apply false\n" : ''}${teavmPlugin}${construoRoot}
}

allprojects {
  repositories {
    mavenCentral()
    google()
    maven { url 'https://jitpack.io' }
    gradlePluginPortal()
  }
}
${extraTasks}
`;
}

function gradleProperties({extraProps, jvmArgs, flixelVersion}) {
  const base = `org.gradle.jvmargs=${jvmArgs || '-Xms256M -Xmx1G -Dfile.encoding=UTF-8'}
org.gradle.daemon=true
gdxVersion=${DEFAULT_GDX}
gdxTeaVMVersion=${DEFAULT_GDX_TEAVM}
miniaudioVersion=${DEFAULT_MINIAUDIO}
reflectasmVersion=${DEFAULT_REFLECTASM}
jansiVersion=${DEFAULT_JANSI}
flixelVersion=${flixelVersion}
`;
  return `${base}\n${extraProps || ''}\n`.trim() + '\n';
}

function coreBuildGradle({flixelVersion}) {
  return `plugins {
  id 'java-library'
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

// Keep shared assets at the project root (../assets from :core).
// They are on the classpath so Gdx.files.internal("game/foo.png") resolves.
sourceSets {
  main {
    resources {
      srcDirs = ['src/main/resources', '../assets']
    }
  }
}

dependencies {
  implementation "com.github.stringdotjar.flixelgdx:flixelgdx-core:\${flixelVersion}"
}
`;
}

function desktopBuildGradle({flixelVersion, mainClass, projectName, pkg, construto}) {
  const pluginsBlock = construto
    ? `plugins {
  id 'application'
  id 'io.github.fourlastor.construo'
}`
    : `plugins {
  id 'application'
}`;

  const human = projectName
    .replace(/[-_]+/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());

  const construoBlock = construto
    ? `
import io.github.fourlastor.construo.Target

construo {
  name.set('${projectName}')
  humanName.set('${human}')
  mainClass.set('${mainClass}')
  outputDir.set(rootProject.file('dist'))
  targets.configure {
    create('linuxX64', Target.Linux) {
      architecture.set(Target.Architecture.X86_64)
      jdkUrl.set('https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.11_9.tar.gz')
    }
    create('winX64', Target.Windows) {
      architecture.set(Target.Architecture.X86_64)
      jdkUrl.set('https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip')
    }
    create('macX64', Target.MacOs) {
      architecture.set(Target.Architecture.X86_64)
      jdkUrl.set('https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_mac_hotspot_17.0.11_9.tar.gz')
      identifier.set('${pkg}')
      buildNumber.set('1.0.0')
      versionNumber.set('1.0')
    }
    create('macM1', Target.MacOs) {
      architecture.set(Target.Architecture.AARCH64)
      jdkUrl.set('https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.11_9.tar.gz')
      identifier.set('${pkg}')
      buildNumber.set('1.0.0')
      versionNumber.set('1.0')
    }
  }
}
`
    : '';

  return `${pluginsBlock}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

application {
  mainClass = '${mainClass}'
}

dependencies {
  implementation project(':core')
  implementation "com.github.stringdotjar.flixelgdx:flixelgdx-lwjgl3:\${flixelVersion}"
}
${construoBlock}
`;
}

function androidModule({applicationId, flixelVersion}) {
  return {
    'android/build.gradle': `plugins {
  id 'com.android.application'
}

android {
  namespace '${applicationId}'
  compileSdk 36
  defaultConfig {
    applicationId '${applicationId}'
    minSdk 24
    targetSdk 35
    versionCode 1
    versionName '1.0'
  }
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_17
    targetCompatibility JavaVersion.VERSION_17
  }
  buildTypes {
    release {
      minifyEnabled false
    }
  }
  // Same asset tree as :core (root assets/ folder, Liftoff-style).
  sourceSets {
    main {
      assets.srcDirs = ['../assets']
    }
  }
}

dependencies {
  implementation project(':core')
  implementation "com.github.stringdotjar.flixelgdx:flixelgdx-android:\${flixelVersion}"
}
`,
    'android/src/main/AndroidManifest.xml': `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="true">
        <activity
            android:name=".AndroidLauncher"
            android:exported="true"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
`,
  };
}

function iosStub({pkg}) {
  return {
    'ios/README.txt': `iOS (MobiVM / RoboVM) - not fully supported in FlixelGDX yet.

This folder is a placeholder. See the main FlixelGDX repository and COMPILING.md for
iOS backend work-in-progress and how contributors set up MobiVM.

Your Java package root for shared game code is: ${pkg}
`,
  };
}

function htmlModule({pkg, flixelVersion, gameClass}) {
  const path = pkgPath(pkg);
  const webLauncher = `package ${pkg}.html;

import me.stringdotjar.flixelgdx.backend.teavm.FlixelTeaVMLauncher;
import ${pkg}.${gameClass};

/**
 * TeaVM entry point: the class named in teavm.all.mainClass in html/build.gradle.
 * FlixelTeaVMLauncher boots the gdx-teavm web backend, then your ${gameClass}.
 */
public final class WebLauncher {

  public static void main(String[] args) {
    FlixelTeaVMLauncher.launch(new ${gameClass}());
  }
}
`;

  const buildGradle = `plugins {
  id 'java-library'
  id 'org.teavm' version '0.13.0'
  id 'me.stringdotjar.flixelgdx.teavm'
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

// Browser output + dev server; keep teavm.js.outputDir aligned with flixelgdx.outputDir.
teavm {
  all {
    mainClass = '${pkg}.html.WebLauncher'
  }
  js {
    addedToWebApp = true
    targetFileName = 'teavm.js'
    outputDir = file("\$buildDir/dist/webapp")
  }
}

// FlixelGDX web helper plugin: assets, index.html, preload manifest, native JS extracts.
// Defaults use rootProject/assets/; optional overrides are commented below.
flixelgdx {
  // title = 'My Game'
  // canvasId = 'flixelgdx-canvas'
  // outputDir = file("\$buildDir/dist/webapp")
  // devServerPort = 8080
  // assetsDir = file('../assets')
  // webappDir = file('src/main/webapp')
  // generateDefaultIndexHtml = true
  // generateDefaultStartupLogo = true
}

dependencies {
  implementation project(':core')
  implementation "com.github.stringdotjar.flixelgdx:flixelgdx-teavm:\${flixelVersion}"
}
`;

  return {
    [`html/src/main/java/${path}/html/WebLauncher.java`]: webLauncher,
    'html/build.gradle': buildGradle,
    'html/README.txt': `Web (TeaVM) module

Gradle:
  ./gradlew :html:generateJavaScript   - compile to JavaScript
  ./gradlew :html:run                  - local dev server (see flixelgdx.devServerPort)
  ./gradlew buildHtml or runHtml       - root aliases (liftoff-style helpers)

Output is under html/build/dist/webapp (keep teavm.js.outputDir aligned with flixelgdx.outputDir).

The FlixelGDX TeaVM Gradle plugin generates reflection metadata (teavm.json) for you.

More context: FlixelGDX COMPILING.md (Web / TeaVM).
`,
  };
}

function coreJavaSources({pkg, gameClass, stateClass}) {
  const path = pkgPath(pkg);
  const playState = `package ${pkg};

import com.badlogic.gdx.Gdx;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.FlixelState;
import me.stringdotjar.flixelgdx.input.keyboard.FlixelKey;

/**
 * A simple first level.
 *
 * <p>FlixelGDX calls create() when this state becomes active, then update(float)
 * each frame for updating the state's logic. The elapsed argument is delta time in seconds
 * (time since the last frame), so multiplying speed by elapsed keeps motion smooth.
 */
public class ${stateClass} extends FlixelState {

  /** Player graphic; loaded from assets/game/flixelgdx-logo.png at project root. */
  private FlixelSprite player;

  @Override
  public void create() {
    super.create();
    player = new FlixelSprite();
    player.loadGraphic(Gdx.files.internal("game/flixelgdx-logo.png"));
    player.setX(120f);
    player.setY(200f);
    add(player);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    float speed = 240f * elapsed;
    if (Flixel.keys.pressed(FlixelKey.W) || Flixel.keys.pressed(FlixelKey.UP)) {
      player.changeY(speed);
    }
    if (Flixel.keys.pressed(FlixelKey.S) || Flixel.keys.pressed(FlixelKey.DOWN)) {
      player.changeY(-speed);
    }
    if (Flixel.keys.pressed(FlixelKey.A) || Flixel.keys.pressed(FlixelKey.LEFT)) {
      player.changeX(-speed);
    }
    if (Flixel.keys.pressed(FlixelKey.D) || Flixel.keys.pressed(FlixelKey.RIGHT)) {
      player.changeX(speed);
    }
  }
}
`;

  const game = `package ${pkg};

import me.stringdotjar.flixelgdx.FlixelGame;

/**
 * Root FlixelGame: window title, size, and the first FlixelState.
 *
 * <p>Keep high-level bootstrapping here; gameplay belongs in states like ${stateClass}.
 */
public class ${gameClass} extends FlixelGame {

  public ${gameClass}() {
    super("My Game", 800, 600, new ${stateClass}());
  }
}
`;

  return {
    [`core/src/main/java/${path}/${stateClass}.java`]: playState,
    [`core/src/main/java/${path}/${gameClass}.java`]: game,
  };
}

function desktopLauncher({pkg, gameClass}) {
  const path = pkgPath(pkg);
  return {
    [`desktop/src/main/java/${path}/desktop/DesktopLauncher.java`]: `package ${pkg}.desktop;

import me.stringdotjar.flixelgdx.backend.lwjgl3.FlixelLwjgl3Launcher;
import ${pkg}.${gameClass};

/**
 * Desktop (LWJGL3) entry point. FlixelLwjgl3Launcher creates the GLFW window and
 * forwards the libGDX lifecycle to your game class ${gameClass}.
 */
public final class DesktopLauncher {

  public static void main(String[] args) {
    FlixelLwjgl3Launcher.launch(new ${gameClass}());
  }
}
`,
  };
}

function androidLauncher({pkg, gameClass}) {
  const path = pkgPath(pkg);
  return {
    [`android/src/main/java/${path}/AndroidLauncher.java`]: `package ${pkg};

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

/**
 * Android entry. Hosts a libGDX AndroidApplication and passes your game class ${gameClass}.
 */
public class AndroidLauncher extends AndroidApplication {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
    initialize(new ${gameClass}(), cfg);
  }
}
`,
    'android/src/main/res/values/strings.xml': `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">MyGame</string>
</resources>
`,
  };
}

function readmeTxt({
  projectName,
  pkg,
  editor,
  desktop,
  android,
  ios,
  web,
  construto,
  flixelVersion,
}) {
  const lines = [];
  lines.push(`FlixelGDX starter — ${projectName}`);
  lines.push('================================');
  lines.push('');
  lines.push(`FlixelGDX version (JitPack tag): ${flixelVersion}`);
  lines.push(`Java package: ${pkg}`);
  lines.push(`Editor hint: ${editor}`);
  lines.push('');
  lines.push('OPEN IN YOUR IDE');
  lines.push('----------------');
  lines.push('1. Unzip this folder.');
  lines.push('2. Open the ROOT directory (where settings.gradle is) in your IDE:');
  lines.push('   - IntelliJ IDEA: File → Open → select folder → Import Gradle project.');
  lines.push('   - Eclipse: Import → Gradle → Existing Gradle Project.');
  lines.push('   - VS Code: open folder; use Extension Pack for Java + Gradle.');
  lines.push('3. Use JDK 17 (IBM Semeru 17 OpenJ9 is recommended — see FlixelGDX COMPILING.md).');
  lines.push('');
  lines.push('RUNNING YOUR GAME (per platform)');
  lines.push('----------------------------------');
  if (desktop) {
    lines.push('Desktop (LWJGL3):');
    lines.push('  ./gradlew :desktop:run   or   ./gradlew run');
    lines.push('  Or run the main class ' + pkg + '.desktop.DesktopLauncher from the IDE.');
    lines.push('');
  }
  if (android) {
    lines.push('Android:');
    lines.push('  Connect a device or start an emulator with API 24+.');
    lines.push('  ./gradlew :android:installDebug   or   ./gradlew installAndroid');
    lines.push('  Or use Android Studio Run on the android module.');
    lines.push('  Note: FlixelGDX Android support is still maturing.');
    lines.push('');
  }
  if (web) {
    lines.push('Web (TeaVM):');
    lines.push('  ./gradlew :html:generateJavaScript   or   ./gradlew buildHtml');
    lines.push('  ./gradlew :html:run   or   ./gradlew runHtml');
    lines.push('  Open the URL printed in the console; output is under html/build/dist/webapp.');
    lines.push('');
  }
  if (ios) {
    lines.push('iOS:');
    lines.push('  See ios/README.txt — tooling is not fully supported yet.');
    lines.push('');
  }
  lines.push('ASSETS');
  lines.push('------');
  lines.push('Put game files under assets/ at the project root (e.g. assets/game/flixelgdx-logo.png).');
  lines.push(':core packs ../assets into the jar; Android and HTML copy from the same folder.');
  lines.push('Load with Gdx.files.internal("game/yourfile.png").');
  lines.push('');
  lines.push('DEPENDENCIES');
  lines.push('------------');
  lines.push('Artifacts resolve from JitPack: com.github.stringdotjar.flixelgdx');
  lines.push('flixelVersion in gradle.properties must be a real Git tag (e.g. 0.1.1-beta).');
  lines.push('');
  if (construto) {
    lines.push('CONSTRUO (native desktop packages)');
    lines.push('----------------------------------');
    lines.push('The desktop module is configured with io.github.fourlastor.construo.');
    lines.push('JDK URLs target Temurin 17; change them in desktop/build.gradle if you use another JDK.');
    lines.push('  ./gradlew :desktop:packageLinuxX64');
    lines.push('  ./gradlew :desktop:packageWinX64');
    lines.push('  ./gradlew :desktop:packageMacX64');
    lines.push('  ./gradlew :desktop:packageMacM1');
    lines.push('  ./gradlew packageNativeAll');
    lines.push('Zips are written under dist/ at the project root.');
    lines.push('');
  }
  lines.push('More: https://github.com/stringdotjar/flixelgdx');

  return lines.join('\n');
}

async function addGradleWrapper(zip, fetchAsset) {
  const load = async (srcPath) => {
    const res = await fetchAsset(srcPath);
    if (!res.ok) {
      throw new Error(`Missing site asset: ${srcPath} (${res.status})`);
    }
    return res;
  };
  const gradlewBin = new Uint8Array(await (await load('gradle-wrapper/gradlew')).arrayBuffer());
  zip.file('gradlew', gradlewBin, {unixPermissions: 0o755});
  zip.file(
    'gradlew.bat',
    new Uint8Array(await (await load('gradle-wrapper/gradlew.bat')).arrayBuffer()),
  );
  zip.file(
    'gradle/wrapper/gradle-wrapper.jar',
    new Uint8Array(await (await load('gradle-wrapper/gradle-wrapper.jar')).arrayBuffer()),
  );
  zip.file(
    'gradle/wrapper/gradle-wrapper.properties',
    await (await load('gradle-wrapper/gradle-wrapper.properties')).text(),
  );
}

/**
 * @param {object} opts
 * @param {(path: string) => Promise<Response>} opts.fetchAsset
 * @param {Uint8Array|ArrayBuffer} [opts.logoBytes]
 */
export async function buildProjectZip(opts) {
  const {
    fetchAsset,
    logoBytes,
    projectNameRaw,
    package: packageRaw,
    flixelVersion,
    desktop,
    android,
    ios,
    web,
    editor,
    construto,
    extraGradleProps,
    jvmArgs,
  } = opts;

  if (!desktop && !android && !ios && !web) {
    throw new Error('Select at least one platform.');
  }

  const projectName = sanitizeProjectName(projectNameRaw);
  const pkg = validatePackage(packageRaw);
  const fv = (flixelVersion || '0.1.1-beta').trim();
  const gameClass = 'MainGame';
  const stateClass = 'PlayState';
  const mainClass = `${pkg}.desktop.DesktopLauncher`;

  const zip = new JSZip();

  await addGradleWrapper(zip, fetchAsset);

  zip.file('.editorconfig', EDITORCONFIG_FOR_ZIP);
  zip.file('settings.gradle', settingsGradle({projectName, desktop, android, ios, web}));
  zip.file(
    'build.gradle',
    rootBuildGradle({
      android,
      web,
      flixelVersion: fv,
      construto: construto && desktop,
      desktop,
    }),
  );
  zip.file(
    'gradle.properties',
    gradleProperties({
      extraProps: extraGradleProps || '',
      jvmArgs,
      flixelVersion: fv,
    }),
  );

  zip.file('core/build.gradle', coreBuildGradle({flixelVersion: fv}));
  Object.entries(coreJavaSources({pkg, gameClass, stateClass})).forEach(([k, v]) =>
    zip.file(k, v),
  );

  zip.file('assets/.gitkeep', '');

  if (logoBytes) {
    zip.file('assets/game/flixelgdx-logo.png', logoBytes);
  }

  if (desktop) {
    zip.file(
      'desktop/build.gradle',
      desktopBuildGradle({
        flixelVersion: fv,
        mainClass,
        projectName,
        pkg,
        construto: construto && desktop,
      }),
    );
    Object.entries(desktopLauncher({pkg, gameClass})).forEach(([k, v]) =>
      zip.file(k, v),
    );
  }

  if (android) {
    const androidFiles = androidModule({applicationId: pkg, flixelVersion: fv});
    Object.entries(androidFiles).forEach(([k, v]) => zip.file(k, v));
    Object.entries(androidLauncher({pkg, gameClass})).forEach(([k, v]) =>
      zip.file(k, v),
    );
  }

  if (ios) {
    Object.entries(iosStub({pkg})).forEach(([k, v]) => zip.file(k, v));
  }

  if (web) {
    Object.entries(htmlModule({pkg, flixelVersion: fv, gameClass})).forEach(([k, v]) =>
      zip.file(k, v),
    );
  }

  zip.file(
    'README.txt',
    readmeTxt({
      projectName,
      pkg,
      editor,
      desktop,
      android,
      ios,
      web,
      construto: construto && desktop,
      flixelVersion: fv,
    }),
  );

  const blob = await zip.generateAsync({type: 'blob'});
  saveAs(blob, `${projectName}-flixelgdx-starter.zip`);
}
