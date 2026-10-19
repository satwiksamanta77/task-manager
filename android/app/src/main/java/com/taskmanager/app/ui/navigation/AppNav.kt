package com.taskmanager.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskmanager.app.data.api.ApiClient
import com.taskmanager.app.data.local.SessionManager
import com.taskmanager.app.data.repository.AuthRepository
import com.taskmanager.app.data.repository.LabelRepository
import com.taskmanager.app.data.repository.NoteRepository
import com.taskmanager.app.data.repository.ProjectRepository
import com.taskmanager.app.data.repository.ScheduleRepository
import com.taskmanager.app.data.repository.SearchRepository
import com.taskmanager.app.data.repository.TaskRepository
import com.taskmanager.app.ui.components.LogoMark
import com.taskmanager.app.ui.components.MeshBackground
import com.taskmanager.app.ui.screens.AddEditTaskScreen
import com.taskmanager.app.ui.screens.LabelsScreen
import com.taskmanager.app.ui.screens.LoginScreen
import com.taskmanager.app.ui.screens.NoteEditorScreen
import com.taskmanager.app.ui.screens.NotesScreen
import com.taskmanager.app.ui.screens.ProfileScreen
import com.taskmanager.app.ui.screens.ProjectsScreen
import com.taskmanager.app.ui.screens.RegisterScreen
import com.taskmanager.app.ui.screens.ScheduleScreen
import com.taskmanager.app.ui.screens.SearchScreen
import com.taskmanager.app.ui.screens.TaskDetailScreen
import com.taskmanager.app.ui.screens.TasksScreen
import com.taskmanager.app.ui.screens.TrashScreen
import com.taskmanager.app.ui.theme.Bg
import com.taskmanager.app.ui.theme.Border
import com.taskmanager.app.ui.theme.Dim
import com.taskmanager.app.ui.theme.Iris
import com.taskmanager.app.ui.theme.Surface
import com.taskmanager.app.ui.theme.Text
import com.taskmanager.app.viewmodel.AddEditTaskViewModel
import com.taskmanager.app.viewmodel.AuthViewModel
import com.taskmanager.app.viewmodel.LabelsViewModel
import com.taskmanager.app.viewmodel.NoteEditorViewModel
import com.taskmanager.app.viewmodel.NotesViewModel
import com.taskmanager.app.viewmodel.ProfileViewModel
import com.taskmanager.app.viewmodel.ProjectsViewModel
import com.taskmanager.app.viewmodel.ScheduleViewModel
import com.taskmanager.app.viewmodel.SearchViewModel
import com.taskmanager.app.viewmodel.TaskDetailViewModel
import com.taskmanager.app.viewmodel.TasksViewModel
import com.taskmanager.app.viewmodel.TrashViewModel
import com.taskmanager.app.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.first

private const val TRANSITION_MS = 280

@Composable
fun AppNav() {
    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val api = remember { ApiClient.service }
    val authRepo = remember { AuthRepository(api, session) }
    val taskRepo = remember { TaskRepository(api, session) }
    val noteRepo = remember { NoteRepository(api, session) }
    val scheduleRepo = remember { ScheduleRepository(api, session) }
    val projectRepo = remember { ProjectRepository(api, session) }
    val labelRepo = remember { LabelRepository(api, session) }
    val searchRepo = remember { SearchRepository(api, session) }
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 5 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeIn(tween(TRANSITION_MS))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeOut(tween(TRANSITION_MS))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeIn(tween(TRANSITION_MS))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 5 },
                animationSpec = tween(TRANSITION_MS)
            ) + fadeOut(tween(TRANSITION_MS))
        }
    ) {
        composable("splash") {
            SplashScreen(
                session = session,
                onDecide = { dest ->
                    navController.navigate(dest) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            val vm: AuthViewModel =
                viewModel(factory = viewModelFactory { AuthViewModel(authRepo, session) })
            LoginScreen(
                vm = vm,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            val vm: AuthViewModel =
                viewModel(factory = viewModelFactory { AuthViewModel(authRepo, session) })
            RegisterScreen(
                vm = vm,
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onGoLogin = { navController.popBackStack() }
            )
        }
        composable("main") {
            MainScaffold(
                navController = navController,
                taskRepo = taskRepo,
                noteRepo = noteRepo,
                scheduleRepo = scheduleRepo,
                authRepo = authRepo,
                session = session,
                projectRepo = projectRepo,
                labelRepo = labelRepo
            )
        }
        composable("search") {
            val vm: SearchViewModel =
                viewModel(factory = viewModelFactory { SearchViewModel(searchRepo) })
            SearchScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenTask = { id -> navController.navigate("taskDetail/$id") },
                onOpenNote = { id -> navController.navigate("noteEditor?noteId=$id") }
            )
        }
        composable("projects") {
            val vm: ProjectsViewModel =
                viewModel(factory = viewModelFactory { ProjectsViewModel(projectRepo) })
            ProjectsScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable("labels") {
            val vm: LabelsViewModel =
                viewModel(factory = viewModelFactory { LabelsViewModel(labelRepo) })
            LabelsScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable("trash") {
            val vm: TrashViewModel =
                viewModel(factory = viewModelFactory { TrashViewModel(taskRepo, noteRepo) })
            TrashScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "taskDetail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty()
            val vm: TaskDetailViewModel =
                viewModel(factory = viewModelFactory { TaskDetailViewModel(taskId, taskRepo, projectRepo) })
            TaskDetailScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("editTask/$taskId") }
            )
        }
        composable("addTask") {
            val vm: AddEditTaskViewModel =
                viewModel(factory = viewModelFactory { AddEditTaskViewModel(null, taskRepo, projectRepo, labelRepo) })
            AddEditTaskScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = "editTask/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty()
            val vm: AddEditTaskViewModel =
                viewModel(factory = viewModelFactory { AddEditTaskViewModel(taskId, taskRepo, projectRepo, labelRepo) })
            AddEditTaskScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = "noteEditor?noteId={noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val noteId = entry.arguments?.getString("noteId")
            val vm: NoteEditorViewModel =
                viewModel(factory = viewModelFactory { NoteEditorViewModel(noteId, noteRepo, labelRepo) })
            NoteEditorScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun SplashScreen(session: SessionManager, onDecide: (String) -> Unit) {
    LaunchedEffect(Unit) {
        val token = session.tokenFlow.first()
        onDecide(if (token.isNullOrBlank()) "login" else "main")
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MeshBackground(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoMark(size = 88.dp, iconSize = 44.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "TaskManager",
                style = MaterialTheme.typography.displaySmall,
                color = Text
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tasks · Notes · Schedule",
                style = MaterialTheme.typography.bodyMedium,
                color = Dim
            )
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    navController: NavController,
    taskRepo: TaskRepository,
    noteRepo: NoteRepository,
    scheduleRepo: ScheduleRepository,
    authRepo: AuthRepository,
    session: SessionManager,
    projectRepo: ProjectRepository,
    labelRepo: LabelRepository
) {
    val tabs = listOf(
        Tab("tabs/tasks", "Tasks", Icons.Filled.List),
        Tab("tabs/notes", "Notes", Icons.Filled.Notes),
        Tab("tabs/schedule", "Schedule", Icons.Filled.CalendarMonth),
        Tab("tabs/profile", "Profile", Icons.Filled.Person)
    )
    val tabNav = rememberNavController()

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border)
                )
                NavigationBar(
                    containerColor = Surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val current by tabNav.currentBackStackEntryAsState()
                    val currentRoute = current?.destination?.route
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                tabNav.navigate(tab.route) {
                                    popUpTo(tabNav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    tab.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Text,
                                unselectedIconColor = Dim,
                                unselectedTextColor = Dim,
                                indicatorColor = Iris.copy(alpha = 0.22f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabNav,
            startDestination = "tabs/tasks",
            modifier = Modifier.padding(padding)
        ) {
            composable("tabs/tasks") {
                val vm: TasksViewModel =
                    viewModel(factory = viewModelFactory { TasksViewModel(taskRepo, projectRepo, labelRepo) })
                TasksScreen(
                    vm = vm,
                    onAdd = { navController.navigate("addTask") },
                    onOpen = { id -> navController.navigate("taskDetail/$id") },
                    onSearch = { navController.navigate("search") },
                    onProjects = { navController.navigate("projects") }
                )
            }
            composable("tabs/notes") {
                val vm: NotesViewModel =
                    viewModel(factory = viewModelFactory { NotesViewModel(noteRepo, labelRepo) })
                NotesScreen(
                    vm = vm,
                    onOpen = { id -> navController.navigate("noteEditor?noteId=$id") },
                    onAdd = { navController.navigate("noteEditor") },
                    onSearch = { navController.navigate("search") },
                    onLabels = { navController.navigate("labels") }
                )
            }
            composable("tabs/schedule") {
                val vm: ScheduleViewModel =
                    viewModel(factory = viewModelFactory { ScheduleViewModel(scheduleRepo) })
                ScheduleScreen(vm = vm)
            }
            composable("tabs/profile") {
                val vm: ProfileViewModel =
                    viewModel(factory = viewModelFactory { ProfileViewModel(authRepo, session) })
                ProfileScreen(
                    vm = vm,
                    onLoggedOut = {
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    },
                    onOpenTrash = { navController.navigate("trash") }
                )
            }
        }
    }
}
