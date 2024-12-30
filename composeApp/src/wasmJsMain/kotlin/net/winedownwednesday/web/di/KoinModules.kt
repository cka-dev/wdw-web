package net.winedownwednesday.web.di

import net.winedownwednesday.web.data.network.KtorClientInstance
import net.winedownwednesday.web.data.network.RemoteDataSource
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.viewmodels.AboutPageViewModel
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.EventsPageViewModel
import net.winedownwednesday.web.viewmodels.HomePageViewModel
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import net.winedownwednesday.web.viewmodels.PodcastsPageViewModel
import net.winedownwednesday.web.viewmodels.WinePageViewModel
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

val provideEventsPageViewModel = module {
    viewModel { EventsPageViewModel(get()) }
}

val providePodcastsPageViewModel = module {
    viewModel { PodcastsPageViewModel(get()) }
}

val provideAboutPageViewModel = module {
    viewModel { AboutPageViewModel(get()) }
}

val provideWinePageViewModel = module {
    viewModel { WinePageViewModel(get()) }
}

val provideAuthPageViewModel = module {
    viewModel { AuthPageViewModel(get()) }
}

val provideHomePageViewModel = module {
    viewModel { HomePageViewModel(get()) }
}

val appModule  =
    listOf(
        provideHttpClient,
        provideApiService,
        provideAppRepository,
        provideMembersViewModel,
        provideEventsPageViewModel,
        providePodcastsPageViewModel,
        provideAboutPageViewModel,
        provideWinePageViewModel,
        provideAuthPageViewModel,
        provideHomePageViewModel
    )
