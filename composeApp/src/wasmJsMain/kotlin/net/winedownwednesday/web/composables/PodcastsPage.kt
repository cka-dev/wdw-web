package net.winedownwednesday.web.composables

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PodcastsPage(){
    PodcastsContent()
}

@Composable
fun PodcastsContent(){
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ){

    }
}