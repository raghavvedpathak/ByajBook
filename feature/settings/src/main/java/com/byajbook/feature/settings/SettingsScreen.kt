package com.byajbook.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byajbook.domain.model.Settings
import com.byajbook.ui.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = viewModel.getBackupJson()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                reader.readText()
            }
            json?.let { viewModel.importBackup(it) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.importResult.collect { result ->
            if (result.isSuccess) {
                Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Import failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Text("Error: ${state.message}", modifier = Modifier.padding(padding))
            }
            is UiState.Success -> {
                SettingsContent(
                    settings = state.data,
                    onUpdate = viewModel::updateSettings,
                    onExport = { exportLauncher.launch("byajbook_backup.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) },
                    onClearData = viewModel::clearAllData,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    settings: Settings,
    onUpdate: (Settings) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("This will delete all customers, records, items, and payments. Your business settings and item rates will be kept.") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        onClearData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Business Information", style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = settings.name,
            onValueChange = { onUpdate(settings.copy(name = it)) },
            label = { Text("Business Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = settings.phone,
            onValueChange = { onUpdate(settings.copy(phone = it)) },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = settings.address,
            onValueChange = { onUpdate(settings.copy(address = it)) },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
        
        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Backup (SAF)")
        }

        OutlinedButton(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Backup (SAF)")
        }

        Spacer(Modifier.height(16.dp))
        
        TextButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Clear All Data")
        }
    }
}
