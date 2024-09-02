package com.example.mobilneprojekat.Pages

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mobilneprojekat.AuthState
import com.example.mobilneprojekat.AuthViewModel
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilneprojekat.NotificationViewModel
import com.example.mobilneprojekat.NotificationViewModelFactory
import com.example.mobilneprojekat.R
import com.example.mobilneprojekat.Service.NotificationService


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val showSettingsDialog = remember { mutableStateOf(false) }
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(context)
    )

    val isNotificationEnabled by notificationViewModel.isNotificationEnabled.observeAsState(false)

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            //navController.navigate("login")
            navController.navigate("login") {
                // Clear the back stack
                popUpTo("home") { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            notificationViewModel.setNotificationEnabled(false)
            stopNotificationService(context)
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, currentRoute = getCurrentRoute(navController))
        },
        content = { paddingValues ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Section
                    Text(
                        text = "Welcome to Foodie Haven!",
                        fontSize = 32.sp,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )

                    // Image Row Section
                    ImageRowSection()

                    // Welcome Message Section
                    Text(
                        text = "Discover the best cafés, restaurants, and more in town. From cozy cafes to gourmet dining, explore options that suit your taste!",
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Top-right corner actions
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    }

                    IconButton(onClick = { showSettingsDialog.value = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }

                // Settings Dialog
                if (showSettingsDialog.value) {
                    SettingsAlertDialog(
                        isOpen = showSettingsDialog.value,
                        notificationViewModel = notificationViewModel,
                        onDismiss = { showSettingsDialog.value = false },
                        isNotificationEnabled = isNotificationEnabled,
                        hasLocationPermission = hasLocationPermission
                    )
                }
            }
        }
    )
}

@Composable
fun ImageRowSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // First image
        Image(
            painter = painterResource(id = R.drawable.baseline_local_cafe_24),
            contentDescription = "Welcome to Foodie Haven",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .padding(end = 8.dp)
        )

        // Second image
        Image(
            painter = painterResource(id = R.drawable.baseline_restaurant_24),
            contentDescription = "Explore our restaurant selection",
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .height(150.dp)
                .padding(start = 8.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = {
                if (currentRoute != "home") {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") },
            label = { Text("Map") },
            selected = currentRoute == "map",
            onClick = {
                if (currentRoute != "map") {
                    navController.navigate("map") {
                        popUpTo("map") { inclusive = true }
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(imageVector = ImageVector.vectorResource(id = R.drawable.baseline_table_view_24), contentDescription = "Table") },  // Replace with a suitable icon
            label = { Text("Table") },
            selected = currentRoute == "table",
            onClick = {
                if (currentRoute != "table") {
                    navController.navigate("table") {
                        popUpTo("table") { inclusive = true }
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Star, contentDescription = "Leaderboard") },
            label = { Text("Leaderboard") },
            selected = currentRoute == "leaderboard",
            onClick = {
                if (currentRoute != "leaderboard") {
                    navController.navigate("leaderboard") {
                        popUpTo("leaderboard") { inclusive = true }
                    }
                }
            }
        )
    }
}

@Composable
fun getCurrentRoute(navController: NavController): String? {
    val backStackEntry by navController.currentBackStackEntryAsState()
    return backStackEntry?.destination?.route
}

private fun startNotificationService(context: Context) {
    val intent = Intent(context, NotificationService::class.java)
    ContextCompat.startForegroundService(context, intent)
}

private fun stopNotificationService(context: Context) {
    val intent = Intent(context, NotificationService::class.java)
    context.stopService(intent)
}

@Composable
fun SettingsAlertDialog(
    isOpen: Boolean,
    notificationViewModel: NotificationViewModel,
    onDismiss: () -> Unit,
    isNotificationEnabled: Boolean,
    hasLocationPermission: Boolean
) {
    val context = LocalContext.current
    
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Enable Notifications", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isNotificationEnabled && hasLocationPermission,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (hasLocationPermission) {
                                        notificationViewModel.setNotificationEnabled(true)
                                        startNotificationService(context)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Location permission required",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Reset switch to OFF
                                        notificationViewModel.setNotificationEnabled(false)
                                    }
                                } else {
                                    notificationViewModel.setNotificationEnabled(false)
                                    stopNotificationService(context)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}