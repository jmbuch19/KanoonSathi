package `in`.kanoonsaathi.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Repositories and TokenStore are @Singleton @Inject constructor — Hilt picks them up automatically.
// This module exists as a placeholder for any future manual bindings.
@Module
@InstallIn(SingletonComponent::class)
object AppModule
