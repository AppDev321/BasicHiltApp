package com.dopsi.webapp.module

import com.dopsi.webapp.data.repo.movie.MovieRepo
import com.dopsi.webapp.data.repo.movie.MovieRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun provideMovieRepo(contactMng: MovieRepoImpl): MovieRepo

}
