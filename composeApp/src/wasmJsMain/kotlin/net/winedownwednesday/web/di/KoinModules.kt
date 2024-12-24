package net.winedownwednesday.web.di

import net.winedownwednesday.web.data.network.KtorClientInstance
import net.winedownwednesday.web.data.network.RemoteDataSource
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val provideHttpClient = module {
    single {
        KtorClientInstance.httpClient
    }
}

val provideApiService = module{
    single { RemoteDataSource(get())}
}

val provideAppRepository = module {
    single {
        AppRepository(get())
    }
}

val provideMembersViewModel = module {
    viewModel { MembersPageViewModel(get()) }
}
val appModule  =
    listOf(
        provideHttpClient,
        provideApiService,
        provideAppRepository,
        provideMembersViewModel
    )
