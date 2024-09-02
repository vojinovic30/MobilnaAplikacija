package com.example.mobilneprojekat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mobilneprojekat.Pages.DetailsPage
import com.example.mobilneprojekat.Pages.HomePage
import com.example.mobilneprojekat.Pages.LeaderboardPage
import com.example.mobilneprojekat.Pages.LoginPage
import com.example.mobilneprojekat.Pages.MapsPage
import com.example.mobilneprojekat.Pages.RegisterPage
import com.example.mobilneprojekat.Pages.TablePage

@Composable
fun Navigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login", builder = {
        composable("login"){
            LoginPage(modifier, navController, authViewModel)
        }
        composable("register"){
            RegisterPage(modifier, navController, authViewModel)
        }
        composable("home"){
            HomePage(modifier, navController, authViewModel)
        }
        composable("map") {
            MapsPage(navController)
        }
        composable("leaderboard") {
            LeaderboardPage(modifier, navController)
        }
        composable("details/{docId}") { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId") ?: ""
            DetailsPage(docId = docId, navController = navController)
        }
        composable("table"){
            TablePage(navController)
        }
    })

}