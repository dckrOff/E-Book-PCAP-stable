package uz.dckroff.pcap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.dckroff.pcap.data.repository.QuizRepository
import uz.dckroff.pcap.data.repository.QuizRepositoryImpl
import javax.inject.Singleton

/**
 * Модуль Hilt для внедрения зависимостей, связанных с тестами
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class QuizModule {

    /**
     * Привязка реализации репозитория тестов к интерфейсу
     */
    @Binds
    @Singleton
    abstract fun bindQuizRepository(
        quizRepositoryImpl: QuizRepositoryImpl
    ): QuizRepository
} 