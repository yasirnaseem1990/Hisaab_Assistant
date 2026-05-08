package hissab.assistant.pk.domain.model

/**
 * Domain model representing a web page that can be loaded inside the WebView.
 *
 * Keeping this as a pure Kotlin data class (no Android dependencies) keeps the
 * domain layer framework-agnostic and easy to unit-test.
 */
data class WebPage(
    val url: String,
    val title: String = "",
)
