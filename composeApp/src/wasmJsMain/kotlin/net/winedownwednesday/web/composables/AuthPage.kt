package net.winedownwednesday.web.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun UnderConstruction(
    onDismissRequest: () -> Unit
){
    Dialog( onDismissRequest = { onDismissRequest()} ){
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.size(
                300.dp
            )
        ) {
            Box() {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Card (
                    ) {
                        Text(
                            text = "This feature is under active development",
                            textAlign = TextAlign.Center
                        )
                    }
                }


            }
        }
    }
}