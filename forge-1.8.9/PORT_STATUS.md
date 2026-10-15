# Forge 1.8.9 port status

Upstream: `laylafights-create/ReGlass-Forge-Port`, commit
`876da958cb8f8cb56ec5462ea9fc3a721c512155`.
Local branch: `port/forge-1.8.9`.

Implemented in the standalone `forge-1.8.9/` target:

- Java 8 / Forge 11.15.1.2318 lifecycle, client proxy, mod-list config entrypoint.
- LWJGL 2 / GLSL 1.20 rounded glass, blur, refraction, tint, shadow, glare,
  hover/focus effects and a fallback for missing shader/framebuffer support.
- Standard vanilla buttons, hotbar background, exterior container frames,
  resource reload/resize handling, opt-out and class exclusions.
- Persisted JSON settings, unbound shortcuts and a draggable panel playground.
- Pinned ForgeGradle, standalone wrapper, Windows CI artifact upload and docs.

Local verification on Windows with portable Temurin 8u504 and Gradle 2.14.1:

```powershell
.\gradlew.bat clean build '-Dreglass.glTests=true' --no-daemon --console=plain
```

Result: successful compilation and `reobfJar`; **16 tests, zero failures,
zero errors, zero skipped**, including two actual offscreen OpenGL shader tests,
four tests using mapped Minecraft classes, a test using the real obfuscated
Minecraft client jar, and configuration/style/transformer regressions.

Installable artifact: `build/libs/reglass-forge-1.8.9-2.1.0.jar`.
Only this target was built; modern Fabric targets were not rebuilt.

Remaining acceptance: launch the packaged jar in a real Forge client, exercise
the menu/config/playground and an inventory in a world, and test the intended
OptiFine/shader/modpack combination. A shader smoke test and bytecode checks do
not establish full gameplay acceptance. See README for the deliberate legacy API
and UI compatibility limits.

Release line: `v2.1.0-forge-1.8.9`. Release packages contain the reobfuscated mod,
the port sources and SHA-256 checksums. GitHub Release notes record the verified
checks and the remaining gameplay acceptance limits.
