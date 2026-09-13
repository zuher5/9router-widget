package com.ninerouter.monitor.ui.solar

import androidx.compose.ui.graphics.Color

data class ProviderMeta(
    val id: String,
    val name: String,
    val color: Color,
    val textIcon: String
)

object ProviderCatalog {
    private val defaultMeta = ProviderMeta(
        id = "",
        name = "",
        color = Color(0xFF6B7280), // #6b7280 web default
        textIcon = "??"
    )

    // Warna dan textIcon resmi dari web 9Router (open-sse/providers/registry)
    private val catalog: Map<String, ProviderMeta> = listOf(
        ProviderMeta("anthropic", "Anthropic", Color(0xFFD97757), "AN"),
        ProviderMeta("openai", "OpenAI", Color(0xFF10A37F), "OA"),
        ProviderMeta("opencode", "OpenCode Free", Color(0xFFE87040), "OC"),
        ProviderMeta("google", "Google", Color(0xFF4285F4), "GG"),
        ProviderMeta("gemini", "Gemini", Color(0xFF4285F4), "GM"),
        ProviderMeta("deepseek", "DeepSeek", Color(0xFF4D6BFE), "DS"),
        ProviderMeta("groq", "Groq", Color(0xFFF55036), "GQ"),
        ProviderMeta("openrouter", "OpenRouter", Color(0xFF6366F1), "OR"),
        ProviderMeta("together", "Together AI", Color(0xFF0F172A), "TG"),
        ProviderMeta("xai", "xAI Grok", Color(0xFF1DA1F2), "XA"),
        ProviderMeta("mistral", "Mistral", Color(0xFFFF7000), "MI"),
        ProviderMeta("cohere", "Cohere", Color(0xFF39594C), "CH"),
        ProviderMeta("perplexity", "Perplexity", Color(0xFF1FB8CD), "PP"),
        ProviderMeta("qwen", "Qwen", Color(0xFF6236FF), "QW"),
        ProviderMeta("cloudflare-ai", "Cloudflare AI", Color(0xFFF38020), "CF"),
        ProviderMeta("cerebras", "Cerebras", Color(0xFFFA5252), "CB"),
        ProviderMeta("antigravity", "Antigravity", Color(0xFF4285F4), "AG")
    ).associateBy { it.id.lowercase() }

    fun get(providerId: String): ProviderMeta {
        val key = providerId.lowercase().trim()
        val found = catalog[key]
        if (found != null) return found

        // Fallback untuk custom/compatible providers
        val cleanName = providerId.replace("openai-compatible-", "")
            .replace("anthropic-compatible-", "")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val icon = providerId.filter { it.isLetterOrDigit() }.take(2).uppercase().ifEmpty { "??" }
        return defaultMeta.copy(
            id = key,
            name = cleanName.ifBlank { providerId },
            textIcon = icon
        )
    }
}
