# Design v3 - report for feature polish agents

## Tokens (`core/designsystem/Theme.kt`)
- Palette retuned: `OrbitViolet #6A4DF4`, `OrbitVioletSoft #8F78FF` (dark primary), `OrbitPurple #9B5CF6`, `OrbitCyan #0EA5C6`, `OrbitCoral #F0617D`, `OrbitAmber #FFB23F`. All old names still exist. Light bg `#EEF0F9`, dark bg `#0C0B1E`; `colorScheme.surface` is the card base.
- `OrbitExtras` (all old members kept) gained: `cardTop`, `cardBottom`, `cardBrush`, `glassFill`, `glassStroke`, `amber`, `ringBrush`, and helpers `tinted(accent, strength)`, `tintedBrush(accent, strength)`, `softGradient(accent, from, to)`.
- `Spacing.section = 24.dp` (gap between top-level sections). `BOTTOM_BAR_CONTENT_PADDING = 112.dp` (a `val`, Dp can't be `const`).
- `OrbitFontFamily` = Plus Jakarta Sans variable font (5 weights) applied to the whole Typography (title/headline ExtraBold-ish, bodyLarge 15/22, labelMedium 13). Licence: `docs/FONT_LICENSE.md`.

## Components (`Components.kt`)
- `OrbitCard(modifier, onClick, contentPadding, accent, tint: Color? = null, style: CardStyle = Soft, elevation: CardElevation = Soft, content)`. `CardStyle { Soft, Tinted, Glass }`, `CardElevation { Flat, Soft, Lifted }`. Use `OrbitCard(tint = accentColor)` for ordinary cards that need colour, `style = Glass` on hero gradients.
- `IconTile(..., style: IconTileStyle = Solid)`; `IconTileStyle { Solid, Tinted, Soft }`. `OrbitTopBar` uses Soft (sub-screens with back) / Tinted (tab screens) automatically.
- `EmptyState` redone centrally (layered circles, dashed orbit ring, satellites, widthIn(max 320)). `SectionHeader` now wraps/ellipsizes. `isLargeFontScale()` helper for stacking rows at font scale >= 1.3.
- Colour/shape of `OrbitWell`, `GradientButton`, `OrbitSheet` unchanged in API.

## Navigation shell
- `BOTTOM_TABS = [Home, Tasks, Calendar, More]`. `OrbitBottomBar(current, onSelect, onQuickAdd, showHint, onDismissHint)` - notched pill, raised 64dp ring button (amber ring, gradient `+`), label shrinks 12 -> 9sp. First-run hint persists via `SettingsStore.setQuickAddHintDismissed` / `AppSettings.quickAddHintDismissed` (key `quick_add_hint_dismissed`); pressing the centre button also dismisses it.
- `QuickAddSheet(nav, onDismiss)` (core/navigation) with tiles New task / note / habit / project / goal + Search. Strings: `dv3_*` in `strings_design_v3.xml` (values, ur, es; other locales fall back to English until the lead translates).
- `OrbitNavigator.isTab` added (true when the visible screen is one of `BOTTOM_TABS`).

## What other owners must do
- **Projects owner (`ProjectsScreen`)**: Projects is no longer a tab. Pass `onBack = if (!nav.isTab) ({ nav.back() }) else null` to `OrbitTopBar`, and drop the 96dp bottom padding when `!nav.isTab` (no bar). `HomeScreen` currently calls `nav.switchTab(AppDestination.Projects)` (HomeScreen.kt ~line 180): change it to `nav.navigate(AppDestination.Projects)`. (Left as is it still works: stack becomes [Home, Projects], no bar.)
- **More owner**: add Projects as the FIRST tile -> `nav.navigate(AppDestination.Projects)`.
- **Tab screens (Home, Tasks, Calendar, More)**: the shell still hosts the bar in `Scaffold.bottomBar`, so the NavDisplay is already padded by the bar's full height (including the raised button). The old 96/104dp list paddings are therefore redundant extra space; use `BOTTOM_BAR_CONTENT_PADDING` where a "contract" value is wanted (or ~24dp since the shell already reserves the bar). Hard-coded spots: `HomeCommon.kt` (`HomeBottomSpace = 104.dp`), `TasksScreen`, `CalendarScreen`, `MoreScreen`, `NotesScreen`, `ProjectsScreen`, `HabitsScreen`, `GoalsScreen`.
- Replace `staggerIn` usage elsewhere only if rows look half-faded; Language no longer uses it.
- Use `Spacing.section` between Home cards, vary `OrbitCard` style/tint per section.
- Not changed (not in my scope): `OrbitEmblem` (animated splash art), About/More Canvas marks. The launcher/notification icon is the new mark (ring with gap + check + amber satellite); if those Canvas marks should match, draw ring + check + satellite.

## Language screen
Rows >= 64dp, 16dp radius, single LazyColumn (no per-row animation; a 220ms fade of the whole list once), 32dp end padding, `private NativeAdSlot()` item (key `native_ad`) at the end renders nothing - the ads work fills it in.

## Icon
Adaptive vector: indigo->violet gradient + light bloom (`ic_launcher_background`), white check inside an orbit ring with a gap holding an amber satellite (`ic_launcher_foreground`), new `ic_launcher_monochrome` used by both mipmap-anydpi-v26 files, flat white `ic_stat_orbit`. Trend research via web search was not done; design follows the well-known pattern of top planner apps (rounded gradient square, one bold white glyph, single accent dot) without copying any mark. No Gradle was run.
