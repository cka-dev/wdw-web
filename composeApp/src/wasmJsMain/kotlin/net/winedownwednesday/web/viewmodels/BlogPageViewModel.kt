package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.winedownwednesday.web.data.models.BlogPost
import net.winedownwednesday.web.data.network.RemoteDataSource
import net.winedownwednesday.web.data.repositories.AppRepository
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class BlogPageViewModel(
    @InjectedParam private val appRepository: AppRepository,
    @InjectedParam private val remoteDataSource: RemoteDataSource
) : ViewModel() {
    val blogPosts: StateFlow<List<BlogPost>?> = appRepository.blogPosts
    val isLoading: StateFlow<Boolean> = remoteDataSource.isLoading
    val error: StateFlow<String?> = remoteDataSource.error
}
