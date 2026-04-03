package com.quizbowl.app.ui.multiplayer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizbowl.app.ui.theme.qbColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerScreen(navController: NavController) {
    val vm: MultiplayerViewModel = viewModel()

    val view by vm.view.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    when (view) {
        MultiplayerView.LOBBY -> LobbyView(vm = vm, navController = navController, error = error)
        MultiplayerView.ROOM -> RoomView(vm = vm, error = error)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lobby
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LobbyView(vm: MultiplayerViewModel, navController: NavController, error: String?) {
    val usernameInput by vm.usernameInput.collectAsStateWithLifecycle()
    val roomNameInput by vm.roomNameInput.collectAsStateWithLifecycle()
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    val roomsLoading by vm.roomsLoading.collectAsStateWithLifecycle()
    val connecting by vm.connecting.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.fetchRooms() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer") },
                navigationIcon = {
                    TextButton(onClick = { navController.navigateUp() }) {
                        Text("←", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.qbColors.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.qbColors.redDim),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(error, color = MaterialTheme.qbColors.red, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.dismissError() }) { Text("✕") }
                    }
                }
            }

            // Join card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.qbColors.surface),
                border = BorderStroke(1.dp, MaterialTheme.qbColors.border),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Join a Room", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { vm.updateUsernameInput(it) },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { vm.updateRoomNameInput(it) },
                        label = { Text("Room name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { vm.joinRoom() }),
                    )
                    Button(
                        onClick = { vm.joinRoom() },
                        enabled = !connecting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (connecting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (connecting) "Connecting…" else "Join")
                    }
                }
            }

            // Public rooms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Public Rooms",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.qbColors.textMuted,
                )
                TextButton(onClick = { vm.fetchRooms() }, enabled = !roomsLoading) {
                    Text(if (roomsLoading) "Loading…" else "Refresh", fontSize = 13.sp)
                }
            }

            if (rooms.isEmpty() && !roomsLoading) {
                Text(
                    "No public rooms found.",
                    fontSize = 13.sp,
                    color = MaterialTheme.qbColors.textMuted,
                )
            }

            rooms.forEach { room ->
                RoomCard(room = room, onClick = {
                    vm.updateRoomNameInput(room.displayName)
                    vm.joinRoom(room.displayName)
                })
            }
        }
    }
}

@Composable
private fun RoomCard(room: com.quizbowl.app.api.models.RoomInfo, onClick: () -> Unit) {
    val qbColors = MaterialTheme.qbColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = qbColors.surface),
        border = BorderStroke(1.dp, qbColors.border),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(room.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (!room.setName.isNullOrEmpty()) {
                    Text(room.setName, fontSize = 12.sp, color = qbColors.textMuted)
                }
            }
            Text(
                "${room.displayPlayerCount} players",
                fontSize = 12.sp,
                color = qbColors.textMuted,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Room
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomView(vm: MultiplayerViewModel, error: String?) {
    val connectedRoomName by vm.connectedRoomName.collectAsStateWithLifecycle()
    val players by vm.players.collectAsStateWithLifecycle()
    val myUserId by vm.myUserId.collectAsStateWithLifecycle()
    val canBuzz by vm.canBuzz.collectAsStateWithLifecycle()
    val buzzedPlayer by vm.buzzedPlayer.collectAsStateWithLifecycle()
    val questionWords by vm.questionWords.collectAsStateWithLifecycle()
    val questionType by vm.questionType.collectAsStateWithLifecycle()
    val bonusLeadin by vm.bonusLeadin.collectAsStateWithLifecycle()
    val bonusParts by vm.bonusParts.collectAsStateWithLifecycle()
    val answerResult by vm.answerResult.collectAsStateWithLifecycle()
    val chatMessages by vm.chatMessages.collectAsStateWithLifecycle()
    val answer by vm.answer.collectAsStateWithLifecycle()
    val chatInput by vm.chatInput.collectAsStateWithLifecycle()
    val ttsEnabled by vm.ttsEnabled.collectAsStateWithLifecycle()
    val ttsRate by vm.ttsRate.collectAsStateWithLifecycle()
    val listening by vm.listening.collectAsStateWithLifecycle()
    val voiceDisabled by vm.voiceDisabled.collectAsStateWithLifecycle()

    val qbColors = MaterialTheme.qbColors
    val iAmBuzzed = buzzedPlayer != null && buzzedPlayer!!.userId == myUserId
    val sortedPlayers = remember(players) { players.sortedByDescending { it.score } }

    val answerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(iAmBuzzed) {
        if (iAmBuzzed) {
            kotlinx.coroutines.delay(150)
            try { answerFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val gameScrollState = rememberScrollState()
    LaunchedEffect(questionWords.size, bonusParts.size) {
        gameScrollState.animateScrollTo(gameScrollState.maxValue)
    }

    val chatListState = rememberLazyListState()
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) chatListState.animateScrollToItem(chatMessages.size - 1)
    }

    var chatExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(connectedRoomName, fontSize = 16.sp) },
                actions = {
                    TextButton(
                        onClick = { vm.leaveRoom() },
                        colors = ButtonDefaults.textButtonColors(contentColor = qbColors.red),
                    ) {
                        Text("Leave")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = qbColors.surface),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // TTS controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(qbColors.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("TTS", fontSize = 13.sp, color = qbColors.textMuted)
                Switch(
                    checked = ttsEnabled,
                    onCheckedChange = { vm.setTtsEnabled(it) },
                    modifier = Modifier.height(24.dp),
                )
                if (ttsEnabled) {
                    Text("${"%.1f".format(ttsRate)}×", fontSize = 12.sp, color = qbColors.textMuted)
                    androidx.compose.material3.Slider(
                        value = ttsRate,
                        onValueChange = { vm.setTtsRate(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier.weight(1f).height(24.dp),
                    )
                }
            }

            HorizontalDivider(color = qbColors.border)

            // Error banner
            if (error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = qbColors.redDim),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = qbColors.red, fontSize = 12.sp,
                    )
                }
            }

            // Players row
            if (sortedPlayers.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(qbColors.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sortedPlayers) { player ->
                        PlayerChip(
                            player = player,
                            isMe = player.userId == myUserId,
                            isBuzzed = player.userId == buzzedPlayer?.userId,
                        )
                    }
                }
                HorizontalDivider(color = qbColors.border)
            }

            // Game area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(gameScrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (questionType == null) {
                    Text(
                        "Waiting for question…",
                        color = qbColors.textMuted,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                    )
                }

                // Tossup: accumulated words
                if (questionType == "tossup" && questionWords.isNotEmpty()) {
                    Text(
                        text = questionWords.joinToString(" "),
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                    )
                }

                // Bonus: leadin + parts
                if (questionType == "bonus") {
                    bonusLeadin?.let { leadin ->
                        Text(
                            text = leadin,
                            fontSize = 15.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 24.sp,
                            color = qbColors.textMuted,
                        )
                    }
                    bonusParts.forEachIndexed { i, part ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = qbColors.surface2),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "Part ${i + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = qbColors.textMuted,
                                )
                                Text(part, fontSize = 15.sp, lineHeight = 24.sp)
                            }
                        }
                    }
                }

                // Buzz indicator
                if (buzzedPlayer != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = qbColors.amberDim),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = if (iAmBuzzed) "You buzzed in!" else "${buzzedPlayer!!.username} buzzed in!",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = qbColors.accentAmber,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }

                // Answer result
                answerResult?.let { result ->
                    val isCorrect = result.directive == "accept"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) qbColors.greenDim else qbColors.redDim,
                        ),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${result.username}: \"${result.givenAnswer}\"",
                                fontSize = 13.sp,
                                color = if (isCorrect) qbColors.green else qbColors.red,
                                fontWeight = FontWeight.Medium,
                            )
                            if (result.score != 0) {
                                Text(
                                    text = if (result.score > 0) "+${result.score} pts" else "${result.score} pts",
                                    fontSize = 12.sp,
                                    color = if (isCorrect) qbColors.green else qbColors.red,
                                )
                            }
                            result.revealedAnswer?.let { ans ->
                                Text(
                                    text = "Answer: $ans",
                                    fontSize = 12.sp,
                                    color = qbColors.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider(color = qbColors.border)

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Buzz button
                if (canBuzz && buzzedPlayer == null && questionType == "tossup") {
                    Button(
                        onClick = { vm.buzz() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = qbColors.accentTeal),
                    ) {
                        Text("Buzz!", fontWeight = FontWeight.Bold)
                    }
                }

                // Answer input (when I buzzed OR bonus answering)
                val showAnswerInput = iAmBuzzed || (questionType == "bonus" && bonusParts.isNotEmpty())
                if (showAnswerInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { newText ->
                                vm.updateAnswer(newText)
                                vm.disableVoiceIfActive()
                            },
                            placeholder = {
                                Text(
                                    text = if (listening) "Listening… (or type)" else "Type your answer…",
                                    fontSize = 14.sp,
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(answerFocusRequester)
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                        vm.submitAnswer(); true
                                    } else false
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { vm.submitAnswer() }),
                        )
                        if (vm.speechSupported && !voiceDisabled) {
                            OutlinedButton(
                                onClick = { if (!listening) vm.startVoice() },
                                modifier = Modifier.size(56.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = if (listening)
                                    ButtonDefaults.outlinedButtonColors(containerColor = qbColors.primaryGlow)
                                else ButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text("🎤", fontSize = 20.sp)
                            }
                        }
                    }
                    Button(
                        onClick = { vm.submitAnswer() },
                        enabled = answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Submit")
                    }
                }

                // Next button
                if (questionType != null) {
                    OutlinedButton(
                        onClick = { vm.requestNext() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Next")
                    }
                }
            }

            HorizontalDivider(color = qbColors.border)

            // Chat section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(qbColors.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Chat (${chatMessages.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    TextButton(onClick = { chatExpanded = !chatExpanded }) {
                        Text(if (chatExpanded) "▲" else "▼", fontSize = 11.sp)
                    }
                }

                if (chatExpanded) {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(chatMessages) { msg ->
                            if (msg.isSystem) {
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = qbColors.textMuted,
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${msg.username}:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = qbColors.primary,
                                    )
                                    Text(msg.text, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { vm.updateChatInput(it) },
                            placeholder = { Text("Message…", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                        vm.sendChat(); true
                                    } else false
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { vm.sendChat() }),
                        )
                        TextButton(onClick = { vm.sendChat() }, enabled = chatInput.isNotBlank()) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerChip(player: MpPlayer, isMe: Boolean, isBuzzed: Boolean) {
    val qbColors = MaterialTheme.qbColors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            isBuzzed -> qbColors.amberDim
            isMe -> qbColors.primaryGlow
            else -> qbColors.surface2
        },
        border = BorderStroke(
            1.dp,
            if (isBuzzed) qbColors.accentAmber else if (isMe) qbColors.primary else qbColors.border,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = player.username,
                fontSize = 12.sp,
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                color = if (isMe) qbColors.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${player.score}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = qbColors.accentAmber,
            )
        }
    }
}
