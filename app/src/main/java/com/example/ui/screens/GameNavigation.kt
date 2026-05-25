package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.network.LanManager
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameNavigation(viewModel: GameViewModel) {
    val state by viewModel.roomState.collectAsState()
    val context = LocalContext.current

    // Local temporary transition screen at startup
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        showSplash = false
    }

    MysteryBackground(drawBloodDrips = showSplash || state.phase == GamePhase.LOBBY) {
        AnimatedContent(
            targetState = if (showSplash) GamePhase.LOBBY else state.phase,
            transitionSpec = {
                val duration = 800
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(duration)) +
                 slideInVertically(initialOffsetY = { 80 }, animationSpec = androidx.compose.animation.core.tween(duration)) +
                 scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(duration))) with
                (fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) +
                 scaleOut(targetScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(500)))
            },
            label = "PhaseTransition"
        ) { phase ->
            if (showSplash) {
                SplashScreen()
            } else {
                when (phase) {
                    GamePhase.LOBBY -> MainMenuOrLobbyScreen(viewModel, state)
                    GamePhase.ROLE_REVEAL -> RoleRevealScreen(viewModel, state)
                    GamePhase.CASE_INTRO -> CaseIntroScreen(viewModel, state)
                    GamePhase.EVIDENCE_ROUND -> EvidenceScreen(viewModel, state)
                    GamePhase.DISCUSSION -> DiscussionScreen(viewModel, state)
                    GamePhase.VOTING -> VotingScreen(viewModel, state)
                    GamePhase.VOTE_RESULT -> VoteResultScreen(viewModel, state)
                    GamePhase.JURY_ROUND -> JuryScreen(viewModel, state)
                    GamePhase.ENDGAME -> EndgameScreen(viewModel, state)
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun ThrillerTitleComponent(fontSize: androidx.compose.ui.unit.TextUnit = 70.sp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFF2C0A05), CircleShape)
                .border(2.dp, GoldShine, CircleShape),
            contentAlignment = Alignment.Center
        ) 
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "مين فينا ؟",
            color = GoldYell,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontFamily = HandjetFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("app_logo_arabic")
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        
    }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ThrillerTitleComponent(fontSize = 54.sp)

        Spacer(modifier = Modifier.height(70.dp))

        CircularProgressIndicator(
            color = RedAccent,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "الكل متهم .......ولكن ؟",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = 30.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// 2. MAIN MENU & LOBBY SYSTEM
// ==========================================
@Composable
fun MainMenuOrLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    var showPlayerSetup by remember { mutableStateOf(false) }
    var showLanJoinLobby by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    
    // Values for drafting new players
    val discoveredHosts by LanManager.discoveredHosts.collectAsState()
    val localIp = remember { LanManager.getLocalIpAddress() }

    if (isSettingsOpen) {
        SettingsDialog(viewModel = viewModel) { isSettingsOpen = false }
    }

    if (state.mode == "LAN") {
        val isHost = state.hostId == viewModel.myPlayerId.value
        if (isHost) {
            HostLobbyScreen(viewModel, state)
        } else {
            ClientWaitingScreen(viewModel, state)
        }
    } else {
        if (showPlayerSetup) {
            LocalSetupScreen(viewModel, state) { showPlayerSetup = false }
        } else if (showLanJoinLobby) {
            LanJoinLobbyScreen(viewModel, state, discoveredHosts, localIp) { showLanJoinLobby = false }
        } else {
            MainMenuHomeScreen(
                viewModel = viewModel,
                onStartPassPlay = {
                    viewModel.setupPassAndPlayGame()
                    showPlayerSetup = true
                },
                onOpenLanJoin = {
                    LanManager.startDiscovery()
                    showLanJoinLobby = true
                },
                onOpenSettings = { isSettingsOpen = true }
            )
        }
    }
}

@Composable
fun HostLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "غرفة المضيف")
        
        Spacer(modifier = Modifier.height(10.dp))
        
        ThrillerTitleComponent(fontSize = 38.sp)
        
        Spacer(modifier = Modifier.height(10.dp))
        
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 789L
        ) {
            Text(
                text = "شارك هذا الكود مع أصدقائك للانضمام:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0A05)),
                border = BorderStroke(2.dp, GoldShine),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = state.roomId,
                    color = GoldShine,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "اللاعبين المنضمون (${state.players.size} ) : ",
                color = Color(0xFF4A1008),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = player.name + if (player.id == state.hostId) " (مضيف)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (player.id != state.hostId) {
                            IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                                Icon(Icons.Default.Delete, "Remove Client", tint = RedAccent)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            val needed = 4 - state.players.size
            if (needed > 0) {
                Text(
                    text = "متبقي $needed لاعبين كحد أدنى للبدء.",
                    color = RedAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "الغرفة جاهزة لبدء القضية!",
                    color = InnocentAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.resetToMainMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1008)),
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء الغرفة", color = GoldShine)
            }
            
            Button(
                onClick = { viewModel.startInvestigationGame() },
                enabled = state.players.size in 4..6,
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(Icons.Default.PlayArrow, "Start game", tint = GoldShine)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ابدأ القضية!", color = GoldShine, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClientWaitingScreen(viewModel: GameViewModel, state: RoomState) {
    val myName = viewModel.myPlayerName.collectAsState().value
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "في انتظار التحقيق")
        
        Spacer(modifier = Modifier.height(10.dp))
        
        ThrillerTitleComponent(fontSize = 38.sp)
        
        Spacer(modifier = Modifier.height(10.dp))
        
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 999L
        ) {
            Text(
                text = "أنت منضم للغرفة رقم:",
                color = PapyrusTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = state.roomId,
                color = Color(0xFF4A1008),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedAccent, strokeWidth = 3.dp)
                Icon(Icons.Default.Fingerprint, "Investigating fingerprints", tint = DarkWoodButton, modifier = Modifier.size(32.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "يرجى الانتظار بينما يجمع المضيف اللاعبين الآخرين لبدء توزيع الأدلة الجنائية السرية...",
                color = PapyrusText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = " اللاعبون الحاليون باللوبي (${state.players.size}) : ",
                color = Color(0xFF4A1008),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x06000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x142C1E14), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (player.name == myName) RedAccent else DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Text(
                            text = player.name + if (player.name == myName) " (أنت)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Button(
            onClick = { viewModel.resetToMainMenu() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("مغادرة والرجوع للرئيسية", color = GoldShine)
        }
    }
}

@Composable
fun LocalSetupScreen(viewModel: GameViewModel, state: RoomState, onBack: () -> Unit) {
    val context = LocalContext.current
    var tempPlayerName by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "إعداد اللاعبين")
        
        Spacer(modifier = Modifier.height(10.dp))
        
        ThrillerTitleComponent(fontSize = 32.sp)
        
        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 123L
        ) {
            Text(
                text = "عدد اللاعبين: ${state.players.size} ",
                color = Color(0xFF4A1008),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "4 - 6 لاعبين (1 مجرم في 4 لاعبين، 2 مجرم في 5+ لاعبين)",
                color = PapyrusTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tempPlayerName,
                    onValueChange = { tempPlayerName = it },
                    label = { Text("اسم اللاعب الجديد") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PapyrusText,
                        unfocusedTextColor = PapyrusText,
                        focusedBorderColor = DarkWoodButton,
                        unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_name_input"),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (tempPlayerName.isNotBlank()) {
                            if (state.players.size < 6) {
                                viewModel.addLocalLobbyPlayer(tempPlayerName)
                                tempPlayerName = ""
                            } else {
                                Toast.makeText(context, "الحد الأقصى هو 6 لاعبين", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.testTag("add_player_button")
                ) {
                    Icon(Icons.Default.Add, "Add player", tint = GoldShine)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = player.name,
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = RedAccent)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                modifier = Modifier.weight(1f)
            ) {
                Text("رجوع")
            }
            
            Button(
                onClick = {
                    if (state.players.size < 4) {
                        Toast.makeText(context, "يجب توافر 4 لاعبين كحد أدنى للبدء", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.startInvestigationGame()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("start_game_button"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Start", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ابدأ اللعبة 🔍", color = GoldShine, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LanJoinLobbyScreen(
    viewModel: GameViewModel,
    state: RoomState,
    discoveredHosts: Map<String, String>,
    localIp: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var playerNameInput by remember { mutableStateOf("حمادة") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "الانضمام للغرفة")

        Spacer(modifier = Modifier.height(10.dp))
        
        ThrillerTitleComponent(fontSize = 32.sp)
        
        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 456L
        ) {
            Text(
                text = "جهازك متصل بالشبكة المحلية IP: $localIp",
                color = PapyrusTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = playerNameInput,
                onValueChange = { playerNameInput = it },
                label = { Text("اسمك في اللعبة") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PapyrusText,
                    unfocusedTextColor = PapyrusText,
                    focusedBorderColor = DarkWoodButton,
                    unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("player_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "انضم عن طريق رمز الغرفة:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { 
                        if (it.length <= 5) {
                            inputCode = it.filter { char -> char.isDigit() }
                        }
                    },
                    label = { Text("اكتب الرمز (5 أرقام)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PapyrusText,
                        unfocusedTextColor = PapyrusText,
                        focusedBorderColor = DarkWoodButton,
                        unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (inputCode.length == 5) {
                            val success = viewModel.joinLanHostByCode(inputCode, playerNameInput)
                            if (!success) {
                                Toast.makeText(
                                    context, 
                                    "يبدو أن الرمز غير نشط بالشبكة حالياً. تأكد من تشغيل الغرفة من المضيف.", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "الرمز يجب أن يتكون من 5 أرقام", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ربط", color = GoldShine)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "أو اختر غرفة من كشف الشبكة (UDP):",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (discoveredHosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DarkWoodButton, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "يبحث عن لغز نشط على الـ WiFi...",
                            color = PapyrusTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    discoveredHosts.forEach { (ip, hostDetails) ->
                        val parts = hostDetails.split("|")
                        val hostName = parts.getOrNull(0) ?: "غرفة مجهولة"
                        val rCode = parts.getOrNull(1) ?: "----"
                        
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                    .border(2.dp, GoldYell, RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.joinLanHost(ip, playerNameInput)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, "Wifi game", tint = RedAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hostName, color = PapyrusText, fontWeight = FontWeight.Bold)
                                    Text("رمز الغرفة: $rCode | IP: $ip", color = PapyrusTextSecondary, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.ArrowForward, "Join details", tint = DarkWoodButton)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back button
        Button(
            onClick = {
                LanManager.stopDiscovery()
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("الرجوع للقائمة الرئيسية", color = GoldShine)
        }
    }
}

@Composable
fun MainMenuHomeScreen(
    viewModel: GameViewModel,
    onStartPassPlay: () -> Unit,
    onOpenLanJoin: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ThrillerTitleComponent()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pass and Play Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable { onStartPassPlay() }
                    .testTag("new_game_opt_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Pass device",
                        tint = DarkWoodButton,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "لعبة جديدة",
                            color = Color(0xFF4A1008),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "جرِّب اللعب بالتمرير (جهاز واحد)",
                            color = PapyrusTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go play",
                        tint = DarkWoodButton
                    )
                }
            }

            // WiFi LAN Multiplayer Connect Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable { onOpenLanJoin() }
                    .testTag("lan_multiplayer_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "WiFi game",
                        tint = DarkWoodButton,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "دخول برمز الغرفة (WiFi)",
                            color = Color(0xFF4A1008),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "العب على جهازك مع أصدقائك بالرمز",
                            color = PapyrusTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go LAN Connect",
                        tint = DarkWoodButton
                    )
                }
            }

            // LAN Host Creator Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.startLanHost("مضيف التحقيق")
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF35120D)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, GoldYell)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddBox, "Host Game", tint = GoldShine)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "إنشاء ومشاركة غرفة جديدة (Host)",
                        color = GoldShine,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Settings & Preferences Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable { onOpenSettings() }
                    .testTag("settings_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        tint = DarkWoodButton,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "الإعدادات وقواعد اللعب",
                            color = Color(0xFF4A1008),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go settings",
                        tint = DarkWoodButton
                    )
                }
            }
        }

        Text(
            text = " !! القاعدة الاولي والاخيرة ... شك في الجميع",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}

// ==========================================
// 3. ROLE REVEAL SCREEN
// ==========================================
@Composable
fun RoleRevealScreen(viewModel: GameViewModel, state: RoomState) {
    val activePassPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
    var revealed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف الملفات السرية")

        Spacer(modifier = Modifier.height(10.dp))

        // Shield warnings during offline passing device sequential reveals
        if (!revealed) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0x3B6E1C11), CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Hide role cards",
                        tint = GoldShine,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ادي التلفون ل : ",
                    color = PapyrusBgLight.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = activePassPlayer.name,
                    color = GoldShine,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("pass_name_reveal")
                )

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { revealed = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYell),
                    modifier = Modifier.testTag("reveal_role_button")
                ) {
                    Text("اكتشف الدور السري 👁️", color = Color(0xFF2C150A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Secret Parchment Card showing role & motive details
            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = state.activePassPlayerIndex.toLong()
            ) {
                Text(
                    text = "الملف السري لـ ${activePassPlayer.name}",
                    color = DarkWoodButton,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Avatar outline
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(DarkBg, CircleShape)
                        .border(3.dp, GoldYell, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Avatar", tint = GoldShine, modifier = Modifier.size(50.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Character Metadata
                val char = activePassPlayer.character
                if (char != null) {
                    Text("الاسم : ${char.name}", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("السن : ${char.age} سنة | المهنة: ${char.occupation}", color = PapyrusTextSecondary, fontSize = 15.sp)
                    Text("الصفات : ${char.traits}", color = PapyrusTextSecondary, fontSize = 15.sp, fontStyle = FontStyle.Italic)

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(color = Color(0x3B2C1E14), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    // SECRET ROLE BADGE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activePassPlayer.isMafia) RedAccent else InnocentAccent)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (activePassPlayer.isMafia) Icons.Default.Dangerous else Icons.Default.Security,
                            contentDescription = "Role Symbol",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activePassPlayer.isMafia) "أنت : المجرم الحقيقية" else "أنت : بريء من الجريمة",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "دافعك المستخبي:",
                        color = Color(0xFF4A1008),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (activePassPlayer.isMafia) char.hiddenMotive else "انت برئ حاول تكتشف المجرم الحقيقي !!",
                        color = PapyrusText,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action to advance
            Button(
                onClick = {
                    viewModel.confirmSecretsRevealed()
                    revealed = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_reveal_advance"),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    text = if (state.activePassPlayerIndex < state.players.size - 1) "خبي ملفك وهات اللي بعده" else "يلا ندخل على تفاصيل القضية",
                    color = GoldShine,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

// ==========================================
// 4. CASE INTRO / DETAILS SCREEN
// ==========================================
@Composable
fun CaseIntroScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "تفاصيل الجريمة")

        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 9991L
        ) {
            Text(
                text = currentCase.title,
                color = Color(0xFF7A1B0C),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Info grid boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Narrative scrolling body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0x12000000), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = currentCase.description,
                            color = PapyrusText,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profiles list
            Text(
                text = " المشتبه فيهم : ",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentCase.characters.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF8C2012), RoundedCornerShape(6.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.name.split(" ").firstOrNull() ?: char.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.startCaseInvestigationIntro() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("case_details_confirm_button"),
            contentPadding = PaddingValues(15.dp)
        ) {
            Icon(Icons.Default.FindInPage, "Start Clues", tint = GoldShine)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ابدأ التحقيق ومراجعة الأدلة 🔎", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

// ==========================================
// 5. EVIDENCE / PROGRESSIVE CLUES SCREEN
// ==========================================
@Composable
fun EvidenceScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    val clueIndex = state.currentEvidenceIndex
    val currentClue = currentCase.evidenceList.getOrNull(clueIndex) ?: "لا أدلة إضافية حالياً."
    
    var showHint by remember(clueIndex) { mutableStateOf(false) }
    var localSuspicionValue by remember(clueIndex) { mutableStateOf(0.4f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "الدليل الجنائي ${clueIndex + 1} من ${currentCase.evidenceList.size}")

        Spacer(modifier = Modifier.height(12.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = (clueIndex + 10).toLong()
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFF35120D), CircleShape)
                    .border(2.dp, GoldShine, CircleShape)
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Evidence Seal",
                    tint = GoldShine,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "دليل جديد تم استنتاجه مفاجئ:",
                color = Color(0xFF531E17),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0x0F000000), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentClue,
                    color = PapyrusText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated / dynamic custom caution warning click hint
            AnimatedVisibility(visible = showHint) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF2CD))
                        .border(1.dp, Color(0xFFFFCD56), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = currentCase.hint,
                        color = Color(0xFF856404),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!showHint) {
                Button(
                    onClick = { showHint = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2A012)),
                    modifier = Modifier.testTag("clue_hint_button")
                ) {
                    Icon(Icons.Default.Warning, "Clues Alert", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عرض تلميح  💡", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Suspicion Slider scale
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.advanceFromEvidenceToDiscussion() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("evidence_reveal_advance"),
            contentPadding = PaddingValues(15.dp)
        ) {
            Icon(Icons.Default.RecordVoiceOver, "Discuss", tint = GoldShine)
            Spacer(modifier = Modifier.width(8.dp))
            Text("فتح طاولة النقاش والمواجهة 🗣️", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ==========================================
// 6. DISCUSSION SCREEN (WITH RADIAL CLOCK)
// ==========================================
@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    var suspectedByClick = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "مرحلة النقاش والمواجهة")

        // Interactive Radial Clock & Circular layout of players around
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Clock Timer in the central region
            val formattedTime = String.format("%02d:%02d", state.timerSecondsLeft / 60, state.timerSecondsLeft % 60)
            
            Canvas(modifier = Modifier.size(170.dp)) {
                // Background dark disk
                drawCircle(color = Color(0xFF1E0604), radius = size.minDimension / 2)
                
                // Red sweep progress trace
                val sweepAngle = if (state.timerTotalSeconds > 0) {
                    (state.timerSecondsLeft.toFloat() / state.timerTotalSeconds.toFloat()) * 360f
                } else 360f
                
                drawArc(
                    color = Color(0xFFE73224),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Central time texts
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("متبقي", color = GoldYell, fontSize = 12.sp)
                Text(
                    text = formattedTime,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("timer_countdown_display")
                )
                Text("للإدلاء بالاستنتاج", color = PapyrusBgLight.copy(alpha = 0.5f), fontSize = 10.sp)
            }

            // Radial distribution placement logic for players
            val alivePlayers = state.players.filter { it.isAlive }
            alivePlayers.forEachIndexed { index, player ->
                val angleRad = (2 * Math.PI * index) / alivePlayers.size
                val xOffset = (130 * cos(angleRad)).dp
                val yOffset = (130 * sin(angleRad)).dp

                val isClickSuspected = player.id in suspectedByClick

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(68.dp)
                        .shadow(3.dp, CircleShape)
                        .background(
                            if (isClickSuspected) Color(0xFFC42512) else Color(0xFF421E14),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            if (isClickSuspected) GoldShine else Color(0x3BFFFFFF),
                            CircleShape
                        )
                        .clickable {
                            if (isClickSuspected) {
                                suspectedByClick.remove(player.id)
                            } else {
                                suspectedByClick.add(player.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = player.name.take(6),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (player.isMafia && isClickSuspected) GoldYell else Color(0x3B000000),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isClickSuspected) "متهم ⚠️" else "قيد السؤال",
                                color = if (isClickSuspected) Color.Black else Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.wrapContentHeight(),
            seed = 771L
        ) {
            Text(
                text = "دوس على أي لاعب عشان تركز الشكوك عليه باللون الأحمر عشان تبدأوا تناقشوه.",
                color = PapyrusTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.advanceFromDiscussionToVoting() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("voting_advance_button")
        ) {
            Icon(Icons.Default.HowToVote, "Start Votes", tint = GoldShine)
            Spacer(modifier = Modifier.width(8.dp))
            Text("يلا ندخل على الاقتراع والتصويت", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

// ==========================================
// 7. VOTING SCREEN
// ==========================================
@Composable
fun VotingScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current

    if (state.mode == "PASS_AND_PLAY") {
        val voterPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
        var isDevicePassed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }

        if (!isDevicePassed) {
            MysteryBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "صندوق التصويت")
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(
                        text = "هات الموبايل ووريه لـ/ ${voterPlayer.name}",
                        color = PapyrusBgLight,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "فكر قبل ما تصوت .....شغل دماغك !!!",
                        color = Color.LightGray,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = { isDevicePassed = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("يلا نصوّت", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        // Show the actual voting options card
        var selectedTargetId by remember { mutableStateOf("") }
        val eligibleCandidates = state.players.filter { it.isAlive && it.id != voterPlayer.id }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")

            Spacer(modifier = Modifier.height(10.dp))

            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = 33L
            ) {
                Text(
                    text = "دور اللاعب: ${voterPlayer.name}",
                    color = Color(0xFF6E1B10),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "اختار الشخص اللي شاكك فيه تفتكر هو المجرم:",
                    color = PapyrusTextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(eligibleCandidates) { candidate ->
                        val isSelected = candidate.id == selectedTargetId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    2.dp,
                                    if (isSelected) RedAccent else Color(0x1F2C1E14),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTargetId = candidate.id }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                    contentDescription = "Pick status target",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                candidate.character?.let {
                                    Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedTargetId.isBlank()) {
                        Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.submitVote(selectedTargetId)
                        selectedTargetId = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_vote_action_button")
            ) {
                Text("أكد صوتك يلا", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    } else {
        // LAN MODE - Simultaneously on each different device
        val localVoter = state.players.find { it.id == viewModel.myPlayerId.value } ?: return
        
        if (!localVoter.isAlive) {
            // Dead players cannot vote
            MysteryBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = " أنت برة اللعب دلوقتي 💀")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "مبروك تصفيتك! استنى تصويت باقي اللعيبة...",
                        color = PapyrusBgLight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (state.votes.containsKey(localVoter.id)) {
            // Player already voted, show awaiting progress and list of players who did and who did not vote
            val activePlayers = state.players.filter { it.isAlive }
            val votedPlayers = activePlayers.filter { it.id in state.votes.keys }
            val waitingPlayers = activePlayers.filter { it.id !in state.votes.keys }

            MysteryBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "تم تسجيل صوتك بنجاح! 🗳️")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "مستنيين باقي اللعيبة يصوتوا...",
                        color = PapyrusBgLight,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ParchmentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("الشفافية والتصويت المفتوح المباشر:", color = Color(0xFF6E1B10), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val votesCast = state.votes.mapNotNull { (vId, tId) ->
                                val voterName = state.players.find { it.id == vId }?.name ?: return@mapNotNull null
                                val targetName = state.players.find { it.id == tId }?.name ?: return@mapNotNull null
                                "👈 اللاعب $voterName صوّت ضد $targetName"
                            }
                            
                            if (votesCast.isEmpty()) {
                                Text("في انتظار الصوت العلني الأول لبدء كشف التواطؤ... 🗳️", color = PapyrusTextSecondary, fontSize = 14.sp)
                            } else {
                                votesCast.forEach { voteLine ->
                                    Text(
                                        text = voteLine,
                                        color = PapyrusText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("مين اللي لسه مصوّتش:", color = Color(0xFF6E1B10), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            val waitingNames = waitingPlayers.joinToString { it.name }.ifEmpty { "الجميع أدلى بصوته علناً!" }
                            Text(waitingNames, color = PapyrusTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            // Show the options for the local player on their own device
            var selectedTargetId by remember { mutableStateOf("") }
            val eligibleCandidates = state.players.filter { it.isAlive && it.id != localVoter.id }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")

                Spacer(modifier = Modifier.height(10.dp))

                ParchmentCard(
                    modifier = Modifier.weight(1f),
                    seed = 33L
                ) {
                    Text(
                        text = "دورك في التصويت: ${localVoter.name}",
                        color = Color(0xFF6E1B10),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "اختار الشخص اللي شاكك فيه تفتكر هو المجرم:",
                        color = PapyrusTextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(eligibleCandidates) { candidate ->
                            val isSelected = candidate.id == selectedTargetId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        2.dp,
                                        if (isSelected) RedAccent else Color(0x1F2C1E14),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTargetId = candidate.id }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                        contentDescription = "Pick status target",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    candidate.character?.let {
                                        Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedTargetId.isBlank()) {
                            Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.submitVote(selectedTargetId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("submit_vote_action_button")
                ) {
                    Text("أكد صوتك يلا", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}

// ==========================================
// 8. JURY ENDGAME MECHANIC SCREEN
// ==========================================
@Composable
fun JuryScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val eliminatedPlayers = state.players.filter { !it.isAlive }
    val remainingSuspects = state.players.filter { it.isAlive }
    val localPlayer = state.players.find { it.id == viewModel.myPlayerId.value }

    if (state.mode == "PASS_AND_PLAY") {
        val juryVoter = eliminatedPlayers.firstOrNull { it.id !in state.juryVotes.keys }
        var isDevicePassed by remember(juryVoter?.id) { mutableStateOf(false) }

        if (juryVoter != null && !isDevicePassed) {
            MysteryBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "مرر الموبايل")
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(
                        text = "هات الموبايل ووريه ل /  ${juryVoter.name}",
                        color = PapyrusBgLight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تصويتك هيكون مهم ومصير الباقيين في إيدك .... متبقاش غبي !!",
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = { isDevicePassed = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("ادخل صوّت ", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        // Display the actual jury voting UI for PASS_AND_PLAY
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = 88L
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0x3B6E1B10), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Gavel judge",
                        tint = RedAccent,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "!!! لا تقلقوا ولكن احذروا !!!",
                    color = Color(0xFF6E1D10),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "بما أنه لم يتبق سوى لاعبين اثنين، يعود اللاعبين الذين تم تصفيتهم سابقاً للإجماع والتصويت لإثبات الإدانة النهائية على المجرم.",
                    color = PapyrusTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (juryVoter != null) {
                    Text(
                        text = "دور اللاعب : ${juryVoter.name}",
                        color = RedAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("jury_voter_title")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(remainingSuspects) { suspect ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0x3B2C1E14), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    suspect.character?.let {
                                        Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    }
                                }
                                
                                Button(
                                    onClick = { viewModel.submitJuryVote(suspect.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                                ) {
                                    Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "تم جمع كافة استنتاجات اللاعبين بنجاح. سنعلن النتيجة الآن!",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        // LAN Mode
        if (localPlayer == null) return
        val isAlive = localPlayer.isAlive
        val hasVoted = state.juryVotes.containsKey(localPlayer.id)

        MysteryBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isAlive) {
                    // Alive suspects waiting for the verdict
                    ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "هيئة المحلفين بتصوّت دلوقتي...",
                        color = PapyrusBgLight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "مصيرك وصاحبك الأخير بين إيدين  اللاعبين اللي خرجوا ! مين هيتبرأ ومين هيدان؟ تفتكر هيختاروا صح؟",
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Dead players (Jury members) voting on their own devices
                    if (hasVoted) {
                        ParchmentHeaderBanner(text = "تم تسجيل صوتك للمحلفين! ⚖️")
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "مستنيين باقي اللاعبين عشان تظهر النتيجة...",
                            color = PapyrusBgLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                        Spacer(modifier = Modifier.height(16.dp))
                        ParchmentCard(
                            modifier = Modifier.weight(1f),
                            seed = 88L
                        ) {
                            Text(
                                text = "اضغط إدانة على المجرم الحقيقي عشان تحسم الجريمة وترجع حق الضحية!",
                                color = Color(0xFF6E1D10),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(remainingSuspects) { suspect ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0x3B2C1E14), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            suspect.character?.let {
                                                Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                            }
                                        }
                                        
                                        Button(
                                            onClick = { viewModel.submitJuryVote(suspect.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                                        ) {
                                            Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 9. ENDGAME WINNER SCREEN
// ==========================================
@Composable
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase
    val isInnocentsWinner = state.winnerSide == "INNOCENTS"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف أوراق القضية النهائية")

        Spacer(modifier = Modifier.height(14.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 4441L
        ) {
            // Trophy Logo Area
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0x1FA2A012), CircleShape)
                    .border(2.dp, GoldYell, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy logo endgame",
                    tint = GoldYell,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BIG WINNER BANNER
            Text(
                text = if (isInnocentsWinner) "!!الف مبرووك عرفتوا تطلعوا المجرم الفاشل!!" else "!المجرم انتصر وضحك على الكل!",
                color = if (isInnocentsWinner) GreenAccent else RedAccent,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("endgame_victory_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Plot reveal details scrolls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = if (isInnocentsWinner) {
                                "الأبرياء عرفوا يجمعوا الأدلة ويكشفوا اللعبة الصح، والمجرم وقع في شر أعماله ."
                            } else {
                                "المجرم عرف يضحك على الكل ويثبت تهم باطلة على الأبرياء، وخرج من القضية زي الشعرة من العجين."
                            },
                            color = PapyrusText,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(10.dp))

                        // --- CRIMINAL DRAMATIC REVEAL ---
                        Text(
                            text = "الهوية الحقيقية للمجرم:",
                            color = Color(0xFF4A1008),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().testTag("dramatic_criminal_reveal_header")
                        )

                        state.players.filter { it.isMafia }.forEach { mafia ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x1CE63946)),
                                border = BorderStroke(1.dp, Color(0xFFE63946)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "المجرم الحقيقي: ${mafia.name}",
                                        color = Color(0xFFD62828),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.testTag("criminal_character_name")
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("العمر: ${mafia.character?.age ?: 30} سنة | المهنة: ${mafia.character?.occupation ?: "مجهول"}", color = PapyrusText, fontSize = 15.sp)
                                    Text("المظهر والطباع: ${mafia.character?.traits ?: ""}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("المستوى الاجتماعي: ${mafia.character?.socialStatus ?: "متوسط الحال"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("علاقته بالضحية: ${mafia.character?.relationshipToVictim ?: "غامضة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("علاقته بالمشتبهين: ${mafia.character?.relationshipToOtherSuspects ?: "منافسة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("السجل الجنائي: ${mafia.character?.relevantHistory ?: "خالي من السوابق"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "الدافع والنية المستخبية: ${mafia.character?.hiddenMotive ?: ""}",
                                        color = Color(0xFF4A1008),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(14.dp))

                        // --- CASE EXPLANATION CLOSURE ---
                        Text(
                            text = "المخطط الكامل وسيناريو الجريمة الداخلي:",
                            color = Color(0xFF355E3B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.fillMaxWidth().testTag("case_explanation_header")
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1F2A9D8F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentCase?.explanation ?: "لم تتوفر سجلات سردية للملف.",
                                color = Color(0xFF1D3557),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(12.dp).testTag("case_explanation_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "كشف هويات كل اللاعبين بغرفة التحقيق:",
                            color = DarkWoodButton,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        state.players.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (p.isMafia) "مجرم" else "بريء ",
                                    color = if (p.isMafia) RedAccent else InnocentAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${p.name} (${p.character?.name ?: ""})",
                                    color = PapyrusTextSecondary,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play Again & Exit Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.playButtonClick(); viewModel.playAgain() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("play_again_button"),
                contentPadding = PaddingValues(15.dp)
            ) {
                Icon(Icons.Default.Refresh, "Play again", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("لعب جولة وقضية جديدة", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            OutlinedButton(
                onClick = { viewModel.playButtonClick(); viewModel.resetToMainMenu() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Home, "Main menu", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("العودة للقائمة الرئيسية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

// ==========================================
// 10. SETTINGS & CONVENTIONS DIALOG
// ==========================================
@Composable
fun SettingsDialog(
    viewModel: GameViewModel,
    onDismissRequest: () -> Unit
) {
    val state by viewModel.roomState.collectAsState()
    var discTimeMins by remember { mutableStateOf(state.settings.discussionTimeMinutes) }
    var voteTimeMins by remember { mutableStateOf(state.settings.votingTimeMinutes) }
    var soundEnabled by remember { mutableStateOf(state.settings.isMusicEnabled) }
    var sliderVol by remember { mutableStateOf(state.settings.volume) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateSettings(discTimeMins, voteTimeMins, soundEnabled, sliderVol)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ التعديلات", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("إلغاء", color = PapyrusTextSecondary, fontSize = 16.sp)
            }
        },
        title = {
            Text(
                text = "إعدادات وقواعد اللعبة",
                color = Color(0xFF4A1008),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            ParchmentCard(
                seed = 77L,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.wrapContentHeight()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Sound switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { soundEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = RedAccent)
                            )
                            Text("المؤثرات الصوتية والموسيقى", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("درجة الصوت: ${(sliderVol * 100).toInt()}%", color = PapyrusTextSecondary, fontSize = 14.sp)
                        Slider(
                            value = sliderVol,
                            onValueChange = { sliderVol = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = Color(0x3B2C1E14))

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom timings
                        Text("وقت جولات المناقشة والتحقيق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { if (discTimeMins > 1) discTimeMins-- },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text("$discTimeMins دقائق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Button(
                                onClick = { if (discTimeMins < 10) discTimeMins++ },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("وقت جولات الاقتراع والتصويت", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { if (voteTimeMins > 1) voteTimeMins-- },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text("$voteTimeMins دقائق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Button(
                                onClick = { if (voteTimeMins < 5) voteTimeMins++ },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                            ) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Divider(color = Color(0x3B2C1E14))

                        Spacer(modifier = Modifier.height(6.dp))

                        // Game Rules
                        Text("قوانين اللعبة الأساسية:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "1. اللعبة تدعم من 4 لـ 6 لاعبين.\n" +
                                   "2. لو عدد اللاعبين 4، بيكون فيه مجرم واحدة بس؛ ولو أكتر من كدة بيتم تعيين 2 مجرم تلقائياً لدعم التحدي والمنافسة.\n" +
                                   "3. في نهاية الجولة لو اتبقى اتنين مشتبه بيهم بس عايشين، بيتلغي تصويت الاقتراع المباشر واللعيبة اللي خرجوا بترجع تلقائياً كـ (هيئة المحلفين) لحسم القرار النهائي وإدانة المجرم الحقيقية.",
                            color = PapyrusTextSecondary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        },
        containerColor = PapyrusBg
    )
}

@Composable
fun VoteResultScreen(viewModel: GameViewModel, state: RoomState) {
    val isHost = state.mode == "PASS_AND_PLAY" || state.hostId == viewModel.myPlayerId.value
    
    MysteryBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ParchmentHeaderBanner(text = "نتائج الاقتراع العام")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ParchmentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (state.tiedVotePlayers.isNotEmpty()) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = "Result Icon",
                        tint = if (state.tiedVotePlayers.isNotEmpty()) Color(0xFFC62828) else GoldShine,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = state.lastEliminatedResult,
                        color = Color(0xFF1C130C),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        modifier = Modifier.testTag("vote_result_text")
                    )
                    
                    if (state.tiedVotePlayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "قانون تصفية التعادل: سيتم تكرار جولة التصويت الآن لتكون محصورة ومقتصرة فقط على المشتبهين المتساوين بالأصوات حتى التوصل إلى أغلبية حاسمة تفصل الشك بالحقيقة.",
                            color = Color(0xFFB71C1C),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "كشف الأصوات العامة  : 🗳️",
                        color = Color(0xFF6E1B10),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val votesSummary = state.votes.mapNotNull { (vId, tId) ->
                        val voter = state.players.find { it.id == vId }?.name ?: return@mapNotNull null
                        val target = state.players.find { it.id == tId }?.name ?: return@mapNotNull null
                        "👤 $voter ➔ صوّت ضد 🎯 $target"
                    }
                    if (votesSummary.isEmpty()) {
                        Text("لم يتم الإدلاء بأي أصوات.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        votesSummary.forEach { voteText ->
                            Text(
                                text = voteText,
                                color = Color(0xFF2C1E14),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isHost) {
                Button(
                    onClick = {
                        viewModel.playButtonClick()
                        viewModel.confirmVoteResultAndProceed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("confirm_vote_result_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (state.tiedVotePlayers.isNotEmpty()) "بدء جولة حسم التعادل" else "متابعة مسار التحقيق",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x3D2C1E14)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "في انتظار المضيف لمتابعة القضية...",
                        color = PapyrusBgLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
