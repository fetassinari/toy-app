package com.toyregistry.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toyregistry.app.ui.auth.AuthScreen
import com.toyregistry.app.ui.auth.AuthViewModel
import com.toyregistry.app.ui.category.CategoryScreen
import com.toyregistry.app.ui.home.HomeScreen
import com.toyregistry.app.ui.toy.AddEditToyScreen

@Composable
fun ToyRegistryNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    val startDestination = if (isLoggedIn) NavRoutes.HOME else NavRoutes.AUTH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onAddToy = { navController.navigate(NavRoutes.ADD_TOY) },
                onEditToy = { toyId -> navController.navigate(NavRoutes.editToy(toyId)) },
                onManageCategories = { navController.navigate(NavRoutes.CATEGORIES) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.ADD_TOY) {
            AddEditToyScreen(
                toyId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.EDIT_TOY,
            arguments = listOf(navArgument("toyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val toyId = backStackEntry.arguments?.getString("toyId")
            AddEditToyScreen(
                toyId = toyId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.CATEGORIES) {
            CategoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
