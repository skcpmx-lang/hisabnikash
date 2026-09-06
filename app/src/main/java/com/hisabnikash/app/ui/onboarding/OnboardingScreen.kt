package com.hisabnikash.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hisabnikash.app.data.container.AppContainer
import com.hisabnikash.app.domain.model.formatMoney
import com.hisabnikash.app.ui.components.AppDropdown
import com.hisabnikash.app.ui.components.AppTextField
import com.hisabnikash.app.ui.components.BrandMark
import com.hisabnikash.app.ui.components.DropOption
import com.hisabnikash.app.ui.components.SectionHeader
import com.hisabnikash.app.ui.nav.Routes
import com.hisabnikash.app.ui.theme.InkFaint
import com.hisabnikash.app.domain.model.parseMoneyInput
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val businessCategories = listOf(
    DropOption("FASHION", "Fashion & Apparel"),
    DropOption("FOOD", "Food & Beverage"),
    DropOption("ELECTRONICS", "Electronics"),
    DropOption("BEAUTY", "Beauty & Care"),
    DropOption("HANDICRAFT", "Handicrafts"),
    DropOption("BOOKS", "Books & Stationery"),
    DropOption("SERVICES", "Services"),
    DropOption("GENERAL", "General Store")
)

private val defaultChannels = listOf(
    DropOption("Facebook", "Facebook"),
    DropOption("Instagram", "Instagram"),
    DropOption("TikTok", "TikTok"),
    DropOption("Website", "Website / Store"),
    DropOption("WhatsApp", "WhatsApp"),
    DropOption("In-person", "In-person")
)

@Composable
fun OnboardingScreen(
    container: AppContainer,
    navController: NavHostController,
    isAdditional: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var invoicePrefix by remember { mutableStateOf("INV-") }
    var cashOpening by remember { mutableStateOf("") }
    var bkashOpening by remember { mutableStateOf("") }
    var nagadOpening by remember { mutableStateOf("") }
    var bankOpening by remember { mutableStateOf("") }
    var selectedChannels by remember { mutableStateOf(defaultChannels.take(2).map { it.id }.toSet()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            BrandMark(64.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                if (isAdditional) "Add another business" else "Set up your business",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Everything stays on this device. Your books, stock, money and orders live in one place — offline first, always private.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint
            )
        }

        SectionHeader("Business")
        AppTextField("Business name", name, { name = it }, placeholder = "e.g. Ayesha's Boutique")
        AppDropdown("Category", businessCategories, category, { category = it.id })
        AppTextField("Address", address, { address = it }, placeholder = "Shop or delivery address", singleLine = false, minLines = 2)
        AppTextField("Phone", phone, { phone = it }, placeholder = "01XXXXXXXXX")
        AppTextField("Email", email, { email = it }, placeholder = "you@example.com")

        SectionHeader("Owner")
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { AppTextField("First name", firstName, { firstName = it }) }
            Column(Modifier.weight(1f)) { AppTextField("Last name", lastName, { lastName = it }) }
        }

        SectionHeader("Selling channels")
        Text(
            "Orders can be tagged with a channel so you know where sales come from.",
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))
        defaultChannels.forEach { channel ->
            val selected = channel.id in selectedChannels
            androidx.compose.material3.ElevatedCard(
                onClick = {
                    selectedChannels = if (selected) selectedChannels - channel.id else selectedChannels + channel.id
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(channel.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (selected) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        SectionHeader("Accounts & opening balances")
        Text(
            "Prefer to leave balances empty? You can set them later from Money.",
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))
        AppTextField("Cash opening balance", cashOpening, { cashOpening = it }, placeholder = "৳ 0.00")
        AppTextField("bKash opening balance", bkashOpening, { bkashOpening = it }, placeholder = "৳ 0.00")
        AppTextField("Nagad opening balance", nagadOpening, { nagadOpening = it }, placeholder = "৳ 0.00")
        AppTextField("Bank opening balance", bankOpening, { bankOpening = it }, placeholder = "৳ 0.00")

        SectionHeader("Invoicing")
        AppTextField("Invoice prefix", invoicePrefix, { invoicePrefix = it }, placeholder = "INV-")

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                if (name.isBlank()) {
                    error = "Give your business a name to continue."
                    return@Button
                }
                saving = true
                error = null
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    val result = runCatching {
                        container.workspaceRepository.createBusiness(
                            name = name,
                            category = category,
                            firstName = firstName,
                            lastName = lastName,
                            address = address.ifBlank { null },
                            phone = phone.ifBlank { null },
                            email = email.ifBlank { null },
                            currency = "BDT",
                            channels = selectedChannels.toList(),
                            initialAccounts = mapOf(
                                "Cash" to (parseMoneyInput(cashOpening) ?: 0),
                                "bKash" to (parseMoneyInput(bkashOpening) ?: 0),
                                "Nagad" to (parseMoneyInput(nagadOpening) ?: 0),
                                "Bank" to (parseMoneyInput(bankOpening) ?: 0)
                            ),
                            invoicePrefix = invoicePrefix.ifBlank { "INV-" },
                            logoPath = null
                        )
                    }
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        result.onSuccess {
                            if (isAdditional) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Routes.MAIN) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        }.onFailure { e ->
                            saving = false
                            error = com.hisabnikash.app.domain.model.SafeMessages.save(e, "Couldn't finish setup.")
                        }
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) { Text(if (saving) "Setting up…" else if (isAdditional) "Add business" else "Start using HisabNikash") }
        Text(
            "Data lives only on this device unless you export a backup.",
            style = MaterialTheme.typography.labelSmall,
            color = InkFaint,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
