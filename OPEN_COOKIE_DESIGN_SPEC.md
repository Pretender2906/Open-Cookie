# Open Cookie — Design & Interaction Specification

**Document status:** Source of truth  
**Project:** Open Cookie  
**Audience:** Developers, AI coding agents, designers  
**Last updated:** 2026-07-23

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
11. The halves open wider as the fortune paper emerges from inside the cookie.
12. The paper reaches its final readable position.
13. The actual prediction text is revealed.
14. The user can open another cookie.

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
- `REVEALED` begins only after confirmation, when the halves open wider and the paper emerges into its final readable position.

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
crumbs appear as a consequence of the crack
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

### Phase F — crumb burst

At the exact moment of the break:

`crumb_burst`

appears.

It represents larger fragments produced by the break.

The fragments should:
- originate visually near the breaking point;
- move outward/downward;
- have slight variation;
- behave independently from the cookie halves.

This is a one-shot event.

The crumb burst must feel like a consequence of the crack itself, not like a disconnected effect that appears later.

### Phase G — falling crumbs

`crumb_fall`

continues the visual consequence of the break.

The larger crumbs should:
- fall downward;
- have slight individual variation;
- not move as a single rigid image if the implementation can reasonably avoid it;
- have a limited active motion lifetime;
- eventually settle near / below the cookie.

Some crumbs may leave the visible focus area or become less prominent, but the final scene should not become unnaturally clean immediately after the break.

### Phase H — fine particles

`crumb_particles`

provides subtle fine crumbs / cookie dust.

This should be restrained.

It should:
- appear briefly;
- fade naturally;
- not overwhelm the scene;
- never look like smoke or a generic particle effect.

### Phase I — pending broken-closed hold

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
crumb_burst at the break point
↓
larger fragments fall after the break
↓
fine particles dissipate
↓
some crumbs settle near / below the cookie
↓
no continuous crumb animation
```

Do NOT create:
- infinite crumb loops;
- crumbs continuously respawning;
- crumbs teleporting back to their initial position;
- crumbs appearing from nowhere;
- all crumbs moving identically;
- crumbs permanently attached to the cookie halves.

The entire crumb sequence should happen once per BREAK action.

After the crumb sequence finishes, the waiting state should be visually calm, with settled crumbs allowed to remain visible.

Current implementation uses separate crumb assets:
- `crumb_burst`
- `crumb_fall`
- `crumb_particles`

These assets are intended to support the choreography. If the current implementation has limitations in exact physical crumb behavior, document those as implementation limitations rather than redefining the desired visual behavior.

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

The fortune paper must not appear during the initial waiting state.

The full paper must NOT appear as a complete sheet behind, above, or outside the cookie during waiting.

Do not make the waiting state look like the completed reveal.

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
REVEALED: cookie opens wider, paper emerges, text appears
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
- the fortune paper emerges from inside the cookie;
- the paper moves toward the user / into the foreground and settles into its final readable position;
- the prediction text appears on the paper only after the paper reaches that final readable position;
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
paper emerges from between them
→
paper gently moves forward / settles into final position
→
text appears
```

The text must be readable.

The fortune paper must have enough usable space for realistic prediction lengths.

Do not force all predictions into tiny text merely to fit a fixed layout.

If a prediction is long:
- use an appropriate readable font size;
- allow multiple lines;
- preserve comfortable margins;
- keep the text visually centered and balanced.

The text must actually fit inside the visible paper.

Never let text:
- overflow the paper;
- extend outside the paper;
- become unreadably small;
- appear beside the paper instead of on it.

---

# 11. Fortune paper

The fortune paper is a separate transparent asset.

It should not be treated as a full-screen bitmap.

The visible paper should be positioned based on its actual visual bounds, not merely the dimensions of the transparent PNG canvas.

The paper should:
- have a warm cream appearance;
- feel like paper;
- have subtle depth;
- remain visually integrated with the cookie;
- provide sufficient space for the prediction.

The fortune paper must not appear during:
- `IDLE`
- `PRESS`
- `TENSION`
- `CRACK`
- `COOKIE_BROKEN_CLOSED`
- the initial `WAITING_FOR_TRANSACTION` state.

It must visually appear to originate from inside the cookie only after the transaction has been confirmed and the reveal begins. The cookie halves should remain in front of and around the paper, naturally framing the emerging paper.

During `WAITING_FOR_TRANSACTION`, the full paper must remain hidden. The waiting state should not look like a sheet is floating behind the cookie.

The full paper must NOT suddenly appear as a complete sheet behind, above, or outside the cookie.

Only after the blockchain transaction is confirmed does the paper emerge from between the cookie halves and move into its final readable position in the `REVEALED` state.

The transition must be smooth and continuous, so it feels like the same physical paper is being pulled/revealed from inside the broken cookie rather than a new paper asset suddenly appearing.

The paper may subtly scale up, move upward / forward, and gain visual prominence during the confirmed reveal, but this must read as one continuous physical object moving from:

```text
hidden inside the broken cookie
→
emerging from between the cookie halves
→
fully visible
→
final readable position
```

The prediction text should appear clearly only after the blank paper has completed this movement.

The current design intentionally prioritizes:
- visual clarity;
- readable text;
- smooth animation;
- believable illusion.

Perfect physical realism is NOT required.

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

crumb_burst
    ↓
large fragments during initial break

crumb_fall
    ↓
larger crumbs falling after break

crumb_particles
    ↓
fine crumbs / subtle particles

fortune_paper_blank
    ↓
blank paper used during the confirmed reveal before prediction text is shown

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

The paper must not appear as a separate overlay behind the cookie.

### Failure 4b — Full paper appearing too early

The paper should not suddenly appear as a complete sheet behind, above, or outside the cookie before transaction confirmation.

Result:
- the paper feels like an overlay;
- the cookie no longer feels physically connected to the reveal;
- the confirmed reveal has no meaningful motion left.

Rule:
During waiting, keep the fortune paper hidden. Reveal the paper only after confirmation, emerging from between the opening halves.

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

# 19. Definition of success

The implementation is successful when a user can watch the sequence and perceive it as one coherent event:

> "I tapped the cookie. It reacted immediately, compressed slightly, resisted and trembled, then cracked with a satisfying tactile impulse. The broken cookie stayed closed / near-closed while my transaction was processed. After confirmation, the cookie opened wider, the paper emerged naturally from inside, and my fortune appeared on the paper."

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
9. The fortune paper emerges naturally from inside the cookie.
10. The paper moves into a comfortable readable position.
11. The actual blockchain-generated prediction appears on the paper.
12. The user can then open another cookie.

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
