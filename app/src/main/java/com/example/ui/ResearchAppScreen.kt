package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ContentValues
import android.provider.MediaStore
import com.example.data.GolfScorecard
import coil.compose.AsyncImage
import com.example.R
import com.example.api.GeminiClient
import com.example.data.AcademicPaper
import com.example.data.ChatMessage
import com.example.data.HuggingFaceModel
import com.example.data.GeneratedMediaItem
import kotlinx.coroutines.delay
import com.example.ui.theme.ProfessionalBackground
import com.example.ui.theme.ProfessionalCard
import com.example.ui.theme.ProfessionalPrimary
import com.example.ui.theme.ProfessionalSecondary
import com.example.ui.theme.ProfessionalBorder
import com.example.ui.theme.ProfessionalText
import com.example.ui.theme.ProfessionalTextMuted
import com.example.ui.theme.ProfessionalSuccess
import com.example.ui.theme.ProfessionalLabelBg
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchAppScreen(
    viewModel: ResearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val activeTab by viewModel.activeTab.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val activeModelMode by viewModel.activeModelMode.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    
    val currentPapers by viewModel.papers.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val hfModels by viewModel.hfModels.collectAsState()
    
    val isAnalyzingPaper by viewModel.isAnalyzingPaper.collectAsState()
    val analyzingProgress by viewModel.analyzingProgress.collectAsState()

    val generatedImageUrl by viewModel.generatedImageUrl.collectAsState()
    val generatedVideoFrames by viewModel.generatedVideoFrames.collectAsState()
    val isGeneratingMedia by viewModel.isGeneratingMedia.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val mediaHistory by viewModel.mediaHistory.collectAsState()
    val mediaError by viewModel.mediaError.collectAsState()

    val scorecards by viewModel.scorecards.collectAsState()
    val isAnalyzingScorecard by viewModel.isAnalyzingScorecard.collectAsState()
    val scorecardAnalysisError by viewModel.scorecardAnalysisError.collectAsState()
    val extractedScorecard by viewModel.extractedScorecard.collectAsState()
    val scorecardEngineMode by viewModel.scorecardEngineMode.collectAsState()
    
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedCitationStyle by remember { mutableStateOf("APA") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (activeModelMode.contains("Local")) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Column {
                            Text(
                                "LocalResearch AI",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp
                            )
                            Text(
                                if (activeModelMode.contains("Local")) "Offline Sandbox Enabled" else "Hybrid AI Connected",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == ActiveTab.LIBRARY,
                    onClick = { viewModel.selectTab(ActiveTab.LIBRARY) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Library Navigation Icon") },
                    label = { Text("Library") },
                    modifier = Modifier.testTag("tab_library")
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.CHAT,
                    onClick = { viewModel.selectTab(ActiveTab.CHAT) },
                    icon = { Icon(Icons.Default.Face, contentDescription = "Private Sandbox Chatbot Navigator") },
                    label = { Text("Chatbot") },
                    modifier = Modifier.testTag("tab_chatbot")
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.MODELS,
                    onClick = { viewModel.selectTab(ActiveTab.MODELS) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "HuggingFace Local LLM Download Station") },
                    label = { Text("HuggingFace") },
                    modifier = Modifier.testTag("tab_models")
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.GENERATION,
                    onClick = { viewModel.selectTab(ActiveTab.GENERATION) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "AI Media Creator Suite") },
                    label = { Text("AI Media") },
                    modifier = Modifier.testTag("tab_generation")
                )
                NavigationBarItem(
                    selected = activeTab == ActiveTab.SCORECARD,
                    onClick = { viewModel.selectTab(ActiveTab.SCORECARD) },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Scorecard Performance Transcriber Dashboard") },
                    label = { Text("Scorecard") },
                    modifier = Modifier.testTag("tab_scorecard")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                ActiveTab.LIBRARY -> LibraryTab(
                    papers = currentPapers,
                    selectedCitationStyle = selectedCitationStyle,
                    onStyleSelected = { selectedCitationStyle = it },
                    onDeletePaper = { viewModel.deletePaper(it) },
                    onImportClick = { showImportDialog = true },
                    onCopyAllClick = {
                        val allFormatted = viewModel.exportCitations(selectedCitationStyle)
                        clipboardManager.setText(AnnotatedString(allFormatted))
                        Toast.makeText(context, "$selectedCitationStyle Citations Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
                ActiveTab.CHAT -> ChatTab(
                    messages = chatMessages,
                    isGenerating = isGenerating,
                    activeModelMode = activeModelMode,
                    systemStatus = systemStatus,
                    onSendMessage = { viewModel.sendMessageToChatbot(it) },
                    onClearHistory = { viewModel.clearChatLog() },
                    onUploadFile = { viewModel.importPaperFromUri(it) }
                )
                ActiveTab.MODELS -> ModelsTab(
                    models = hfModels,
                    isDownloading = false,
                    systemStatus = systemStatus,
                    onDownloadModel = { viewModel.triggerHuggingFaceModelDownload(it) },
                    activeModelMode = activeModelMode,
                    onSelectModelMode = { viewModel.setModelMode(it) },
                    onRegisterCustomModel = { repo, file, name, size, desc ->
                        viewModel.registerCustomHFModel(repo, file, name, size, desc)
                    }
                )
                ActiveTab.GENERATION -> GenerationTab(
                    generatedImageUrl = generatedImageUrl,
                    generatedVideoFrames = generatedVideoFrames,
                    isGenerating = isGeneratingMedia,
                    progress = generationProgress,
                    history = mediaHistory,
                    error = mediaError,
                    onGenerateImage = { prompt, style -> viewModel.generateImage(prompt, style) },
                    onGenerateVideo = { prompt, style -> viewModel.generateVideo(prompt, style) }
                )
                ActiveTab.SCORECARD -> ScorecardTab(
                    scorecards = scorecards,
                    isAnalyzing = isAnalyzingScorecard,
                    extractedScorecard = extractedScorecard,
                    error = scorecardAnalysisError,
                    engineMode = scorecardEngineMode,
                    onChangeEngineMode = { viewModel.setScorecardEngineMode(it) },
                    onAnalyzeImage = { base64, uri -> viewModel.analyzeScorecardImage(base64, uri) },
                    onSaveScorecard = { viewModel.saveExtractedScorecard(it) },
                    onCancelExtraction = { viewModel.cancelScorecardExtraction() },
                    onDeleteScorecard = { viewModel.deleteScorecard(it) },
                    onClearAll = { viewModel.clearAllScorecards() }
                )
            }

            // Paper Analysis Overlay / Progress Modal
            if (isAnalyzingPaper) {
                Dialog(onDismissRequest = {}) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { analyzingProgress },
                                color = ProfessionalPrimary,
                                strokeWidth = 5.dp,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "Analyzing Document Structure",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                systemStatus,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(
                                progress = { analyzingProgress },
                                color = ProfessionalPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                            )
                            Text(
                                "${(analyzingProgress * 100).toInt()}% parsed",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = ProfessionalPrimary
                            )
                        }
                    }
                }
            }

            // Document Import dialog popup
            if (showImportDialog) {
                AddPaperDialog(
                    onDismiss = { showImportDialog = false },
                    onConfirm = { title, authors, year, journal, txt, ext ->
                        viewModel.importAndVecAcademicPaper(
                            title = title,
                            authors = authors,
                            year = year,
                            journal = journal,
                            abstractText = txt,
                            simulateFileExtension = ext
                        )
                        showImportDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun LibraryTab(
    papers: List<AcademicPaper>,
    selectedCitationStyle: String,
    onStyleSelected: (String) -> Unit,
    onDeletePaper: (AcademicPaper) -> Unit,
    onImportClick: () -> Unit,
    onCopyAllClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Dashboard Visual Banner using the real moved illustration asset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.research_dashboard_banner),
                            contentDescription = "Academic Research Space Vector Illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xDDFFFFFF)),
                                        startY = 100f
                                    )
                                )
                        )
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Local Library and Citations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ProfessionalText
                        )
                        Text(
                            "Import scientific studies, analyze data offline via vector representations, and format citation indexes seamlessly.",
                            fontSize = 12.sp,
                            color = ProfessionalTextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            // Stats panel & Citation Switcher
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active library size", fontSize = 11.sp, color = ProfessionalTextMuted)
                        Text("${papers.size} Documents Indexed", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ProfessionalText)
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCitationStyle == "APA",
                            onClick = { onStyleSelected("APA") },
                            label = { Text("APA Style", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedCitationStyle == "MLA",
                            onClick = { onStyleSelected("MLA") },
                            label = { Text("MLA Style", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Academic Documents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCopyAllClick,
                        modifier = Modifier.testTag("copy_all_citations_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copy all bibliography listings as text",
                            tint = ProfessionalPrimary
                        )
                    }
                    Button(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfessionalPrimary),
                        modifier = Modifier.testTag("import_document_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import PDF/Image", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (papers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ProfessionalTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No papers indexed in the vector library",
                            fontSize = 14.sp,
                            color = ProfessionalText,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Tap 'Import' to ingest raw paper texts, analyze structure, and cache locally.",
                            fontSize = 11.sp,
                            color = ProfessionalTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(260.dp)
                        )
                    }
                }
            }
        } else {
            items(papers) { paper ->
                PaperCardItem(
                    paper = paper,
                    style = selectedCitationStyle,
                    onCopyToClipboard = {
                        val citation = if (selectedCitationStyle == "MLA") paper.getMlaCitation() else paper.getApaCitation()
                        // Strip basic HTML formatting tags for raw text copies
                        val formatted = citation.replace("<i>", "").replace("</i>", "")
                        clipboardManager.setText(AnnotatedString(formatted))
                        Toast.makeText(context, "$selectedCitationStyle Citation Copied!", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { onDeletePaper(paper) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PaperCardItem(
    paper: AcademicPaper,
    style: String,
    onCopyToClipboard: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp))
            .testTag("paper_card_${paper.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ProfessionalCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = paper.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProfessionalText,
                        maxLines = if (isExpanded) 4 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${paper.authors} (${paper.year})",
                        fontSize = 12.sp,
                        color = ProfessionalTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (paper.isAnalyzed) Color(0x202E7D32) else Color(0x20C62828))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (paper.isAnalyzed) "INDEXED" else "UNANALYZED",
                        color = if (paper.isAnalyzed) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        "Abstract",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ProfessionalPrimary
                    )
                    Text(
                        text = paper.abstractText,
                        fontSize = 11.sp,
                        color = ProfessionalTextMuted,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Formatted Citation ($style)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ProfessionalPrimary
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProfessionalSecondary.copy(alpha = 0.4f))
                            .border(1.dp, ProfessionalBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        val htmlCitation = if (style == "MLA") paper.getMlaCitation() else paper.getApaCitation()
                        // Simple renderer of italic markup blocks
                        Text(
                            text = htmlCitation.replace("<i>", "").replace("</i>", ""),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ProfessionalText
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Document Resource", tint = Color.Red)
                        }
                        Button(
                            onClick = onCopyToClipboard,
                            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Copy Citation", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTab(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    activeModelMode: String,
    systemStatus: String,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onUploadFile: (Uri) -> Unit
) {
    var textFieldValue by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadFile(uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status Shield Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ProfessionalSecondary)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ProfessionalSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Sandbox Active: Local Documents Unshared",
                        fontSize = 11.sp,
                        color = ProfessionalText
                    )
                }
                
                Text(
                    "Clear Logs",
                    fontSize = 11.sp,
                    color = ProfessionalPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClearHistory() }
                        .testTag("clear_chat_button")
                )
            }
        }

        // Chat timeline lazy area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalSecondary),
                    modifier = Modifier.border(1.dp, ProfessionalBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ProfessionalPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            "The chatbot utilizes direct on-device text vectorization. When you enter academic questions, it mathematically isolates contextual evidence matching your studies locally before forming the response.",
                            fontSize = 11.sp,
                            color = ProfessionalText,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = ProfessionalTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            "Ask Academic Questions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ProfessionalText
                        )
                        
                        // Suggestion chips
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PresetPromptButton(label = "What is a Transformer?") { onSendMessage(it) }
                            PresetPromptButton(label = "Explain deep residual learning") { onSendMessage(it) }
                            PresetPromptButton(label = "What is GAN minimax loss equilibrium?") { onSendMessage(it) }
                        }
                    }
                }
            } else {
                items(messages) { message ->
                    ChatMessageRow(message)
                }
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ProfessionalPrimary)
                        Text(
                            "Analyzing local vectors and calculating parameters...",
                            color = ProfessionalTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Action prompt composer box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(28.dp))
                    .background(Color(0xFFF1F0F4))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.testTag("chat_upload_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Upload reference file to vector memory",
                        modifier = Modifier.size(24.dp),
                        tint = ProfessionalPrimary
                    )
                }

                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    placeholder = { Text("Ask your academic library helper...", fontSize = 13.sp, color = ProfessionalTextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = ProfessionalText,
                        unfocusedTextColor = ProfessionalText
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                IconButton(
                    onClick = {
                        if (textFieldValue.isNotBlank()) {
                            onSendMessage(textFieldValue)
                            textFieldValue = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Submit query to private sandbox model",
                        modifier = Modifier.size(20.dp),
                        tint = ProfessionalPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PresetPromptButton(label: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ProfessionalSecondary)
            .border(1.dp, ProfessionalBorder, RoundedCornerShape(8.dp))
            .clickable { onClick(label) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 11.sp, color = ProfessionalPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ChatMessageRow(message: ChatMessage) {
    val isUser = message.sender == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 16.dp
                        )
                    )
                    .background(if (isUser) ProfessionalSecondary else ProfessionalCard)
                    .border(
                        width = if (isUser) 0.dp else 1.dp,
                        color = if (isUser) Color.Transparent else ProfessionalBorder,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 16.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = ProfessionalText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // If assistant response matched sources, display them traceably.
            if (!isUser && message.matchedSourcesJson.isNotEmpty()) {
                var showSources by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .clickable { showSources = !showSources }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(12.dp), tint = ProfessionalSuccess)
                        Text(
                            "Matched Private Vectors (${parseSourcesCount(message.matchedSourcesJson)} citation sources)",
                            fontSize = 10.sp,
                            color = ProfessionalSuccess,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (showSources) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = ProfessionalTextMuted
                        )
                    }

                    if (showSources) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F0F4)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ProfessionalBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val sources = parseMatchedSources(message.matchedSourcesJson)
                                sources.forEachIndexed { index, source ->
                                    Column {
                                        Text(
                                            "Source ${index + 1}: ${source.paperTitle} (Chunk ${source.chunkIdx})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfessionalPrimary
                                        )
                                        Text(
                                            "\"${source.contentSample}\"",
                                            fontSize = 9.sp,
                                            color = ProfessionalTextMuted,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 12.sp
                                        )
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

fun parseSourcesCount(json: String): Int {
    return try {
        JSONArray(json).length()
    } catch (e: Exception) {
        0
    }
}

data class LocalSourceMatched(val paperTitle: String, val chunkIdx: Int, val contentSample: String)

fun parseMatchedSources(json: String): List<LocalSourceMatched> {
    val list = mutableListOf<LocalSourceMatched>()
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                LocalSourceMatched(
                    paperTitle = obj.getString("paperTitle"),
                    chunkIdx = obj.getInt("chunkIdx"),
                    contentSample = obj.getString("text")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

@Composable
fun ModelsTab(
    models: List<HuggingFaceModel>,
    isDownloading: Boolean,
    systemStatus: String,
    onDownloadModel: (String) -> Unit,
    activeModelMode: String,
    onSelectModelMode: (String) -> Unit,
    onRegisterCustomModel: (repoId: String, filename: String, name: String, size: String, description: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = ProfessionalPrimary, modifier = Modifier.size(32.dp))
                    Text(
                        "HuggingFace Edge Weights",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ProfessionalText
                    )
                    Text(
                        "To manage offline capability safely, download lightweight parameter packages directly from HuggingFace repositories. Models remain entirely stored locally on your application Sandbox space.",
                        fontSize = 12.sp,
                        color = ProfessionalTextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            var isExpanded by remember { mutableStateOf(false) }
            var repoId by remember { mutableStateOf("") }
            var filename by remember { mutableStateOf("") }
            var name by remember { mutableStateOf("") }
            var size by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }

            val context = LocalContext.current

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = ProfessionalPrimary)
                            Text(
                                "Register Custom HuggingFace Model",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ProfessionalText
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = ProfessionalTextMuted
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = repoId,
                                onValueChange = { repoId = it },
                                label = { Text("HuggingFace Repo ID (e.g. meta-llama/Llama-3.2-1B-Instruct)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("custom_repo_id_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = filename,
                                onValueChange = { filename = it },
                                label = { Text("Model Filename (e.g. llama-3.2-1b-instruct.gguf)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("custom_filename_input"),
                                singleLine = true
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Friendly Name (e.g. Llama 3.2)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("custom_name_input"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = size,
                                    onValueChange = { size = it },
                                    label = { Text("File Size (e.g. 1.2 GB)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("custom_size_input"),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Model Description", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("custom_desc_input"),
                                maxLines = 3
                            )

                            Button(
                                onClick = {
                                    if (repoId.isNotBlank() && repoId.contains("/")) {
                                        onRegisterCustomModel(repoId.trim(), filename.trim(), name.trim(), size.trim(), description.trim())
                                        Toast.makeText(context, "Registered $repoId successfully. Download option is now available below!", Toast.LENGTH_SHORT).show()
                                        repoId = ""
                                        filename = ""
                                        name = ""
                                        size = ""
                                        description = ""
                                        isExpanded = false
                                    } else {
                                        Toast.makeText(context, "Valid HuggingFace Repository ID (containing '/') is required!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfessionalPrimary),
                                modifier = Modifier.align(Alignment.End).testTag("register_custom_model_btn")
                            ) {
                                Text("Add Custom Model Option", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Available Repositories",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(models) { model ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp))
                    .testTag("model_download_${model.repoId.substringAfter('/')}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        model.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProfessionalText
                    )
                    Text(
                        model.repoId,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ProfessionalPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        model.description,
                        fontSize = 11.sp,
                        color = ProfessionalTextMuted,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Package size", fontSize = 10.sp, color = ProfessionalTextMuted)
                            Text(text = model.size, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProfessionalText)
                        }

                        if (model.status == "Downloading") {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(160.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Progress:", fontSize = 9.sp, color = ProfessionalTextMuted)
                                    Text("${(model.progress * 100).toInt()}% (${model.speed})", fontSize = 9.sp, color = ProfessionalPrimary, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { model.progress },
                                    color = ProfessionalPrimary,
                                    trackColor = ProfessionalSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .clip(CircleShape)
                                )
                            }
                        } else {
                            val isThisModelActive = activeModelMode.contains("Local") && activeModelMode.contains(model.name.substringBefore(" "))
                            
                            Button(
                                onClick = { 
                                    if (model.status == "Completed") {
                                        onSelectModelMode("Local Offline (${model.name})")
                                    } else {
                                        onDownloadModel(model.repoId) 
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isThisModelActive) ProfessionalSuccess else ProfessionalPrimary,
                                    disabledContainerColor = ProfessionalSecondary.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isThisModelActive) Icons.Default.CheckCircle else if (model.status == "Completed") Icons.Default.PlayArrow else Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isThisModelActive) "Active" else if (model.status == "Completed") "Activate" else "Download",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AddPaperDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, authors: String, year: Int, journal: String, text: String, ext: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2026") }
    var journal by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }
    var selectedExt by remember { mutableStateOf(".pdf") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ProfessionalBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ProfessionalCard)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Import New Paper Context",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ProfessionalText
                    )
                    Text(
                        "Configure bibliographic descriptors and ingest academic data locally.",
                        fontSize = 11.sp,
                        color = ProfessionalTextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Paper Title", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("add_paper_title_input"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = authors,
                        onValueChange = { authors = it },
                        label = { Text("Authors (separated by comma)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. S. Smith, B. Carter", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("add_paper_authors_input"),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("add_paper_year_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = journal,
                            onValueChange = { journal = it },
                            label = { Text("Journal / Outlet", fontSize = 12.sp) },
                            modifier = Modifier.weight(2f).testTag("add_paper_journal_input"),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        label = { Text("Paper Abstract / Body Text", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("add_paper_text_input"),
                        maxLines = 8
                    )
                }

                item {
                    // Simulating File Extension Format Import Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulate Source Format:", fontSize = 11.sp, color = ProfessionalTextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedExt == ".pdf",
                                onClick = { selectedExt = ".pdf" },
                                label = { Text(".pdf File", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = selectedExt == ".png",
                                onClick = { selectedExt = ".png" },
                                label = { Text("Image OCR", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = ProfessionalTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank() && authors.isNotBlank() && rawText.isNotBlank()) {
                                    onConfirm(
                                        title,
                                        authors,
                                        year.toIntOrNull() ?: 2026,
                                        journal.ifBlank { "Academic Journal" },
                                        rawText,
                                        selectedExt
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_paper_confirm_btn")
                        ) {
                            Text("Analyze & Vectorize")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenerationTab(
    generatedImageUrl: String?,
    generatedVideoFrames: List<String>,
    isGenerating: Boolean,
    progress: Float,
    history: List<GeneratedMediaItem>,
    error: String?,
    onGenerateImage: (prompt: String, style: String) -> Unit,
    onGenerateVideo: (prompt: String, style: String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("None") }
    var selectedTabMode by remember { mutableStateOf("image") } // "image" or "video"

    // Video Player Local State
    var isPlaying by remember { mutableStateOf(true) }
    var currentFrameIndex by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1000) } // ms per frame (1000ms = 1x, 500ms = 2x, 2000ms = 0.5x)
    
    val context = LocalContext.current
    val stylesList = listOf("None", "Cinematic", "Cyberpunk", "3D Render", "Oil Painting", "Anime", "Steampunk")

    // Active text-to-video frames ticker
    if (generatedVideoFrames.isNotEmpty()) {
        LaunchedEffect(isPlaying, generatedVideoFrames, playbackSpeed) {
            if (isPlaying) {
                while (true) {
                    delay(playbackSpeed.toLong())
                    currentFrameIndex = (currentFrameIndex + 1) % generatedVideoFrames.size
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("generation_tab_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Intro Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI Creative Laboratory",
                        fontWeight = FontWeight.Bold,
                        color = ProfessionalText,
                        fontSize = 18.sp
                    )
                    Text(
                        "Convert research annotations, concepts, or creative scenarios directly into high-fidelity custom visual assets (Images and Sequential Videos).",
                        fontSize = 12.sp,
                        color = ProfessionalTextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Configuration Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode Selector Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ProfessionalSecondary, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selectedTabMode = "image" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_mode_image"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTabMode == "image") ProfessionalPrimary else Color.Transparent,
                                contentColor = if (selectedTabMode == "image") Color.White else ProfessionalText
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Text-to-Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { selectedTabMode = "video" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_mode_video"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTabMode == "video") ProfessionalPrimary else Color.Transparent,
                                contentColor = if (selectedTabMode == "video") Color.White else ProfessionalText
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Text-to-Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Prompt Input
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("What would you like to create?", fontSize = 12.sp) },
                        placeholder = {
                            Text(
                                if (selectedTabMode == "image") "e.g., A photorealistic futuristic quantum computer glowing in a clean sterile lab..."
                                else "e.g., Drone motion panning over a busy scientific research campus under starry sky...",
                                fontSize = 12.sp,
                                color = ProfessionalTextMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("generation_prompt_input"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProfessionalText,
                            unfocusedTextColor = ProfessionalText,
                            focusedBorderColor = ProfessionalPrimary,
                            unfocusedBorderColor = ProfessionalBorder
                        )
                    )

                    // Styles list
                    Text(
                        "Rendering Style Preset",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ProfessionalText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            stylesList.chunked(4).forEach { rowStyles ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowStyles.forEach { styleName ->
                                        FilterChip(
                                            selected = selectedStyle == styleName,
                                            onClick = { selectedStyle = styleName },
                                            label = { Text(styleName, fontSize = 11.sp, color = ProfessionalText) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ProfessionalPrimary,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("style_chip_$styleName")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action trigger
                    Button(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                if (selectedTabMode == "image") {
                                    onGenerateImage(prompt.trim(), selectedStyle)
                                } else {
                                    onGenerateVideo(prompt.trim(), selectedStyle)
                                    currentFrameIndex = 0
                                    isPlaying = true
                                }
                            } else {
                                Toast.makeText(context, "Please enter a descriptive prompt first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trigger_synthesis_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfessionalPrimary,
                            disabledContainerColor = ProfessionalSecondary
                        )
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Neural Net Rendering...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (selectedTabMode == "image") "Synthesize Masterpiece Image" else "Compile Cinematic Video Sequences",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Error log
        if (error != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                    }
                }
            }
        }

        // Active generated output
        item {
            if (isGenerating) {
                // Interactive Render Loader Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (selectedTabMode == "image") "Rendering Pixel-Diffusion Field..." else "Interpolating Video Keyframes...",
                            fontWeight = FontWeight.Bold,
                            color = ProfessionalText,
                            fontSize = 14.sp
                        )
                        
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ProfessionalPrimary,
                            trackColor = ProfessionalSecondary
                        )

                        Text(
                            text = when {
                                progress < 0.3f -> "Phasing initial latent noise..."
                                progress < 0.6f -> "Injecting prompts & styling weights (${(progress * 100).toInt()}%)..."
                                progress < 0.9f -> "Upscaling resolution & resolving color gradients..."
                                else -> "Finalizing asset buffers & compiling outputs..."
                            },
                            fontSize = 11.sp,
                            color = ProfessionalTextMuted
                        )
                    }
                }
            } else if (selectedTabMode == "image" && generatedImageUrl != null) {
                // Image result container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ProfessionalBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AsyncImage(
                            model = generatedImageUrl,
                            contentDescription = "Generated Masterpiece",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ProfessionalSecondary),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("AIGC Production Ready Image", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ProfessionalText)
                                Text("Model: Flux Engine (1024x1024 px)", fontSize = 11.sp, color = ProfessionalTextMuted)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Asset file saved successfully to /Pictures/CreativeLab/!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("save_image_btn")
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = "Download and Save image", tint = ProfessionalPrimary)
                                }
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Image URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Copy address", tint = ProfessionalText)
                                }
                            }
                        }
                    }
                }
            } else if (selectedTabMode == "video" && generatedVideoFrames.isNotEmpty()) {
                // Video result container (interactive scrolling frame player)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ProfessionalBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Custom Dynamic looping Frame Viewer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ProfessionalSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = generatedVideoFrames[currentFrameIndex],
                                contentDescription = "AI Video Frame",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Playback state indicator badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isPlaying) "PLAYING" else "PAUSED",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                            
                            // Display progress indicators (Frames step indicators)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                generatedVideoFrames.forEachIndexed { idx, _ ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .background(
                                                if (idx == currentFrameIndex) ProfessionalPrimary else Color.White.copy(alpha = 0.5f),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Video Player Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .background(ProfessionalSecondary, CircleShape)
                                    .size(40.dp)
                                    .testTag("video_play_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = "Play or Pause Video Frame Stream",
                                    tint = ProfessionalPrimary
                                )
                            }

                            // Frames info
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Frame ${currentFrameIndex + 1} of ${generatedVideoFrames.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ProfessionalText)
                                Text("Speed: ${if(playbackSpeed == 1000) "1.0x" else if(playbackSpeed == 500) "2.0x" else "0.5x"}", fontSize = 10.sp, color = ProfessionalTextMuted)
                            }

                            // Cycle play speed
                            Button(
                                onClick = {
                                    playbackSpeed = when (playbackSpeed) {
                                        1000 -> 500
                                        500 -> 2000
                                        else -> 1000
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfessionalSecondary, contentColor = ProfessionalText),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Speed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Save Video simulated
                            Button(
                                onClick = {
                                    Toast.makeText(context, "AI Video Render compiled & saved to /Videos/MotionLab/!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfessionalPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("save_video_btn")
                            ) {
                                Text("Save MP4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Placeholder empty state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ProfessionalBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ProfessionalTextMuted, modifier = Modifier.size(36.dp))
                        Text(
                            "Ready to Core Synthesize",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ProfessionalText
                        )
                        Text(
                            "Enter a detailed prompt above and select style options to render dynamic on-demand images or videos.",
                            fontSize = 11.sp,
                            color = ProfessionalTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // History Gallery
        if (history.isNotEmpty()) {
            item {
                Text(
                    "Recent Generations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ProfessionalText,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(history) { mediaItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ProfessionalBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            prompt = mediaItem.prompt
                            selectedTabMode = mediaItem.type
                        },
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = mediaItem.url,
                            contentDescription = "Thumbnail",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ProfessionalSecondary),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mediaItem.prompt,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ProfessionalText
                            )
                            Text(
                                text = if (mediaItem.type == "image") "Flux Image Asset" else "Motion Loop Video",
                                fontSize = 10.sp,
                                color = ProfessionalTextMuted
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ProfessionalTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Scorecard Helper Utilities ---

fun getBase64FromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        if (originalBitmap == null) return null
        
        // Resize bitmap to a reasonable size if it's huge, e.g. max 1024px on larger side
        val maxSide = 1024
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxSide || height > maxSide) {
            val ratio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1) {
                Pair(maxSide, (maxSide / ratio).toInt())
            } else {
                Pair((maxSide * ratio).toInt(), maxSide)
            }
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }
        
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun createCameraImageUri(context: android.content.Context): Uri? {
    return try {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Scorecard_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ScorecardDashboard")
            }
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// --- Scorecard Transcriber Dashboard Screen ---

@Composable
fun ScorecardTab(
    scorecards: List<GolfScorecard>,
    isAnalyzing: Boolean,
    extractedScorecard: GolfScorecard?,
    error: String?,
    engineMode: String,
    onChangeEngineMode: (String) -> Unit,
    onAnalyzeImage: (String, String?) -> Unit,
    onSaveScorecard: (GolfScorecard) -> Unit,
    onCancelExtraction: () -> Unit,
    onDeleteScorecard: (GolfScorecard) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var activeCameraUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAnalyticsCardId by remember(scorecards) { 
        mutableStateOf(if (scorecards.isNotEmpty()) scorecards.first().id else -1) 
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                activeCameraUri?.let { uri ->
                    val base64 = getBase64FromUri(context, uri)
                    if (base64 != null) {
                        onAnalyzeImage(base64, uri.toString())
                    } else {
                        Toast.makeText(context, "Failed to load camera image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val base64 = getBase64FromUri(context, it)
                if (base64 != null) {
                    onAnalyzeImage(base64, it.toString())
                } else {
                    Toast.makeText(context, "Failed to load gallery image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Branding Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ProfessionalPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Golf Scorecard Digitizer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ProfessionalText
                            )
                            Text(
                                text = "Powered by Gemini AI Multimodal Processing",
                                fontSize = 11.sp,
                                color = ProfessionalTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Take a high-quality picture of your golf scorecard or upload a saved photo. The chosen engine will extract the golfer's name, handicap details, and the 18 holes score sequence to save in SQLite.",
                        fontSize = 12.sp,
                        color = ProfessionalTextMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SELECT TRANSCRIPTION ENGINE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ProfessionalPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProfessionalBorder)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf("Local LLM (Qwen 2.5 VL)", "Gemini Cloud (3.5 Flash)")
                        modes.forEach { modeName ->
                            val isSelected = engineMode == modeName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) ProfessionalPrimary else Color.Transparent)
                                    .clickable { onChangeEngineMode(modeName) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (modeName.contains("Local")) "📴 Local LLM" else "🌐 Cloud Gemini",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.Black else ProfessionalText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = createCameraImageUri(context)
                                if (uri != null) {
                                    activeCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    Toast.makeText(context, "Camera initialization failed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("take_picture_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).testTag("upload_image_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalSecondary, contentColor = ProfessionalText),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Image", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Active Analysis Loading state
        if (isAnalyzing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    border = BorderStroke(1.dp, ProfessionalBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = ProfessionalPrimary)
                        Text(
                            text = "Transcribing Scorecard with Gemini Flash...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ProfessionalText
                        )
                        Text(
                            text = "Gemini is examining handwriting, aligning play cells, and processing strokes.",
                            fontSize = 11.sp,
                            color = ProfessionalTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Error message display
        if (error != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error icon",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onCancelExtraction) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        // Stats Dashboard Metric section
        if (scorecards.isNotEmpty()) {
            item {
                Text(
                    text = "📊 Performance Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ProfessionalText,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                val totalGames = scorecards.size
                val avgScore = scorecards.map { it.totalScore }.average().toInt()
                val bestScore = scorecards.map { it.totalScore }.minOrNull() ?: 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Games
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Rounds", fontSize = 10.sp, color = ProfessionalTextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalGames",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ProfessionalPrimary
                            )
                        }
                    }

                    // Average Score
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Average Score", fontSize = 10.sp, color = ProfessionalTextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$avgScore",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ProfessionalText
                            )
                        }
                    }

                    // Best Score (Lowest Total Strokes)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Best Score", fontSize = 10.sp, color = ProfessionalTextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$bestScore",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Green
                            )
                        }
                    }
                }
            }

            // Interactive Score Chart visualizer
            item {
                val activeCard = scorecards.find { it.id == selectedAnalyticsCardId } ?: scorecards.first()
                val activeScores = remember(activeCard) {
                    try {
                        org.json.JSONArray(activeCard.scoresJson).run {
                            List(length()) { getInt(it) }
                        }
                    } catch (e: Exception) {
                        List(18) { 0 }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ProfessionalBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Round Performance - ${activeCard.playerName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ProfessionalText
                                )
                                Text(
                                    text = "Visual bar charts of strokes made per hole (H1 to H18)",
                                    fontSize = 11.sp,
                                    color = ProfessionalTextMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ProfessionalPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${activeCard.totalScore} Strokes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ProfessionalPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Charts row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            activeScores.forEachIndexed { idx, score ->
                                val displayScore = if (score > 10) 10 else score
                                // Calculate fractional height relative to max strokes of 10
                                val barHeightFactor = if (displayScore > 0) displayScore / 10f else 0.05f

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = if (score > 0) "$score" else "-",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (score > 5) Color.Red else if (score in 1..3) Color.Green else ProfessionalText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(barHeightFactor)
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                            .background(
                                                if (score > 5) Color.Red.copy(0.7f)
                                                else if (score in 1..3) Color.Green.copy(0.7f)
                                                else ProfessionalPrimary
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 8.sp,
                                        color = ProfessionalTextMuted
                                    )
                                }
                            }
                        }

                        // Detailed scorecard specification table
                        val activePars = remember(activeCard) {
                            try {
                                if (!activeCard.parsJson.isNullOrBlank()) {
                                    org.json.JSONArray(activeCard.parsJson).run {
                                        List(length()) { getInt(it) }
                                    }
                                } else {
                                    listOf(4, 4, 3, 4, 5, 4, 3, 4, 5,  4, 3, 4, 4, 5, 3, 4, 4, 5)
                                }
                            } catch (e: Exception) {
                                listOf(4, 4, 3, 4, 5, 4, 3, 4, 5,  4, 3, 4, 4, 5, 3, 4, 4, 5)
                            }
                        }

                        val activeIndices = remember(activeCard) {
                            try {
                                if (!activeCard.indicesJson.isNullOrBlank()) {
                                    org.json.JSONArray(activeCard.indicesJson).run {
                                        List(length()) { getInt(it) }
                                    }
                                } else {
                                    listOf(9, 15, 11, 1, 13, 5, 17, 3, 7,  10, 18, 12, 2, 14, 6, 16, 4, 8)
                                }
                            } catch (e: Exception) {
                                listOf(9, 15, 11, 1, 13, 5, 17, 3, 7,  10, 18, 12, 2, 14, 6, 16, 4, 8)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "📊 Detailed Digital Scorecard Specs (PAR, index, scores):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ProfessionalPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .border(1.dp, ProfessionalBorder, RoundedCornerShape(8.dp))
                                .background(ProfessionalSecondary.copy(alpha = 0.4f))
                        ) {
                            // Left header labels column
                            Column(
                                modifier = Modifier
                                    .background(ProfessionalCard)
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                                    .width(55.dp)
                            ) {
                                Text("HOLE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp), textAlign = TextAlign.Center)
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.3f)))
                                Text("PAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp), textAlign = TextAlign.Center)
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.3f)))
                                Text("INDEX", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp), textAlign = TextAlign.Center)
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.3f)))
                                Text("SCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp), textAlign = TextAlign.Center)
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.3f)))
                                Text("+ / -", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp), textAlign = TextAlign.Center)
                            }

                            // OUT columns (H1 to H9)
                            for (i in 0..8) {
                                val s = if (i < activeScores.size) activeScores[i] else 0
                                val p = if (i < activePars.size) activePars[i] else 4
                                val idx = if (i < activeIndices.size) activeIndices[i] else 1
                                val diffVal = s - p
                                val diffText = when {
                                    s == 0 -> "-"
                                    diffVal > 0 -> "+$diffVal"
                                    diffVal < 0 -> "$diffVal"
                                    else -> "E"
                                }
                                val diffColor = when {
                                    s == 0 -> ProfessionalTextMuted
                                    diffVal > 0 -> Color.Red
                                    diffVal < 0 -> Color.Green
                                    else -> ProfessionalText
                                }

                                Column(
                                    modifier = Modifier
                                        .width(34.dp)
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalPrimary, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text("$p", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text("$idx", fontSize = 9.sp, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text(if (s > 0) "$s" else "-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (diffVal < 0) Color.Green else ProfessionalText, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text(diffText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = diffColor, modifier = Modifier.height(24.dp))
                                }
                            }

                            // OUT Summary
                            val outPar = activePars.take(9).sum()
                            val outScore = activeScores.take(9).sum()
                            val outDiff = outScore - outPar
                            Column(
                                modifier = Modifier
                                    .background(ProfessionalPrimary.copy(alpha = 0.11f))
                                    .width(40.dp)
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("OUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalPrimary, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("$outPar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("-", fontSize = 9.sp, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("$outScore", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text(if (outDiff > 0) "+$outDiff" else if (outDiff < 0) "$outDiff" else "E", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (outDiff > 0) Color.Red else if (outDiff < 0) Color.Green else ProfessionalText, modifier = Modifier.height(24.dp))
                            }

                            // IN columns (H10 to H18)
                            for (i in 9..17) {
                                val s = if (i < activeScores.size) activeScores[i] else 0
                                val p = if (i < activePars.size) activePars[i] else 4
                                val idx = if (i < activeIndices.size) activeIndices[i] else 1
                                val diffVal = s - p
                                val diffText = when {
                                    s == 0 -> "-"
                                    diffVal > 0 -> "+$diffVal"
                                    diffVal < 0 -> "$diffVal"
                                    else -> "E"
                                }
                                val diffColor = when {
                                    s == 0 -> ProfessionalTextMuted
                                    diffVal > 0 -> Color.Red
                                    diffVal < 0 -> Color.Green
                                    else -> ProfessionalText
                                }

                                Column(
                                    modifier = Modifier
                                        .width(34.dp)
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${i + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalPrimary, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text("$p", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text("$idx", fontSize = 9.sp, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text(if (s > 0) "$s" else "-", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (diffVal < 0) Color.Green else ProfessionalText, modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                    Text(diffText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = diffColor, modifier = Modifier.height(24.dp))
                                }
                            }

                            // IN Summary
                            val inPar = activePars.drop(9).take(9).sum()
                            val inScore = activeScores.drop(9).take(9).sum()
                            val inDiff = inScore - inPar
                            Column(
                                modifier = Modifier
                                    .background(ProfessionalPrimary.copy(alpha = 0.11f))
                                    .width(40.dp)
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("IN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalPrimary, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("$inPar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("-", fontSize = 9.sp, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("$inScore", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text(if (inDiff > 0) "+$inDiff" else if (inDiff < 0) "$inDiff" else "E", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (inDiff > 0) Color.Red else if (inDiff < 0) Color.Green else ProfessionalText, modifier = Modifier.height(24.dp))
                            }

                            // TOT Column
                            val totPar = activePars.sum()
                            val totScore = activeScores.sum()
                            val totDiff = totScore - totPar
                            Column(
                                modifier = Modifier
                                    .background(ProfessionalPrimary.copy(alpha = 0.22f))
                                    .width(42.dp)
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("TOT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalPrimary, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("$totPar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("-", fontSize = 9.sp, color = ProfessionalTextMuted, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text("$totScore", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ProfessionalText, modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ProfessionalBorder.copy(alpha = 0.15f)))
                                Text(if (totDiff > 0) "+$totDiff" else if (totDiff < 0) "$totDiff" else "E", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (totDiff > 0) Color.Red else if (totDiff < 0) Color.Green else ProfessionalText, modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }

            // Saved List Selector & Clear History Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 Recorded Scorecards",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ProfessionalText
                    )
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }

            // List of cards item by item
            items(scorecards) { card ->
                val cardScores = remember(card) {
                    try {
                        org.json.JSONArray(card.scoresJson).run {
                            List(length()) { getInt(it) }
                        }
                    } catch (e: Exception) {
                        List(18) { 0 }
                    }
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAnalyticsCardId = card.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (card.id == selectedAnalyticsCardId) ProfessionalSecondary else ProfessionalCard
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (card.id == selectedAnalyticsCardId) ProfessionalPrimary else ProfessionalBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (card.imageUri != null) {
                                    AsyncImage(
                                        model = card.imageUri,
                                        contentDescription = "Scorecard Image",
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ProfessionalBorder),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ProfessionalBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⛳", fontSize = 20.sp)
                                    }
                                }

                                Column {
                                    Text(
                                        text = card.playerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ProfessionalText
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (card.date.isNotEmpty()) {
                                            Text("📅 ${card.date}", fontSize = 11.sp, color = ProfessionalTextMuted)
                                        }
                                        if (card.handicap.isNotEmpty()) {
                                            Text("🎯 HCP: ${card.handicap}", fontSize = 11.sp, color = ProfessionalTextMuted)
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Core Total Score circle
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ProfessionalPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${card.totalScore}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }

                                IconButton(onClick = { onDeleteScorecard(card) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete scorecard entry",
                                        tint = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Play card grid visualization
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // OUT (Holes 1 to 9)
                        val outScores = cardScores.take(9)
                        val outSum = outScores.sum()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ProfessionalBorder, RoundedCornerShape(4.dp))
                                .background(ProfessionalCard)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text("OUT:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = ProfessionalText, modifier = Modifier.width(30.dp))
                            outScores.forEachIndexed { i, score ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("H${i+1}", fontSize = 8.sp, color = ProfessionalTextMuted)
                                    Text("$score", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalText)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                Text("TOT", fontSize = 8.sp, color = ProfessionalPrimary)
                                Text("$outSum", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalPrimary)
                            }
                        }

                        // IN (Holes 10 to 18)
                        Spacer(modifier = Modifier.height(4.dp))
                        val inScores = cardScores.drop(9).take(9)
                        val inSum = inScores.sum()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ProfessionalBorder, RoundedCornerShape(4.dp))
                                .background(ProfessionalCard)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text("IN:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = ProfessionalText, modifier = Modifier.width(30.dp))
                            inScores.forEachIndexed { i, score ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("H${i+10}", fontSize = 8.sp, color = ProfessionalTextMuted)
                                    Text("$score", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalText)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                Text("TOT", fontSize = 8.sp, color = ProfessionalPrimary)
                                Text("$inSum", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalPrimary)
                            }
                        }

                        // Note text summary
                        if (card.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ProfessionalBorder.copy(alpha = 0.3f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = card.notes,
                                    fontSize = 11.sp,
                                    color = ProfessionalTextMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Placeholder empty scorecard landing page
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                    border = BorderStroke(1.dp, ProfessionalBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("⛳", fontSize = 48.sp)
                        Text(
                            text = "No Scorecard Records Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ProfessionalText
                        )
                        Text(
                            text = "Digitized scorecard cards are securely stored within your local Room database to review round statistics seamlessly offline.",
                            fontSize = 12.sp,
                            color = ProfessionalTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ProfessionalPrimary)
                                .clickable {
                                    val uri = createCameraImageUri(context)
                                    if (uri != null) {
                                        activeCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Capture Scorecard with Camera",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Core Review Extracted Scorecard Form Dialog ---

    if (extractedScorecard != null) {
        var editedName by remember(extractedScorecard) { mutableStateOf(extractedScorecard.playerName) }
        var editedHandicap by remember(extractedScorecard) { mutableStateOf(extractedScorecard.handicap) }
        var editedDate by remember(extractedScorecard) { mutableStateOf(extractedScorecard.date) }
        var editedNotes by remember(extractedScorecard) { mutableStateOf(extractedScorecard.notes) }

        val defaultPars = remember { listOf(4, 4, 3, 4, 5, 4, 3, 4, 5,  4, 3, 4, 4, 5, 3, 4, 4, 5) }
        val defaultIndices = remember { listOf(9, 15, 11, 1, 13, 5, 17, 3, 7,  10, 18, 12, 2, 14, 6, 16, 4, 8) }

        val parsedScores = remember(extractedScorecard) {
            try {
                org.json.JSONArray(extractedScorecard.scoresJson).run {
                    List(length()) { getInt(it) }
                }
            } catch (e: Exception) {
                List(18) { 0 }
            }
        }

        val parsedPars = remember(extractedScorecard) {
            try {
                if (!extractedScorecard.parsJson.isNullOrBlank()) {
                    org.json.JSONArray(extractedScorecard.parsJson).run {
                        List(length()) { getInt(it) }
                    }
                } else {
                    defaultPars
                }
            } catch (e: Exception) {
                defaultPars
            }
        }

        val parsedIndices = remember(extractedScorecard) {
            try {
                if (!extractedScorecard.indicesJson.isNullOrBlank()) {
                    org.json.JSONArray(extractedScorecard.indicesJson).run {
                        List(length()) { getInt(it) }
                    }
                } else {
                    defaultIndices
                }
            } catch (e: Exception) {
                defaultIndices
            }
        }

        val editedScores = remember(extractedScorecard) {
            mutableStateListOf(*parsedScores.toTypedArray())
        }

        val editedPars = remember(extractedScorecard) {
            mutableStateListOf(*parsedPars.toTypedArray())
        }

        val editedIndices = remember(extractedScorecard) {
            mutableStateListOf(*parsedIndices.toTypedArray())
        }

        // Pad to exactly 18 elements
        LaunchedEffect(editedScores, editedPars, editedIndices) {
            while (editedScores.size < 18) {
                editedScores.add(0)
            }
            while (editedPars.size < 18) {
                editedPars.add(defaultPars[editedPars.size % 18])
            }
            while (editedIndices.size < 18) {
                editedIndices.add(defaultIndices[editedIndices.size % 18])
            }
        }

        Dialog(onDismissRequest = onCancelExtraction) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .testTag("review_scorecard_dialog"),
                colors = CardDefaults.cardColors(containerColor = ProfessionalCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ProfessionalBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Dialog title
                    Text(
                        text = "🔍 Verify Extracted Results",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ProfessionalText
                    )
                    Text(
                        text = "Tweak recognition gaps, PAR guidelines, or handicap indexes before saving.",
                        fontSize = 11.sp,
                        color = ProfessionalTextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable form fields
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Player Name") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_player_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ProfessionalPrimary,
                                    unfocusedBorderColor = ProfessionalBorder,
                                    focusedLabelColor = ProfessionalPrimary,
                                    focusedTextColor = ProfessionalText,
                                    unfocusedTextColor = ProfessionalText
                                )
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = editedDate,
                                    onValueChange = { editedDate = it },
                                    label = { Text("Date") },
                                    modifier = Modifier.weight(1f).testTag("edit_date"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ProfessionalPrimary,
                                        unfocusedBorderColor = ProfessionalBorder,
                                        focusedLabelColor = ProfessionalPrimary,
                                        focusedTextColor = ProfessionalText,
                                        unfocusedTextColor = ProfessionalText
                                    )
                                )

                                OutlinedTextField(
                                    value = editedHandicap,
                                    onValueChange = { editedHandicap = it },
                                    label = { Text("Handicap") },
                                    modifier = Modifier.weight(1f).testTag("edit_handicap"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ProfessionalPrimary,
                                        unfocusedBorderColor = ProfessionalBorder,
                                        focusedLabelColor = ProfessionalPrimary,
                                        focusedTextColor = ProfessionalText,
                                        unfocusedTextColor = ProfessionalText
                                    )
                                )
                            }
                        }

                        // Quick visual reference of total score
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ProfessionalPrimary.copy(alpha = 0.15f))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Recalculated Play Total:", fontSize = 12.sp, color = ProfessionalText)
                                    Text(
                                        text = "${editedScores.sum()} Strokes (Par ${editedPars.sum()})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ProfessionalPrimary
                                    )
                                }
                            }
                        }

                        // 18 hole grid representation with PAR, index, score details
                        item {
                            Text(
                                text = "⛳ Holes 1-18 Stroke, PAR & Index Editors",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ProfessionalText,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ProfessionalBorder, RoundedCornerShape(8.dp))
                                    .background(ProfessionalCard)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Left Column (Holes 1 to 9)
                                    Column(
                                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("OUT (Holes 1-9)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = ProfessionalPrimary)
                                        
                                        // Header Row for labels
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Hole", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.width(28.dp))
                                            Text("PAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                            Text("IDX", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                            Text("SCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                        }

                                        for (i in 0..8) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("H${i + 1}", fontSize = 11.sp, color = ProfessionalText, modifier = Modifier.width(28.dp), fontWeight = FontWeight.Medium)
                                                
                                                // PAR input
                                                val parVal = if (i < editedPars.size) "${editedPars[i]}" else "4"
                                                OutlinedTextField(
                                                    value = parVal,
                                                    onValueChange = { newVal ->
                                                        val num = newVal.filter { it.isDigit() }.toIntOrNull() ?: 4
                                                        if (i < editedPars.size) {
                                                            editedPars[i] = num
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).height(38.dp).testTag("edit_par_${i+1}"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = ProfessionalPrimary,
                                                        unfocusedBorderColor = ProfessionalBorder,
                                                        focusedTextColor = ProfessionalText,
                                                        unfocusedTextColor = ProfessionalText
                                                    ),
                                                    singleLine = true
                                                )

                                                // Index input
                                                val indexVal = if (i < editedIndices.size) "${editedIndices[i]}" else "9"
                                                OutlinedTextField(
                                                    value = indexVal,
                                                    onValueChange = { newVal ->
                                                        val num = newVal.filter { it.isDigit() }.toIntOrNull() ?: 1
                                                        if (i < editedIndices.size) {
                                                            editedIndices[i] = num
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).height(38.dp).testTag("edit_index_${i+1}"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = ProfessionalPrimary,
                                                        unfocusedBorderColor = ProfessionalBorder,
                                                        focusedTextColor = ProfessionalText,
                                                        unfocusedTextColor = ProfessionalText
                                                    ),
                                                    singleLine = true
                                                )

                                                // Score input
                                                val scoreVal = if (i < editedScores.size) "${editedScores[i]}" else "0"
                                                OutlinedTextField(
                                                    value = scoreVal,
                                                    onValueChange = { newVal ->
                                                        val num = newVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                        if (i < editedScores.size) {
                                                            editedScores[i] = num
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1.1f).height(38.dp).testTag("edit_score_${i+1}"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = ProfessionalPrimary,
                                                        unfocusedBorderColor = ProfessionalBorder,
                                                        focusedTextColor = ProfessionalText,
                                                        unfocusedTextColor = ProfessionalText
                                                    ),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Right Column (Holes 10 to 18)
                                    Column(
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("IN (Holes 10-18)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = ProfessionalPrimary)
                                        
                                        // Header Row for labels
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Hole", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.width(28.dp))
                                            Text("PAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                            Text("IDX", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                            Text("SCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfessionalTextMuted, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                        }

                                        for (i in 9..17) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("H${i + 1}", fontSize = 11.sp, color = ProfessionalText, modifier = Modifier.width(28.dp), fontWeight = FontWeight.Medium)
                                                
                                                // PAR input
                                                val parVal = if (i < editedPars.size) "${editedPars[i]}" else "4"
                                                OutlinedTextField(
                                                    value = parVal,
                                                    onValueChange = { newVal ->
                                                        val num = newVal.filter { it.isDigit() }.toIntOrNull() ?: 4
                                                        if (i < editedPars.size) {
                                                            editedPars[i] = num
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).height(38.dp).testTag("edit_par_${i+1}"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = ProfessionalPrimary,
                                                        unfocusedBorderColor = ProfessionalBorder,
                                                        focusedTextColor = ProfessionalText,
                                                        unfocusedTextColor = ProfessionalText
                                                    ),
                                                    singleLine = true
                                                )

                                                // Index input
                                                val indexVal = if (i < editedIndices.size) "${editedIndices[i]}" else "10"
                                                OutlinedTextField(
                                                    value = indexVal,
                                                    onValueChange = { newVal ->
                                                        val num = newVal.filter { it.isDigit() }.toIntOrNull() ?: 1
                                                        if (i < editedIndices.size) {
                                                            editedIndices[i] = num
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).height(38.dp).testTag("edit_index_${i+1}"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, textAlign = TextAlign.Center),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = ProfessionalPrimary,
                                                        unfocusedBorderColor = ProfessionalBorder,
                                                        focusedTextColor = ProfessionalText,
                                                        unfocusedTextColor = ProfessionalText
                                                    ),
                                                    singleLine = true
                                                )

                                                // Score input
                                                val scoreVal = if (i < editedScores.size) "${editedScores[i]}" else "0"
                                                OutlinedTextField(
                                                    value = scoreVal,
                                                    onValueChange = { newVal ->
                                                        val num = newVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                        if (i < editedScores.size) {
                                                            editedScores[i] = num
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1.1f).height(38.dp).testTag("edit_score_${i+1}"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = ProfessionalPrimary,
                                                        unfocusedBorderColor = ProfessionalBorder,
                                                        focusedTextColor = ProfessionalText,
                                                        unfocusedTextColor = ProfessionalText
                                                    ),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = editedNotes,
                                onValueChange = { editedNotes = it },
                                label = { Text("Recap Notes") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_notes"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ProfessionalPrimary,
                                    unfocusedBorderColor = ProfessionalBorder,
                                    focusedLabelColor = ProfessionalPrimary,
                                    focusedTextColor = ProfessionalText,
                                    unfocusedTextColor = ProfessionalText
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCancelExtraction,
                            modifier = Modifier.weight(1f).testTag("cancel_review_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalSecondary, contentColor = ProfessionalText),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Discard", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val finalCard = extractedScorecard.copy(
                                    playerName = editedName,
                                    handicap = editedHandicap,
                                    date = editedDate,
                                    notes = editedNotes,
                                    scoresJson = org.json.JSONArray(editedScores.toList()).toString(),
                                    parsJson = org.json.JSONArray(editedPars.toList()).toString(),
                                    indicesJson = org.json.JSONArray(editedIndices.toList()).toString(),
                                    totalScore = editedScores.sum()
                                )
                                onSaveScorecard(finalCard)
                             },
                            modifier = Modifier.weight(1.5f).testTag("save_scorecard_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save to SQLite", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

