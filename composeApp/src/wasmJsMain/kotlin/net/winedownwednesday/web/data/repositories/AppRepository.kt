package net.winedownwednesday.web.data.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.network.RemoteDataSource
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single

@Single
class AppRepository (
    @InjectedParam
    private val remoteDataSource: RemoteDataSource
) {

    private val _members = MutableStateFlow<List<Member?>>(listOf())
    val members = _members.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {

        repositoryScope.launch {
            fetchMembers()
        }

    }

    private suspend fun fetchMembers () {
        try {
            val remoteMemberList = remoteDataSource.fetchMembers()

            if (!remoteMemberList.isNullOrEmpty()) {
                _members.value = remoteMemberList
            } else {
                println("AppRepository: Returned member list is empty.")
            }
        } catch (e: Exception) {
            println("AppRepository: Error fetching members.")
        }

    }

    companion object{
        private const val TAG = "AppRepository"
    }
}