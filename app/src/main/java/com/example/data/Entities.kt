package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academic_papers")
data class AcademicPaper(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val authors: String, // Comma separated, e.g., "S. Russell, P. Norvig"
    val year: Int,
    val outlet: String, // Journal or Conference, e.g., "AI Journal"
    val volume: String = "",
    val issue: String = "",
    val pages: String = "",
    val publisher: String = "",
    val abstractText: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val isAnalyzed: Boolean = false,
    val citationType: String = "APA" // APA or MLA
) {
    // Generates an APA format citation string
    fun getApaCitation(): String {
        val initials = authors.split(",").joinToString(", ") { author ->
            val parts = author.trim().split(" ")
            if (parts.size > 1) {
                "${parts.first()} ${parts.last().first()}."
            } else {
                author.trim()
            }
        }
        val volumeIssueStr = if (volume.isNotEmpty()) {
            if (issue.isNotEmpty()) "$volume($issue)" else volume
        } else ""
        val pagesStr = if (pages.isNotEmpty()) ", $pages" else ""
        val outletStr = if (outlet.isNotEmpty()) {
            if (volumeIssueStr.isNotEmpty() || pagesStr.isNotEmpty()) "$outlet, " else outlet
        } else ""
        
        return "$initials ($year). $title. <i>$outletStr</i>$volumeIssueStr$pagesStr."
    }

    // Generates an MLA format citation string
    fun getMlaCitation(): String {
        val authorsList = authors.split(",")
        val primaryAuthor = if (authorsList.isNotEmpty()) {
            val first = authorsList[0].trim().split(" ")
            if (first.size > 1) {
                "${first.last()}, ${first.first()}"
            } else {
                authorsList[0].trim()
            }
        } else ""
        
        val secondaryAuthors = if (authorsList.size > 1) {
            ", " + authorsList.drop(1).joinToString(", ") { it.trim() }
        } else ""
        
        val fullAuthorStr = if (primaryAuthor.isNotEmpty()) "$primaryAuthor$secondaryAuthors. " else ""
        val outletStr = if (outlet.isNotEmpty()) " <i>$outlet</i>," else ""
        val volStr = if (volume.isNotEmpty()) " vol. $volume," else ""
        val noStr = if (issue.isNotEmpty()) " no. $issue," else ""
        val yearStr = " $year,"
        val pagesStr = if (pages.isNotEmpty()) " pp. $pages." else "."
        
        return "$fullAuthorStr\"$title.\"$outletStr$volStr$noStr$yearStr$pagesStr"
    }
}

@Entity(tableName = "document_chunks")
data class DocumentChunk(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val paperId: Int,
    val paperTitle: String,
    val chunkIndex: Int,
    val content: String,
    // Store localized term-frequency statistics representing our "local vector"
    val termVectorJson: String = "" 
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val sender: String, // "user" or "assistant"
    val timestamp: Long = System.currentTimeMillis(),
    val matchedSourcesJson: String = "" // JSON representation of matched source chunks for UI citation reference
)

@Entity(tableName = "huggingface_models")
data class HuggingFaceModel(
    @PrimaryKey val repoId: String, // e.g., "TinyLlama/TinyLlama-1.1B-Chat-v1.0"
    val filename: String,
    val name: String,
    val size: String,
    val description: String,
    val status: String = "Not Downloaded", // "Not Downloaded", "Downloading", "Completed"
    val progress: Float = 0.0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val speed: String = ""
)

data class GeneratedMediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val prompt: String,
    val type: String, // "image" or "video"
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)
