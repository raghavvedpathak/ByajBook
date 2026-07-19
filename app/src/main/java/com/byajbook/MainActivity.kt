package com.byajbook

import android.Manifest
import com.byajbook.notification.AlarmScheduler
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.byajbook.feature.customers.CustomerDetailScreen
import com.byajbook.feature.customers.CustomerListScreen
import com.byajbook.feature.dashboard.DashboardScreen
import com.byajbook.feature.entry.AddEditRecordBottomSheet
import com.byajbook.feature.payments.PaymentModal
import com.byajbook.feature.reports.CustomerReportDetailScreen
import com.byajbook.feature.reports.ReportsScreen
import com.byajbook.feature.settings.SettingsScreen
import com.byajbook.navigation.AppRoutes
import com.byajbook.ui.ByajBookTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ByajBookTheme {
                PermissionManager()
                ByajBookApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                AlarmScheduler.scheduleDailyAlarm(this)
            }
        }
    }
}

@Composable
fun PermissionManager() {
    val context = LocalContext.current
    var showExactAlarmPrompt by remember { mutableStateOf(false) }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        // 1. Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Check SCHEDULE_EXACT_ALARM on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmPrompt = true
            }
        }
    }

    if (showExactAlarmPrompt) {
        AlertDialog(
            onDismissRequest = { showExactAlarmPrompt = false },
            title = { Text("Exact Alarms Required") },
            text = { Text("To receive daily overdue collection warnings and reminders at exactly 10:00 AM, please grant the Exact Alarm permission in the system settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmPrompt = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // catch ActivityNotFoundException
                            }
                        }
                    }
                ) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ByajBookApp() {
    val navController = rememberNavController()
    var showEntrySheet by remember { mutableStateOf<String?>(null) } // null: hidden, "": new, "id": edit
    var showPaymentModal by remember { mutableStateOf<String?>(null) } // null: hidden, "id": recordId

    if (showEntrySheet != null) {
        AddEditRecordBottomSheet(
            recordId = showEntrySheet!!.takeIf { it.isNotEmpty() },
            onDismiss = { showEntrySheet = null }
        )
    }

    if (showPaymentModal != null) {
        PaymentModal(
            recordId = showPaymentModal!!,
            onBack = { showPaymentModal = null },
            onSuccess = { showPaymentModal = null }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val tabs = listOf(
                    Triple(AppRoutes.DashboardGraph, "Dashboard", Icons.Filled.Home),
                    Triple(AppRoutes.CustomersGraph, "Customers", Icons.Filled.Person),
                    Triple(AppRoutes.ReportsGraph, "Reports", Icons.AutoMirrored.Filled.List),
                    Triple(AppRoutes.SettingsGraph, "Settings", Icons.Filled.Settings)
                )

                tabs.forEach { (route, title, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.DashboardGraph,
            modifier = Modifier.padding(innerPadding)
        ) {
            navigation<AppRoutes.DashboardGraph>(startDestination = AppRoutes.Dashboard) {
                composable<AppRoutes.Dashboard> { 
                    DashboardScreen(
                        onAddRecord = { showEntrySheet = "" },
                        onRecordClick = { id -> showEntrySheet = id }
                    )
                }
            }
            
            navigation<AppRoutes.CustomersGraph>(startDestination = AppRoutes.Customers) {
                composable<AppRoutes.Customers> { 
                    CustomerListScreen(
                        onCustomerClick = { id -> 
                            navController.navigate(AppRoutes.CustomerDetail(id)) 
                        }
                    )
                }
                composable<AppRoutes.CustomerDetail> { backStackEntry ->
                    val route: AppRoutes.CustomerDetail = backStackEntry.toRoute()
                    CustomerDetailScreen(
                        customerId = route.customerId,
                        onBack = { navController.popBackStack() },
                        onRecordClick = { id -> showPaymentModal = id }
                    )
                }
            }
            
            navigation<AppRoutes.ReportsGraph>(startDestination = AppRoutes.Reports) {
                composable<AppRoutes.Reports> { 
                    ReportsScreen(
                        onCustomerDetail = { id -> 
                            navController.navigate(AppRoutes.CustomerReportDetail(id)) 
                        }
                    )
                }
                composable<AppRoutes.CustomerReportDetail> { backStackEntry ->
                    val route: AppRoutes.CustomerReportDetail = backStackEntry.toRoute()
                    CustomerReportDetailScreen(
                        customerId = route.customerId,
                        onBack = { navController.popBackStack() },
                        onRecordClick = { id -> showPaymentModal = id }
                    )
                }
            }
            
            navigation<AppRoutes.SettingsGraph>(startDestination = AppRoutes.Settings) {
                composable<AppRoutes.Settings> { SettingsScreen() }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}
