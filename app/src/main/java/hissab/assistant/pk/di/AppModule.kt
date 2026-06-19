package hissab.assistant.pk.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hissab.assistant.pk.data.image.ImageProcessorImpl
import hissab.assistant.pk.data.repository.WebViewRepositoryImpl
import hissab.assistant.pk.domain.repository.ImageProcessor
import hissab.assistant.pk.domain.repository.WebViewRepository
import javax.inject.Singleton

/**
 * Hilt module that binds domain interfaces to their data-layer implementations.
 *
 * Using `@Binds` (rather than `@Provides`) is the preferred form for simple
 * interface-to-impl mappings — it lets Dagger generate the binding directly with
 * no extra runtime cost.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindWebViewRepository(
        impl: WebViewRepositoryImpl,
    ): WebViewRepository

    @Binds
    @Singleton
    abstract fun bindImageProcessor(
        impl: ImageProcessorImpl,
    ): ImageProcessor
}
