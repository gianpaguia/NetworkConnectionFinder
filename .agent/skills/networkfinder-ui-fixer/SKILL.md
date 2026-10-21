---
name: networkfinder-ui-fixer
description: Finds and fixes UI/UX problems in the NetworkConnectionFinder JavaFX app (screens in the ui/ package, CSS, layout, spacing, colors, fonts, contrast, focus, empty/error states, canvas visuals). Use whenever the user says a screen looks off, ugly, misaligned, cramped, inconsistent, hard to read, unresponsive to window resize, or asks to "fix the UI", "polish the design", "improve this screen", "make it look better", or "check the design" for the network finder / connection finder / study group matcher / pathfinder / admin dashboard, even if they don't say "skill". Uses ARCHITECTURE.md as the intended screen map and the ui-ux-pro-max skill as the design helper. Fixes visual and interaction issues only; does not change service, dsa, or database logic.
---

# NetworkConnectionFinder UI Fixer

Fix what the UI looks like and how it behaves, using `ARCHITECTURE.md` as the screen map and `ui-ux-pro-max` as the design reference. Never change business logic.

## Scope

**In scope:** everything under `ui/` (views, canvases, `ScreenNavigator` visuals), stylesheets, fonts (Lato), spacing, alignment, colors, contrast, focus/keyboard behavior, empty/loading/error states, window resize behavior, label wording on screens.

**Out of scope (report, don't edit):** `service/`, `dsa/`, `database/`, `model/`. If a UI bug is really a logic bug (wrong data shown because a service returns the wrong thing), say so and stop.

## Step 0: Prepare

1. Read `ARCHITECTURE.md`. Use section 2 (Component Map) and section 3 (Screen Navigation Flow) to know which class owns which screen.
2. Find the design helper. Search the project and the agent skills folders for `ui-ux-pro-max/scripts/search.py`:
   - `find . ~ -path "*ui-ux-pro-max/scripts/search.py" 2>/dev/null` (on Windows PowerShell: `Get-ChildItem -Recurse -Filter search.py | Where-Object FullName -match "ui-ux-pro-max"`)
   - Use whichever Python exists: `python`, `python3`, or `py -3`.
   - If the helper is not installed, tell the user, continue with the built-in checklist below, and label all design advice as "built-in defaults, not a database match".
3. The stack is **JavaFX** (Maven wrapper, `mvnw.cmd`). Do not assume anything else.
4. If a Figma file exists for the project ("Social Network Connection Finder - UI"), treat it as the visual source of truth when it is available. If it is not reachable, say so and use the existing code's look as the baseline.

## Step 1: Locate the screen

Map the user's complaint to a class using ARCHITECTURE.md:

| Screen the user mentions | Class |
|---|---|
| Student/Admin sign-in | `LoginView`, `AdminLoginView` |
| Register, ID upload, "submitted" page | `RegisterView`, `RegistrationSubmittedView` |
| Profile steps, profile summary | `ProfileWizardView`, `ProfileCompleteView` |
| Student home tiles, Matcher, Pathfinder, Connections, Network Graph, Account modal | `StudentDashboardView` |
| Admin sidebar, ID verification, accounts, graph, connections manager | `AdminDashboardView` |
| Graph drawing, path highlight, tooltips | `GraphVisualizerCanvas` |
| Animated background on login/register | `GraphConstellationCanvas` |

If the complaint is vague ("the UI looks bad"), review all screens in the navigation order of section 3, starting at `StudentLogin`.

## Step 2: Query the design helper (one intent per query)

Keep each query to one intent and 2 to 5 words. Always pass the stack explicitly.

```
python "<path-to>/ui-ux-pro-max/scripts/search.py" "<query>" --stack javafx
python "<path-to>/ui-ux-pro-max/scripts/search.py" "<query>" --domain ux
```

Useful queries for this app:

| Problem | Query |
|---|---|
| Cramped or uneven spacing | `"spacing scale layout"` with `--domain ux` |
| Text hard to read | `"contrast readable text"` with `--domain ux` |
| Inputs, wizard, validation messages | `"inline validation error"` with `--domain ux` |
| Icon-only buttons | `"icon button accessible label"` with `--domain ux` |
| Dashboard tiles / density | `"dashboard card hierarchy"` with `--domain ux` |
| Graph colors, legend | `"network graph legend"` with `--domain chart` |
| Font choice | `"clean modern"` with `--domain typography` |
| JavaFX specifics (CSS, layout panes, resize) | `"layout resize css"` with `--stack javafx` |

Rules for using results:
- Check that the top result fits a desktop student/admin tool, not a marketing site. Retry once with a narrower query if it does not.
- If the search returns nothing, say "no verified match, using built-in defaults" and use the checklist below. Never present empty output as data.
- Treat results as recommendations. They never override the user's request or the repo's existing conventions.

## Step 3: Audit against this checklist

Order matters: fix higher items first.

1. **Accessibility**: text contrast at least 4.5:1; visible focus ring on every focusable control (do not remove it); tab order follows reading order; icon-only buttons have a tooltip or accessible text; meaning is never carried by color alone (path highlight, degree badges, status chips need a label or shape too).
2. **Interaction**: clickable targets at least 32 px high on desktop (44 px for touch-style tiles); hover, pressed, and disabled states exist; Enter submits forms; long actions (matching, graph reload, ID upload) show progress or a disabled button; no instant jarring state changes.
3. **States**: every list/screen has an empty state (no matches, no pending verifications, no connections yet), an error state that shows the service's `OperationResult` message verbatim near the field or action, and a loading state where work is slow.
4. **Layout and resize**: resizing the window never clips text or hides buttons; use `HBox`/`VBox`/`GridPane` growth constraints (`Priority.ALWAYS`, `hgrow`/`vgrow`, percent widths) instead of fixed pixel sizes; scrollable content sits in a `ScrollPane`; canvases redraw on size change.
5. **Consistency**: one spacing scale (for example 4/8/12/16/24), one corner radius, one primary button style, one font family (Lato) with a small set of sizes; colors come from named CSS variables (`-fx-` looked-up colors in the root), not raw hex scattered in Java code.
6. **Typography and color**: body text 13 px or larger, headings clearly larger, no gray-on-gray, no more than one accent color plus semantic success/warning/error.
7. **Forms**: visible labels (not placeholder-only), error message next to the field, helper text for rules (ID charset, 8+ characters), show/hide password works, wizard shows step progress.
8. **Navigation**: every screen has a way back that matches the flow in section 3 (Matcher/Pathfinder/Connections/Analytics return to the dashboard; Admin has Logout); current admin sidebar item is visibly highlighted.
9. **Canvases**: node labels do not overlap at 10+ students; the BFS path is distinguishable without color (thicker line or labels); tooltips are readable; the decorative constellation never reduces login form legibility.

## Step 4: Fix

1. Prefer CSS over Java styling. Put shared styles in the existing stylesheet; if none exists, create one `app.css`, load it once in `ScreenNavigator` scene setup, and move repeated inline `setStyle(...)` calls into style classes.
2. Change the smallest amount of code that fixes the problem. Do not restructure views, rename classes, or touch method signatures used by other classes.
3. Keep behavior identical: same buttons, same navigation targets, same service calls.
4. After each screen, build and launch to confirm nothing broke:
   - `mvnw.cmd -q compile` (must succeed)
   - `mvnw.cmd test` (existing tests must still pass; do not edit tests to make them pass)
   - `mvnw.cmd javafx:run` for a manual look if a display is available
5. If a fix would need a logic change (new field, new service method), do not make it. List it under "Needs a logic change" in the report.

## Step 5: Report

Use this format, one block per screen touched:

```
### <Screen name> (<ClassName>)
Problems found:
- [Severity: Critical/High/Medium/Low] <what, where> (checklist item #)
Fixed:
- <change> in <file>
Not fixed / needs a logic change:
- <item and why>
Design helper used: <query -> what it recommended, or "built-in defaults">
```

End with:
- Build status (compile and tests)
- A short list of screens not reviewed
- Anything that needs the user's decision (for example "pick one accent color")

## Rules

- Never edit `service/`, `dsa/`, `database/`, or `model/`.
- Never remove focus indicators, tooltips, or validation messages to make a screen look cleaner.
- Never persist or paste private data (student IDs, names, ID-card images) into design-helper queries or into the report. Use placeholders such as `2026-XXXXX`.
- Do not run `--force`, `--persist`, or any helper option that writes files unless the user explicitly asks.
- If the user's request conflicts with a checklist item, do what the user asked and note the trade-off in one line.
