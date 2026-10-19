package com.taskmanager.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.taskmanager.app.data.repository.NoteRepository
import com.taskmanager.app.data.repository.ScheduleRepository
import com.taskmanager.app.data.repository.TaskRepository
import com.taskmanager.app.ui.screens.AddEditTaskScreen
import com.taskmanager.app.ui.screens.LoginScreen
import com.taskmanager.app.ui.screens.NoteEditorScreen
import com.taskmanager.app.ui.screens.NotesScreen
import com.taskmanager.app.ui.screens.ProfileScreen
import com.taskmanager.app.ui.screens.RegisterScreen
import com.taskmanager.app.ui.screens.ScheduleScreen
import com.taskmanager.app.ui.screens.TaskDetailScreen
import com.taskmanager.app.ui.screens.TasksScreen
import com.taskmanager.app.viewmodel.AddEditTaskViewModel
import com.taskmanager.app.viewmodel.AuthViewModel
import com.taskmanager.app.viewmodel.NoteEditorViewModel
import com.taskmanager.app.viewmodel.NotesViewModel
import com.taskmanager.app.viewmodel.ProfileViewModel
import com.taskmanager.app.viewmodel.ScheduleViewModel
import com.taskmanager.app.viewmodel.TaskDetailViewModel
import com.taskmanager.app.viewmodel.TasksViewModel
import com.taskmanager.app.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.first

@Composable
fun AppNav() {
    val context = LocalContext.current
    val session = remember { SessionManager(context.applicationContext) }
    val api = remember { ApiClient.service }
    val authRepo = remember { AuthRepository(api, session) }
    val taskRepo = remember { TaskRepository(api, session) }
    val noteRepo = remember { NoteRepository(api, session) }
    val scheduleRepo = remember { ScheduleRepository(api, session) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
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
                session = session
            )
        }
        composable(
            route = "taskDetail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty()
            val vm: TaskDetailViewModel =
                viewModel(factory = viewModelFactory { TaskDetailViewModel(taskId, taskRepo) })
            TaskDetailScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("editTask/$taskId") }
            )
        }
        composable("addTask") {
            val vm: AddEditTaskViewModel =
                viewModel(factory = viewModelFactory { AddEditTaskViewModel(null, taskRepo) })
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
                viewModel(factory = viewModelFactory { AddEditTaskViewModel(taskId, taskRepo) })
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
                viewModel(factory = viewModelFactory { NoteEditorViewModel(noteId, noteRepo) })
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
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
    session: SessionManager
) {
    val tabs = listOf(
        Tab("tabs/tasks", "Tasks", Icons.Filled.List),
        Tab("tabs/notes", "Notes", Icons.Filled.Notes),
        Tab("tabs/schedule", "Schedule", Icons.Filled.CalendarMonth),
        Tab("tabs/profile", "Profile", Icons.Filled.Person)
    )
    val tabNav = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
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
                        label = { Text(tab.label) }
                    )
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
                    viewModel(factory = viewModelFactory { TasksViewModel(taskRepo) })
                TasksScreen(
                    vm = vm,
                    onAdd = { navController.navigate("addTask") },
                    onOpen = { id -> navController.navigate("taskDetail/$id") }
                )
            }
            composable("tabs/notes") {
                val vm: NotesViewModel =
                    viewModel(factory = viewModelFactory { NotesViewModel(noteRepo) })
                NotesScreen(
                    vm = vm,
                    onOpen = { id -> navController.navigate("noteEditor?noteId=$id") },
                    onAdd = { navController.navigate("noteEditor") }
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
                    }
                )
            }
        }
    }
}
