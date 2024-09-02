package com.example.mobilneprojekat.Pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mobilneprojekat.Model.User
import com.example.mobilneprojekat.R
import com.example.mobilneprojekat.ui.theme.Bronze
import com.example.mobilneprojekat.ui.theme.Gold
import com.example.mobilneprojekat.ui.theme.Silver
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserWithPosition(
    val user: User,
    val position: Int
)


@Composable
fun LeaderboardPage(modifier: Modifier = Modifier, navController: NavController) {

    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var usersWithPosition by remember { mutableStateOf<List<UserWithPosition>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val usersSnapshot = firestore.collection("users")
                    .orderBy("points", Query.Direction.DESCENDING)
                    .get().await()

                val fetchedUsers = usersSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }

                // Determine positions and handle ties
                val sortedUsers = fetchedUsers.sortedByDescending { it.points }
                val userWithPosition = mutableListOf<UserWithPosition>()
                var currentPosition = 1
                var lastPoints = -1
                var lastPosition = 0 // To handle the case where multiple users share the same rank

                for ((index, user) in sortedUsers.withIndex()) {
                    if (user.points != lastPoints) {
                        // Update the position for users with different points
                        currentPosition = lastPosition + 1
                        lastPosition = currentPosition
                    } else {
                        // For users with the same score, use the last position
                        currentPosition = lastPosition
                    }
                    lastPoints = user.points

                    userWithPosition.add(UserWithPosition(user = user, position = currentPosition))
                }

                usersWithPosition = userWithPosition
                Log.d("lider", "evo")

            } catch (e: Exception) {
                errorMessage = "Failed to load leaderboard!"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, currentRoute = getCurrentRoute(navController))
        },
        content = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Table header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Position", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Email", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Points", style = MaterialTheme.typography.titleMedium)
                        }

                        // Divider
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()

                        // Table content
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(usersWithPosition) { userWithPosition ->
                                LeaderboardItem(user = userWithPosition.user, position = userWithPosition.position)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun LeaderboardItem(user: User, position: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Position
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )

        // Trophy Icon
        if (position <= 3) {
            TrophyIcon(position = position)
        } else {
            Spacer(modifier = Modifier.size(24.dp)) // Space for trophy icon
        }

        // Email
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            fontSize = 18.sp
        )

        // Points
        Text(
            text = user.points.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End,
            fontSize = 18.sp
        )
    }
}

@Composable
fun TrophyIcon(position: Int) {

    val color = when (position) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> Color.Transparent // Default color if not in the top 3
    }

    val iconResId = when (position) {
        1 -> R.drawable.trophy_24
        2 -> R.drawable.trophy_24
        3 -> R.drawable.trophy_24
        else -> null
    }

    iconResId?.let {
        Image(
            painter = painterResource(id = it),
            contentDescription = "Trophy for position $position",
            modifier = Modifier.size(26.dp),
            colorFilter = ColorFilter.tint(color)
        )
    }
}
