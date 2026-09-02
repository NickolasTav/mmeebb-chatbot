package dev.langchain4j.model.googleai;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.lang.reflect.Field;
import java.time.Duration;

/**
 * Utilitário para correção do bug de timeout no LangChain4j 0.35.0.
 * O GeminiService padrão do LangChain4j define apenas callTimeout, deixando
 * o readTimeout do OkHttp no valor padrão de 10 segundos, o que causa
 * java.net.SocketTimeoutException em chamadas que demandam mais de 10 segundos
 * (como modelos com thinking e prompts RAG).
 */
public class GeminiServiceCustomizer {

    public static void configureTimeouts(GoogleAiGeminiChatModel model, Duration timeout) {
        try {
            Field field = GoogleAiGeminiChatModel.class.getDeclaredField("geminiService");
            field.setAccessible(true);

            OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(timeout)
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(GeminiService.GEMINI_AI_ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            GeminiService customService = retrofit.create(GeminiService.class);
            field.set(model, customService);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao configurar timeouts customizados no GeminiService", e);
        }
    }

    public static void configureTimeouts(GoogleAiEmbeddingModel model, Duration timeout) {
        try {
            Field field = GoogleAiEmbeddingModel.class.getDeclaredField("geminiService");
            field.setAccessible(true);

            OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(timeout)
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(GeminiService.GEMINI_AI_ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            GeminiService customService = retrofit.create(GeminiService.class);
            field.set(model, customService);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao configurar timeouts customizados no GeminiService de Embedding", e);
        }
    }
}
