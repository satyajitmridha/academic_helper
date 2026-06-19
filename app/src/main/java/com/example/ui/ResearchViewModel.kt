package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.repository.ResearchRepository
import com.example.api.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

enum class ActiveTab {
    LIBRARY, CHAT, MODELS
}

class ResearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ResearchRepository(db, application)

    // UI state flows
    val papers: StateFlow<List<AcademicPaper>> = repository.allPapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hfModels: StateFlow<List<HuggingFaceModel>> = repository.allModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTab = MutableStateFlow(ActiveTab.LIBRARY)
    val activeTab: StateFlow<ActiveTab> = _activeTab.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeModelMode = MutableStateFlow("Local Offline (SmolLM)") // "Local Offline (SmolLM)", "Hybrid Cloud (Gemini)"
    val activeModelMode: StateFlow<String> = _activeModelMode.asStateFlow()

    private val _systemStatus = MutableStateFlow("Ready - Secure & Private")
    val systemStatus: StateFlow<String> = _systemStatus.asStateFlow()

    // Add Paper State Dialog indicators
    private val _isAnalyzingPaper = MutableStateFlow(false)
    val isAnalyzingPaper: StateFlow<Boolean> = _isAnalyzingPaper.asStateFlow()

    private val _analyzingProgress = MutableStateFlow(0f)
    val analyzingProgress: StateFlow<Float> = _analyzingProgress.asStateFlow()

    init {
        // Pre-populate with fundamental research paper baselines if empty
        viewModelScope.launch {
            repository.prepopulatePapers()
        }
    }

    fun selectTab(tab: ActiveTab) {
        _activeTab.value = tab
    }

    fun setModelMode(mode: String) {
        _activeModelMode.value = mode
        _systemStatus.value = if (mode.contains("Local")) "On-Device Local AI Only" else "Hybrid (Cloud LLM + Local Vector Context)"
    }

    // Deletes an academic paper from local catalog and cleans corresponding vectors
    fun deletePaper(paper: AcademicPaper) {
        viewModelScope.launch {
            repository.deletePaper(paper)
        }
    }

    // Formats and exports all active citation catalog items
    fun exportCitations(style: String): String {
        val currentPapers = papers.value
        if (currentPapers.isEmpty()) return "Your citation library is empty. Please add or analyze academic papers."
        
        return currentPapers.joinToString("\n\n") { paper ->
            if (style == "MLA") paper.getMlaCitation() else paper.getApaCitation()
        }
    }

    // Handles document imports: simulates full OCR PDF/Image content pull and indexes vectors locally
    fun importAndVecAcademicPaper(
        title: String,
        authors: String,
        year: Int,
        journal: String,
        abstractText: String,
        simulateFileExtension: String // ".pdf", ".png", ".jpg"
    ) {
        viewModelScope.launch {
            _isAnalyzingPaper.value = true
            _analyzingProgress.value = 0.1f
            _systemStatus.value = "Extracting raw text from $simulateFileExtension file..."
            
            // Simulating OCR parsing latency steps
            delay(800)
            _analyzingProgress.value = 0.4f
            _systemStatus.value = "Running offline layout mapping..."
            
            delay(1000)
            _analyzingProgress.value = 0.7f
            _systemStatus.value = "Computing term vectors locally to protect private content..."
            
            val newPaper = AcademicPaper(
                title = title,
                authors = authors,
                year = year,
                outlet = journal,
                abstractText = abstractText,
                isAnalyzed = false
            )
            
            val newId = repository.insertPaper(newPaper)
            
            delay(700)
            _analyzingProgress.value = 0.9f
            _systemStatus.value = "Storing localized SQLite semantic indexing..."
            
            repository.vectorizePaper(newId.toInt())
            
            _analyzingProgress.value = 1.0f
            _isAnalyzingPaper.value = false
            _systemStatus.value = "Successfully integrated '$title' with local database!"
        }
    }

    // Triggers download of desired lightweight model directly from HuggingFace
    fun triggerHuggingFaceModelDownload(repoId: String) {
        viewModelScope.launch {
            _systemStatus.value = "Initiating direct HuggingFace request..."
            repository.startHuggingFaceDownload(repoId)
            _systemStatus.value = "Download completed / updated."
        }
    }

    // Main LLM chat trigger coordinating Local Vector DB check + prompt build
    fun sendMessageToChatbot(userMessage: String) {
        if (userMessage.isBlank()) return

        viewModelScope.launch {
            _isGenerating.value = true
            _systemStatus.value = "Searching local SQLite term indices..."

            // Save user prompt
            val userMsg = ChatMessage(text = userMessage, sender = "user")
            repository.insertMessage(userMsg)

            // Step 1: Query Local Private Vector DB
            val matchedChunks = repository.searchLocalVectorDb(userMessage, topK = 3)
            
            // Formulate matched sources representation
            val sourcesJsonArray = JSONArray()
            matchedChunks.forEach { chunk ->
                val obj = JSONObject()
                obj.put("paperTitle", chunk.paperTitle)
                obj.put("chunkIdx", chunk.chunkIndex)
                obj.put("text", chunk.content)
                sourcesJsonArray.put(obj)
            }
            val sourcesJsonStr = sourcesJsonArray.toString()

            _systemStatus.value = "Synthesizing localized context..."
            delay(400) // slight realism latency

            if (_activeModelMode.value.contains("Local")) {
                // RUN LOCAL OFFLINE INFERENCE SIMULATOR
                _systemStatus.value = "Inference running on-device..."
                
                // Construct highly relevant local token-by-token stream
                val responseBuilder = StringBuilder()
                val targetResponse = generateLocalResponseText(userMessage, matchedChunks)
                
                val tokens = targetResponse.split(" ")
                val placeholderMsgId = repository.insertMessage(
                    ChatMessage(
                        text = "",
                        sender = "assistant",
                        matchedSourcesJson = sourcesJsonStr
                    )
                ).toInt()

                // Stream tokens locally
                for (token in tokens) {
                    delay(35) // simulates ~30 tokens/sec processing
                    responseBuilder.append(token).append(" ")
                    db.chatDao().insertMessage(
                        ChatMessage(
                            id = placeholderMsgId,
                            text = responseBuilder.toString().trim(),
                            sender = "assistant",
                            matchedSourcesJson = sourcesJsonStr
                        )
                    )
                }
            } else {
                // HYBRID CLOUD MODE (via Gemini API + Local Vector DB Context)
                _systemStatus.value = "Calling cloud via secure REST..."
                
                val contextPrompt = if (matchedChunks.isNotEmpty()) {
                    val contextBuilder = StringBuilder("CONTEXT FROM PRIVATE LOCAL VECTOR DATABASE:\n")
                    matchedChunks.forEachIndexed { idx, chunk ->
                        contextBuilder.append("[Source $idx] Title: ${chunk.paperTitle}, Content: ${chunk.content}\n\n")
                    }
                    contextBuilder.append("INSTRUCTIONS: Solve the user query incorporating the provided local sources cleanly. Maintain academic and professional tone.\n")
                    contextBuilder.toString()
                } else {
                    "No local sources matched in the vector database."
                }

                val resultText = GeminiClient.generateResponse(
                    prompt = userMessage,
                    systemInstruction = "You are a private LLM chatbot for academic researches. Integrate user paper vector context precisely:\n$contextPrompt"
                )

                repository.insertMessage(
                    ChatMessage(
                        text = resultText,
                        sender = "assistant",
                        matchedSourcesJson = sourcesJsonStr
                    )
                )
            }

            _isGenerating.value = false
            _systemStatus.value = "Local database secure & private"
        }
    }

    // Clear active conversation chat log
    fun clearChatLog() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Local deterministic response constructor supporting real keyword matching
    private fun generateLocalResponseText(query: String, chunks: List<DocumentChunk>): String {
        val lowercaseQuery = query.lowercase()
        return when {
            chunks.isEmpty() -> {
                "Based on the local vector database, I couldn't find any direct reference matching your exact search terms. Because I'm operating fully offline on-device to preserve your absolute privacy, I don't have access to global internet search. Please add or analyze papers related to this keyword in your Library!"
            }
            lowercaseQuery.contains("attention") || lowercaseQuery.contains("transformer") -> {
                "According to Vaswani et al. (2017) in 'Attention Is All You Need', the proposed model entirely dispenses with recurrences and convolutions. Self-attention mechanisms connect all sequence positions in a single time-complexity step. This allows massive computational parallelization, solving traditional LSTM training bottlenecks local-first."
            }
            lowercaseQuery.contains("resnet") || lowercaseQuery.contains("residual") || lowercaseQuery.contains("layer") -> {
                "Under 'Deep Residual Learning for Image Recognition' (He et al., 2016), the authors address training issues in deeper networks by explicitly approximating residual functions F(x) = H(x) - x mapping layers instead. This maintains high training feasibility despite extremely large deep architectures."
            }
            lowercaseQuery.contains("gan") || lowercaseQuery.contains("adversarial") || lowercaseQuery.contains("generative") -> {
                "Goodfellow et al. (2014) introduced 'Generative Adversarial Nets' where a generator G attempts to create realistic samples that confuse a discriminator D. Both play a minimax zero-sum game with value function V(D,G). This operates offline with precise mathematical equilibrium."
            }
            else -> {
                val bestChunk = chunks.first()
                "Based on local vector matching of '${bestChunk.paperTitle}' (Chunk #${bestChunk.chunkIndex}), here is the relevant evidence from the database: \n\n\"${bestChunk.content}\"\n\nThis highlights how the local Vector Database (using cosine similarity calculation) matches private text chunks instantly without exposing document data to external cloud networks."
            }
        }
    }
}
