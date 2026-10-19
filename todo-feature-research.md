# Finshit — Todo/Task App Feature Research Pack
Prepared 2026-10-19 ~17:15 IST for the main agent (Neomix's big-round request, side chat 17:07 IST).
Sources: Notion (kdnuggets review, Play Store listing), Todoist/TickTick/Things comparisons
(slant.co, zapier.com, yourtechcompass.com, navthemes.com, clickup.com, truegeometry blog).
This is research for his explicit ask: "research karke dekh ki is tarah app mai kya kya features hota hai —
Like popular app toh notion hi hai and baki aur bhi research krlena."

## What Finshit is today (context for feature mapping)
- Kotlin / Jetpack Compose, offline-first, JSON persistence (finshit-data.json, autosave debounce)
- Has: projects, tasks, notes, Google sign-in, no Android permissions (SAF picker), fonts
  Bricolage Grotesque + DM Sans, accent #E8714A, custom glassmorphism UI
- UI zero-change history: new features must keep the SAME visual style (he re-confirmed repeatedly)

## Tier 1 — Core todo essentials (table stakes; do these first)
1. **Subtasks / checklist inside a task** (Todoist, TickTick, Any.do) — task detail opens checklist
2. **Due dates + times, recurring tasks** (Todoist's natural-language "every Friday 3pm" is best-in-class;
   Things 3 handles only simple patterns — Finshit should parse common patterns)
3. **Reminders/notifications** (Todoist, TickTick) — TickTick allows up to 5 reminders per task,
   repeated + location-based. Android: AlarmManager/exact alarms need SCHEDULE_EXACT_ALARM permission
   (Play Protect note: adding permissions slightly raises suspicion — keep the set minimal)
4. **Priority levels (P1–P4)** (Todoist, TickTick) + **labels/tags with #hashtag filter** (TickTick)
5. **Projects/Lists + sections/headings inside a project** (Todoist sections, Things 3 headings)
6. **Today / Upcoming / Overdue smart views** (Things 3's Today vs Anytime vs Someday; TickTick Smart Lists)
7. **Search with filters** (Notion's filtered search; TickTick custom filters)
8. **Drag-to-reorder** (TickTick advanced sorting)
9. **Quick-add / natural-language input** — Todoist's signature feature: type "Submit report every
   Friday 3pm #work p1" and everything parses. Huge delight, moderate effort (regex + date parser)

## Tier 2 — Power features (what makes it feel "pro")
10. **Kanban board view** (Todoist, TickTick) — per project, columns = statuses
11. **Calendar view** (TickTick, Notion) — tasks laid out on a month/week grid
12. **Eisenhower matrix** (TickTick) — urgent/important quadrant view
13. **Pomodoro focus timer** (TickTick's standout) — pairs naturally with his Focivo background
14. **Habit tracker** (TickTick basic; Notion habit-tracking templates) — daily check-ins + streaks
15. **Task comments / activity log** (Todoist, TickTick collaboration)
16. **Attachments via system picker** (already SAF — no permission needed)
17. **Templates** (Notion's biggest lever; TickTick custom templates) — project templates, daily-planner template
18. **Widgets** (Todoist customizable Android widget) — home-screen task list widget
19. **Archive + trash with restore** (safety net for deletes)
20. **Statistics / productivity insights** (TickTick focus stats; Notion analytics) — completed-per-day,
    streaks, time-focused charts

## Tier 3 — Notion-like (his explicit ask: "Notion me jo jo hai wo bhi add krde")
21. **Rich notes pages** — headings, checklists, toggles (collapsible), dividers, code blocks, quotes,
    inline images; Markdown support (Notion core)
22. **Databases with multiple views** — same data as table / board / calendar / list (Notion's signature);
    custom properties (status, date, select, checkbox, number), sort + filter per view
23. **Linked views / filtered perspectives** — e.g. one "Tasks" database shown as "This Week" in one
    place and "Backlog" in another (lemon8 Notion-hacks roundup)
24. **Templates gallery** — study planner, meeting notes, habit tracker, project kickoff (Notion markets
    these hard to students — his audience)
25. **Inbox / quick-capture page** — brain-dump inbox, triage later (GTD-style; Things 3 Inbox)
26. **Toggle lists + synced blocks** — collapsible nesting to keep pages clean

## Tier 4 — Customization everywhere (his explicit ask: "task mai khud customize ka option bhi de,
   har jagah customisation laga de")
27. **Custom themes** — accent color, dark/light, card styles (keep default = current design)
28. **Custom statuses per project** (beyond todo/doing/done)
29. **Custom fields/properties on tasks** (Notion-style: user-defined select/date/number fields)
30. **Custom sort orders + saved filters ("Smart lists")**
31. **Home dashboard** — Notion-style personalized dashboard: today's tasks, habits, notes pinned,
    user-arranged widgets
32. **Custom icons/covers per project** (Notion pages have icons + covers — big delight, cheap to build)

## Apple-like smooth motion (his explicit ask)
Jetpack Compose already has the toolkit — the work is applying it consistently:
- `animate*AsState` + `spring()` (low stiffness, medium damping) for every state change instead of snaps
- `AnimatedVisibility` (fade + expand/shrink) for appearing/disappearing UI
- **Bottom nav tap motion**: animated selection pill/indicator that slides between tabs
  (`Modifier` with animated offset or `NavigationBar` indicator + `AnimatedContent` for icons)
- Shared-element transitions between list → detail (`SharedTransitionLayout`, Compose BOM 2024.09+ has it)
- `animateContentSize` on cards that expand; swipe-to-complete with `AnchoredDraggable` + spring back
- 120Hz: avoid recomposition storms (remember/derivedStateOf), keep animations on the render thread
  (graphicsLayer for scale/alpha/translation)

## AI integration (his explicit ask — OpenRouter)
- **Model verified 2026-10-19 on openrouter.ai**: `nvidia/nemotron-3.5-super-vl` EXISTS (paid,
  ~$0.15/1M input, ~$0.50/1M output, 1M context, multimodal: text+images+docs+video, reasoning on by
  default — can be disabled or token-budgeted). Free variant `nvidia/nemotron-3.5-super-vl:free` also
  exists but is **rate-limited and evaluation-only under NVIDIA API Trial Terms** (no production use,
  don't send personal data) — so: free variant for HIS testing, paid variant in the shipped feature.
- API is OpenAI-compatible: POST https://openrouter.ai/api/v1/chat/completions with
  `Authorization: Bearer $OPENROUTER_API_KEY`, body `{"model": "nvidia/nemotron-3.5-super-vl", "messages": [...]}`.
- **Key handling (his instruction: "secure karke rakhdena, publicly mat rakh")**:
  1. Store ONLY as GitHub Actions secret (e.g. `OPENROUTER_API_KEY`), inject at build time into
     BuildConfig — never in source, never in the APK as a plain committed string if avoidable
     (note: any key shipped inside an APK is extractable by a determined user; acceptable for his scale,
     but say it plainly).
  2. **Rotate the key after setup**: it was pasted in plaintext chat (2026-10-19 17:07 IST message).
     After the Actions secret works, regenerate at openrouter.ai/keys and delete the old one.
  3. Optional guardrail: set a spend limit on the OpenRouter key so a leaked/extracted key can't burn money.
- Suggested AI features (map to model strengths — agentic + visual): AI task breakdown ("break this
  project into subtasks"), smart scheduling ("plan my day from these tasks"), natural-language task
  creation assist, summarize long notes, extract tasks from pasted text/syllabus photo (multimodal).

## Security gates BEFORE the repo goes public (his ask: "us repo ko public krke mujhe de Dena")
Verified locally 2026-10-19: `finshit-release.keystore` sits in `~/workspace/finshit/` root and memory
confirms it is **committed at the repo root** (`bf9bde0a`). gradle.properties has NO plaintext
password (only android.useAndroidX etc.) — password lives in `keystore-backup/keystore-password.txt`
(local only) + Actions secret `FINSHIT_KEYSTORE_PASSWORD`. `debug.keystore` committed is harmless
(shared Android debug key everyone has).
- **RISK**: a public repo containing the PRIVATE release keystore = anyone can sign APKs as "Finshit";
  Android would accept them as genuine updates on users' devices (same package + same signature).
  Keystore passwords are brute-forceable — "password not in repo" is not enough protection.
- **Options for his one-tap decision**:
  A. Keep repo private (simplest; AI integration + Actions work identically on private repos).
  B. Before making public: purge `finshit-release.keystore` from git history (BFG/git-filter-repo +
     force-push) and switch signing to a NEW keystore stored ONLY as an Actions file-secret, injected
     at build time. Cheapest moment to do this: Phase-2 already forces one final uninstall+fresh
     install (cert change), so rotating the key now costs users nothing extra.
- Also before public: double-check no `google-services.json` secrets — it's a client config, safe to
  commit (standard practice); and confirm `keystore-backup/` is NOT tracked.

## Play Protect — honest expectation to set with him
New proper release cert (CN=finshit...) removes the "Android Debug certificate" trigger, which was the
scariest warning. But: sideloaded APKs (outside Play Store) can still show Google's generic
"unknown app" scan notice — that part only fully disappears with a Play Store listing. Say this
plainly so he doesn't call it "still broken".

## Suggested build order for the main agent (his priorities, cheapest first)
1. Remove demo footage/script content → user-created naming everywhere (his first bullet)
2. Nav-bar tap motion + global spring/AnimatedVisibility pass (visible delight, low risk)
3. Tier-1 todo essentials (subtasks, due dates/recurring, priorities, labels, smart views, search,
   quick-add) — makes it a real todo app
4. Tier-2 power (kanban, calendar view, pomodoro, habits, widgets, templates, stats)
5. AI integration (OpenRouter, paid variant, Actions secret, rotate key after)
6. Tier-3 Notion-like (rich pages, databases w/ multiple views, inbox, dashboard)
7. Repo-public decision + security gates (do BEFORE public, not after)
8. Phase-2 cert rebuild (still blocked: he must ADD the new SHA-1 in Firebase first — asked 16:55 IST,
   no confirmation yet) + Play Protect re-test
