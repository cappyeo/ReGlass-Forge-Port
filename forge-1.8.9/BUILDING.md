# ReGlass Forge 1.8.9

This directory is an independent Forge 1.8.9 target. It uses the official ForgeGradle 2.1 legacy toolchain:

- Minecraft/Forge: `1.8.9-11.15.1.2318-1.8.9`
- mappings: `stable_22`
- ForgeGradle: `net.minecraftforge.gradle:ForgeGradle:2.1-20211118.174922-42`
- Gradle wrapper: `2.14.1`
- Java: JDK 8 (64-bit)

ForgeGradle 2.1 cannot run on Java 21. Install or unpack a portable JDK 8 and set `JAVA_HOME` only in the current shell. On Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk8'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat setupDecompWorkspace
.\gradlew.bat build
```

`build` downloads the binary workspace automatically; `setupDecompWorkspace` is
only needed for IDE source browsing and development. `build` includes `reobfJar`
and produces the installable mod at `build/libs/reglass-forge-1.8.9-2.1.0.jar`.

Useful commands:

```powershell
.\gradlew.bat tasks
.\gradlew.bat setupDecompWorkspace
.\gradlew.bat runClient
.\gradlew.bat clean build
.\gradlew.bat reobfJar
```

The client run task explicitly loads the ReGlass coremod in development, so its
GUI hooks run from classes as well as from the packaged jar.

Tests for configuration, style overrides and ASM hooks run in a regular build.
The optional shader smoke tests require a local desktop OpenGL driver; they
compile the real shaders in an offscreen LWJGL context, render pixels, check
transparent panel margins and verify blur brightness:

```powershell
.\gradlew.bat test '-Dreglass.glTests=true' --rerun-tasks --no-daemon
```

The native LWJGL libraries are extracted automatically for this opt-in test.
Default headless CI skips only these OpenGL tests.

The GitHub Actions workflow provisions Temurin 8 for this directory and runs `clean build` on Windows. Do not use the root Fabric Gradle tasks for this target.
