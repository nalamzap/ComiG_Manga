package dev.nalamzap.comig.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dev.nalamzap.comig.feature.home.HomeScreen
import dev.nalamzap.comig.feature.library.LibraryScreen
import dev.nalamzap.comig.feature.reader.ReaderScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                onComicClick = { uri ->
                    navController.navigate("${NavRoutes.READER}/${Uri.encode(uri.toString())}")
                },
                onViewLibraryClick = { navController.navigate(NavRoutes.LIBRARY) }
            )
        }

        composable(NavRoutes.LIBRARY) {
            LibraryScreen(
                onComicClick = { uri ->
                    navController.navigate("${NavRoutes.READER}/${Uri.encode(uri.toString())}")
                }
            )
        }

        composable(
            route = "${NavRoutes.READER}/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) {
            val uri = Uri.parse(it.arguments!!.getString("uri")!!)
            ReaderScreen(
                comicUri = uri,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
