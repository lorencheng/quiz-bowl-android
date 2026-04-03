# QuizBowl Android

Android port of the QuizBowl TTS web app (`../QuizBowl`). Goal: 1:1 feature parity.

## Web App Reference
- Location: `../QuizBowl` (React/Vite, JavaScript)
- All source at `../QuizBowl/src/` — read it when porting a screen.

## Android Tech Stack
| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow/Flow |
| Navigation | Navigation Compose |
| HTTP | Retrofit2 + OkHttp3 |
| WebSocket | OkHttp3 WebSocket |
| TTS | Android `TextToSpeech` |
| STT | Android `SpeechRecognizer` |
| Persistence | DataStore Preferences |
| JSON | Gson |
| Async | Kotlin Coroutines |
| minSdk | 26 (Android 8.0) |
| Package | `com.quizbowl.app` |

## Screens
1. **HomeScreen** — 3 nav cards
2. **TossupPracticeScreen** — solo tossup practice ✓ done
3. **BonusPracticeScreen** — solo bonus practice
4. **MultiplayerScreen** — real-time multiplayer

## Current Priority: Bonus → Multiplayer
Tossup is complete. Implement Bonus next, then Multiplayer.
HomeScreen nav cards now route to their screens (no more "Coming Soon").

## API
- REST base: `https://www.qbreader.org/api`
- WS base: `wss://www.qbreader.org`
- Key endpoints: `/random-tossup`, `/random-bonus`, `/check-answer`, `/multiplayer/room-list`
- WS: `/play/mp/room/{roomName}?roomName=X&userId=Y&username=Z`
- Rate limit: 50ms between requests
- checkAnswer returns: `{directive: "accept"|"reject"|"prompt", directedPrompt?}`

## Feature Map: Web → Android

### Critical TTS Sync Requirement
The web app's word display does NOT sync accurately with audio on Android (Chrome's WebSpeech
`onboundary` is unreliable on mobile). Native Android MUST do this correctly.
**Solution**: `UtteranceProgressListener.onRangeStart(utteranceId, start, end, frame)` — API 26+
- Fires with exact character offset of each spoken word from the TTS engine
- Build `charOffset → wordIndex` map before calling `tts.speak()`
- Map char offset back to word index in `onRangeStart`
- Use `advanceWordIndex()` (max-only) to prevent flashing
- Already implemented in `TtsEngine.kt` — verify it works on a real device

### TTS (useTTS.js → TtsEngine.kt)
- Android `TextToSpeech` with `UtteranceProgressListener.onRangeStart()` (API 26+) for word index
- No need for Android chunking workaround — that was the web's hack for mobile Chrome
- States: `speaking`, `paused`, `wordIndex`, `done`
- Expose: `speak(words)`, `pause()`, `resume()`, `stop(): Int (stoppedWordIndex)`, `reset()`

### STT (useSpeechRecognition.js → SpeechEngine.kt)
- Android `SpeechRecognizer` with interim results
- Must call `startListening()` from user gesture (Button onClick)
- Callbacks: `onInterimResult(text)`, `onFinalResult(text)`
- States: `listening`, `supported`

### Settings (Settings.jsx → shared SettingsPanel composable)
- Voice selector, speed slider (0.5–2.0x)
- Buzz timer (0–15s), Answer timer (0–15s) — tossup and bonus
- Category chips: Literature, History, Science, Fine Arts, Religion, Mythology, Philosophy, Social Science, Current Events, Geography, Other Academic, Trash
- Difficulty chips: 0 (Unrated) through 10 (National)
- Persisted in DataStore

### Tossup Game States
`IDLE → READING → BUZZING → RESULT → IDLE (Next)`
- READING: word-by-word TTS, highlight current word, Buzz button, Pause/Resume
- After TTS done: buzz timer counts down
- BUZZING: answer input + mic, answer timer
- RESULT: banner (correct/wrong/timeout), reveal answer, Next button
- Points: power(*) before buzz = 15, correct = 10, neg = -5

### Bonus Game States
`IDLE → READING_LEADIN → READING_PART → ANSWERING → [PROMPTING] → PART_RESULT → (×3) → DONE`

**Transitions:**
- IDLE → READING_LEADIN: "Start" tapped; fetch bonus, split leadin words, tts.speak(leadinWords)
- READING_LEADIN → READING_PART (part 0): tts.done; split part words, tts.speak(partWords)
- READING_PART → ANSWERING: tts.done; focus input, start SpeechEngine, start answer timer
- ANSWERING → PROMPTING: checkAnswer returns directive="prompt"; show directedPrompt, await re-answer
- ANSWERING → PART_RESULT: directive="accept"/"reject"; stop timer, record result
- PROMPTING → PART_RESULT: re-answer submitted; accept/reject only (no nested prompt)
- PART_RESULT → READING_PART (next): 1.5s delay; if currentPart < 2
- PART_RESULT → DONE: 1.5s delay; if currentPart == 2; update session score
- DONE → IDLE: "Next Bonus" tapped

**Scoring:** 10 pts correct / 0 incorrect per part (no negatives)
Session: `total`, `bonuses` (count attempted), `thirties` (30/30 count), PPB = total/bonuses

**Answer Timer:** same 0–15s DataStore setting as tossup answer timer; 0 = no limit
On timeout → treat as incorrect (0 pts), auto-advance to PART_RESULT

**Answer submission rules:**
- Normalize answer before API call (leading `-N` → `negative N`)
- Debounce ref prevents double-submit
- First keypress while SpeechEngine active → disable voice for that part
- Voice final result auto-submits (same as tossup)

**UI layout:**
- Scoreboard: total | bonuses | 30s | PPB (PPB hidden until first bonus completed)
- Question meta: category, subcategory, difficulty, set name
- Leadin: italic, word-by-word highlight via TtsEngine.wordIndex
- 3 part cards:
  - Active: highlighted border, text reveals word-by-word
  - Done: dimmed; shows "Correct!" or "Incorrect.", user answer, correct answer
- Controls per phase:
  - IDLE: Start button
  - READING_*: Pause/Resume (no buzz mechanic)
  - ANSWERING/PROMPTING: AnswerInput + mic + Submit + answer timer countdown
  - DONE: "X/30" summary + "Next Bonus" button

**New files:** `ui/bonus/BonusScreen.kt`, `ui/bonus/BonusViewModel.kt`, `util/BonusUtils.kt`
**Reuse:** TtsEngine, SpeechEngine, SettingsRepository, QbReaderService, AnswerInput composable

### Multiplayer

#### Views
- **LOBBY**: username input (DataStore persisted), room name field, public room list
  (REST GET `/multiplayer/room-list`), join button. Any room name creates or joins.
- **ROOM**: game area (center) + sidebar (player list + chat)

#### Connection
- WS URL: `wss://www.qbreader.org/play/mp/room/{roomName}?roomName=X&userId=Y&username=Z`
- userId: generate `user_XXXXXXXX` per session (not persisted)
- Ping keepalive: send `{"type":"ping"}` every 30s
- On disconnect: return to LOBBY, show error. No auto-reconnect.

#### WebSocket Messages — Client → Server
| type | payload | when |
|---|---|---|
| `ping` | `{}` | every 30s |
| `buzz` | `{}` | player taps Buzz |
| `give-answer` | `{givenAnswer}` | submit answer |
| `give-answer-live-update` | `{givenAnswer}` | each keystroke / voice interim |
| `next` | `{}` | request next question |
| `pause` | `{pausedTime}` | pause game |
| `chat` | `{message}` | send chat |
| `start-bonus-answer` | `{}` | signal ready for bonus part |

#### WebSocket Messages — Server → Client
| type | key fields | effect |
|---|---|---|
| `connection-acknowledged` | `players[], canBuzz, currentQuestionType` | initial state sync |
| `connection-acknowledged-question` | `question, currentQuestionType` | resume in-progress question |
| `join` / `leave` | `userId, username` | update player list + add system chat message |
| `start-next-tossup` | `tossup` | begin tossup; set canBuzz=true |
| `start-next-bonus` | `bonus` | begin bonus |
| `update-question` | `word` | append word to display; speak via inline TTS if enabled |
| `reveal-leadin` | `leadin` | show + speak bonus leadin |
| `reveal-next-part` | `currentPartNumber, part` | show + speak bonus part |
| `buzz` | `userId, username` | show who buzzed; lock others out of input |
| `give-tossup-answer` | `userId, directive, score, revealedAnswer` | show result; update scores |
| `give-bonus-answer` | `userId, directive, correct, score` | show part result |
| `reveal-tossup-answer` | `answer` | show correct answer |
| `end-current-tossup` / `end-current-bonus` | — | reset canBuzz, clear question state |
| `chat` | `userId, username, message` | append to chat |
| `pause` | `paused` | pause/resume TTS |
| `error` | `message` | show error snackbar |
| `enforcing-removal` | `removalType` | ban/kick → disconnect + show reason |

#### Tossup Buzz Mechanics
- `canBuzz` = true when tossup active and no one has buzzed
- Tap Buzz button → send `buzz` → server broadcasts to all
- Only the buzzed player sees answer input + mic; others see "[username] buzzed in!"
- `canBuzz` = false after any buzz or `end-current-tossup`

#### Bonus Mechanics
- No buzz — all players answer each part simultaneously after TTS reads it
- Send `start-bonus-answer` when ready; then `give-answer` with response

#### Multiplayer TTS
- Toggle in room header (session-only, not persisted)
- Speed slider (0.5–2x)
- Per-word: on each `update-question` word → `tts.speak(word, QUEUE_ADD)` — no buffering
  (buffering would lag behind other players who see clues in real-time)
- Bonus leadin/parts: speak full text on `reveal-leadin` / `reveal-next-part` events
- Pause/resume TTS on `pause` event

#### Live Answer Updates
- On each keystroke and voice interim result → send `give-answer-live-update`
- Other players see the buzzer's in-progress answer in real-time

#### Player List
- Sorted by score descending; self highlighted
- Updated on `give-tossup-answer` and `give-bonus-answer` events
- System chat messages on join/leave

#### Chat
- Message: `{username, text, userId, system?}`
- Show last 50 messages; system messages (join/leave) styled italic + dimmed
- Enter to send; input cleared after send

**MultiplayerClient.kt** already implemented — reuse it.
**New files:** `ui/multiplayer/LobbyScreen.kt`, `ui/multiplayer/MultiplayerScreen.kt`,
`ui/multiplayer/MultiplayerViewModel.kt`

## Design System

**Aesthetic:** Dark-first game show UI. Vivid but controlled — color is used for meaning, not decoration.
The app targets middle/high school students and should feel like a competitive game, not a productivity tool.

### Screen Layout Pattern (apply to every screen)
- No `TopAppBar`. Use a custom `GameHeader` row at the top of the screen's `Column`.
- `Scaffold(containerColor = qbColors.bg)` with no `topBar` — provides correct inset padding.
- Settings moved out of the screen body into a `ModalBottomSheet` (gear icon in header → sheet opens pre-expanded).
- `SettingsPanel` accepts `initiallyExpanded = true` when hosted in a bottom sheet.
- Inner content: `Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp))`.
- Spacing baseline is **8dp**. Use `Spacer(Modifier.height(8.dp))` or `Arrangement.spacedBy(8.dp/12.dp/16.dp)` — not arbitrary values.

### GameHeader Pattern
```kotlin
Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), SpaceBetween, CenterVertically) {
    IconButton { Icon(Icons.AutoMirrored.Filled.ArrowBack, tint = qbColors.textMuted, size = 22.dp) }
    Text("SCREEN NAME", 12sp, ExtraBold, letterSpacing = 3.sp, color = qbColors.primary)
    IconButton { Icon(Icons.Filled.Settings, tint = qbColors.textMuted, size = 20.dp) }
}
```
- Always use `Icon()` + real `Icons.*` vectors. Never Unicode characters or emoji as navigation controls.
- Title: short ALL-CAPS name, 12sp, `letterSpacing = 3.sp`, `qbColors.primary`.

### Score / Stats HUD Pattern
- Score hero: **30sp ExtraBold `accentAmber`** with a small `"pts"` label at `13sp` at the bottom of the number.
- Secondary stats (correct, neg, played) aligned right: 16sp bold value + 9sp muted label below, `Arrangement.spacedBy(16.dp)`.
- No borders, no cards. Just a `Row(SpaceBetween)` on `qbColors.bg`. Flat, scannable in peripheral vision.
- Labels use plain words ("correct", "neg", "played"), NOT emoji checkmarks (render inconsistently across OEMs).

### Question / Content Card Pattern
- `Card(shape = RoundedCornerShape(20.dp), colors = containerColor = qbColors.surface, elevation = 0.dp)`
- **No gradient border.** The `surface` vs `bg` color contrast is sufficient separation.
- Internal padding: `20.dp` all sides.
- Category metadata: one colored pill (dot + "Category · Subcategory") + separate muted `"D5"` difficulty badge.
  - Pill: `RoundedCornerShape(6.dp)`, `background = accentColor.copy(alpha = 0.15f)`, no border.
  - Dot: 6dp `CircleShape` filled with accentColor.
  - Text: 11sp SemiBold in accentColor.
  - Difficulty: 11sp Bold, `qbColors.textMuted`, `surface2` background, same corner shape.
- Category → accent color map: Science=teal, History/Religion/Mythology=amber, Literature/Fine Arts=rose, else=primary.

### Primary Action Button (GradientButton)
- `Box + clip(RoundedCornerShape(14.dp)) + background(Brush) + clickable(interactionSource, ripple(White))`
- **Always include ripple** using `MutableInteractionSource` + `indication = ripple(color = Color.White, bounded = true)`.
- Height: 54–58dp. Text: ExtraBold, 16–17sp, `letterSpacing = 1–2.sp`.
- Gradient: `Brush.horizontalGradient(listOf(qbColors.primary, qbColors.accentTeal))`.
- Disabled: `Brush.horizontalGradient(listOf(Gray.copy(0.35f), Gray.copy(0.35f)))`, text at 0.4 alpha.
- **De-prioritized actions** (e.g. "NEXT" after result, secondary confirms): use `surface2` flat gradient + `onSurface` text color. This creates hierarchy — not every button should shout.

### Buzz Button
- Height 72dp, `RoundedCornerShape(20.dp)`, gradient teal→primary.
- Text: `"BUZZ"`, 22sp ExtraBold, `letterSpacing = 3.sp`, Color.White. **No emoji prefix.**
- Pulse animation when active (TTS done or buzz timer running):
  ```kotlin
  val pulseScale by infiniteTransition.animateFloat(1f, 1.03f,
      infiniteRepeatable(tween(500, FastOutSlowInEasing), RepeatMode.Reverse))
  Modifier.scale(if (shouldPulse) pulseScale else 1f)
  ```
- Ripple: `ripple(color = Color.White)`.

### Pause / Resume
- **Never full-width.** Always a small `IconButton(52.dp, CircleShape, background = surface2)`.
- Icons: `Icons.Filled.Pause` / `Icons.Filled.PlayArrow`, tint = `textMuted`, size = 22dp.
- Sits in a `Row` beside the primary action button (not below it).

### Countdown Timer (CountdownTimer composable)
```
        4.2          ← 32sp ExtraBold, centered, color = timerColor
▓▓▓▓▓▓▓░░░░        ← 6dp LinearProgressIndicator, rounded, same color
```
- Color animates amber→red via `animateColorAsState(tween(300))` when `seconds <= warningThreshold`.
- Bar: `height = 6.dp`, `clip(RoundedCornerShape(3.dp))`, `trackColor = qbColors.surface2`.
- The big number is the primary read; the bar is context. Do not shrink the number.

### Mic Card (MicCard composable)
- Static `background = qbColors.surface2` (never changes).
- Border animates: `animateColorAsState` (border/primary) + `animateDpAsState` (1dp→2dp) on listening state.
- Icon: real `Icon(Icons.Filled.Mic)` inside a 36dp `CircleShape` container.
  - Idle: container = `qbColors.surface`, icon tint = `textMuted`.
  - Listening: container = `primary.copy(0.15f)`, icon tint = `primary`.
- Animated dots: `animateFloat(0f → 3.99f, infiniteRepeatable(tween(750), Restart))` → `".".repeat(toInt()+1)`.
- Ripple: `ripple(color = qbColors.primary, bounded = true)`.

### Result Banner (ResultBanner composable)
- Layout: `Card(border = 1dp borderColor, elevation = 0)` with a `Row(SpaceBetween)`:
  - Left: headline ("Correct!" / "Wrong" / "POWER!" / "No buzz"), 22sp ExtraBold.
  - Right: point delta ("+10" / "-5" / "0"), 28sp ExtraBold. Right-aligned.
- Colors: `(green, greenDim)` / `(red, redDim)` / `(textMuted, surface2)`.
- Animation: `slideInVertically(spring(DampingRatioMediumBouncy)) + fadeIn(tween(200))`.
  - Use `spring` not `tween` for result entrance — physical weight matters here.
  - Trigger via `var visible by remember { mutableStateOf(false) }; LaunchedEffect(Unit) { visible = true }`.
- "You said: X" subtitle at 12sp, `textColor.copy(alpha = 0.7f)`, below the row.

### Screen Flash
- `var flashActive by remember { mutableStateOf(false) }` toggled in `LaunchedEffect(phase)`.
- `animateFloatAsState(if (flashActive) 0.18f else 0f, tween(400))` → overlay `Box(fillMaxSize, bg = color.copy(flashAlpha))`.
- Green flash for correct, red for wrong. Alpha 0.18 — perceptible but not blinding.
- Flash is a `Box` inside the Scaffold content `Box`, rendered last (on top of everything).

### Animation Rules
| Situation | Spec |
|---|---|
| Pulse (idle/buzz waiting) | `infiniteRepeatable(tween(500, FastOutSlowInEasing), Reverse)` |
| Result entrance | `spring(DampingRatioMediumBouncy, StiffnessMedium)` |
| Color/border state change | `animateColorAsState(tween(250–300))` — from `androidx.compose.animation` (not `.core`) |
| Size/width state change | `animateDpAsState(tween(250))` |
| Simple fade | `tween(200–280)` |
| Screen flash | `animateFloatAsState(tween(400))` |

### Anti-Patterns (do not use)
- **No Unicode/emoji as icons** (`←`, `⚙`, `⚡`, `⏸`, `⏩`, `🎤`, `✓`, `✗`). Always `Icon(Icons.*)`.
- **No gradient borders** (`Modifier.border(width, Brush)`). Adds noise, barely visible on dark bg.
- **No full-width secondary actions.** Pause, Resume, settings-related secondaries are icon buttons.
- **No emoji in button labels.** Button shape + color communicates urgency; text conveys the action.
- **No `Text("  ")` as layout spacer.** Use `Spacer`, `Arrangement.spacedBy`, or `padding`.
- **No instant background color swap** on state change. Animate with `animateColorAsState`.
- **No `shadowElevation` inside `graphicsLayer` after `.clip()`** — shadow draws outside bounds, clip removes it.
- **No `SuggestionChip` for metadata** — overengineered for read-only labels. Use plain `Box + background + Text`.

### Dependencies Required
`material-icons-extended` must be in `build.gradle.kts`:
```kotlin
implementation(libs.androidx.material.icons.extended)
```
And in `libs.versions.toml`:
```toml
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```
All icon imports: `import androidx.compose.material.icons.Icons` + specific icon paths.

## Design Colors (map to Compose theme)
```
Primary:       #7c83ff    Teal:   #2dd4bf    Rose: #f472b6    Amber: #fbbf24
BG dark:       #1a1b2e    Surface dark:  #242540    Surface2 dark:  #2d2e4a
BG light:      #f1f5f9    Surface light: #ffffff
Green:         #34d399 (dark) / #10b981 (light)
Red:           #f87171 (dark) / #ef4444 (light)
```

## File Structure
```
app/src/main/java/com/quizbowl/app/
├── MainActivity.kt
├── navigation/NavGraph.kt
├── ui/
│   ├── theme/{Color,Theme,Type}.kt
│   ├── components/{SettingsPanel,QuestionText,BuzzButton,AnswerInput,ScoreBoard}.kt
│   ├── home/HomeScreen.kt
│   ├── tossup/{TossupScreen,TossupViewModel}.kt
│   ├── bonus/{BonusScreen,BonusViewModel}.kt
│   └── multiplayer/{MultiplayerScreen,MultiplayerViewModel,LobbyScreen}.kt
├── api/{QbReaderApi,QbReaderService,MultiplayerClient}.kt
├── api/models/{Tossup,Bonus,CheckAnswerResult,RoomInfo}.kt
├── engine/{TtsEngine,SpeechEngine}.kt
├── data/SettingsRepository.kt
└── util/{TossupUtils,BonusUtils}.kt
```

## Important Notes
- TtsEngine must be tied to Activity lifecycle (TextToSpeech requires Context)
- SpeechRecognizer must be created/destroyed with each use (not reused across sessions)
- WebSocket ping: send `{"type":"ping"}` every 30s
- Answer normalization: leading `-N` → `negative N` before API call
- Power marker `(*)` is in raw question text, not sanitized; strip it before TTS
- Store username in DataStore (web used localStorage)
- Multiplayer userId: generate random `user_XXXXXXXX` per session
