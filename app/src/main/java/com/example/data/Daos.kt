package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM academic_papers ORDER BY dateAdded DESC")
    fun getAllPapers(): Flow<List<AcademicPaper>>

    @Query("SELECT * FROM academic_papers WHERE id = :id")
    suspend fun getPaperById(id: Int): AcademicPaper?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: AcademicPaper): Long

    @Update
    suspend fun updatePaper(paper: AcademicPaper)

    @Delete
    suspend fun deletePaper(paper: AcademicPaper)

    @Query("DELETE FROM academic_papers")
    suspend fun clearAllPapers()
}

@Dao
interface ChunkDao {
    @Query("SELECT * FROM document_chunks WHERE paperId = :paperId ORDER BY chunkIndex ASC")
    fun getChunksByPaper(paperId: Int): Flow<List<DocumentChunk>>

    @Query("SELECT * FROM document_chunks")
    suspend fun getAllChunksDirect(): List<DocumentChunk>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunk>)

    @Query("DELETE FROM document_chunks WHERE paperId = :paperId")
    suspend fun deleteChunksByPaperId(paperId: Int)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface ModelDao {
    @Query("SELECT * FROM huggingface_models")
    fun getAllModels(): Flow<List<HuggingFaceModel>>

    @Query("SELECT * FROM huggingface_models WHERE repoId = :repoId")
    suspend fun getModelById(repoId: String): HuggingFaceModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: HuggingFaceModel)

    @Update
    suspend fun updateModel(model: HuggingFaceModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<HuggingFaceModel>)
}

@Dao
interface ScorecardDao {
    @Query("SELECT * FROM golf_scorecards ORDER BY timestamp DESC")
    fun getAllScorecards(): Flow<List<GolfScorecard>>

    @Query("SELECT * FROM golf_scorecards WHERE id = :id")
    suspend fun getScorecardById(id: Int): GolfScorecard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScorecard(card: GolfScorecard): Long

    @Delete
    suspend fun deleteScorecard(card: GolfScorecard)

    @Query("DELETE FROM golf_scorecards")
    suspend fun clearAllScorecards()
}

