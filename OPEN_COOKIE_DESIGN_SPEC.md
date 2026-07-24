# Open Cookie — Design & Interaction Specification

**Document status:** Source of truth  
**Project:** Open Cookie  
**Audience:** Developers, AI coding agents, designers  
**Last updated:** 2026-07-24

---

## 1. Purpose of this document

This document is the visual and interaction source of truth for the Open Cookie app.

Before changing the cookie-opening experience, any developer or AI coding agent must read this document and inspect the current implementation.

When implementation details and this specification conflict, evaluate the implementation against the intended choreography described here.

This document describes intended UX and visual behavior, not exact Kotlin implementation details. Exact animation durations, offsets, easing curves, and internal variable names may change as long as they preserve the intended visual experience.

The goal is not merely to make the code technically functional. The goal is to create a polished, coherent, satisfying visual experience in which the user feels that one physical event is happening:

> The user taps the cookie → the cookie presses, resists, and cracks → `cookie_broken_closed` holds while the blockchain transaction is processed → the cookie opens wider → the fortune paper emerges → the prediction is revealed.

The animation must feel like a continuous physical event, not like several unrelated PNG files being swapped and animated independently.

---

# 2. Core product concept

Open Cookie is a simple, playful experience centered around opening a fortune cookie.

The main interaction is:

1. User sees a beautiful intact fortune cookie.
2. User understands that the cookie itself is the thing to interact with.
3. User taps the cookie / BREAK action.
4. The intact cookie reacts immediately with a subtle press / compression.
5. The intact cookie briefly resists and trembles with restrained tension.
6. A clear crack impulse occurs, supported by visual and tactile feedback.
7. The intact cookie transitions into `cookie_broken_closed`.
8. The app waits for the blockchain transaction while the cookie remains broken but closed / near-closed.
9. The transaction is confirmed and the actual result is retrieved.
10. The broken-closed cookie transitions into the separate left/right halves.
11. A small fortune paper is already present behind the cookie while waiting; as the halves open, the paper is physically revealed between them — not via fade-in.
12. The paper zooms forward along a gentle arc, passes over the cookie halves, and settles into its final readable size and position in the foreground.
13. After a brief pause, the prediction text is revealed left-to-right across all lines simultaneously.
14. The user can open another cookie via the bottom action or by tapping the revealed cookie/paper area once reset is unlocked.

The experience should feel warm, elegant, tactile, playful, and polished.

It should NOT feel like:
- a generic Android UI demo;
- a collection of unrelated animations;
- a physics simulator;
- a game with excessive particle effects;
- a technical blockchain transaction screen.

The blockchain transaction is part of the underlying mechanism, but the visual experience should remain simple and magical.

The cookie should feel as if it is being physically cracked open by hand.

It should NOT feel like the cookie instantly explodes, flies apart, or swaps into a fully open composition before the transaction is confirmed.

---

# 3. Visual direction

The visual language should be based on the beautiful early Open Cookie references developed for this project.

Important characteristics:

- warm, refined background;
- elegant typography;
- restrained, sophisticated color palette;
- warm cream / golden cookie tones;
- soft glow and subtle depth;
- visually pleasing contrast;
- generous spacing;
- premium rather than childish;
- playful but not cartoonish;
- calm motion;
- no random gradients or colors added without a reason.

The visual design should feel intentional.

Avoid:
- generic Material defaults;
- random fonts;
- overly bright saturated colors;
- childish typography;
- ugly gray shadows;
- arbitrary green or neon accents;
- large empty rectangular image backgrounds;
- visible checkerboard backgrounds;
- assets that look pasted on top of one another.

The top title "Open Cookie" should remain elegant and visually integrated with the design.

Do not add unnecessary explanatory text about "fortune" on the home screen unless explicitly requested. The current main visual hierarchy should focus on the cookie and the interaction.

---

# 4. Main interaction states

The primary state machine is:

```text
IDLE
  ↓
PRESS
  ↓
TENSION
  ↓
CRACK
  ↓
COOKIE_BROKEN_CLOSED
  ↓
WAITING_FOR_TRANSACTION
  ↓
REVEALED
```

There may be implementation-specific internal state names, but the user-facing conceptual choreography must remain:

```text
IDLE → PRESS → TENSION → CRACK → COOKIE_BROKEN_CLOSED → WAITING_FOR_TRANSACTION → REVEALED
```

The blockchain transaction is the source of truth for the transition to `REVEALED`.

Do not reveal the prediction before the transaction result is actually available.

Conceptually:

- `PRESS` is the immediate tactile response to the tap while the cookie remains intact.
- `TENSION` keeps the intact cookie visible while it subtly resists and trembles.
- `CRACK` is the short physical break impulse.
- `COOKIE_BROKEN_CLOSED` is the production bridge visual state immediately after the crack.
- `WAITING_FOR_TRANSACTION` keeps `cookie_broken_closed` visually stable while the transaction is processed.
- `REVEALED` begins only after confirmation, when the halves open wider, the hidden paper is uncovered, the paper zooms forward, and the text is revealed.

Current Android implementation maps these concepts to:

```text
IDLE
BREAKING            // press + tension + crack + transition to broken-closed
WAITING_FOR_TRANSACTION
REVEALED
```

`BREAKING` contains the internal press/tension/crack choreography described in this document.

---

# 5. IDLE state

The user sees:

- one intact fortune cookie;
- centered in the main visual area;
- elegant Open Cookie branding;
- a beautiful background;
- an obvious but subtle indication that the cookie is interactive.

The cookie may have a very subtle idle animation:

- slow breathing / scale;
- tiny rotation;
- gentle floating;
- subtle glow.

The idle animation must be slow and calm.

It must NOT:
- bounce aggressively;
- continuously rotate;
- look like a loading spinner;
- distract from the cookie.

## Tap hint

A transparent warm-gold line-art hand can be used to show the user how to interact.

The hand should:
- visually point toward the cookie;
- approach the cookie;
- perform a subtle tap gesture;
- optionally create a small ripple;
- then disappear or be marked as seen.

The hand must be a real custom transparent visual, not a white system emoji or a generic Android icon.

The hand must NOT:
- look like a white blob;
- appear as a system hand emoji;
- have a visible rectangular background;
- repeatedly pop in unexpectedly.

In DEBUG builds it may be shown repeatedly for development/testing.

In RELEASE builds it should respect persistence and not repeatedly annoy the user.

---

# 6. BREAKING state

This is the most important animation.

The goal is to create the illusion of one physical cookie breaking.

The animation should not feel like:

```text
intact PNG disappears
→
two PNGs suddenly appear
```

Instead it should feel like:

```text
whole cookie
→
immediate subtle press / compression
→
brief resistance and restrained trembling
→
clear crack impulse
→
cookie becomes cookie_broken_closed
→
individual crumbs scatter from the break line
→
cookie waits in the broken-closed state
→
transaction confirms
→
halves open wider and the fortune paper emerges
```

## Recommended choreography

Approximate timing; exact timing may be tuned visually.

### Phase A — tactile press

The intact cookie must respond immediately after the tap.

It can briefly:
- squash slightly;
- compress as if pressed by a finger;
- shift by a tiny amount;
- rotate by a very small angle.

This is not a long delay. It is the immediate physical response that tells the user the cookie was touched.

This should feel like the user has applied physical pressure to the cookie, not like an impact or explosion.

### Phase B — tension / anticipation

After the initial press, the intact cookie should remain visually intact for a short moment.

The cookie may:
- hold the compression briefly;
- release subtly;
- make small horizontal trembling movements;
- rotate by a tiny amount;
- perform one or two restrained tension pulses.

The intended feeling is:

> The cookie is resisting and is about to crack.

The motion must remain elegant and subtle.

It must NOT:
- look like a UI error shake;
- shake the entire screen;
- feel aggressive or chaotic;
- imply the cookie has already opened.

The current implementation direction uses an anticipation sequence of approximately 600–800 ms overall before the visual crack state takes over. Exact timing, offsets, and easing may be tuned in implementation as long as the tactile feeling remains immediate and the anticipation remains readable.

### Phase C — crack impulse

At the actual crack moment:

- the intact cookie receives one slightly sharper micro-jolt;
- the crack reads as a physical break;
- haptic feedback should fire at the same moment;
- the intact cookie transitions into `cookie_broken_closed`.

The transition must preserve the apparent scale, position, and visual identity of the cookie.

Avoid sudden teleportation.

The immediate post-break state is:

```text
INTACT COOKIE
→
CRACK
→
COOKIE_BROKEN_CLOSED
```

not:

```text
INTACT COOKIE
→
instantly separated left/right halves
```

### Phase D — cookie_broken_closed bridge

`cookie_broken_closed` is a real production visual state.

It is not merely a reference image.

It exists because the separate left/right broken-half assets cannot convincingly represent the immediate post-break state by themselves.

During this phase:
- the cookie has broken;
- the two sides are still visually close;
- the fortune paper is not visible as a full sheet;
- the scene is calm enough to become the waiting state.

### Phase E — haptic feedback

Haptic feedback is part of the intended interaction and currently substitutes for the lack of sound.

The tactile sequence should support the visual sequence:

- PRESS: very light haptic feedback may occur immediately on tap.
- CRACK: a short, slightly more noticeable haptic response should occur at the exact visual crack / transition moment.

The haptic must remain subtle and pleasant.

Do not use:
- continuous vibration;
- aggressive vibration;
- annoying repeated pulses.

Future possibility: a short crack sound may later be added and synchronized with the same visual crack moment and haptic event.

Sound is not currently required.

### Phase F — individual crumb scatter

At the exact moment of the break, individual cookie crumbs appear as a direct consequence of the crack.

Production uses **separate single-crumb PNG assets** (`crumb_01` … `crumb_13`). Each file contains one crumb on a transparent background with no shadow.

This is **not** a classic particle system.

Each displayed crumb is an independent object with its own:
- start position near the break line;
- initial impulse;
- trajectory;
- size / scale;
- rotation;
- delay;
- duration;
- optional small bounce;
- final resting position.

The same PNG may be reused multiple times with different parameters.

Desired physical feeling:

```text
crack
→ short initial impulse
→ brief inertial motion
→ gravity / fall
→ optional small bounce
→ quick damping
→ full stop
```

Crumbs must NOT feel like confetti, fireworks, smoke, or endless flying particles.

Active flight should be relatively short (roughly ~500–900 ms per crumb, varying by instance).

After motion completes, each crumb remains visible at its final position.

### Phase G — pending broken-closed hold

The end of the initial break should establish the `WAITING_FOR_TRANSACTION` composition:

- the cookie is cracked but still closed / near-closed;
- `cookie_broken_closed` is the primary visual object;
- the separate left/right halves are not yet the primary composition;
- the fortune paper is hidden;
- released crumbs continue their independent motion and begin to settle.

The scene should feel like the cookie has just cracked and is waiting for the next real event.

---

# 7. Crumb animation rules

Crumbs are a separate visual system from the cookie halves.

This is critical.

The cookie halves may:
- shake;
- rotate;
- separate;
- settle.

The crumbs must NOT inherit the same movement.

A crumb that has fallen away from the cookie must not continue vibrating together with the cookie.

The intended conceptual crumb timing is:

```text
PRESS / TENSION
↓
no major crumbs yet
↓
CRACK
↓
individual crumbs launch from the break line
↓
crumbs fall / scatter with independent trajectories
↓
some crumbs settle near / below / beside the cookie
↓
no continuous crumb animation
```

Do NOT create:
- infinite crumb loops;
- crumbs continuously respawning;
- crumbs teleporting back to their initial position;
- crumbs appearing from nowhere;
- all crumbs moving identically or symmetrically;
- crumbs permanently attached to the cookie halves;
- composite full-stage crumb overlays that move as one image;
- crumbs resting on top of moving cookie halves after the halves separate.

The entire crumb sequence should happen once per BREAK action.

After the crumb sequence finishes, the waiting state should be visually calm, with settled crumbs allowed to remain visible.

## Current production crumb system

Production uses only individual assets:

```text
crumb_01 … crumb_13
```

Each asset is one crumb. Do **not** use legacy composite crumb layers:
- `crumb_burst` (deprecated)
- `crumb_fall` (deprecated)
- `crumb_particles` (deprecated)

Legacy composite assets in `android/design/cookie_opening_v2/` are reference-only for the old system and must not be mixed into the current implementation.

## Crumb size and quantity

The scene should contain enough visible crumbs to feel like a real break, but not so many that it reads as an explosion.

Use a mix of:
- a few larger fragments;
- several medium pieces;
- many small crumbs / dust-like pieces.

The same PNG may appear more than once with different scale, rotation, and trajectory.

Scale variation must remain natural. Avoid huge crumbs next to microscopic ones in the same cluster.

## Crumb motion character

Prefer:
- asymmetric, independent trajectories;
- short post-crack motion;
- slight rotation during flight;
- small, uneven bounce on only some crumbs;
- quick settling.

Avoid:
- long graceful arcs;
- identical left/right symmetry;
- strong bouncy ball behavior;
- crumbs flying far off-screen;
- infinite rotation after landing.

## Random scatter scenarios

Each BREAK randomly selects one of several predefined scatter scenarios and keeps it fixed for the entire break cycle.

Current production direction includes six scenario presets with different character, for example:
- balanced scatter;
- left-heavy scatter;
- right-heavy scatter;
- compact center scatter;
- wide asymmetric scatter;
- chunky scatter with more large fragments.

Scenarios must differ in density, direction bias, size mix, and final resting pattern — not merely by tiny coordinate offsets.

The selected scenario must not change mid-animation.

## Final resting positions

After motion completes, crumbs remain where they landed.

They must:
- stay visible;
- stop moving;
- stop rotating;
- not loop or respawn.

Final positions should be distributed naturally around the cookie:
- some closer to the break;
- some lower;
- some slightly left or right;
- different vertical levels;
- different sizes.

Do not scatter crumbs far across the whole screen.

Critically, final positions must account for the later separation of the cookie halves during `REVEALED`.

Settled crumbs must **not** appear to rest on the moving surface of a cookie half once the halves open apart.

They should read as separate objects lying around the broken cookie, not glued to the halves.

## Layer order

Crumbs are independent scene objects.

During `REVEALED`:
- crumbs may remain visible around the broken cookie;
- separated cookie halves render above crumbs where overlap would otherwise make crumbs look attached to the moving halves;
- the fortune paper and text render above crumbs when the paper moves to the foreground.

Crumbs must never cover the paper or prediction text.

---

# 8. WAITING_FOR_TRANSACTION state

At this point:

- the cookie is already broken;
- `cookie_broken_closed` is the primary visual state;
- the two sides are still visually close;
- the fortune paper is not yet visible as a sheet;
- some crumbs may still be settling independently;
- the app is waiting for blockchain confirmation.

The scene should now become calm.

The broken-closed cookie may have:
- extremely subtle micro-movement;
- a tiny settling motion;
- occasional very small movement.

But this must be restrained.

The sides should remain visually close during this state.

The cookie should look freshly cracked and near-closed, not fully opened.

The waiting state should NOT look like:
- continuous shaking;
- constant particle effects;
- endless falling crumbs;
- a loading animation disguised as cookie physics.

The crumbs should complete their main motion independently and settle naturally.

Already-released crumbs must not shake together with the cookie halves.

The scene should communicate:

> The cookie has cracked open. Something is inside, but the final reveal is still waiting for confirmation.

During the initial waiting state, the fortune paper object may already exist in the scene, but it must remain:
- fully hidden behind `cookie_broken_closed`;
- small in scale;
- fully opaque as an object (no paper fade-in);
- invisible to the user until the confirmed reveal begins.

The waiting state must NOT look like the completed reveal. The user should not perceive a readable sheet, emerging text, or final paper size before confirmation.

---

# 9. Transaction and wallet flow

The blockchain transaction is real and must remain the source of truth.

The animation must not fake a successful result.

Conceptually:

```text
User taps BREAK
↓
PRESS / TENSION / CRACK animation starts
↓
cookie_broken_closed appears
↓
WAITING_FOR_TRANSACTION with the broken cookie held close
↓
Wallet interaction / signing
↓
Transaction confirmation
↓
Prediction result received
↓
REVEALED: halves open, hidden paper is uncovered, paper zooms forward, text reveals left-to-right
```

If the user leaves the app for wallet approval and returns:

- the current visual state must remain stable;
- the broken halves must not restart;
- crumbs must not reappear;
- the animation must not jump back to IDLE;
- the scene must not visually reset.

The app should resume from the correct state.

The transaction logic itself should not be changed unless strictly necessary for state synchronization.

---

# 10. REVEALED state

Only after the transaction result is available:

- the broken-closed cookie transitions into the separate left/right halves;
- the halves open wider;
- the small fortune paper behind them becomes visible as the halves physically open around it;
- the paper zooms forward along a gentle arc, transitions from behind the halves to in front of them, and settles into its final readable position;
- the prediction text appears on the paper only after the paper zoom completes;
- the broken cookie halves remain continuous with the previous waiting composition;
- the scene transitions smoothly.

The user should feel that the paper was physically inside the cookie and is now being revealed.

Avoid:

```text
waiting composition
→
everything disappears
→
new paper appears somewhere else
```

Prefer:

```text
waiting composition
→
cookie_broken_closed transitions into aligned left/right halves
→
halves gradually open wider
→
small paper is physically revealed between them
→
halves stop opening
→
paper zooms forward / settles into final position
→
text is revealed left-to-right
```

## Recommended REVEALED choreography

Approximate current tuning values; exact timing may be adjusted visually.

```text
CRACK
→ TRANSACTION CONFIRMED
→ COOKIE HALVES MOVE APART
→ PAPER IS REVEALED BEHIND THEM
→ PAPER ZOOM / MOVES FORWARD TO FINAL SIZE
→ PAPER STOPS
→ TEXT REVEALS LEFT-TO-RIGHT
→ RESET UNLOCKS
```

Current reference timings:

| Stage | Approx. duration / offset |
|---|---|
| Halves open | ~760 ms |
| Paper zoom lead (starts before halves fully stop) | ~400 ms |
| Paper zoom | ~2160 ms |
| Pause before text reveal | ~20 ms |
| Text reveal | ~560 ms |
| Reset unlock delay after full text reveal | ~1000 ms |

Important rules:

- the paper must not fade in; it becomes visible because the cookie halves move away from it;
- the paper zoom may begin slightly before the halves fully stop, but must still read as one continuous reveal;
- the paper must start behind the cookie halves and move to the foreground during zoom, ideally along a subtle upward arc so it does not appear to pass through the top edge of the halves;
- text must not appear before paper zoom completes;
- reset to IDLE must not be available until the message has fully appeared.

## Text readability

The text must be readable.

The fortune paper must have enough usable space for realistic prediction lengths.

Do not force all predictions into tiny text merely to fit a fixed layout.

If a prediction is long:
- use an appropriate readable font size;
- allow 3 lines by default and 4 lines for longer messages;
- preserve comfortable margins;
- keep the text visually centered and balanced.

The text must actually fit inside the visible paper.

Never let text:
- overflow the paper;
- extend outside the paper;
- become unreadably small;
- appear beside the paper instead of on it.

## Reset interaction

After the reveal completes, the user may return to IDLE by:
- tapping `Open Another` in the bottom area;
- tapping the revealed cookie/paper stage area.

Reset must remain disabled until the full text reveal has finished and an additional short unlock delay has passed, to prevent accidental dismissal while the message is still appearing.

---

# 11. Fortune paper

The fortune paper is a separate transparent asset (`fortune_paper`).

It should not be treated as a full-screen bitmap.

The visible paper should be positioned based on its actual visual bounds, not merely the dimensions of the transparent PNG canvas.

The paper should:
- have a warm cream appearance;
- feel like paper;
- have subtle depth;
- remain visually integrated with the cookie;
- provide sufficient space for the prediction.

## Visibility rules

The fortune paper must not be readable during:
- `IDLE`
- `BREAKING` / press / tension / crack
- the initial `WAITING_FOR_TRANSACTION` state.

During `WAITING_FOR_TRANSACTION`, the paper object may already be present in the scene, but it must remain hidden behind the broken cookie and must not read as a visible sheet.

During `REVEALED`, the paper must visually originate from inside the cookie:
- first hidden behind the broken cookie / halves;
- then physically revealed as the halves open;
- then zoom forward into its final readable position in the foreground.

Do NOT reveal the paper by fading it in.

The paper must be fully opaque as an object from the moment it becomes visible. Visibility should come from depth, scale, and the cookie halves moving away from it — not from alpha animation of the paper itself.

## Paper motion

The paper uses two visual scale states:

```text
hidden behind cookie
→
small scale, centered between halves
→
zoom forward to final readable scale
```

The paper zoom should feel like the sheet is coming toward the user.

Prefer:
- smooth ease-out motion;
- a subtle upward arc during the transition from behind the halves to the foreground;
- layer reordering so the paper moves from behind the cookie to in front of it during zoom.

Avoid:
- sudden pop-in;
- paper fading from transparent to opaque;
- paper passing visibly through the top edge of the cookie halves;
- text appearing while the paper is still zooming.

Current reference scale values:
- hidden scale: ~0.39
- final scale: ~1.02

Exact values may be tuned, but the hidden paper must remain small enough to feel contained inside the cookie gap, and the final paper must remain large enough for 3–4 readable lines.

## Paper layout

Current reference layout values:
- paper width inside stage: ~88% of available width;
- text area width: ~64% of paper width;
- text area height: ~74% of paper height;
- horizontal / vertical padding around text: small but comfortable.

These values are tuned so long 3–4 line messages fit without reducing font size below readable levels.

Do not solve text overflow primarily by shrinking the font if the paper layout can be adjusted instead.

## Typography

The fortune text should feel like warm ink on paper, not like default UI text.

Current production direction:
- font: **Lora Italic** (`lora_italic_wght.ttf`);
- weight: SemiBold;
- alignment: centered horizontally and vertically within the paper text area;
- color: warm dark brown ink (`PaperInk`, currently `#5A4034`);
- line count: 3 lines by default, 4 for longer messages;
- responsive font sizes based on message length and screen width.

Avoid:
- pure black text;
- generic system serif defaults if a dedicated paper font is available;
- decorative handwritten fonts that harm readability;
- changing text position during reveal.

## Text reveal

The prediction text must appear only after the paper zoom completes.

Preferred reveal behavior:
- all lines reveal simultaneously;
- reveal direction: left to right;
- effect: soft horizontal mask / feathered reveal, optionally with a very light accompanying alpha fade;
- no character-by-character typing;
- no sequential line-by-line reveal;
- text remains centered during the entire reveal.

The reveal should feel magical and organic, not like a rigid rectangular wipe.

A stylized illusion is preferred over a technically complex but visually awkward physical simulation.

---

# 12. Current assets and their roles

The project uses separate transparent assets.

Expected conceptual asset set:

```text
cookie_intact
    ↓
whole cookie used in IDLE, PRESS, and TENSION

cookie_broken_closed
    ↓
real production bridge state
    ↓
used immediately after CRACK and during WAITING_FOR_TRANSACTION

cookie_left_broken
    ↓
left broken half used for the later REVEALED opening stage
    ↓
NOT the immediate post-break waiting composition
    ↓
NO crumbs included

cookie_right_broken
    ↓
right broken half used for the later REVEALED opening stage
    ↓
NOT the immediate post-break waiting composition
    ↓
NO crumbs included

crumb_01 … crumb_13
    ↓
individual single-crumb PNGs used by the production scatter system
    ↓
each instance is animated independently at runtime
    ↓
the same PNG may be reused with different scale / rotation / trajectory

fortune_paper
    ↓
blank / readable paper asset used during the confirmed reveal
    ↓
text is rendered on top of the paper in-app

cookie_waiting
    ↓
reference-only visual composition
    ↓
NOT a production animation layer
```

The production animation should be composed from separate assets.

Do not use `cookie_waiting` as a single full-screen composition if independent animation is required.

`cookie_broken_closed` is the exception: it is an approved single composite production state because it represents the immediate post-crack cookie before the separate halves take over.

The current separate left/right broken cookie halves are production assets for the later opening stage. Their role is:

```text
COOKIE_BROKEN_CLOSED
→
transition into separate left/right halves
→
wider opening during REVEALED
```

The halves must visually inherit the appearance of `cookie_broken_closed`. Because the half PNGs contain transparent canvas space and their visible cookie geometry may not be centered exactly in the canvas, implementation may require scale, translation, alpha-bound, or rotation compensation. This is an implementation detail, not a change to the conceptual choreography.

---

# 13. Asset integrity rules

All production image assets intended to be transparent must have real alpha transparency.

A checkerboard pattern that is visually used to represent transparency must NEVER be baked into the PNG.

Before integrating a new asset:
- verify the PNG has an alpha channel;
- verify the visible object is actually transparent around its edges;
- verify no white/gray checkerboard background is embedded in the image.

If an asset is opaque when it is supposed to be transparent, stop and report the problem before changing the animation code.

Do not silently compensate for a bad asset by adding complex clipping or blending hacks.

---

# 14. Composition rules

The entire scene must feel like one composition.

The following must remain visually connected:

```text
intact cookie
    ↓
press / tension
    ↓
cookie_broken_closed
    ↓
separate cookie halves
    ↕
break point
    ↕
crumb origin
    ↕
fortune paper after confirmation
```

The break point should be consistent.

Crumbs should visually originate from the break.

The paper should appear to belong to the cookie-opening event.

The halves should not be positioned so far apart that the scene becomes disconnected.

Before transaction confirmation, the cookie must remain close enough that it reads as freshly cracked and near-closed rather than fully split apart.

The paper must remain hidden inside the cookie until the confirmed reveal begins.

The composition must remain responsive.

The cookie must not:
- leave the screen;
- become excessively large;
- become clipped;
- jump between screen sizes.

Use constrained, responsive layout rather than hard-coded oversized dimensions.

The home-screen bottom area (tap hint, cost, status, `Open Another`, stats) must respect the device navigation-bar safe area so text is not clipped or hidden by system UI.

---

# 15. Animation quality rules

Every animation should have:
- a clear beginning;
- a clear physical cause;
- a clear end.

Avoid animations that exist only because an asset needs to move.

Ask:

> Why is this object moving?

If the answer is not visually understandable, the animation is probably unnecessary.

Prefer:
- subtle easing;
- natural deceleration;
- small variation;
- short one-shot effects;
- coherent timing.

Avoid:
- linear robotic movement;
- excessive bouncing;
- excessive rotation;
- constant jitter;
- simultaneous movement of everything;
- random motion without purpose.

The user should never feel that the UI is "trying too hard".

---

# 16. No visual jumps

This is a hard requirement.

There must be no unexplained:
- teleportation;
- sudden scale changes;
- sudden layout shifts;
- asset popping;
- sudden changes of anchor point;
- sudden paper repositioning;
- crumb respawning.

When transitioning between states, preserve visual continuity.

If two assets represent the same physical object at different stages, their visual positions should align before the transition.

---

# 17. Current known failure modes — DO NOT REPEAT

The following mistakes have already occurred and must not be repeated:

### Failure 1 — Broken halves with baked checkerboard backgrounds

Broken cookie PNGs were previously used that contained an opaque checkerboard/white background.

Result:
- giant white rectangles;
- visible checkerboard areas;
- broken composition.

Rule:
Always verify alpha transparency.

### Failure 2 — Crumbs attached to cookie halves

If crumbs are included inside the same bitmap as a cookie half, the crumbs move whenever the cookie half moves.

Result:
- crumbs vibrate with the cookie;
- physics looks impossible.

Rule:
Cookie halves and crumbs must be separate assets.

### Failure 3 — Full composition used as a production layer

A waiting-state reference image should not replace individually controllable objects unless it is explicitly approved as a production bridge state.

Rule:
Use reference compositions for visual guidance only.

Exception:
`cookie_broken_closed` is an approved production bridge state and should be used as the immediate post-crack / waiting visual.

### Failure 4 — Paper treated as an unrelated overlay

The paper must feel like part of the cookie-opening event.

Rule:
The paper's movement must begin from inside the cookie after confirmation and remain continuous through the reveal.

The paper must not appear as a separate overlay that fades in above the cookie.

Prefer physical uncovering by the halves, then forward zoom into the foreground.

### Failure 4b — Full paper appearing too early

The paper should not suddenly appear as a complete readable sheet behind, above, or outside the cookie before transaction confirmation.

Result:
- the paper feels like an overlay;
- the cookie no longer feels physically connected to the reveal;
- the confirmed reveal has no meaningful motion left.

Rule:
During waiting, keep the fortune paper hidden behind the cookie even if the paper object already exists in the scene. Reveal it only after confirmation, physically uncovered by the opening halves.

### Failure 4c — Paper fade-in instead of physical reveal

The paper must not appear by animating alpha from 0 to 1.

Result:
- the reveal feels like a UI overlay rather than an object inside the cookie;
- the halves no longer appear to uncover anything.

Rule:
Keep the paper fully opaque and reveal it through depth, scale, and cookie motion.

### Failure 4d — Text appears before paper zoom completes

The prediction text must not fade or reveal while the paper is still zooming forward.

Result:
- the choreography feels rushed and disconnected;
- the message appears before the paper has settled.

Rule:
Text reveal starts only after paper zoom completes, followed by a brief pause.

### Failure 4e — Accidental early reset

The user must not be able to dismiss the reveal while the message is still appearing.

Result:
- the experience feels fragile;
- users accidentally return to IDLE before reading the fortune.

Rule:
Enable reset only after the full text reveal completes and a short unlock delay passes.

### Failure 5 — Excessive spacing

The broken halves must not be separated so far that they leave the visual scene or make the paper appear disconnected.

Rule:
Keep the composition constrained and visually balanced.

Before transaction confirmation, keep the halves especially restrained and close together.

### Failure 6 — Text not fitting on paper

The fortune text must fit comfortably and remain readable.

Rule:
Design the paper's usable area around realistic prediction lengths.

### Failure 7 — Endless crumb animation

Crumbs should not continuously appear while waiting for a transaction.

Rule:
Crumb animation is a one-shot consequence of the crack / BREAK.

Some settled crumbs may remain visible near / below the cookie after active motion finishes.

### Failure 7b — Composite crumb overlays

Do not animate crumbs as one or more full-stage composite bitmap overlays.

Result:
- crumbs look disconnected from the break point;
- all crumbs move as one rigid image;
- final positions look like suspended dust rather than settled pieces.

Rule:
Use only individual `crumb_01` … `crumb_13` assets, each as an independent animated instance.

### Failure 7c — Crumbs resting on moving cookie halves

Settled crumbs must not appear to sit on the surface of a cookie half that later moves apart during `REVEALED`.

Result:
- crumbs look glued to the cookie;
- physics feels impossible once the halves separate.

Rule:
Place final crumb positions around the cookie, below/beside the break zone, and ensure layer order keeps moving halves above overlapping crumbs where needed.

### Failure 7d — Particle-explosion crumb behavior

Crumbs must not behave like confetti or a generic particle emitter.

Result:
- the break feels like an explosion rather than fragile cookie fragments;
- motion continues too long or looks too uniform.

Rule:
Keep post-crack motion short, asymmetric, weighted, and quickly settling.

### Failure 8 — Explosive immediate break

The cookie should not instantly fly apart when the user taps BREAK.

Result:
- the interaction feels like an explosion rather than a hand-cracked fortune cookie;
- the waiting state looks already complete;
- the later transaction-confirmed reveal feels disconnected.

Rule:
Start with immediate press, restrained tension, a clear crack impulse, then `cookie_broken_closed`. Do not separate the halves or reveal the paper before the transaction-confirmed reveal.

---

# 18. AI coding agent instructions

Any AI coding agent working on Open Cookie must:

1. Read this document before modifying the cookie-opening experience.
2. Inspect the existing implementation before changing code.
3. Preserve the existing blockchain transaction logic.
4. Preserve the state machine unless a change is explicitly justified.
5. Do not redesign unrelated screens.
6. Do not regenerate or replace approved assets without permission.
7. Do not create new visual assets when an approved asset already exists.
8. Do not make broad refactors for a visual task.
9. Make changes incrementally.
10. Explain which files will be changed before large modifications.
11. After implementation, verify the full flow from IDLE through REVEALED.
12. Check the flow when the user leaves the app for wallet approval and returns.
13. Ensure no animation restarts unexpectedly.
14. Ensure no visual jumps occur.
15. Ensure assets have correct transparency.
16. Prefer simple, deterministic animations over unnecessarily complex physics.

---

### Failure 9 — Bottom content hidden by system navigation

Bottom labels, actions, and stats must remain readable above the device navigation bar.

Rule:
Apply navigation-bar safe-area padding to the bottom content area on the home screen.

---

# 19. Definition of success

The implementation is successful when a user can watch the sequence and perceive it as one coherent event:

> "I tapped the cookie. It reacted immediately, compressed slightly, resisted and trembled, then cracked with a satisfying tactile impulse. The broken cookie stayed closed / near-closed while my transaction was processed. After confirmation, the cookie opened wider, a small paper inside was uncovered, the paper came forward naturally, and my fortune appeared on it from left to right."

The user should NOT perceive:

> "An image disappeared. Two PNGs flew apart. Some particles appeared. A full paper was inserted behind the cookie. The app waited. Then another image appeared."

The first experience is the goal.

The second is a failure.

The final mental model is:

1. The user taps the cookie.
2. The cookie reacts immediately.
3. It compresses slightly.
4. It resists and trembles briefly.
5. A crack happens with visual and tactile feedback.
6. The cookie becomes `cookie_broken_closed`.
7. The broken cookie remains slightly open / near-closed while the blockchain transaction is processed.
8. Once the transaction is confirmed, the cookie opens wider.
9. A small paper inside the cookie is physically uncovered by the opening halves.
10. The paper zooms forward into a comfortable readable position in the foreground.
11. The actual blockchain-generated prediction appears on the paper from left to right.
12. The user can then open another cookie via the bottom action or by tapping the revealed stage once reset unlocks.

The experience should feel:
- tactile;
- warm;
- playful;
- physical;
- visually coherent;
- calm rather than chaotic;
- satisfying rather than abrupt.

---

# 20. Final design principle

The Open Cookie experience should prioritize:

```text
Coherence
    >
Physical plausibility
    >
Visual polish
    >
Technical complexity
```

A simple, well-timed illusion is better than a technically complex animation that looks artificial.

When in doubt:

**Make it simpler. Make it smoother. Make every movement have a reason.**
