package com.example.mobilneprojekat.Pages

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mobilneprojekat.Model.MyObject
import com.example.mobilneprojekat.Model.Rate
import com.example.mobilneprojekat.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat
import com.example.mobilneprojekat.ui.theme.Gold
import com.google.firebase.firestore.FieldValue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsPage(
    docId: String,
    navController: NavController
) {
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var objectDetails by remember { mutableStateOf<MyObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedRating by remember { mutableIntStateOf(0) }
    var averageRating by remember { mutableStateOf<Double?>(null) }
    var userHasRated by remember { mutableStateOf(false) }
    val userEmail = FirebaseAuth.getInstance().currentUser?.email
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var ratings by remember { mutableStateOf<List<Rate>>(emptyList()) }

    LaunchedEffect(docId) {
        coroutineScope.launch {
            try {
                Log.d("objekat", docId)
                val docSnapshot = firestore.collection("objects").document(docId).get().await()
                if (docSnapshot.exists()) {
                    objectDetails = docSnapshot.toObject(MyObject::class.java)
                    Log.d("detalji", objectDetails?.imageUrls?.get(0) ?: "")

                    // Fetch ratings
                    val ratingsSnapshot = firestore.collection("ratings")
                        .whereEqualTo("objectId", docId)
                        .get().await()

                    ratings = ratingsSnapshot.documents.mapNotNull { it.toObject(Rate::class.java) }

                    if (ratings.isNotEmpty()) {
                        averageRating = ratings.map { it.value.toDouble() }.average()
                    }

                    // Check if the current user has already rated
                    userHasRated = ratings.any { it.userEmail == userEmail }


                } else {
                    errorMessage = "Object not found!"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load details!"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Object Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                objectDetails?.let { details ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),  // Adjust padding if needed
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${details.name}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold // Set to bold for thicker text
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        Text(
                            text = "Pictures:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(modifier = Modifier.padding(bottom = 16.dp)) {
                            items(objectDetails!!.imageUrls) { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Slike objekata",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(end = 8.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Type:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "${details.type}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Description:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "${details.description}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Address:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "${details.address}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Phone Number:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = "${details.phone}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val decimalFormat = DecimalFormat("#.##")

                        Row(
                            verticalAlignment = Alignment.CenterVertically, // Align text vertically
                            horizontalArrangement = Arrangement.Start // Arrange items from start to end
                        ) {
                            Text(
                                text = "Average Rating: ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = decimalFormat.format(averageRating ?: 0.0),
                                color = getRatingColor(averageRating ?: 0.0),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.baseline_star_24),
                                contentDescription = "star",
                                tint = getRatingColor(averageRating ?: 0.0)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rating button logic
                        if (!userHasRated) {
                            StarRatingBar(
                                currentRating = selectedRating,
                                onRatingChanged = { rating ->
                                    selectedRating = rating
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = {
                                coroutineScope.launch {
                                    try {
                                        // Add rating to Firestore
                                        val rate = Rate(
                                            value = selectedRating,
                                            userEmail = userEmail ?: "",
                                            objectId = docId
                                        )
                                        firestore.collection("ratings")
                                            .document("${userEmail}_${docId}") // Unique ID per user-object pair
                                            .set(rate)
                                            .await()

                                        // Update state to prevent further ratings
                                        userHasRated = true
                                        // Update the average rating locally (simple average adjustment for new rating)
                                        averageRating = if (averageRating == null) {
                                            selectedRating.toDouble()
                                        } else {
                                            ((averageRating!! * ratings.size) + selectedRating) / (ratings.size + 1)
                                        }

                                        userId?.let {
                                            val userRef = firestore.collection("users").document(it)
                                            userRef.update("points", FieldValue.increment(1)).await()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Firestore", "Failed to add rating", e)
                                    }
                                }
                            }) {
                                Text("Submit Rating")
                            }
                        } else {
                            Text(
                                text = "You have already rated this restaurant.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                    }
                }
            }
        }
    }
}


@Composable
fun StarRatingBar(
    currentRating: Int,
    onRatingChanged: (Int) -> Unit,
    maxRating: Int = 5
) {
    Row {
        for (i in 1..maxRating) {
            Icon(
                imageVector = if (i <= currentRating) Icons.Filled.Star else ImageVector.vectorResource(id = R.drawable.baseline_star_outline_24),
                contentDescription = if (i <= currentRating) "Filled Star" else "Outlined Star",
                tint = Gold,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onRatingChanged(i) } // Clickable to update rating
            )
        }
    }
}