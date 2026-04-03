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
