package hissab.assistant.pk.domain.usecase

import hissab.assistant.pk.domain.model.WebPage
import hissab.assistant.pk.domain.repository.WebViewRepository
import javax.inject.Inject

/**
 * Use case that returns the default [WebPage] to display when the app starts.
 *
 * Use cases keep business logic out of the ViewModel and make each unit of
 * behaviour easy to test in isolation.
 */
class GetDefaultWebPageUseCase @Inject constructor(
    private val repository: WebViewRepository,
) {
    operator fun invoke(): WebPage = repository.getDefaultPage()
}
