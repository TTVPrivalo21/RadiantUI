# ♥ RadiantUI

Rediseño completo de la interfaz de Minecraft para **Paper / Purpur 1.21.x – 26.x** (Java 21+):
HUD, corazones y barras, inventarios y cofres, scoreboard moderno, menú de pausa y Server Links,
con **resource pack generado automáticamente**, texturas pixel-art pintadas por código y
**servidor HTTP integrado** para enviarlo a los jugadores.

Tema principal y por defecto: **`snoopy_classic`**: cómic clásico en **blanco y negro** (`#FFFFFF`, `#E5E7EB`,
`#52525B`, `#18181B`, `#09090B`), con **Pip**, la mascota original del plugin (perrito blanco de perfil, oreja negra, mancha en el ojo y collar), huellitas, estrellas y corazones blancos con tinta negra.

### Estilo cartoon / cómic clásico

Todas las texturas se pintan como una tira cómica: **tinta negra de 2 px** con esquinas redondeadas,
brillo blanco arriba a la izquierda y **sombra dura** abajo a la derecha, **tramado de puntos grises**
(Ben-Day) que se desvanece, **insignias redondas** con el perrito, **corazones adhesivos** con brillo,
separadores de tinta discontinua y costuras grises.

---

## ✿ Qué cambia

| Parte | Qué se genera |
|---|---|
| **Corazones** | llenos, medios, vacíos, parpadeo, hardcore (con manchita negra), veneno, wither, absorción, congelado y montura |
| **Hambre** | huesitos blancos en lugar de muslos (+ variantes de hambre) |
| **Armadura / Aire** | escuditos con corazón y burbujas blancas con sombra gris (los estados vacíos son gris oscuro) |
| **XP / Hotbar** | barra negra con brillos blancos y marcas de tinta; hotbar con celdas hundidas, separadores negros y selector con corazón |
| **Inventario** | `inventory.png`: panel cómic, recuadro del jugador con tramado, casilla de resultado con aro tramado, flecha entintada, separadores y sello del perrito |
| **Cofres** | `generic_54.png` (simples y dobles) con cabecera tramada, sello del perrito, separador "Inventario ─ ─ ♥" y overlays `chest_1` … `chest_6` |
| **Creativo** | `creative_inventory/tab_items`, `tab_item_search` (buscador oscuro legible), `tab_inventory` (armadura, mano secundaria, papelera con ✕), 28 pestañas y la barra de scroll |
| **Menú de pausa** | botones carbón con tinta negra, línea interior, tramado y remaches de corazón (nine-slice que no los estira), sliders con pista discontinua, campos de texto, separadores cosidos y fondos con huellitas |
| **HUD superior** | BossBars con glifos y placeholders (vida, hambre, XP, dinero, coordenadas…) + panel `hud_panel` cómic |
| **Scoreboard** | sin números rojos (`NumberFormat.blank()`), líneas en MiniMessage y título animado |
| **Server Links** | cada enlace con icono propio y nombre en el menú nativo de enlaces, más accesos clicables en el chat (`/ui links`) |

## ✿ Instalación

1. Copia `RadiantUI-<versión>.jar` a `plugins/`.
2. Arranca el servidor: se generan `config.yml`, `hud.yml`, `containers.yml`, `scoreboard.yml` y `pausemenu.yml`.
3. Abre el puerto HTTP (por defecto **8163/TCP**) o pon tu dominio en `http.public-url`.
4. Entra al servidor: el pack se envía solo.

Opcionales (`softdepend`): **PlaceholderAPI**, **Vault** (para `{money}`) y **Oraxen** (merge del pack).

## ✿ Temas

Los temas están en `config.yml → themes`. Cada tema habilitado genera **su propio resource pack**
(`plugins/RadiantUI/packs/<tema>.zip`), así cada jugador puede tener un tema distinto.

```yaml
themes:
  mi_tema:
    enabled: true
    display-name: "<#7FD8BE>✿ Mi tema"
    description: "Lo que se ve al pasar el ratón"
    motif: paws            # snoopy | paws | hearts | plain
    colors:
      background: "#FFFFFF"
      background-alt: "#EEF6F3"
      dark: "#1E1E1E"
      outline: "#000000"
      accent: "#4FC3A1"
      accent-light: "#7FD8BE"
      accent-pale: "#BDEBDC"
      text: "#2B2B2B"
      highlight: "#FFFFFF"
      shade: "#B5C4BF"
```

Incluidos: `snoopy_classic` (por defecto), `snoopy_night` (fondos negro/carbón con tinta blanca) y `paw_mint` (ejemplo con color, desactivado).

- `theme.default`: tema global del servidor.
- `theme.allow-player-themes`: si hay 2 o más temas habilitados, cada jugador puede usar `/ui theme <tema>`.
- La elección se guarda en el propio jugador (PersistentDataContainer), así que no hace falta base de datos.

### Texturas personalizadas

Cualquier archivo que pongas en `plugins/RadiantUI/assets/` **tiene prioridad** sobre lo generado
(misma ruta que dentro de un pack). Ejemplo:

```
plugins/RadiantUI/assets/minecraft/textures/gui/sprites/hud/heart/full.png
plugins/RadiantUI/themes/snoopy_night/assets/...   ← solo para ese tema
plugins/RadiantUI/overlays/mi_menu.png             ← overlays propios (containers.yml → custom-overlays)
```

Con `pack.export-unzipped: true` tienes una copia descomprimida en `plugins/RadiantUI/generated/<tema>/`
para usarla como base.

## ✿ Etiquetas de texto

Todos los textos usan **MiniMessage** con etiquetas extra:

| Etiqueta | Uso |
|---|---|
| `<space:px>` | mueve el cursor `px` píxeles (negativo = izquierda) |
| `<glyph:id>` | icono: `heart`, `heart_half`, `heart_empty`, `bone`, `star`, `coin`, `pin`, `clock`, `paw`, `shield`, `bubble`, `puppy`, `bar_full`, `bar_empty`, `dot`, `link`, `chat`, `cart`, `book`, `globe`, `bug` |
| `<overlay:id[:dx]>` | dibuja una imagen sin avanzar el cursor (`chest_1`…`chest_6`, `hud_panel` u overlays propios) |
| `<accent>` `<accent_light>` `<accent_pale>` `<dark>` `<outline>` `<background>` `<background_alt>` `<text>` `<highlight>` `<shade>` | colores del tema del jugador |

## ✿ Placeholders

Internos (HUD, scoreboard, títulos de GUIs):

`{player}` `{health}` `{max_health}` `{hearts}` `{food}` `{food_bar}` `{armor}` `{armor_bar}` `{air}`
`{level}` `{xp_percent}` `{xp_bar}` `{x}` `{y}` `{z}` `{world}` `{money}` `{online}` `{max_online}`
`{ping}` `{theme}` `{time}`

Con PlaceholderAPI también puedes usar `%...%` en cualquier línea, y RadiantUI registra:

| Placeholder | Valor |
|---|---|
| `%radiantui_theme%` / `%radiantui_theme_display%` | tema del jugador |
| `%radiantui_theme_global%` | tema global |
| `%radiantui_hud%` / `%radiantui_scoreboard%` | `on` / `off` |
| `%radiantui_glyph_<id>%` | carácter del glifo (p. ej. `%radiantui_glyph_heart%`) |
| `%radiantui_<placeholder>%` | cualquier placeholder interno (`%radiantui_health%`, `%radiantui_money%`…) |

## ✿ Comandos

`/radiantui` (alias `/rui`, `/ui`):

| Comando | Permiso | Descripción |
|---|---|---|
| `/ui theme` | `radiantui.theme` | lista los temas (clic para elegir) |
| `/ui theme <tema\|reset>` | `radiantui.theme` | elige tu tema o vuelve al global |
| `/ui theme <tema> <jugador>` | `radiantui.admin` | cambia el tema de otro jugador |
| `/ui globaltheme <tema>` | `radiantui.admin` | cambia el tema global (se guarda en config) |
| `/ui hud <on\|off>` | `radiantui.hud` | muestra u oculta tu HUD superior |
| `/ui scoreboard <on\|off>` | `radiantui.scoreboard` | muestra u oculta tu scoreboard |
| `/ui pack [jugador]` | `radiantui.pack` | reenvía el resource pack |
| `/ui links` | `radiantui.use` | enlaces del servidor clicables en el chat |
| `/ui menu` | `radiantui.use` | abre el menú rápido nativo (1.21.6+/26.x); si no, la lista del chat |
| `/ui reload` | `radiantui.admin` | recarga todo, regenera los packs y los reenvía |

## ✿ Server Links: qué es nativo y qué no

**Nativo de Minecraft 1.21+ (sin mods), vía Paper:** si el servidor envía enlaces, el menú de pausa
muestra un único botón **"Enlaces del servidor"** que abre una lista; cada enlace es un botón con el
texto que envías y, al pulsarlo, el cliente pide confirmación y abre el navegador. Los enlaces con
`type` (p. ej. `REPORT_BUG`) usan un nombre traducido por el cliente.
El cliente **no permite** poner un botón por enlace directamente en el menú de pausa.

**Nativo desde 1.21.6 / 26.x (sin mods), vía datapack:** los *diálogos*. RadiantUI genera
`<mundo>/datapacks/radiantui` con un diálogo `radiantui:quick_menu` (botones de URL y de comando) y lo
añade a los tags `pause_screen_additions` (aparece un **botón destacado en el menú ESC**, p. ej.
"💬 ENLACES ★") y `quick_actions` (tecla de *Acciones rápidas* del cliente). Se abre también con `/ui menu`.

**Resuelto por RadiantUI (configuración + textura):**
- `pausemenu.yml → server-links.links.<id>`: `name` (MiniMessage), `icon` (glifo del pack), `description`,
  `short-name`, `type` opcional, `chat` / `quick-menu` para incluirlo o no en cada sitio.
- `link-format` y `numbered` para que cada enlace quede claramente identificado ("2 · 🛒 Tienda").
- El título "Menú del juego" se reemplaza por el **logo del plugin en blanco y negro** con Pip (la mascota)
  a cada lado (`pause-title`, glifo `pause_logo`), mediante
  `lang-overrides` en todos los idiomas; botones tipo tablón gris con marco claro, bisel y veta diagonal.
- `chat-shortcuts`: al entrar, una línea compacta clicable `[Discord] [Tienda] [Web]…`; lista completa
  con `/ui links`.

> 26.x: `PlayerLinksSendEvent` se lanza en la fase de configuración y ya no tiene `getPlayer()`.
> RadiantUI resuelve el jugador por reflexión (1.21.x y 26.x) y reenvía los enlaces con el tema
> personal del jugador al entrar.

## ✿ Logo del pack y migración

- `pack.png` del resource pack = logo del plugin (`radiantui96.png`, embebido en el jar). Para usar otro,
  pon `plugins/RadiantUI/pack.png`.
- Al actualizar desde la versión rosada, los YAML antiguos se guardan como `*.old-v1.yml` y se generan
  los nuevos (`config-version: 2`).

## ✿ Re-skin de GUIs de otros plugins

En `containers.yml → skins` cada entrada busca una regex en el título y lo reemplaza por
`overlay + título formateado` usando `InventoryOpenEvent#titleOverride` de Paper:

```yaml
skins:
  tienda:
    match: "(?i).*(tienda|shop).*"
    overlay: auto            # chest_<filas>
    title-format: "<glyph:coin> <text>{title}"
```

## ✿ Resource pack y HTTP

- El pack se genera de forma **determinista** (mismo contenido = mismo SHA-1), así el cliente lo cachea.
- URL: `http.public-url` o automática (`server-ip` o IP local detectada). Formato: `/pack/<tema>-<sha1>.zip`.
- Sin HTTP propio: `http.enabled: false` + `pack.external-url: "https://cdn.tld/{theme}.zip"`.
- `pack.mcmeta` declara `pack_format`, `supported_formats` y `min_format`/`max_format` (1.21 → 26.x).
- **Oraxen**: con `pack.oraxen-merge.enabled` se combina `plugins/Oraxen/pack/pack.zip` dentro de nuestro pack
  (fuentes y atlas se fusionan). Desactiva el envío de pack de Oraxen para no mandar dos.

## ✿ API

```java
RadiantUIApi ui = RadiantUIApi.get();
ui.setTheme(player, "snoopy_night");      // null = tema global
ui.setGlobalTheme("snoopy_classic");
ui.refreshHud(player);
ui.refreshScoreboard(player);
ui.resendPack(player);
ui.rebuildPacks().thenAccept(n -> getLogger().info(n + " packs"));

// Tu propio menú con el fondo del tema:
Inventory inv = Bukkit.createInventory(null, 54, ui.containerTitle(player, 6, "<text>Mi menú"));
```

Evento: `dev.privalo.ui.api.event.ThemeChangeEvent` (cancelable).

## ✿ Compilación

Requiere **JDK 21+**.

```bash
mvn package                 # → target/RadiantUI-1.0.0.jar
bash build-local.sh         # usa Maven si existe; si no, javac contra ~/.m2 + jar
```

## Licencia

[MIT](LICENSE) © 2026 Privalo21