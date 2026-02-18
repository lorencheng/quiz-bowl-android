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
1. **HomeScreen** — 3 nav cards; Bonus + Multiplayer show "Coming Soon" message
2. **TossupPracticeScreen** — solo tossup practice ← **active focus**
3. ~~BonusPracticeScreen~~ — deferred
4. ~~MultiplayerScreen~~ — deferred

## Current Priority: Tossup
Bonus and Multiplayer are deferred. HomeScreen already has 3 cards; tapping Bonus or Multiplayer
should show a "Coming Soon" toast/snackbar rather than navigating anywhere.

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
- Buzz timer (0–15s), Answer timer (0–15s) — tossup only
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
`IDLE → READING_LEADIN → READING_PART → ANSWERING → PART_RESULT → (repeat or DONE)`
- 3 parts, each 10 pts; track total/bonuses/30s/PPB
- Auto-advance after part result (1.5s)

### Multiplayer
- Lobby: list public rooms + manual join by name
- Room: WebSocket events for question words, buzz, answers, chat
- Ping keepalive every 30s
- TTS toggle + speed in room header
- Sidebar: player list (sorted by score) + chat
- Voice recognition on buzz

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
