/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.lunchtray

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lunchtray.datasource.DataSource
import com.example.lunchtray.ui.AccompanimentMenuScreen
import com.example.lunchtray.ui.CheckoutScreen
import com.example.lunchtray.ui.EntreeMenuScreen
import com.example.lunchtray.ui.OrderViewModel
import com.example.lunchtray.ui.SideDishMenuScreen
import com.example.lunchtray.ui.StartOrderScreen
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @get:StringRes
    val title: Int
}

sealed class LunchTrayScreen : Screen {
    @Serializable
    data class Start(override val title: Int = R.string.start_order) : LunchTrayScreen()

    @Serializable
    data object Entree : LunchTrayScreen() {
        override val title: Int
            get() = R.string.choose_entree
    }

    @Serializable
    data object SideDish : LunchTrayScreen() {
        override val title: Int
            get() = R.string.choose_side_dish
    }

    @Serializable
    data object Accompaniment : LunchTrayScreen() {
        override val title: Int
            get() = R.string.choose_accompaniment
    }

    @Serializable
    data object Checkout : LunchTrayScreen() {
        override val title: Int
            get() = R.string.order_checkout
    }
}


fun NavBackStackEntry?.fromRoute(): LunchTrayScreen {
    this?.destination?.route?.substringBefore("?")?.substringBefore("/")
        ?.substringAfterLast(".")?.let {
            println("the new class $it")
            when (it) {
                LunchTrayScreen.Start::class.simpleName -> return LunchTrayScreen.Start()
                LunchTrayScreen.Entree::class.simpleName -> return LunchTrayScreen.Entree
                LunchTrayScreen.SideDish::class.simpleName -> return LunchTrayScreen.SideDish
                LunchTrayScreen.Accompaniment::class.simpleName -> return LunchTrayScreen.Accompaniment
                LunchTrayScreen.Checkout::class.simpleName -> return LunchTrayScreen.Checkout
                else -> LunchTrayScreen.Start
            }
        }
    return LunchTrayScreen.Start()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    currentScreen: LunchTrayScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(id = currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back_button)
                    )
                }
            }
        },
        modifier = modifier
    )

}

@Composable
fun LunchTrayApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = backStackEntry.fromRoute()

    // Create ViewModel
    val viewModel: OrderViewModel = viewModel()

    Scaffold(topBar = {
        AppBar(currentScreen = currentScreen,
            canNavigateBack = navController.previousBackStackEntry != null,
            navigateUp = { navController.navigateUp() })
    }) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        NavHost(
            navController = navController,
            startDestination = LunchTrayScreen.Start(),
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LunchTrayScreen.Start> {
                StartOrderScreen(
                    onStartOrderButtonClicked = {
                        navController.navigate(LunchTrayScreen.Entree)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(id = R.dimen.padding_medium))
                )
            }
            composable<LunchTrayScreen.Entree> {
                EntreeMenuScreen(
                    options = DataSource.entreeMenuItems,
                    onCancelButtonClicked = {
                        cancelLunchAndNavigateToStart(
                            navController, viewModel
                        )
                    },
                    onNextButtonClicked = { navController.navigate(LunchTrayScreen.SideDish) },
                    onSelectionChanged = { viewModel.updateEntree(it) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
            composable<LunchTrayScreen.SideDish> {
                SideDishMenuScreen(
                    options = DataSource.sideDishMenuItems,
                    onCancelButtonClicked = {
                        cancelLunchAndNavigateToStart(
                            navController,
                            viewModel
                        )
                    },
                    onNextButtonClicked = { navController.navigate(LunchTrayScreen.Accompaniment) },
                    onSelectionChanged = { viewModel.updateSideDish(it) },
                    modifier = Modifier.fillMaxHeight()
                )
            }

            composable<LunchTrayScreen.Accompaniment> {
                AccompanimentMenuScreen(
                    options = DataSource.accompanimentMenuItems,
                    onCancelButtonClicked = {
                        cancelLunchAndNavigateToStart(
                            navController,
                            viewModel
                        )
                    },
                    onNextButtonClicked = { navController.navigate(LunchTrayScreen.Checkout) },
                    onSelectionChanged = { viewModel.updateAccompaniment(it) },
                    modifier = Modifier.fillMaxHeight()
                )
            }

            composable<LunchTrayScreen.Checkout> {
                CheckoutScreen(
                    orderUiState = uiState,
                    onNextButtonClicked = {
                        cancelLunchAndNavigateToStart(
                            navController,
                            viewModel
                        )
                    },
                    onCancelButtonClicked = {
                        cancelLunchAndNavigateToStart(
                            navController,
                            viewModel
                        )
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }

    }


}

fun cancelLunchAndNavigateToStart(
    navController: NavController, viewModel: OrderViewModel
) {
    viewModel.resetOrder()
    navController.popBackStack(LunchTrayScreen.Start(), inclusive = false)
}

