# Chaos Mod (Fabric, Minecraft 26.3)

Alle 20 Sekunden ein Ereignis: 10 s Vorwarnung (Bar GRÜN, zeigt das nächste Ereignis),
dann 10 s Laufzeit (Bar ROT). 100 Ereignisse, 5 davon positiv.

## Bauen
1. JDK 25 und Gradle 9.6.0 installieren (oder mit dem Fabric-Template-Generator ein Projekt erzeugen
   und dessen gradlew/gradle-wrapper.jar hierher kopieren).
2. Im Projektordner: `gradle build`  (bzw. `./gradlew build`)
3. Die Mod liegt in `build/libs/chaosmod-1.0.0.jar` (NICHT die -sources.jar).
4. In den mods-Ordner legen, zusätzlich Fabric Loader 0.19.5+ und Fabric API 0.162.0+26.3 installieren.

## Testen
`/chaos event <1-100>` setzt das nächste Ereignis (nur in der Wartephase).

## Hinweise
- Die Bar ist eine Boss-Bar (Server-seitig, immer sichtbar). Die Mod muss auf Server UND Client liegen.
- Client-Effekte ohne Mixins sind Annäherungen: Schieflage = Kamera-Pendeln, Pixel-Modus = Farbraster,
  Sonnenfinsternis = grauer Filter, Fisheye = FOV 110, Spiegelwelt = gespiegelte Maus,
  Invertierte Steuerung = Gegenimpuls, Villager-Sounds = Villager-Chor, HUD weg = schwarzer Balken über der Hotbar.
- Ungetestet gegen 26.3 kompiliert. Falls Namen abweichen (z.B. GuiGraphics.fill, Identifier, snapTo,
  hurtServer-frei, HudElementRegistry), sind es meist Ein-Zeilen-Fixes.
