package com.example.data

import org.json.JSONObject
import kotlin.math.sqrt

object VectorEngine {

    private val STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "but", "if", "then", "else", "when", 
        "at", "by", "for", "with", "about", "against", "between", "into", 
        "through", "during", "before", "after", "above", "below", "to", 
        "from", "up", "down", "in", "out", "on", "off", "over", "under", 
        "again", "further", "then", "once", "here", "there", "all", "any", 
        "both", "each", "few", "more", "most", "other", "some", "such", 
        "no", "nor", "not", "only", "own", "same", "so", "than", "too", 
        "very", "s", "t", "can", "will", "just", "don", "should", "now",
        "is", "of", "that", "this", "it", "was", "were", "be", "been", "have", "has"
    )

    // Tokenizes text, removes punctuation, lowercase, and filters stopwords
    fun getTokens(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it !in STOPWORDS }
    }

    // Creates a Term Frequency Vector Map (Word -> Count)
    fun createTermVector(text: String): Map<String, Int> {
        val tokens = getTokens(text)
        return tokens.groupingBy { it }.eachCount()
    }

    // Converts a term vector map to a compact JSON string representing our "local embedding"
    fun termVectorToJson(vector: Map<String, Int>): String {
        val json = JSONObject()
        vector.forEach { (word, count) ->
            json.put(word, count)
        }
        return json.toString()
    }

    // Parses a term vector JSON string back into a map
    fun jsonToTermVector(jsonStr: String): Map<String, Int> {
        if (jsonStr.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getInt(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    // Computes mathematical Cosine Similarity between two term frequency vectors (Map representation)
    // Cosine Sim = (A . B) / (||A|| * ||B||)
    fun calculateCosineSimilarity(vectorA: Map<String, Int>, vectorB: Map<String, Int>): Double {
        if (vectorA.isEmpty() || vectorB.isEmpty()) return 0.0

        var dotProduct = 0.0
        // Calculate dot product
        for ((word, countA) in vectorA) {
            val countB = vectorB[word] ?: 0
            dotProduct += countA * countB
        }

        if (dotProduct == 0.0) return 0.0

        // Calculate magnitude A
        var sumSquaresA = 0.0
        for (countA in vectorA.values) {
            sumSquaresA += countA * countA
        }
        val magnitudeA = sqrt(sumSquaresA)

        // Calculate magnitude B
        var sumSquaresB = 0.0
        for (countB in vectorB.values) {
            sumSquaresB += countB * countB
        }
        val magnitudeB = sqrt(sumSquaresB)

        if (magnitudeA == 0.0 || magnitudeB == 0.0) return 0.0

        return dotProduct / (magnitudeA * magnitudeB)
    }

    // Splits academic paper text into logical chunks of ~150 words using sentence bounds
    fun chunkAcademicPaper(title: String, paperText: String): List<String> {
        val sentences = paperText.split(Regex("(?<=[.!?])\\s+"))
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        var wordCount = 0

        for (sentence in sentences) {
            val wordsInSentence = sentence.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
            if (wordCount + wordsInSentence > 150 && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
                wordCount = 0
            }
            currentChunk.append(sentence).append(" ")
            wordCount += wordsInSentence
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }
        return chunks
    }
}
