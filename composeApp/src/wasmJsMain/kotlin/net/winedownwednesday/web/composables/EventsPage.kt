package net.winedownwednesday.web.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.winedownwednesday.web.data.Event
import net.winedownwednesday.web.data.EventType

@Composable
fun EventsPage() {
    WebEventsScreen()
}

@Composable
fun WebEventsScreen(
) {
//    val upcomingEvents by viewModel.upcomingEvents.collectAsState()
//    val pastEvents by viewModel.pastEvents.collectAsState()

    var showUpcoming by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF141414))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Row {
                Button(
                    onClick = { showUpcoming = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showUpcoming) Color(0xFFFF7F33) else Color(0xFF2A2A2A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Upcoming Events")
                }

                Button(
                    onClick = { showUpcoming = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showUpcoming) Color(0xFFFF7F33) else Color(0xFF2A2A2A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Past Events")
                }
            }
        }

        val eventsToDisplay = if (showUpcoming) upcomingEvents else pastEvents
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(eventsToDisplay) { event ->
                EventCard(event)
            }
        }
    }
}

@Composable
fun EventCard(event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
//                AsyncImage(
//                    model = event.imageUrl,
//                    contentDescription = null,
//                )

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Location: ${event.location}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        // handle RSVP logic
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RSVP")
                }
            }
        }
    }
}

val upcomingEvents =
    listOf(
        Event(
            id = 1,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 2,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 3,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 4,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 5,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 6,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 7,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 8,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 9,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        ),
        Event(
            id = 10,
            name = "Wine Tasting Event",
            date = "Friday, August 20th",
            time = "6:00 PM",
            eventType = EventType.HOLIDAY_EVENT,
            description = "Join us for a night of wine tasting and fun!",
            imageUrl = "wine_placeholder.png",
            wineSelection = "Cabernet Sauvignon",
            wineSelector = "John Doe",
            registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
            location = "1234 Wine St, Napa, CA",
            additionalInfo = "Must be 21+ to attend",
            gallery = emptyList()
        )
    )

val pastEvents = listOf(
    Event(
        id = 2,
        name = "Wine Tasting Event",
        date = "Friday, August 20th",
        time = "6:00 PM",
        eventType = EventType.HOLIDAY_EVENT,
        description = "Join us for a night of wine tasting and fun!",
        imageUrl = "wine_placeholder.png",
        wineSelection = "Cabernet Sauvignon",
        wineSelector = "John Doe",
        registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
        location = "1234 Wine St, Napa, CA",
        additionalInfo = "Must be 21+ to attend",
        gallery = emptyList()
    ),
    Event(
        id = 3,
        name = "Wine Tasting Event",
        date = "Friday, August 20th",
        time = "6:00 PM",
        eventType = EventType.HOLIDAY_EVENT,
        description = "Join us for a night of wine tasting and fun!",
        imageUrl = "wine_placeholder.png",
        wineSelection = "Cabernet Sauvignon",
        wineSelector = "John Doe",
        registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
        location = "1234 Wine St, Napa, CA",
        additionalInfo = "Must be 21+ to attend",
        gallery = emptyList()
    ),
    Event(
        id = 4,
        name = "Wine Tasting Event",
        date = "Friday, August 20th",
        time = "6:00 PM",
        eventType = EventType.HOLIDAY_EVENT,
        description = "Join us for a night of wine tasting and fun!",
        imageUrl = "wine_placeholder.png",
        wineSelection = "Cabernet Sauvignon",
        wineSelector = "John Doe",
        registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
        location = "1234 Wine St, Napa, CA",
        additionalInfo = "Must be 21+ to attend",
        gallery = emptyList()
    ),
    Event(
        id = 5,
        name = "Wine Tasting Event",
        date = "Friday, August 20th",
        time = "6:00 PM",
        eventType = EventType.HOLIDAY_EVENT,
        description = "Join us for a night of wine tasting and fun!",
        imageUrl = "wine_placeholder.png",
        wineSelection = "Cabernet Sauvignon",
        wineSelector = "John Doe",
        registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
        location = "1234 Wine St, Napa, CA",
        additionalInfo = "Must be 21+ to attend",
        gallery = emptyList()
    ),
    Event(
        id = 6,
        name = "Wine Tasting Event",
        date = "Friday, August 20th",
        time = "6:00 PM",
        eventType = EventType.HOLIDAY_EVENT,
        description = "Join us for a night of wine tasting and fun!",
        imageUrl = "wine_placeholder.png",
        wineSelection = "Cabernet Sauvignon",
        wineSelector = "John Doe",
        registrationLink = "https://www.eventbrite.com/e/wine-tasting-tickets-1234567890",
        location = "1234 Wine St, Napa, CA",
        additionalInfo = "Must be 21+ to attend",
        gallery = emptyList()
    ),

)

