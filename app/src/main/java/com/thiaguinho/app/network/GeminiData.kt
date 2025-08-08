package com.thiaguinho.app.network

import com.google.gson.annotations.SerializedName

// Estruturas para o Request
data class GeminiRequest(val contents: List<Content>)
data class Content(val parts: List<Part>)
data class Part(@SerializedName("text") val text: String)

// Estruturas para a Response
data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: Content?) {
    val text: String?
        get() = this.content?.parts?.firstOrNull()?.text
}
