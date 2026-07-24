# Sleep Roulette — Origin, Vision & Roadmap

This document is the product source of truth. The raw ChatGPT brainstorm lives in the parent folder as `TXT.txt` (archive only). Prefer this file when deciding what to build next.

---

## 1. How it started

It began as a personal question, not a product pitch:

> Sometimes I sleep during the day. Today I feel sleepy at work at the same time. What’s the science?

That led into the **two-process model of sleep** (sleep pressure / adenosine vs circadian rhythm), afternoon dips, sleep debt, and why gaming / day-sleep / irregular schedules make weekdays feel worse even when “total hours” look fine.

From there the conversation shifted to building something useful on an **Oppo Reno 11F (Android 16 / ColorOS)** — a personal tool for someone who:

- works full-time (backend)
- sometimes keeps irregular hours (gaming, late nights)
- wants measurement and feedback, not another generic sleep app

Early ideas included on-device LLMs, lots of sensors, Life360-style geofencing, and “yell at me when I’m home but still on YouTube.” The important correction was:

> Don’t build an AI sleep tracker. Build a **behavior-change / personal observability** loop. AI (if ever) narrates patterns that SQL and stats already found.

The ChatGPT thread is archived as `../TXT.txt`. It is **not** maintained documentation.

---

## 2. Initial MVP goal (V1)

### Promise (one sentence)

**Help the only user (you) get to bed more consistently on irregular nights** by reacting to life events — with a real sleep log + charts that serve that goal (not vanity averages alone).

### Framing that won

Closer to **Life360 for sleep** than to Fitbit/Oura:

| Life event | Why it matters |
|------------|----------------|
| Left work / arrived home | Decision window for bedtime starts |
| Still using phone after goal | Intervention moment (JITAI) |
| Sunrise approaching | Biological deadline for shift-ish schedules |
| Bedtime achieved / missed | Consistency signal |

### Explicit V1 scope

**In**

- Local-only Android app (no signup, cloud, iOS, web)
- Home geofence (+ optional Work later)
- Arrive-home **Sleep Countdown** (goal bedtime vs sunrise)
- Usage Access → soft **nudge** after goal (notification only, not a phone lock)
- Manual sleep start/end + history
- Basic consistency stats
- Sideload APK; survive ColorOS battery killers as a first-class concern

**Out**

- Auth / multi-user / privacy theater for a store audience
- Health Connect, microphone, LLM chatbot
- Smart home, experiments UI, CSV polish as product surface
- Blocking the phone after bedtime

### Success for V1

You can run the app for real nights on the Reno 11F and answer:

1. Did Home arrival trigger a countdown?
2. Did the countdown survive screen-off / overnight?
3. Did a post-goal phone-use nudge fire?
4. Can I log sleep and see a streak/history?

If ColorOS silently kills background work, fixing that beats adding sensors.

### North-star metric (keep this)

Not “average hours slept.” Prefer:

- **% of nights sleep started before goal bedtime**
- Secondary: **median home → bed minutes**

---

## 3. What V1 shipped

| Area | Status |
|------|--------|
| App name / package | Sleep Roulette · `com.sleeproulette.app` |
| Stack | Kotlin, Compose, Hilt, Room, DataStore, Geofencing, WorkManager |
| Screens | Today, Log, Stats, Setup |
| Geofence Home | Set / overwrite / **clear** |
| Countdown | FGS + ongoing notification; manual “I’m home” for testing |
| Usage nudge | Soft notification after goal + threshold |
| Sleep log | Manual start/end |
| Stats | Duration average + streak; home→bed / goal hit rate still thin |
| Docs | README (build/arch) + this file (product) |

Repo: https://github.com/Shahzaibalikhawaja/sleep-roulette

---

## 4. Product principles (don’t forget)

1. **One job** — consistency before bedtime goal. Features must serve that.
2. **Be both a logger and a coach** — logging + charts are required infrastructure; interventions are the differentiator. Charts that don’t serve the north star are vanity.
3. **Intervene at decision points** — arrive home, past goal, sunrise near — not generic 9pm reminders.
4. **Infer 80%, ask 20%** — sensors suggest; user confirms. Don’t silently invent truth.
5. **Explain with your data** — “Tuesdays you average +45m after home,” not stock tips.
6. **Survive ColorOS** is a feature.
7. **AI is a narrator, not the scientist** — stats/SQL first; LLM later optional.
8. **Avoid OS bloat** — no coffee-machine / Home Assistant / Spotify integrations until the core loop is sticky.

Copy tone: shift chaos is fine (“tonight’s window,” “gambling past sunrise”). Clinical “sleep hygiene score” is not the brand.

### Logger vs behavior-change — resolved

**Yes, it can be both.** The earlier line “isn’t a sleep logger with charts” meant: don’t *stop* at passive logging like every other tracker. It did **not** mean “never log” or “never chart.”

| Layer | Role |
|-------|------|
| Sleep log + graphs | Memory and proof — without data you can’t learn or trust interventions |
| Life events + nudges | Behavior change — what makes this not just another diary |

**Hierarchy:** log everything useful → visualize the north-star metrics → intervene on life events. If a chart doesn’t help answer “am I getting more consistent?”, deprioritize it.

**Charts we want (after Phase 0):**

- Goal hit rate over time (% nights before goal) — primary
- Home → bed minutes (trend / median)
- Sleep duration as secondary context (not the hero metric)
- Optional: weekday pattern bars

---

## 5. Ideas & discussion backlog

Captured from the original brainstorm and post-MVP conversation. Not a commitment — a menu.

### Real-world scenarios (decided after V1 ship)

These came up while planning the first real night of testing. **Build order preference:** morning confirm → “sleeping but scrolling” nudge → Work UI → overnight disturbances.

#### A. Tapped “Start sleep” then doomscrolled TikTok / YouTube / Instagram

| Today | Wanted |
|-------|--------|
| Session opens in DB only. Usage nudge is tied to **past goal bedtime**, not to “sleep started but still scrolling.” | If `ongoingSleep` **and** interactive use of doomscroll apps for N minutes → disappointed / accountability nudge (“You said you were sleeping”). Soft notification first; escalate later. |

#### B. Forgot to open the app and start the timer

| Today | Wanted |
|-------|--------|
| No morning suggestion. Night is missing unless logged manually. | Infer likely window (screen interactive quiet + stillness / charging) → morning: **“Slept ~X–Y? Confirm / edit.”** This is the main fix for 4am friction. |

#### C. Poor sleep: wake ~6–7am, water / bathroom, sleep again

| Today | Wanted |
|-------|--------|
| One manual session; mid-night wakes invisible. | **Do not end the night** on a brief wake. **Do record** phone interaction during the sleep window as a **disturbance** (not a “false positive”). Interactive use = awake enough that sleep was interrupted. Morning copy e.g. “7h 40m window, 2 phone disturbances.” Bathroom-only with no unlock can stay invisible until better sensing. |

**Signal preference:** interactive use (UsageStats / unlock + apps), not raw screen-on. Always-on display (AOD) should **not** count as wake — verify on ColorOS; filter with interactive minutes if needed.

#### D. Work geofence (leave work → arrive home)

| Today | Wanted |
|-------|--------|
| `PlaceKind.WORK` + ENTER/EXIT_WORK events + geofence registration exist in code; **Setup UI only configures Home.** | Add Set / Clear Work like Home. Leave Work → commute / bedtime pressure context; Enter Home → countdown (existing). Phase 2 unless Phase 0 is already solid. |

### Interventions

- Soft nudge → escalate after repeated ignores (full-screen activity, sound, stronger copy)
- **“Sleeping but still scrolling”** accountability nudge (scenario A)
- Sleep countdown richness (progress bar, sunrise warning copy)
- Personalized nudge text from historical latency / home→bed
- Optional hard friction later (overlay / app limits) — only after soft loop has data; never as V1 default

### Sleep detection

- Suggest sleep window from quiet interactive period + stillness (accel/gyro) + charging
- Confidence score + “Confirm / edit”
- Morning unlock → “End sleep?” / confirm suggested session (scenario B)
- Overnight **disturbances**: interactive unlocks during a sleep session or inferred window — append events, keep one night (scenario C)
- Prefer interactive use over SCREEN_ON (AOD-safe)
- Full sensor fusion (light, mic-as-classifier without storing audio) — later
- Health Connect / watch import — later

### Places & context

- **Work geofence UI** (backend ready) — leave work → home arrival chain (scenario D)
- Travel / hotel detection (confounder)
- Commute duration → expected home→bed

### Analytics

- Home→bed median, goal hit rate (complete Stats)
- **In-app charts** for north-star metrics (not vanity-only duration)
- Disturbance count per night (once scenario C exists)
- Weekday patterns (“Tuesdays delay bedtime”)
- Sleep debt / rolling averages
- Experiment mode (“no phone in bed for 7 days”)
- CSV export for personal analysis

### Architecture evolution (personal observability)

Same engines, new datasets over time: mood, caffeine, focus, gym — **Grafana for humans**. Only after sleep loop works.

### Explicit non-goals (until you reopen them)

- Store launch, accounts, sync
- iOS / web
- LLM therapist / chatbot as core UX
- Mic recording to cloud
- Smart home automation
- Treating AOD / notification glow as “awake” without interactive use

---

## 6. Road ahead (recommended order)

### Phase 0 — Prove the loop (**in progress — first real night**)

Pause feature work until soak results are in. Checklist:

- [ ] Geofence ENTER_HOME fires in real life
- [ ] Countdown notification survives overnight / Doze / ColorOS
- [ ] Usage nudge fires after goal with Usage Access on
- [ ] Manual log usable at 4am (muscle memory)
- [ ] Note failures: geofence miss? FGS killed? no nudge? forgot Start sleep?
- [ ] **Duration UX:** Today no longer shows goal countdown as if it were sleep length while asleep (fix after first night feedback)

**Tonight walkthrough (manual):** Setup permissions → set Home at real home → set goal → optional “I’m home” test → live: hope geofence starts countdown (else tap I’m home) → Start sleep when you mean it → End sleep in the morning → jot what broke.

**Gate:** don’t add features until this is trustworthy.

**Soak notes (2026-07-24):** User reported sleep hours looking wrong / tied to goal. Root cause for Today: hero clock stayed on **time until goal** even after Start sleep. Log/Stats already used Start→End; Today now shows **elapsed since Start sleep** while a session is ongoing. Reinstall to verify; if Log “Start …” time still ≠ tap time, capture that screenshot.

### Phase 1 — Friction down + visible proof

Priority within phase (from scenario discussion):

1. Morning suggest / confirm if timer forgotten  
2. “Sleeping but scrolling” nudge  
3. Richer Today + Stats/charts (north star)  
4. Overnight disturbance logging (interactive use)  

Checklist:

- [x] Clear / overwrite Home (UX)
- [ ] Suggested sleep + morning confirm / edit
- [ ] Accountability nudge while `ongoingSleep` + doomscroll apps
- [ ] Today shows last night + streak, not only countdown
- [ ] Finish Stats: home→bed median, **goal hit rate**
- [ ] **Charts:** goal-hit % and home→bed over time (duration secondary)
- [ ] Disturbances during night (don’t split session)

### Phase 2 — Smarter interventions + Work

- [ ] Escalation ladder for ignored nudges (still no hard phone lock by default)
- [ ] Copy driven by simple personal stats
- [ ] **Work geofence Set/Clear UI** + leave-work → home chain

### Phase 3 — Detection quality

- [ ] Sensor fusion + confidence
- [ ] Health Connect optional import
- [ ] Correction feedback improves per-user heuristics
- [ ] AOD / ColorOS validation for wake signals

### Phase 4 — Narration / experiments (optional)

- [ ] Local or cloud LLM as narrator over SQL results
- [ ] Experiment planner
- [ ] Broader “personal observability” datasets

---

## 7. Engineering notes that affect product

- **Nudge ≠ lock.** Usage Access only *observes* foreground time; intervention is a notification.
- **Debug package id:** `com.sleeproulette.app.debug`
- **AGP 9 / Hilt 2.60:** blocked until KSP supports AGP built-in Kotlin; stay on AGP 8.13 line (see README).
- **Destructive Room migrations** OK for personal MVP; add real migrations before caring about long-term DB.
- **AOD:** Prefer unlock + interactive UsageStats over raw screen-on for sleep/wake inference.
- **Work:** Domain + geofence path exist; only Home is exposed in Setup UI today.

---

## 8. Changelog of product thinking

| When | Decision |
|------|----------|
| Brainstorm | Science → sensors → Life360 framing; reject “AI-first” marketing |
| Spec | Single-user Oppo MVP; local-only; consistency promise |
| Build | V1 spine shipped: geofence, countdown, nudge, manual log |
| Testing | Soft nudge only; Clear Home needed; auto-detect deferred |
| Post-V1 | Document here; Phase 0 soak before sensors/AI |
| Direction lock | North star = goal hit % + home→bed; charts yes; logger + coach; escalate soft→strong later |
| Scenario lock | Morning confirm &gt; scroll-while-sleeping nudge &gt; Work UI &gt; overnight disturbances; interactive unlock = real disturbance, not false positive; AOD ignored |

---

*Update this file when you cut a phase or kill an idea. Leave `TXT.txt` as historical archive. Next session: review Phase 0 soak notes, then pick Phase 1 items.*
