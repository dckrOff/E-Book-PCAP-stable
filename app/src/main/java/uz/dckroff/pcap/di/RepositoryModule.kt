package uz.dckroff.pcap.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.dckroff.pcap.data.repository.BookmarkRepository
import uz.dckroff.pcap.data.repository.BookmarkRepositoryImpl
import uz.dckroff.pcap.data.repository.GlossaryRepository
import uz.dckroff.pcap.data.repository.GlossaryRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGlossaryRepository(): GlossaryRepository {
        return GlossaryRepository.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideBookmarkRepository(
        bookmarkRepositoryImpl: BookmarkRepositoryImpl
    ): BookmarkRepository {
        return bookmarkRepositoryImpl
    }
} 