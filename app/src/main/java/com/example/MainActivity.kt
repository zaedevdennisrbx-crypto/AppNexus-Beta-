package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.data.model.AdStatus
import com.example.data.repository.AiScoutRepository
import com.example.data.repository.StoreRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AiScoutScreen
import com.example.ui.screens.AppDetailScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AppNexusTheme
import com.example.ui.theme.ElectricCyan
import com.example.ui.viewmodel.AiScoutViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DISCOVER = "discover"
const val ROUTE_SCOUT = "scout"
const val ROUTE_CREATE = "create"
const val ROUTE_LIBRARY = "library"
const val ROUTE_PROFILE = "profile"
const val ROUTE_ADMIN = "admin"
const val ROUTE_APP_DETAIL = "app_detail/{appId}"

private data class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    NavDestination(
        route = ROUTE_DISCOVER,
        label = "Discover",
        selectedIcon = Icons.Rounded.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_discover"
    ),
    NavDestination(
        route = ROUTE_SCOUT,
        label = "AI Scout",
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_scout"
    ),
    NavDestination(
        route = ROUTE_CREATE,
        label = "Create",
        selectedIcon = Icons.Rounded.AddBox,
        unselectedIcon = Icons.Outlined.AddBox,
        testTag = "nav_create"
    ),
    NavDestination(
        route = ROUTE_LIBRARY,
        label = "Library",
        selectedIcon = Icons.Rounded.CollectionsBookmark,
        unselectedIcon = Icons.Outlined.CollectionsBookmark,
        testTag = "nav_library"
    ),
    NavDestination(
        route = ROUTE_PROFILE,
        label = "Profile",
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppNexusTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainAppShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppShell(user: FirebaseUser) {
    val uid = user.uid
    val displayName = user.displayName ?: "AppNexus Explorer"
    val email = user.email
    val isEmailVerified = user.isEmailVerified
    val photoUrl = user.photoUrl?.toString()

    val storeViewModel: StoreViewModel = viewModel(
        key = "store_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(
                    repository = StoreRepository(db),
                    currentUserId = uid,
                    userDisplayName = displayName,
                    userEmail = email,
                    userEmailVerified = isEmailVerified,
                    userPhotoUrl = photoUrl
                )
            }
        }
    )

    val aiScoutViewModel: AiScoutViewModel = viewModel(
        key = "scout_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AiScoutViewModel(
                    aiRepository = AiScoutRepository(),
                    storeRepository = StoreRepository(db),
                    currentUserId = uid
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isStandaloneScreen = currentRoute?.startsWith("app_detail") == true || currentRoute == ROUTE_ADMIN

    val snackbarHostState = remember { SnackbarHostState() }
    val actionMessage by storeViewModel.actionMessage.collectAsStateWithLifecycle()
    val isAdmin by storeViewModel.isAdmin.collectAsStateWithLifecycle()
    val adminAdsState by storeViewModel.adminAdsState.collectAsStateWithLifecycle()
    val pendingAdsCount = when (val s = adminAdsState) {
        is UiState.Success -> s.data.count { it.statusEnum() == AdStatus.PENDING }
        else -> 0
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            storeViewModel.clearActionMessage()
        }
    }

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openDetail: (String) -> Unit = { appId ->
        navController.navigate("app_detail/${Uri.encode(appId)}")
    }

    val openAdmin: () -> Unit = {
        navController.navigate(ROUTE_ADMIN) {
            launchSingleTop = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                if (!isStandaloneScreen) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.appnexus_icon_fg),
                                    contentDescription = "AppNexus icon",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                )
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            if (isAdmin) {
                                IconButton(
                                    onClick = openAdmin,
                                    modifier = Modifier.testTag("topbar_admin_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (pendingAdsCount > 0) {
                                                Badge(containerColor = AmberGold) {
                                                    Text(pendingAdsCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AdminPanelSettings,
                                            contentDescription = "Open Admin Panel",
                                            tint = AmberGold
                                        )
                                    }
                                }
                            }
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Open profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(end = 14.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable { navigateTab(ROUTE_PROFILE) }
                                        .testTag("topbar_profile_avatar")
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail && !isStandaloneScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && !isStandaloneScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.testTag}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DISCOVER,
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        composable(ROUTE_DISCOVER) {
                            DiscoverScreen(
                                storeViewModel = storeViewModel,
                                onOpenAppDetail = openDetail,
                                onOpenAiScout = { navigateTab(ROUTE_SCOUT) },
                                onOpenCreate = { navigateTab(ROUTE_CREATE) }
                            )
                        }
                        composable(ROUTE_SCOUT) {
                            AiScoutScreen(
                                aiScoutViewModel = aiScoutViewModel,
                                storeViewModel = storeViewModel,
                                onOpenAppDetail = openDetail
                            )
                        }
                        composable(ROUTE_CREATE) {
                            CreateScreen(
                                currentUserId = uid,
                                defaultCreatorName = displayName,
                                storeViewModel = storeViewModel,
                                onOpenAppDetail = openDetail
                            )
                        }
                        composable(ROUTE_LIBRARY) {
                            LibraryScreen(
                                storeViewModel = storeViewModel,
                                onOpenAppDetail = openDetail,
                                onNavigateDiscover = { navigateTab(ROUTE_DISCOVER) }
                            )
                        }
                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                currentUserId = uid,
                                displayName = displayName,
                                email = email,
                                photoUrl = photoUrl,
                                storeViewModel = storeViewModel,
                                onOpenAppDetail = openDetail,
                                onOpenCreate = { navigateTab(ROUTE_CREATE) },
                                onOpenAdmin = openAdmin
                            )
                        }
                        composable(ROUTE_ADMIN) {
                            AdminScreen(
                                storeViewModel = storeViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = ROUTE_APP_DETAIL,
                            arguments = listOf(navArgument("appId") { type = NavType.StringType })
                        ) { entry ->
                            val rawId = entry.arguments?.getString("appId") ?: ""
                            AppDetailScreen(
                                appId = Uri.decode(rawId),
                                currentUserId = uid,
                                storeViewModel = storeViewModel,
                                aiScoutViewModel = aiScoutViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
