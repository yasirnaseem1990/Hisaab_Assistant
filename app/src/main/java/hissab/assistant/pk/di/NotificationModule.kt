package hissab.assistant.pk.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hissab.assistant.pk.data.notification.NotificationRepositoryImpl
import hissab.assistant.pk.domain.repository.NotificationDisplayer
import hissab.assistant.pk.domain.repository.NotificationRepository
import hissab.assistant.pk.data.notification.NotificationDisplayerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationDisplayer(
        impl: NotificationDisplayerImpl,
    ): NotificationDisplayer

    companion object {

        @Provides
        @Singleton
        fun provideNotificationSharedPrefs(
            @ApplicationContext context: Context,
        ): SharedPreferences = context.getSharedPreferences(
            NotificationRepositoryImpl.PREFS_NAME,
            Context.MODE_PRIVATE,
        )
    }
}
