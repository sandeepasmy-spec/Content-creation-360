package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "FirebaseAiRepo"
private const val AI_TIMEOUT_MS = 60_000L

class FirebaseAiPromptRepository : AiPromptRepository {

    override val isLiveGeminiConnected: Boolean = true
    override val noticeBanner: String =
        "Live Gemini 3.8 Flash • Firebase AI Logic with App Check"

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private fun buildStudioPrompt(
        channelName: String,
        promptType: String,
        topicBrief: String
    ): String {
        val cleanBrief = topicBrief.ifBlank { "Studio production episode for $channelName" }
        return when (promptType) {
            "SCRIPT_PROMPT" -> """
                You are Manoj, Strategic Partner & AI Content Prompt Lead at Serplive Solutions.
                Channel: $channelName
                Episode Brief: $cleanBrief

                Create a production-ready video script prompt and structured episode outline:
                1. High-retention 15-second visual hook
                2. 3 clear chapters with anchor cue notes & B-roll visual callouts
                3. Fact-check & pacing notes
                4. Mid-roll retention cue and ending CTA.
                Keep it crisp, studio-ready, and immediately usable by anchors (Parchi / Kritika / Pandit Ji / Art & Craft Teacher).
            """.trimIndent()

            "IMAGE_PROMPT" -> """
                You are Manoj, AI Visual Prompt Lead at Serplive Solutions.
                Channel: $channelName
                Visual Brief: $cleanBrief

                Write 3 detailed, high-CTR 16:9 thumbnail / key visual image generation prompts.
                For each prompt specify: Subject composition, camera angle/lens, dramatic studio lighting, color palette, and negative-space placement for text overlay (do NOT embed text inside the image).
            """.trimIndent()

            "IMAGE_TO_VIDEO_PROMPT" -> """
                You are Manoj, AI Motion Prompt Lead at Serplive Solutions.
                Channel: $channelName
                Scene Brief: $cleanBrief

                Write 3 cinematic image-to-video motion prompts for B-roll animation:
                Include camera movement (dolly/parallax/crane), subtle subject motion, atmospheric lighting, 24fps pacing, and artifact-prevention cues.
            """.trimIndent()

            else -> """
                You are Manoj, Strategic Content Planner at Serplive Solutions.
                Channel: $channelName
                Content Pillar / Brief: $cleanBrief

                Provide a structured content plan including:
                - Episode hook & target audience value
                - Recommended studio room & shoot duration
                - Required deliverables (script, thumbnail variants, vertical cutdowns)
                - Suggested publishing window & SEO keywords.
            """.trimIndent()
        }
    }

    override suspend fun generatePromptOutput(
        channelName: String,
        promptType: String,
        topicBrief: String
    ): Result<String> {
        return try {
            val prompt = buildStudioPrompt(channelName, promptType, topicBrief)
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                Result.failure(IllegalStateException("Gemini returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini prompt generation timed out", e)
            Result.failure(IllegalStateException("Generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            Result.failure(e)
        }
    }

    override fun generatePromptStream(
        channelName: String,
        promptType: String,
        topicBrief: String
    ): Flow<String> = flow {
        val prompt = buildStudioPrompt(channelName, promptType, topicBrief)
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty stream response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini streaming timed out", e)
            throw IllegalStateException("Generation timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI streaming failed", e)
            throw e
        }
    }
}
