package com.example.mobilneprojekat.Pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.mobilneprojekat.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.mobilneprojekat.Model.FilterCriteria
import com.example.mobilneprojekat.Model.MyObject
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


suspend fun uploadImageToFirebase(imageUri: Uri): String {
    val storageReference = FirebaseStorage.getInstance().reference
    val imageRef = storageReference.child("images/${imageUri.lastPathSegment}")

    try {
        imageRef.putFile(imageUri).await() // Upload image
        val downloadUrl = imageRef.downloadUrl.await() // Get download URL
        return downloadUrl.toString()
    } catch (e: Exception) {
        // Handle upload exception
        throw e
    }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MapsPage(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val mapView = remember { MapView(context) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationErrorMessage by remember { mutableStateOf<String?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf(listOf<Uri>()) }

    var restaurantName by remember { mutableStateOf("") }
    var restaurantDescription by remember { mutableStateOf("") }
    var restaurantAddress by remember { mutableStateOf("") }
    var restaurantPhone by remember { mutableStateOf("") }
    var restaurantType by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterCriteria by remember { mutableStateOf<FilterCriteria?>(null) }

    val firestore = FirebaseFirestore.getInstance()
    val objectsCollection = firestore.collection("objects")

    fun saveObjectToFirestore(objekat: MyObject) {
        coroutineScope.launch {
            try {
                firestore.collection("objects").add(objekat).await()

                val userEmail = objekat.userEmail
                if (userEmail != null) {
                    if (userEmail.isNotEmpty()) {

                        // Get the user's document reference
                        val query = firestore.collection("users")
                            .whereEqualTo("email", userEmail)
                            .get()
                            .await()
                        Log.d("Firebase", "User document found $userEmail")

                        // Fetch the user document
                        if (query.documents.isNotEmpty()) {
                            val document = query.documents[0]
                            val userId = document.id
                            val currentPoints = document.getLong("points") ?: 0L

                            // Update the points field
                            firestore.collection("users").document(userId)
                                .update("points", currentPoints + 2)
                                .await()
                        } else {
                            // Handle case where user document does not exist
                            Log.e("Firestore", "User document does not exist")
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle Firestore exception
                throw e
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGiven ->
        if (isGiven) {
            currentLocationUpdates(context, fusedLocationClient) { location ->
                if (location != null) {
                    currentLocation = location
                    if (isMapReady) {
                        googleMap?.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), 15f
                            )
                        )
                        googleMap?.isMyLocationEnabled = true
                        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
                    }
                } else {
                    locationErrorMessage = "Not able to get location!"
                }
            }
        } else {
            locationErrorMessage = "Permission denied!"
        }
    }

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            currentLocationUpdates(context, fusedLocationClient) { location ->
                if (location != null) {
                    currentLocation = location
                    if (isMapReady) {
                        googleMap?.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), 15f
                            )
                        )
                        googleMap?.isMyLocationEnabled = true
                        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController, currentRoute = getCurrentRoute(navController)) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView.apply { onCreate(null); onResume() } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                update = { view ->
                    view.getMapAsync { map ->
                        googleMap = map
                        isMapReady = true

                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            googleMap?.isMyLocationEnabled = true
                            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
                        }

                        currentLocation?.let { location ->
                            val userLocation = LatLng(location.latitude, location.longitude)
                            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        }

                        filterCriteria?.let { criteria ->
                            googleMap?.clear()
                            objectsCollection.addSnapshotListener { snapshot, _ ->
                                snapshot?.let {
                                    for (doc in it.documents) {
                                        val lat = doc.getDouble("latitude") ?: 0.0
                                        val lng = doc.getDouble("longitude") ?: 0.0
                                        val title = doc.getString("name") ?: "No title"
                                        val address = doc.getString("address") ?: "No address"
                                        val type = doc.getString("type") ?: "No type"
                                        val phone = doc.getString("phone") ?: "No phone"
                                        val userEmail = doc.getString("userEmail") ?: "No email"
                                        val dateCreated = doc.getTimestamp("dateCreated")

                                        val position = LatLng(lat, lng)
                                        val markerId = doc.id

                                        val distance = currentLocation?.let { location ->
                                            calculateDistance(location.latitude, location.longitude, lat, lng)
                                        }

                                        val matchesUserEmail = criteria.userEmail?.let { it in userEmail } ?: true
                                        val matchesName = criteria.name?.let { it in title } ?: true
                                        val matchesType = criteria.type?.let { it in type } ?: true
                                        val matchesDateRange = criteria.dateRange?.let { (start, end) ->
                                            (start == null || dateCreated?.toDate()?.after(start.toDate()) == true) &&
                                                    (end == null || dateCreated?.toDate()?.before(end.toDate()) == true)
                                        } ?: true

                                        val withinRadius = criteria.radius?.let { distance != null && distance <= it } ?: true


                                        if (matchesUserEmail && matchesName && matchesDateRange && matchesType && withinRadius) {
                                            googleMap?.addMarker(
                                                MarkerOptions()
                                                    .position(position)
                                                    .title(title)
                                                    .snippet("Type: $type\nAddress: $address\nPhone: $phone")
                                            )?.tag = markerId
                                        }
                                    }
                                }
                            }
                        }
                        if(filterCriteria==null) {
                            objectsCollection.addSnapshotListener { snapshot, _ ->
                                snapshot?.let {
                                    for (doc in it.documents) {
                                        val lat = doc.getDouble("latitude") ?: 0.0
                                        val lng = doc.getDouble("longitude") ?: 0.0
                                        val title = doc.getString("name") ?: "No title"
                                        val address = doc.getString("address") ?: "No address"
                                        val type = doc.getString("type") ?: "No type"
                                        val phone = doc.getString("phone") ?: "No phone"
                                        val position = LatLng(lat, lng)
                                        val markerId = doc.id

                                        googleMap?.addMarker(
                                            MarkerOptions()
                                                .position(position)
                                                .title(title)
                                                .snippet("Type: $type\nAddress: $address\nPhone: $phone")
                                        )?.tag = markerId
                                    }
                                }
                            }
                        }

                        googleMap?.setOnMarkerClickListener { marker ->
                            val markerId = marker.tag as? String
                            if (markerId != null) {
                                // Navigate to the new screen, passing the marker ID
                                navController.navigate("details/$markerId")
                                Log.d("Marker", markerId)
                            }
                            true  // Return true to indicate that we have handled the click
                        }
                    }
                },
                onReset = {
                    mapView.onPause()
                    mapView.onStop()
                    mapView.onDestroy()
                }
            )

            locationErrorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(text = message)
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(end = 40.dp) // Adjust this to account for the built-in button
                    .padding(top = 25.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        showDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Marker")
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        showFilterDialog = true
                    },
                    modifier = Modifier
                        .size(40.dp)
                ) {
                    Icon(imageVector = ImageVector.vectorResource(id = R.drawable.baseline_filter_list_alt_24), contentDescription = "Filter")
                }
            }

            if (showFilterDialog) {
                FilterDialog(
                    onDismiss = { showFilterDialog = false },
                    onApplyFilter = { criteria ->
                        filterCriteria = criteria
                        showFilterDialog = false
                    }
                )
            }

            if (showDialog) {
                AddObjectDialog(
                    onDismiss = { showDialog = false },
                    onSave = {
                        if (currentLocation != null) {
                            val location = currentLocation!!
                            coroutineScope.launch {
                                try {
                                    // Upload all images concurrently and collect their URLs
                                    val imageUrlsDeferred = imageUris.map { uri ->
                                        coroutineScope.async {
                                            uploadImageToFirebase(uri)
                                        }
                                    }

                                    val imageUrls = imageUrlsDeferred.awaitAll() // Await all results

                                    val objekat = MyObject(
                                        id = objectsCollection.document().id,
                                        name = restaurantName,
                                        description = restaurantDescription,
                                        type = restaurantType,
                                        address = restaurantAddress,
                                        phone = restaurantPhone,
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        imageUrls = imageUrls, // List of image URLs
                                        userEmail = auth.currentUser?.email ?: "unknown",
                                        dateCreated = Timestamp.now()
                                    )


                                    saveObjectToFirestore(objekat)
                                    googleMap?.addMarker(
                                        MarkerOptions()
                                            .position(LatLng(location.latitude, location.longitude))
                                            .title(objekat.name)
                                            .snippet(
                                                "Description: ${objekat.description}\n" +
                                                        "Address: ${objekat.address}\n" +
                                                        "Phone: ${objekat.phone}\n" +
                                                        "Type: ${objekat.type}"
                                            )
                                    )
                                    googleMap?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))

                                } catch (e: Exception) {
                                    // Handle exceptions, e.g., show error message
                                }
                            }
                        }
                        showDialog = false
                    },
                    objectName = restaurantName,
                    onObjectNameChange = { restaurantName = it },
                    objectDescription = restaurantDescription,
                    onObjectDescriptionChange = { restaurantDescription = it },
                    objectAddress = restaurantAddress,
                    onObjectAddressChange = { restaurantAddress = it },
                    objectPhone = restaurantPhone,
                    onObjectPhoneChange = { restaurantPhone = it },
                    objectType = restaurantType,
                    onObjectTypeChange = { restaurantType = it },
                    imageUris = imageUris,
                    onImageUrisChange = { newUris -> imageUris = newUris }
                )
            }
        }
    }
}





// Funkcija za dobijanje trenutne lokacije
private fun currentLocationUpdates(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?) -> Unit
) {
    // Proveravamo da li aplikacija ima dozvolu za pristup lokaciji
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (hasLocationPermission) {
        // Ako je dozvola data, zatraži poslednju poznatu lokaciju
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            onLocationReceived(location)
        }
    } else {
        // Ako nema dozvolu, prosledi `null`
        onLocationReceived(null)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    // Opcije za izbor
    val options = listOf("Kafić", "Restoran", "Kafe-Restoran")
    var expanded by remember { mutableStateOf(false) }

    // `ExposedDropdownMenuBox` za prikaz padajućeg menija
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        // Tekstualni unos koji prikazuje izabrani tip
        TextField(
            readOnly = true,
            value = selectedType,
            onValueChange = {},
            label = { Text("Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
            ,
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        Log.d("Trazim", expanded.toString())

        // Padajući meni sa opcijama
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onTypeSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddObjectDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    objectName: String,
    onObjectNameChange: (String) -> Unit,
    objectDescription: String,
    onObjectDescriptionChange: (String) -> Unit,
    objectAddress: String,
    onObjectAddressChange: (String) -> Unit,
    objectPhone: String,
    onObjectPhoneChange: (String) -> Unit,
    objectType: String,
    onObjectTypeChange: (String) -> Unit,
    imageUris: List<Uri>,
    onImageUrisChange: (List<Uri>) -> Unit
) {
    var showImagePicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    // Validation state variables
    var nameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var typeError by remember { mutableStateOf<String?>(null) }

    // Validation and formatting functions
    fun formatAndValidateName(name: String): String {
        return if (name.isNotEmpty()) {
            // Capitalize the first letter
            name.capitalize()
        } else {
            name
        }
    }

    fun validateInput(): Boolean {
        var isValid = true

        // Validate restaurant name
        nameError = if (objectName.isBlank()) {
            isValid = false
            "Restaurant name cannot be empty"
        } else null

        // Validate description
        descriptionError = if (objectDescription.isBlank()) {
            isValid = false
            "Description cannot be empty"
        } else if (objectDescription.length < 10) {
            isValid = false
            "Description should be at least 10 characters long"
        } else null

        // Validate address
        addressError = if (objectAddress.isBlank()) {
            isValid = false
            "Address cannot be empty"
        } else null

        // Validate phone number (simple format validation)
        phoneError = if (objectPhone.isBlank()) {
            isValid = false
            "Phone number cannot be empty"
        } else if (!objectPhone.matches("\\d{10}".toRegex())) {
            isValid = false
            "Phone number must be 10 digits"
        } else null

        // Validate type
        typeError = if (objectType.isBlank()) {
            isValid = false
            "Type must be selected"
        } else null

        return isValid
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let { onImageUrisChange(it) }
        showImagePicker = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add object") },
        text = {
            Column {
                TextField(
                    value = objectName,
                    onValueChange = { input ->
                        val formattedInput = formatAndValidateName(input)
                        onObjectNameChange(formattedInput)
                        if (nameError != null) validateInput() // Validate on change
                    },
                    label = { Text("Restaurant Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null
                )
                nameError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showImagePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Images")
                }

                Spacer(modifier = Modifier.height(8.dp))

                imageUris.forEach { uri ->
                    // Display selected images URIs (replace with actual image loading logic)
                    Text(text = uri.toString(), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = objectDescription,
                    onValueChange = {input ->
                        val formattedInput = formatAndValidateName(input)
                        onObjectDescriptionChange(formattedInput)
                        if (descriptionError != null) validateInput() // Validate on change
                    },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    isError = descriptionError != null
                )
                descriptionError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = objectAddress,
                    onValueChange = {input ->
                        val formattedInput = formatAndValidateName(input)
                        onObjectAddressChange(formattedInput)
                        if (addressError != null) validateInput() // Validate on change
                    },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = addressError != null
                )
                addressError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = objectPhone,
                    onValueChange = {
                        onObjectPhoneChange(it)
                        if (phoneError != null) validateInput() // Validate on change
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = phoneError != null
                )
                phoneError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                RestaurantTypeDropdown(
                    selectedType = objectType,
                    onTypeSelected = { type ->
                        onObjectTypeChange(type)
                        if (typeError != null) validateInput() // Validate on change
                    }
                )
                typeError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateInput()) {
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                // Upload all images concurrently and collect their URLs
                                val imageUrlsDeferred = imageUris.map { uri ->
                                    coroutineScope.async {
                                        uploadImageToFirebase(uri)
                                    }
                                }

                                val imageUrls = imageUrlsDeferred.awaitAll() // Await all results

                                /*val objekat = Object(
                                    name = objectName,
                                    description = objectDescription,
                                    type = objectType,
                                    address = objectAddress,
                                    phone = objectPhone,
                                    latitude = 0.0, // Set required latitude
                                    longitude = 0.0, // Set required longitude
                                    imageUrls = imageUrls // List of image URLs
                                )*/

                                //saveObjectToFirestore(objekat)
                                onSave()
                                onDismiss()
                            } catch (e: Exception) {
                                // Handle exceptions, e.g., show error message
                            } finally {
                                isSaving = false // End saving
                            }
                        }
                    }
                },
                enabled = !isSaving
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showImagePicker) {
        imagePickerLauncher.launch("image/*")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    onApplyFilter: (FilterCriteria) -> Unit
) {
    var userEmail by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var radius by remember { mutableFloatStateOf(100f) }

    var expanded by remember { mutableStateOf(false) }
    val types = listOf("Kafić", "Restoran", "Kafe-Restoran")


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Options") },
        text = {
            Column {
                TextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
                    label = { Text("User email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Object Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = type,
                        onValueChange = { },
                        label = { Text("Object Type") },
                        readOnly = true,  // Make the TextField read-only
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .clickable { expanded = true }  // Toggle dropdown on click
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        types.forEach { item ->
                            DropdownMenuItem(
                                text = {Text(item)},
                                onClick = {
                                    type = item  // Set the selected type
                                    expanded = false  // Close the dropdown
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Select Radius (meters): ${radius.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                    letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing
                ))
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 0f..5000f, // Range from 0 to 5000 meters (5 km)
                    steps = 4999, // Define steps if needed
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Add Date Picker for start date
                DatePicker(
                    label = "Start Date",
                    selectedDate = startDate,
                    onDateChange = { startDate = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                DatePicker(
                    label = "End Date",
                    selectedDate = endDate,
                    onDateChange = { endDate = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyFilter(
                        FilterCriteria(
                            userEmail = userEmail,
                            name = name,
                            type = type,
                            dateRange = Pair(
                                startDate?.let { Timestamp(it) },
                                endDate?.let { Timestamp(it) }
                            ),
                            radius = radius
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun DatePicker(
    label: String,
    selectedDate: Date?,
    onDateChange: (Date?) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateChange(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    OutlinedButton(onClick = { datePickerDialog.show() }) {
        Text(text = if (selectedDate != null) dateFormat.format(selectedDate) else label)
    }
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // Earth's radius in meters

    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLat = Math.toRadians(lat2 - lat1)
    val deltaLon = Math.toRadians(lon2 - lon1)

    val a = sin(deltaLat / 2).pow(2) +
            cos(lat1Rad) * cos(lat2Rad) * sin(deltaLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c // distance in meters
}


