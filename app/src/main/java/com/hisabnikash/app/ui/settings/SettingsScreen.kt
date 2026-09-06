package com.hisabnikash.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.hisabnikash.app.BuildConfig
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.data.db.BusinessEntity
import com.hisabnikash.app.data.db.BusinessSettingsEntity
import com.hisabnikash.app.data.repo.HealthReport
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.BrandMark
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.EmptyState
import com.hisabnikash.app.ui.components.LabelValueRow
import com.hisabnikash.app.ui.components.ScreenFrame
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.components.StatusChip
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.BrandGreen
import com.hisabnikash.app.ui.theme.BrandGreenDeep
import com.hisabnikash.app.ui.theme.Error
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.ui.theme.Warning
import com.hisabnikash.app.ui.vm.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

data class SettingsUi(
    val businessId: Long = 0,
    val name: String = "",
    val category: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val invoicePrefix: String = "INV-",
    val invoiceFooter: String = "",
    val invoiceTerms: String = "",
    val lowStockDefaultText: String = "5",
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val workspace = container.workspaceRepository
    private val form = MutableStateFlow(SettingsUi())

    val state: StateFlow<SettingsUi> = container.prefs.activeBusinessId
        .flatMapLatest { id ->
            if (id == null || id <= 0) flowOf(form.value)
            else combine(
                workspace.observeActiveWorkspace(),
                flowOf(form.value)
            ) { ws, f ->
                if (!f.loaded) {
                    SettingsUi(
                        businessId = id,
                        name = ws.business?.name ?: "",
                        category = ws.business?.category ?: "GENERAL",
                        address = ws.business?.address ?: "",
                        phone = ws.business?.phone ?: "",
                        email = ws.business?.email ?: "",
                        invoicePrefix = ws.settings?.invoicePrefix ?: "INV-",
                        invoiceFooter = ws.settings?.invoiceFooter ?: "",
                        invoiceTerms = ws.settings?.invoiceTerms ?: "",
                        lowStockDefaultText = "${ws.settings?.lowStockThresholdDefault ?: 5}",
                        loaded = true
                    )
                } else f.copy(businessId = id)
            }
        }
        .stateIn(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            SharingStarted.WhileSubscribed(5_000),
            SettingsUi()
        )

    fun setName(v: String) { form.value = form.value.copy(name = v) }
    fun setCategory(v: String) { form.value = form.value.copy(category = v) }
    fun setAddress(v: String) { form.value = form.value.copy(address = v) }
    fun setPhone(v: String) { form.value = form.value.copy(phone = v) }
    fun setEmail(v: String) { form.value = form.value.copy(email = v) }
    fun setInvoicePrefix(v: String) { form.value = form.value.copy(invoicePrefix = v) }
    fun setInvoiceFooter(v: String) { form.value = form.value.copy(invoiceFooter = v) }
    fun setInvoiceTerms(v: String) { form.value = form.value.copy(invoiceTerms = v) }
    fun setLowStockDefault(v: String) { form.value = form.value.copy(lowStockDefaultText = v.filter(Char::isDigit)) }

    fun save(container: AppContainer, onDone: () -> Unit) {
        val f = form.value
        if (f.name.isBlank()) {
            form.value = f.copy(error = "Business name is required.")
            return
        }
        form.value = f.copy(saving = true, error = null, saved = false)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val result = runCatching {
                val businessId = workspace.requireActiveBusiness()
                workspace.updateBusiness(
                    BusinessEntity(
                        id = businessId,
                        name = f.name.trim(),
                        category = f.category,
                        address = f.address.ifBlank { null },
                        phone = f.phone.ifBlank { null },
                        email = f.email.ifBlank { null },
                        currency = "BDT"
                    )
                )
                workspace.updateSettings(
                    BusinessSettingsEntity(
                        businessId = businessId,
                        invoicePrefix = f.invoicePrefix.ifBlank { "INV-" },
                        invoiceFooter = f.invoiceFooter,
                        invoiceTerms = f.invoiceTerms,
                        lowStockThresholdDefault = f.lowStockDefaultText.toLongOrNull() ?: 5
                    )
                )
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result.onSuccess {
                    form.value = f.copy(saving = false, saved = true)
                    onDone()
                }.onFailure { e ->
                    form.value = f.copy(saving = false, error = e.message ?: "Couldn't save settings.")
                }
            }
        }
    }
}

@Composable
fun SettingsScreenRoute(container: AppContainer, navController: NavHostController) {
    val vm = appViewModel(container) { SettingsViewModel(it) }
    val state by vm.state.collectAsState()

    ScreenFrame("Settings", onBack = { navController.popBackStack() }) {
        SectionHeader("Business profile")
        AppTextField("Business name", state.name, { vm.setName(it) })
        AppDropdown(
            "Category",
            listOf(
                DropOption("FASHION", "Fashion & Apparel"),
                DropOption("FOOD", "Food & Beverage"),
                DropOption("ELECTRONICS", "Electronics"),
                DropOption("BEAUTY", "Beauty & Care"),
                DropOption("HANDICRAFT", "Handicrafts"),
                DropOption("BOOKS", "Books & Stationery"),
                DropOption("SERVICES", "Services"),
                DropOption("GENERAL", "General Store")
            ),
            state.category,
            { vm.setCategory(it.id) }
        )
        AppTextField("Address", state.address, { vm.setAddress(it) }, singleLine = false, minLines = 2)
        AppTextField("Phone", state.phone, { vm.setPhone(it) }, keyboardType = KeyboardType.Phone)
        AppTextField("Email", state.email, { vm.setEmail(it) }, keyboardType = KeyboardType.Email)
        SectionHeader("Invoicing")
        AppTextField("Invoice prefix", state.invoicePrefix, { vm.setInvoicePrefix(it) })
        AppTextField("Invoice footer", state.invoiceFooter, { vm.setInvoiceFooter(it) }, singleLine = false, minLines = 2)
        AppTextField("Invoice terms", state.invoiceTerms, { vm.setInvoiceTerms(it) }, singleLine = false, minLines = 2)
        AppTextField("Default low-stock threshold", state.lowStockDefaultText, { vm.setLowStockDefault(it) }, keyboardType = KeyboardType.Number)
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        Button(
            onClick = { vm.save(container) {} },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) { Text(if (state.saving) "Saving…" else "Save settings") }
        if (state.saved) {
            Text(
                "Saved ✓",
                color = BrandGreen,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Security (app lock)
// ---------------------------------------------------------------------------

@Composable
fun SecurityScreenRoute(container: AppContainer, navController: NavHostController) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var biometric by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val pinHash by container.prefs.pinHash.collectAsState(initial = null)
    val pinSet = pinHash != null
    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }

    ScreenFrame("App lock", onBack = { navController.popBackStack() }) {
        if (pinSet) {
            // Unlock mode (also used by boot gating).
            AppTextField("Enter PIN", pin, { pin = it.filter(Char::isDigit) }, keyboardType = KeyboardType.NumberPassword)
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        val ok = container.securityRepository.verifyPin(pin)
                        busy = false
                        if (ok) {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.BOOT) { inclusive = true }
                            }
                        } else {
                            error = "Wrong PIN. Try again."
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Unlock") }
            if (container.securityRepository.canUseBiometric()) {
                OutlinedButton(
                    onClick = {
                        val activity = context as? FragmentActivity ?: return@OutlinedButton
                        val prompt = BiometricPrompt(
                            activity,
                            java.util.concurrent.Executors.newSingleThreadExecutor(),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(
                                    result: BiometricPrompt.AuthenticationResult
                                ) {
                                    scope.launch {
                                        navController.navigate(Routes.MAIN) {
                                            popUpTo(Routes.BOOT) { inclusive = true }
                                        }
                                    }
                                }

                                override fun onAuthenticationFailed() {
                                    scope.launch { error = "Biometric check failed. Use your PIN." }
                                }
                            }
                        )
                        prompt.authenticate(
                            BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Unlock HisabNikash")
                                .setNegativeButtonText("Use PIN")
                                .build()
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) { Text("Use biometric unlock") }
            }
        } else {
            Text(
                "Optionally protect the app with a 4–8 digit PIN and your device biometrics. Your data stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            AppTextField("New PIN (4–8 digits)", pin, { pin = it.filter(Char::isDigit).take(8) }, keyboardType = KeyboardType.NumberPassword)
            AppTextField("Confirm PIN", confirm, { confirm = it.filter(Char::isDigit).take(8) }, keyboardType = KeyboardType.NumberPassword)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Enable biometric unlock", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(
                    checked = biometric,
                    onCheckedChange = { biometric = it },
                    enabled = container.securityRepository.canUseBiometric()
                )
            }
            if (!container.securityRepository.canUseBiometric()) {
                Text(
                    "Biometrics are not available on this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkFaint,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Button(
                onClick = {
                    if (pin.length < 4) {
                        error = "PIN must be at least 4 digits."
                        return@Button
                    }
                    if (pin != confirm) {
                        error = "PINs don't match."
                        return@Button
                    }
                    scope.launch {
                        busy = true
                        container.securityRepository.setPin(pin)
                        container.securityRepository.setBiometric(biometric && container.securityRepository.canUseBiometric())
                        busy = false
                        navController.popBackStack()
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            ) { Text("Turn on app lock") }
        }
    }
}

// ---------------------------------------------------------------------------
// Support / About
// ---------------------------------------------------------------------------

@Composable
fun SupportScreenRoute(container: AppContainer, navController: NavHostController) {
    ScreenFrame("Support", onBack = { navController.popBackStack() }) {
        Text(
            "HisabNikash is a self-contained commerce tool. There is no server and no hidden data collection — everything you record lives in this app.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaint,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SectionHeader("Common questions")
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("How is revenue counted?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "An order becomes revenue when it is delivered. Invoices, receipts and refund documents never create revenue by themselves.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint
                )
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Where is my data stored?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "In a local database on this device. Use Settings → Backup & restore to export a JSON file you can keep somewhere safe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint
                )
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Can I switch businesses?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Yes — from the business switcher on Home. Each business keeps its own books, stock and documents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint
                )
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Do you sync with banks or couriers?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "No. Integrations would require sending your data to third parties. Enter courier settlements and bank movements manually with guaranteed accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkFaint
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun AboutScreenRoute(container: AppContainer, navController: NavHostController) {
    ScreenFrame("About", onBack = { navController.popBackStack() }) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(72.dp)
            Spacer(Modifier.height(14.dp))
            Text("HisabNikash", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = BrandGreenDeep)
            Text("Your Complete Commerce OS", style = MaterialTheme.typography.bodyLarge, color = InkFaint)
            Spacer(Modifier.height(20.dp))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    LabelValueRow("Version", BuildConfig.VERSION_NAME)
                    LabelValueRow("Build", "${BuildConfig.VERSION_CODE}")
                    LabelValueRow("Data", "Offline-first, on this device")
                    LabelValueRow("Currency", "BDT (৳)")
                    LabelValueRow("Privacy", "No servers, no tracking")
                }
            }
        }
        SectionHeader("Built for sellers")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            listOf(
                "End-to-end order and inventory tracking",
                "Honest profit: revenue only counts when it's earned",
                "COD, courier and settlement workflows",
                "Invoices, receipts, returns, exchanges and refunds",
                "Budgets, campaigns and ROAS without guesswork",
                "One-tap JSON backup and restore",
                "Optional app lock with PIN/biometrics"
            ).forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("  •  ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Backup & restore
// ---------------------------------------------------------------------------

@Composable
fun BackupScreenRoute(container: AppContainer, navController: NavHostController) {
    val context = LocalContext.current
    val businessId by container.prefs.activeBusinessId.collectAsState(initial = null)
    val lastBackup by container.prefs.lastBackupAt.collectAsState(initial = null)
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingInfo by remember { mutableStateOf<com.hisabnikash.app.data.repo.BackupInfo?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val bid = businessId
        if (uri == null || bid == null) return@rememberLauncherForActivityResult
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                val file = File(context.cacheDir, "export-${System.currentTimeMillis()}.json")
                val rows = container.backupRepository.exportToFile(file, bid)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                container.prefs.setLastBackup(System.currentTimeMillis())
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    message = "Exported $rows records."
                    error = null
                }
                file.delete()
            } catch (e: Exception) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    error = "Export failed: ${e.message}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                val file = File(context.cacheDir, "import-${System.currentTimeMillis()}.json")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                val info = container.backupRepository.readBackupInfo(file)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    if (info != null) {
                        pendingInfo = info
                        pendingFile = file
                        error = null
                    } else {
                        file.delete()
                        error = "This file isn't a valid HisabNikash backup."
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    error = "Import failed: ${e.message}"
                }
            }
        }
    }

    ScreenFrame("Backup & restore", onBack = { navController.popBackStack() }) {
        Text(
            "Your data lives only on this device. Export a backup regularly and keep it somewhere safe.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaint,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        if (lastBackup != null) {
            Text(
                "Last backup: ${com.hisabnikash.app.domain.model.formatDateTime(lastBackup!!)}",
                style = MaterialTheme.typography.labelMedium,
                color = BrandGreen,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        message?.let {
            Text(it, color = BrandGreen, modifier = Modifier.padding(horizontal = 16.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Button(
            onClick = {
                busy = true
                exportLauncher.launch("hisabnikash-backup-${System.currentTimeMillis()}.json")
                busy = false
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Export backup (JSON)")
        }
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) { Text("Import a backup") }

        pendingInfo?.let { info ->
            SectionHeader("Confirm restore")
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    LabelValueRow("Business", "#${info.businessId}")
                    LabelValueRow("Exported", com.hisabnikash.app.domain.model.formatDateTime(info.exportedAt))
                    LabelValueRow("Records", "${info.rowCount}")
                    Text(
                        "Restoring REPLACES business #${info.businessId}'s data on this device with the backup contents. Other businesses are not touched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Warning
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            pendingInfo = null
                            pendingFile = null
                        }) { Text("Cancel") }
                        Button(onClick = {
                            val file = pendingFile
                            if (file == null) return@Button
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                                val result = container.backupRepository.restoreFromFile(file, replaceExisting = true)
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    pendingInfo = null
                                    pendingFile = null
                                    file.delete()
                                    message = result.message
                                }
                            }
                        }) { Text("Replace data") }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Data health
// ---------------------------------------------------------------------------

@Composable
fun DataHealthScreenRoute(container: AppContainer, navController: NavHostController) {
    val businessId by container.prefs.activeBusinessId.collectAsState(initial = null)
    var report by remember { mutableStateOf<HealthReport?>(null) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenFrame("Data health", onBack = { navController.popBackStack() }) {
        Text(
            "Runs non-destructive checks for orphaned records, duplicate document numbers and impossible money states.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaint,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        Button(
            onClick = {
                val bid = businessId
                if (bid == null) return@Button
                running = true
                report = null
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    val result = runCatching { container.dataHealthRepository.checkBusiness(bid) }
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        running = false
                        report = result.getOrNull()
                        message = result.fold({ null }, { "Check failed: ${it.message}" })
                    }
                }
            },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text(if (running) "Checking…" else "Run health check") }

        message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        val current = report
        if (current != null) {
            SectionHeader("Findings")
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(current.summary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
            current.findings.forEach { finding ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(
                                finding.severity,
                                when (finding.severity) {
                                    "CRITICAL" -> Error
                                    "WARNING" -> Warning
                                    else -> BrandGreen
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(finding.type, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(finding.message, style = MaterialTheme.typography.bodySmall, color = InkFaint)
                    }
                }
            }
            val repairable = current.findings.mapNotNull { f -> f.repair?.let { f.type } }.toSet().minus("CLEAN")
            if (repairable.isNotEmpty()) {
                Button(
                    onClick = {
                        val bid = businessId
                        if (bid == null) return@Button
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            val fixed = container.dataHealthRepository.repair(bid, repairable)
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                message = "Repaired $fixed issue(s). Run the check again to confirm."
                                report = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("Repair safe issues") }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
