package com.example.mobilneprojekat.Pages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mobilneprojekat.Model.MyObject
import com.example.mobilneprojekat.Model.Rate
import com.example.mobilneprojekat.ui.theme.ExcellentRatingColor
import com.example.mobilneprojekat.ui.theme.HighRatingColor
import com.example.mobilneprojekat.ui.theme.LowRatingColor
import com.example.mobilneprojekat.ui.theme.MediumRatingColor
import com.example.mobilneprojekat.ui.theme.PoorRatingColor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat


data class MyObjectWithRating(
    val myObject: MyObject,
    val averageRating: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablePage(
    navController: NavController
) {
    val firestore = FirebaseFirestore.getInstance()
    val objectsWithRatings = remember { mutableStateOf<List<MyObjectWithRating>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val objectsSnapshot = firestore.collection("objects").get().await()
            val objects = objectsSnapshot.documents.map { doc ->
                val myObject = doc.toObject(MyObject::class.java)
                val objectId = doc.id  // Get the document ID as object ID

                // Fetch ratings for each object
                val ratingsSnapshot = firestore.collection("ratings")
                    .whereEqualTo("objectId", objectId)
                    .get().await()

                val ratings = ratingsSnapshot.documents.mapNotNull { it.toObject(Rate::class.java) }
                val averageRating = if (ratings.isNotEmpty()) {
                    ratings.map { it.value.toDouble() }.average()
                } else {
                    0.0  // Default to 0 if no ratings
                }

                MyObjectWithRating(myObject!!, averageRating)
            }

            // Update the state with fetched objects and their ratings
            objectsWithRatings.value = objects
        } catch (e: Exception) {
            // Handle any errors
            e.printStackTrace()
        } finally {
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Name",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            textAlign = TextAlign.Center

                        )
                        Text(
                            text = "Type",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Address",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Phone",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Rating",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController, currentRoute = getCurrentRoute(navController))
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(objectsWithRatings.value.sortedByDescending { it.averageRating }) { objWithRating ->
                        TableRow(objectDetails = objWithRating.myObject, averageRating = objWithRating.averageRating)
                        Divider(
                            color = Color.Gray,
                            thickness = 2.dp,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun TableRow(objectDetails: MyObject, averageRating: Double) {
    val decimalFormat = DecimalFormat("#.##")
    val ratingColor = getRatingColor(averageRating)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),  // Padding around the row
        horizontalArrangement = Arrangement.spacedBy(4.dp),  // Space between cells
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(2f)
                .background(MaterialTheme.colorScheme.background)
                .wrapContentWidth()  // Ensure the width is flexible
        ) {
            Text(
                text = objectDetails.name ?: "",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())  // Make the text scrollable horizontally
                    .padding(8.dp)  // Padding inside the text box
                    .fillMaxWidth(),// Ensure text fills the box width
                textAlign = TextAlign.Start,  // Align text to the start
                softWrap = false  // Prevent text from wrapping to the next line
            )
        }

        //DataCell(text = objectDetails.name ?: "", modifier = Modifier.weight(2f))
        DataCell(text = objectDetails.type ?: "", modifier = Modifier.weight(1f))
        DataCell(text = objectDetails.address ?: "", modifier = Modifier.weight(3f))
        DataCell(text = objectDetails.phone ?: "", modifier = Modifier.weight(2f))

        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .wrapContentWidth()  // Ensure the width is flexible
        ) {
            Text(
                text = decimalFormat.format(averageRating),
                color = ratingColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())  // Make the text scrollable horizontally
                    .padding(8.dp)  // Padding inside the text box
                    .fillMaxWidth(),// Ensure text fills the box width
                textAlign = TextAlign.Start,  // Align text to the start
                softWrap = false  // Prevent text from wrapping to the next line
            )
        }
        //DataCell(text = decimalFormat.format(averageRating), modifier = Modifier.weight(1f))
    }
}

fun getRatingColor(rating: Double): Color {
    return when {
        rating > 4.5 -> ExcellentRatingColor
        rating in 4.0..4.5 -> HighRatingColor
        rating in 3.0..3.9 -> MediumRatingColor
        rating in 2.0..2.9 -> LowRatingColor
        else -> PoorRatingColor
    }
}

@Composable
fun DataCell(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .wrapContentWidth()  // Ensure the width is flexible
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())  // Make the text scrollable horizontally
                .padding(8.dp)  // Padding inside the text box
                .fillMaxWidth(),  // Ensure text fills the box width
            textAlign = TextAlign.Start,  // Align text to the start
            softWrap = false  // Prevent text from wrapping to the next line
        )
    }
}