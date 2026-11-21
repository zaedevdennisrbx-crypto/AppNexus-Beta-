package com.example.data.repository

import android.util.Log
import com.example.data.model.CatalogApp
import com.example.data.model.GroundingSource
import com.example.data.model.ScoutDiscoveryApp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiScoutRepository"

sealed interface ScoutStreamChunk {
    data class Delta(
        val accumulatedText: String,
        val sources: List<GroundingSource>,
        val searchEntryPointHtml: String?
    ) : ScoutStreamChunk

    data class Completed(
        val fullText: String,
        val sources: List<GroundingSource>,
        val searchEntryPointHtml: String?
    ) : ScoutStreamChunk
}

class AiScoutRepository {

    // Lazy initialization ensures Firebase.ai is only evaluated on first inference.
    private val groundedScoutModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch()),
            systemInstruction = content {
                text(
                    "You are Nexus AI Scout, the intelligent Android app discovery and security analyst inside AppNexus. " +
                        "Always use Google Search grounding to verify current app versions, developers, privacy reputation, and alternatives. " +
                        "Provide concise, structured, helpful guidance with clear headings and bullet points."
                )
            }
        )
    }

    private val structuredAppSchema = Schema.obj(
        properties = mapOf(
            "apps" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "appName" to Schema.string(description = "Official app name"),
                        "developer" to Schema.string(description = "Developer or publisher name"),
                        "category" to Schema.enumeration(
                            values = listOf(
                                "AI & Productivity",
                                "Developer Tools",
                                "Security & Privacy",
                                "Creative Studio",
                                "Audio & Media",
                                "Games"
                            ),
                            description = "Best matching AppNexus store category"
                        ),
                        "version" to Schema.string(description = "Approximate current version or release tag"),
                        "sizeMb" to Schema.double(description = "Estimated Android APK size in MB"),
                        "tagline" to Schema.string(description = "Concise one-line summary under 100 chars"),
                        "websiteUrl" to Schema.string(description = "Official website or Google Play URL")
                    )
                ),
                description = "Up to 4 recommended Android apps extracted from the discovery response"
            )
        )
    )

    private val structuredExtractorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredAppSchema
            }
        )
    }

    fun streamGroundedAppDiscovery(userQuery: String): Flow<ScoutStreamChunk> = flow {
        val prompt = """
            User discovery request: "$userQuery"
            
            Search for the best, currently maintained Android apps matching this request.
            Include:
            1. Top 3-4 standout Android apps with their developer, core strengths, and privacy/pricing notes.
            2. A quick comparison recommendation on which app fits best for different user needs.
            Keep the tone crisp and editorial.
        """.trimIndent()

        val accumulated = StringBuilder()
        val collectedSources = linkedMapOf<String, GroundingSource>()
        var searchEntryHtml: String? = null

        withTimeout(60_000L) {
            groundedScoutModel.generateContentStream(prompt).collect { chunk ->
                chunk.text?.let { accumulated.append(it) }
                extractGrounding(chunk, collectedSources)?.let { html ->
                    searchEntryHtml = html
                }
                emit(
                    ScoutStreamChunk.Delta(
                        accumulatedText = accumulated.toString(),
                        sources = collectedSources.values.toList(),
                        searchEntryPointHtml = searchEntryHtml
                    )
                )
            }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
        emit(
            ScoutStreamChunk.Completed(
                fullText = finalText,
                sources = collectedSources.values.toList(),
                searchEntryPointHtml = searchEntryHtml
            )
        )
    }

    fun streamAppSecurityAndComparisonBrief(app: CatalogApp): Flow<ScoutStreamChunk> = flow {
        val prompt = """
            Analyze the Android app "${app.appName}" by ${app.developer} (package: ${app.packageName}, category: ${app.category}).
            Use Google Search to check its latest current status, security & privacy reputation, recent notable updates, and top 2 comparable Android alternatives.
            Keep it concise (under 220 words) with clear bullet sections:
            - Latest Status & Highlights
            - Privacy & Permission Posture
            - Top Alternatives
        """.trimIndent()

        val accumulated = StringBuilder()
        val collectedSources = linkedMapOf<String, GroundingSource>()
        var searchEntryHtml: String? = null

        withTimeout(60_000L) {
            groundedScoutModel.generateContentStream(prompt).collect { chunk ->
                chunk.text?.let { accumulated.append(it) }
                extractGrounding(chunk, collectedSources)?.let { html ->
                    searchEntryHtml = html
                }
                emit(
                    ScoutStreamChunk.Delta(
                        accumulatedText = accumulated.toString(),
                        sources = collectedSources.values.toList(),
                        searchEntryPointHtml = searchEntryHtml
                    )
                )
            }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isBlank()) {
            throw IllegalStateException("Gemini returned an empty analysis.")
        }
        emit(
            ScoutStreamChunk.Completed(
                fullText = finalText,
                sources = collectedSources.values.toList(),
                searchEntryPointHtml = searchEntryHtml
            )
        )
    }

    suspend fun extractStructuredApps(scoutText: String): List<ScoutDiscoveryApp> {
        val prompt = """
            Extract up to 4 real Android apps mentioned in the following discovery summary into structured JSON.
            Summary:
            $scoutText
        """.trimIndent()

        val response = withTimeout(45_000L) {
            structuredExtractorModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("Empty structured response.")
        val root = JSONObject(rawJson)
        val appsArray = root.optJSONArray("apps") ?: return emptyList()
        val results = mutableListOf<ScoutDiscoveryApp>()
        for (i in 0 until appsArray.length()) {
            val obj = appsArray.optJSONObject(i) ?: continue
            val name = obj.optString("appName").trim()
            if (name.isBlank()) continue
            results.add(
                ScoutDiscoveryApp(
                    appName = name,
                    developer = obj.optString("developer", "Independent").ifBlank { "Independent" },
                    category = obj.optString("category", "AI & Productivity").ifBlank { "AI & Productivity" },
                    version = obj.optString("version", "Latest").ifBlank { "Latest" },
                    sizeMb = obj.optDouble("sizeMb", 28.0).let { if (it.isNaN() || it <= 0.0) 28.0 else it },
                    tagline = obj.optString("tagline", "Recommended Android app").ifBlank { "Recommended Android app" },
                    websiteUrl = obj.optString("websiteUrl", "https://play.google.com/store")
                )
            )
        }
        return results
    }

    private fun extractGrounding(
        response: GenerateContentResponse,
        targetSources: LinkedHashMap<String, GroundingSource>
    ): String? {
        return try {
            val metadata = response.candidates.firstOrNull()?.groundingMetadata ?: return null
            metadata.groundingChunks.forEach { chunk ->
                val web = chunk.web
                val uri = web?.uri
                if (!uri.isNullOrBlank()) {
                    val title = web.title?.takeIf { it.isNotBlank() } ?: uri
                    targetSources[uri] = GroundingSource(title = title, uri = uri)
                }
            }
            metadata.searchEntryPoint?.renderedContent?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to parse grounding metadata chunk", e)
            null
        }
    }
}
