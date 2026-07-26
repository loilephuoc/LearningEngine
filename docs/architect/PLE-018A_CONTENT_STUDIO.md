# PLE-018A — Content Studio Layout Rework

**Status:** Authorized for implementation  
**Target baseline:** `94c26b9` — `feat: guard Learning Browser navigation with unsaved changes`  
**Owner:** Chief Architect  
**Implementer:** Antigravity  
**Scope type:** Desktop layout, interaction architecture, and visual UX  

## 1. Role and execution boundary

You are implementing the approved Product Architecture for Learning Engine. You are not creating an unrelated redesign.

The current Learning Browser must evolve into **Content Studio**: a professional desktop Content IDE inspired by JetBrains IDEs, VS Code, Figma Inspector, and professional asset-management tools.

The supplied mockups are **Product Vision**, not pixel-perfect specifications. Preserve their workspace philosophy, information hierarchy, focus, and interaction model. Improve spacing, sizing, responsiveness, and component arrangement when a better desktop solution is evident. The finished result must clearly feel like an evolution of the mockups, not a different application.

When a trade-off exists, prioritize:

1. professional desktop UX;
2. daily usability for large libraries;
3. architectural maintainability;
4. responsiveness and performance;
5. visual similarity to the mockup.

Do not optimize only for today's dataset. Design for a future library of **100,000+ contents** and for a user who may edit content for many hours per day.

## 2. Repository contract

Before changing code:

```powershell
git status
git branch --show-current
git log -6 --oneline
```

Required starting state:

- branch: `develop`;
- HEAD: `94c26b9` or an explicitly newer Product Owner-approved baseline;
- working tree: clean.

Rules:

- do not reset;
- do not revert prior accepted capabilities;
- do not squash or amend accepted commits;
- do not push;
- do not start PLE-018B;
- preserve Source of Truth and existing architectural boundaries;
- inspect real source and existing tests before designing new APIs.

If the starting state differs materially, stop and report the discrepancy instead of guessing.

## 3. Primary objective

Transform the package-scoped Learning Browser into **Content Studio**, a full-height, focused workspace with three coordinated panes:

```text
Content Explorer  |  Content Editor  |  Media & Details
```

When Content Studio is open:

- it becomes the primary workspace;
- Library dashboard, collection cards, statistics, and unrelated sections below it are not rendered;
- Content Studio uses nearly all available application height;
- `Back to Library` remains the clear exit path;
- existing library state must be preserved when returning.

This capability must preserve all accepted PLE-017A behavior:

- edit;
- double-click edit;
- save persistence;
- discard;
- safe delete;
- unsaved-changes guard;
- package browsing;
- search, filters, sort, selection, and error behavior.

## 4. Product interaction principles

Every implementation decision should reduce one or more of:

- unnecessary clicks;
- unnecessary scrolling;
- cognitive load;
- loss of context;
- accidental data loss;
- waiting when navigating large libraries.

Primary actions must remain discoverable and visible. Selection must never become ambiguous. The user should naturally scan from Explorer → Editor → Media/Quality without losing focus.

## 5. Global workspace architecture

Use a responsive three-pane desktop layout:

### Left — Content Explorer

A compact, high-density list for navigating thousands of contents.

### Center — Content Editor

The main editing surface, receiving remaining width and supporting vertical scrolling only when needed.

### Right — Media & Details Inspector

A stable inspector for image metadata, audio assets, and initial quality status.

The panes remain visible together at normal desktop widths. Use graceful degradation at narrower widths; do not allow any pane to collapse into unusable slivers.

Prefer Compose layout primitives such as:

- `weight()`;
- `fillMaxWidth()`;
- `fillMaxHeight()`;
- `BoxWithConstraints()`;
- minimum/maximum constraints where genuinely required.

Do not build the layout from a collection of arbitrary fixed pixel widths. Small fixed values are acceptable for icons, minimum touch targets, separators, and deliberate compact controls—not for the entire workspace architecture.

## 6. Content Explorer contract

Replace all content pagination with virtual scrolling.

Required:

- `LazyColumn` or equivalent lazy virtualized list;
- always-usable vertical scrollbar with a visible draggable thumb;
- mouse-wheel scrolling;
- Page Up / Page Down;
- Home / End;
- keyboard row navigation where existing behavior supports it;
- selected row automatically kept visible;
- no numbered page controls;
- no `1 2 3 … 66` navigation.

The explorer contains:

- package title and concise content count;
- search field;
- lesson filter;
- media filter;
- sort control;
- scrollable rows;
- compact total/result count.

Each row should show, at minimum:

- stable ordinal or compact identity cue;
- Question;
- Vietnamese Answer;
- selected state;
- concise media/status indicators derived from real available data.

Do not add fake status icons that imply real validation. If only basic media availability is known, display only that truthfully.

Performance requirements:

- must remain responsive with 10,000+ rows;
- do not eagerly compose the whole collection;
- avoid recreating expensive derived row state on every recomposition;
- use stable keys;
- do not trigger full-list persistence queries for every row render.

## 7. Content Editor contract

### 7.1 Sticky editor toolbar

Keep primary actions visible while editing:

- Save;
- Discard;
- Delete;
- Duplicate — disabled placeholder only;
- AI Assistant — disabled placeholder only;
- History — disabled placeholder only;
- optional overflow menu for secondary future actions.

Placeholder controls must be visually and semantically disabled. They must not pretend to work and must not introduce domain behavior.

### 7.2 Field structure

Display independent editable fields in this order:

1. Question;
2. Answer;
3. IPA;
4. POS;
5. Example (English);
6. Translation (Vietnamese);
7. Image.

**Example and Translation are separate semantic fields.** Never concatenate English and Vietnamese into one text area. The currently observed combined rendering must be removed.

Preserve the existing canonical persistence route and IDs. Do not invent parallel content storage.

### 7.3 Audio affordances beside fields

For each semantic field that has a real audio asset, provide an individual play/stop affordance:

- Question Audio;
- Answer Audio;
- Example Audio;
- Translation Audio.

Rules:

- enable only when the package actually contains a resolvable asset;
- disable or clearly mark unavailable when absent;
- one active playback at a time unless the current media architecture explicitly supports more;
- Play must visibly become Stop while playing;
- playback failures must surface safely and not mutate content;
- do not silently map one audio asset to the wrong field;
- preserve the existing media service/adapter architecture.

This capability may wire existing playback support where available. Complex waveform generation, recording, transcoding, and new codec infrastructure are non-goals.

## 8. Image contract

Restore and preserve image display. Image is a first-class part of the content, not a tiny thumbnail.

Required:

- a large, clear image viewer in the center editor;
- preserve aspect ratio;
- no distortion or aggressive thumbnail scaling;
- use the available center width effectively;
- target roughly 30–40% of the useful editor viewport when an image exists, while remaining responsive;
- meaningful empty/unavailable state when no image exists;
- show actual image rather than only a file path.

Provide working controls where feasible with existing architecture, otherwise truthful disabled placeholders:

- Open Fullscreen;
- Zoom out;
- current zoom percentage;
- Zoom in;
- Fit Width;
- Fit Height;
- reset to 100%.

At minimum, the restored large image must work in this capability. Do not remove current image support while reorganizing UI.

## 9. Media & Details Inspector contract

The right pane is an inspector, not a duplicate editor.

### Image details

Show truthful available metadata where resolvable:

- preview thumbnail;
- availability;
- filename;
- file type;
- file size;
- dimensions;
- Open or focus image action;
- Replace as a disabled placeholder unless replacement is already safely supported.

### Audio assets

Provide separate inspector rows for:

- Question Audio;
- Answer Audio;
- Example Audio;
- Translation Audio.

Each row may show:

- availability;
- Play/Stop;
- duration if reliably known;
- progress if already supported;
- source/file details when useful.

A decorative waveform is permitted only if clearly a visual placeholder and not represented as measured audio data. Prefer a simple progress track over fabricated waveform data.

## 10. Initial Quality & Validation panel

Create the visual and component structure only. Do not implement a broad validation engine in PLE-018A.

The panel may truthfully project currently known checks such as:

- image present/missing;
- each audio asset present/missing;
- IPA non-empty;
- Example non-empty;
- Translation non-empty.

Duplicate detection, orphan analysis, IPA format validation, and advanced quality scoring must be disabled or marked `Not evaluated` unless backed by real application data. Never display fabricated `Good` results.

The future validation engine belongs to a later capability.

## 11. Visual design contract

Use and refine the approved modern visual direction:

- light neutral surfaces;
- restrained purple accent;
- clear typography hierarchy;
- compact professional density;
- consistent rounded cards and controls;
- subtle dividers;
- strong selected-row state;
- accessible contrast;
- no decorative clutter;
- no flashy animation;
- no mobile-style oversized controls.

The current app theme may be improved locally through reusable tokens/components, but do not perform an uncontrolled whole-application theme rewrite. Content Studio must still belong to Learning Engine.

Reduce excessive whitespace. Prioritize content density without becoming cramped.

## 12. Component architecture

Do not grow a monolithic composable or move business logic into UI files.

Prefer cohesive components such as:

- `ContentStudioScreen`;
- `ContentStudioToolbar`;
- `ContentExplorerPane`;
- `ContentExplorerToolbar`;
- `ContentList`;
- `ContentRow`;
- `ContentEditorPane`;
- field editor components;
- `ContentImageViewer`;
- `MediaInspectorPane`;
- `ImageAssetInspector`;
- `AudioAssetInspector`;
- `QualityPanel`.

Names may adapt to the existing codebase, but responsibilities must stay separated.

Rules:

- Desktop UI projects application state; it does not own domain persistence;
- do not add layout-only state to `ContentLibraryViewModel` unless interaction requires it;
- keep Compose-only scroll, focus, zoom, and pane state local where appropriate;
- use immutable UI state and explicit callbacks;
- reuse PLE-017A pending-action and dirty-guard architecture;
- do not duplicate save/delete/query logic.

## 13. Responsive and accessibility behavior

Verify at multiple practical desktop window sizes.

Required:

- center editor receives flexible remaining width;
- explorer and inspector maintain reasonable minimum widths;
- text does not overlap or become inaccessible;
- important actions remain reachable;
- scrollbars and focus indicators remain visible;
- keyboard navigation continues to work;
- tooltips or accessible labels exist for icon-only actions;
- disabled placeholders are clearly distinguishable.

If the window becomes too narrow for three usable panes, implement a deliberate fallback based on existing architecture—such as a collapsible inspector—rather than squeezing all panes into unusable widths. Do not invent a mobile navigation model.

## 14. Regression contract

Must preserve and test:

- opening the package-scoped workspace;
- Back to Library;
- search;
- lesson/media filters;
- sorting;
- row selection;
- double-click editing;
- Save persistence;
- Discard;
- safe Delete;
- unsaved-changes Save/Discard/Cancel guard;
- pending actions executing exactly once;
- save/delete failure behavior;
- package and content identity;
- media references;
- existing tests.

No regression is acceptable merely to obtain the new appearance.

## 15. Non-goals

Do not implement in this capability:

- AI generation or assistant behavior;
- content history/versioning;
- duplicate-content mutation logic;
- full validation engine;
- batch operations;
- image replacement/editor;
- audio recording;
- audio transcoding;
- new media codecs;
- advanced waveform extraction;
- PLE-018B features beyond minimal wiring required to expose already-existing assets.

## 16. Implementation approach

1. Inspect current browser state, UI components, media projection, and PLE-017A tests.
2. Produce a concise architecture and file plan before editing.
3. Implement in coherent internal slices, keeping the working tree recoverable.
4. Run focused tests after each meaningful slice.
5. Perform a real visual run of the Desktop app.
6. Refine layout after seeing the rendered result; do not rely only on compilation.
7. Run the full verification gate.

Do not create multiple capability commits unless recovery safety requires it. The final accepted capability commit remains one coherent commit.

## 17. Test and verification evidence

Run focused browser/editor tests first, including the existing `PackageContentBrowserEditStateTest` suite and any relevant navigation/media projection tests.

Then run:

```powershell
.\gradlew.bat clean test
```

Also run the app:

```powershell
.\gradlew.bat :desktop:run
```

Capture before/after screenshots from the real application. Generated mockups are not implementation evidence.

Manual UAT must cover at least:

- full-height takeover and Back to Library;
- lazy list scrolling using wheel and scrollbar thumb;
- no pagination controls;
- selected row retained and kept visible;
- Example and Translation displayed separately;
- large image visible and correctly scaled;
- Question/Answer/Example/Translation audio availability mapped correctly;
- Save, Discard, Delete, and dirty guard still work;
- resize behavior at normal and narrower desktop widths.

## 18. Self-design review

Before committing, review the real application as a professional content editor, not as a developer.

Ask:

- Is it immediately clear what is selected and editable?
- Is there unnecessary scrolling?
- Are primary actions always visible?
- Does the eye naturally flow Explorer → Editor → Media/Quality?
- Is the image large enough to be genuinely useful?
- Is each audio asset easy to locate and correctly mapped?
- Can thousands of contents be navigated quickly without pagination?
- Would this remain comfortable with 100,000 contents?
- Would this be acceptable for eight hours of daily use?

If the answer is no, improve the UX before committing. Small improvements within the approved architecture are encouraged. Large deviations require Product Owner approval.

## 19. Commit contract

Only after focused tests, full tests, visual run, and self-review pass:

```powershell
git add ...
git commit -m "feat: redesign Learning Browser into Content Studio"
git status
```

Required final state:

- working tree clean;
- no push;
- PLE-018B not started.

## 20. Final report

Report:

- starting and final commit SHA;
- layout architecture;
- component decomposition;
- responsive decisions;
- Explorer scrolling implementation;
- Example/Translation separation;
- image restoration and sizing behavior;
- real audio-field mapping;
- Quality panel truthfulness limitations;
- files changed;
- focused test names and counts;
- full test result;
- manual UAT result;
- real before/after screenshot paths;
- known limitations;
- `git status`;
- confirmation no push and no PLE-018B work.
