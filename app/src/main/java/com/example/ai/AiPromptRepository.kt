package com.example.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Abstraction for StudioFlow 360 AI prompt & script assistance.
 * Backed by Firebase AI Logic (Gemini 3.8 Flash) via FirebaseAiPromptRepository.
 */
interface AiPromptRepository {
    val isLiveGeminiConnected: Boolean
    val noticeBanner: String

    suspend fun generatePromptOutput(
        channelName: String,
        promptType: String,
        topicBrief: String
    ): Result<String>

    fun generatePromptStream(
        channelName: String,
        promptType: String,
        topicBrief: String
    ): Flow<String> = flow {
        val result = generatePromptOutput(channelName, promptType, topicBrief)
        emit(result.getOrThrow())
    }
}
