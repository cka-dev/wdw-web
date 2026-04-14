package net.winedownwednesday.web.di

import net.winedownwednesday.web.data.network.KtorClientInstance
import net.winedownwednesday.web.data.network.RemoteDataSource
import net.winedownwednesday.web.data.repositories.AppRepository
import net.winedownwednesday.web.viewmodels.AboutPageViewModel
import net.winedownwednesday.web.viewmodels.AuthPageViewModel
import net.winedownwednesday.web.viewmodels.BlogPageViewModel
import net.winedownwednesday.web.viewmodels.EventsPageViewModel
import net.winedownwednesday.web.viewmodels.HomePageViewModel
import net.winedownwednesday.web.viewmodels.MembersPageViewModel
import net.winedownwednesday.web.viewmodels.PodcastsPageViewModel
import net.winedownwednesday.web.viewmodels.WinePageViewModel
import net.winedownwednesday.web.viewmodels.MessagingViewModel
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
    single { MembersPageViewModel(get()) }
}

val provideEventsPageViewModel = module {
    single { EventsPageViewModel(get()) }
}

val providePodcastsPageViewModel = module {
    single { PodcastsPageViewModel(get()) }
}

val provideAboutPageViewModel = module {
    single { AboutPageViewModel(get()) }
}

val provideWinePageViewModel = module {
    single { WinePageViewModel(get()) }
}

val provideAuthPageViewModel = module {
    single { AuthPageViewModel(get()) }
}

val provideHomePageViewModel = module {
    single { HomePageViewModel(get()) }
}

val provideBlogPageViewModel = module {
    single { BlogPageViewModel(get()) }
}

val provideMessagingViewModel = module {
    single { MessagingViewModel(get()) }
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
        provideHomePageViewModel,
        provideBlogPageViewModel,
        provideMessagingViewModel,
    )
