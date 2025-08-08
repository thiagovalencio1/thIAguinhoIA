package com.thiaguinho.app.network

import com.thiaguinho.app.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GeminiRepository {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    suspend fun getDtcExplanation(dtcCode: String): Result<String> {
        val apiKey = AppConfig.GEMINI_API_KEY
        if (apiKey == "SUA_API_KEY_AQUI") {
            return Result.failure(Exception("Chave da API não configurada. Por favor, adicione sua chave no arquivo AppConfig.kt"))
        }

        val prompt = """
        Você é o thIAguinho, um assistente automotivo especialista e amigável.
        Um veículo apresentou o seguinte código de falha (DTC): $dtcCode.

        Explique para um motorista leigo, em português do Brasil, o que esse código significa.
        Use markdown para formatar a resposta. Estruture sua resposta da seguinte forma:

        **O que significa?**
        [Explicação clara e simples do problema.]

        **Possíveis Causas:**
        [Liste as causas mais comuns em formato de tópicos (usando *).]

        **O que devo fazer?**
        [Dê um conselho prático sobre a urgência e os próximos passos (ex: 'Pode continuar dirigindo com atenção e procurar um mecânico em breve' ou 'Pare o carro imediatamente em um local seguro e chame um guincho').]
        """.trimIndent()

        val request = GeminiRequest(contents = listOf(Content(parts = listOf(Part(text = prompt)))))

        return try {
            val response = apiService.generateContent(apiKey, request)
            val textResponse = response.candidates?.firstOrNull()?.text
            if (textResponse != null) {
                Result.success(textResponse)
            } else {
                Result.failure(Exception("A IA não retornou uma resposta válida. Pode ser um problema com a chave da API ou com a segurança do conteúdo."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Falha na comunicação com a API. Verifique sua conexão com a internet."))
        }
    }
}
