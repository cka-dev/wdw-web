package net.winedownwednesday.web.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.winedownwednesday.web.data.Member
import net.winedownwednesday.web.data.MemberSection
import net.winedownwednesday.web.data.MembershipType
import net.winedownwednesday.web.data.repositories.AppRepository

class MembersPageViewModel (
    private val repository: AppRepository
)  : ViewModel() {
private val _organizers = MutableStateFlow<List<Member>>(emptyList())
    val organizers = _organizers.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members = _members.asStateFlow()

    private val _guests = MutableStateFlow<List<Member>>(emptyList())
    val guests = _guests.asStateFlow()

    private val _allMemberSections = MutableStateFlow<List<MemberSection>>(emptyList())
    val allMemberSections = _allMemberSections.asStateFlow()

    init {
        viewModelScope.launch {
            loadMembers()
        }
    }

    private suspend fun loadMembers() {
        repository.members.collect { fetchedMembers ->
            val newOrganizers = mutableListOf<Member>()
            val newMembers = mutableListOf<Member>()
            val newGuests = mutableListOf<Member>()

            fetchedMembers.filterNotNull().forEach { member ->
                when (member.memberType) {
                    MembershipType.MEMBER -> newMembers.add(member)
                    MembershipType.GUEST -> newGuests.add(member)
                    MembershipType.LEADER -> newOrganizers.add(member)
                }
            }

            _organizers.value = newOrganizers
            _members.value = newMembers
            _guests.value = newGuests

            _allMemberSections.value = listOf(
                MemberSection("Leadership", newOrganizers),
                MemberSection("Members", newMembers),
                MemberSection("Guests", newGuests)
            )
        }
    }
}