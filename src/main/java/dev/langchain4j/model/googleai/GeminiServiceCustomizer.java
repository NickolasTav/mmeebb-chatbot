package dev.langchain4j.model.googleai;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;

/**
 * Utilitário para correção de timeout e resiliência a alta demanda (HTTP 503 / 429) no LangChain4j 0.35.0.
 *
 * 1. Define connectTimeout, readTimeout e writeTimeout adequados no OkHttpClient.
 * 2. Adiciona interceptor de resiliência com retentativas automáticas e failover entre
 *    gemini-3.5-flash-lite e gemini-3.5-flash quando o Google retornar 503 (High Demand).
 */
public class GeminiServiceCustomizer {

    private static final Logger log = LoggerFactory.getLogger(GeminiServiceCustomizer.class);

    public static void configureTimeouts(GoogleAiGeminiChatModel model, Duration timeout) {
        try {
            Field field = GoogleAiGeminiChatModel.class.getDeclaredField("geminiService");
            field.setAccessible(true);

            OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(timeout)
                    .connectTimeout(timeout)
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .addInterceptor(new HighDemandRetryInterceptor(true))
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
                    .addInterceptor(new HighDemandRetryInterceptor(false))
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

    private static class HighDemandRetryInterceptor implements Interceptor {
        private final boolean enableModelFailover;

        public HighDemandRetryInterceptor(boolean enableModelFailover) {
            this.enableModelFailover = enableModelFailover;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            int maxAttempts = 3;
            long baseBackoffMs = 800;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    response = chain.proceed(request);

                    // 503 (Unavailable / High Demand) ou 429 (Rate Limit / Quota Spikes)
                    if (response.code() != 503 && response.code() != 429) {
                        return response;
                    }

                    log.warn("[GeminiInterceptor] Google retornou HTTP {} na tentativa {}/{} para: {}",
                            response.code(), attempt, maxAttempts, request.url().encodedPath());

                    if (attempt < maxAttempts) {
                        response.close();

                        // Se for a última retentativa e failover estiver habilitado, tenta o modelo irmão
                        if (attempt == 2 && enableModelFailover) {
                            String urlStr = request.url().toString();
                            if (urlStr.contains("gemini-3.5-flash-lite")) {
                                String newUrl = urlStr.replace("gemini-3.5-flash-lite", "gemini-3.5-flash");
                                log.info("[GeminiInterceptor] Acionando failover de modelo: gemini-3.5-flash-lite -> gemini-3.5-flash");
                                request = request.newBuilder().url(newUrl).build();
                            } else if (urlStr.contains("gemini-3.5-flash")) {
                                String newUrl = urlStr.replace("gemini-3.5-flash", "gemini-3.5-flash-lite");
                                log.info("[GeminiInterceptor] Acionando failover de modelo: gemini-3.5-flash -> gemini-3.5-flash-lite");
                                request = request.newBuilder().url(newUrl).build();
                            }
                        }

                        Thread.sleep(baseBackoffMs * attempt);
                    }
                } catch (IOException e) {
                    log.warn("[GeminiInterceptor] Falha de I/O na tentativa {}/{}: {}", attempt, maxAttempts, e.getMessage());
                    if (attempt == maxAttempts) {
                        throw e;
                    }
                    try {
                        Thread.sleep(baseBackoffMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (response != null) return response;
                    throw new IOException("Interrompido durante retry no Gemini", e);
                }
            }

            return response;
        }
    }
}
