package com.pegoku.curem3.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pegoku.curem3.R
import com.pegoku.curem3.data.Address
import com.pegoku.curem3.ui.AddressResult
import com.pegoku.curem3.ui.AppViewModel
import com.pegoku.curem3.ui.components.DetailTopBar
import kotlinx.coroutines.launch

private val POSTCODE = Regex("^[1-9][0-9]{3}[A-Z]{2}$")

@Composable
fun AddressScreen(vm: AppViewModel, onBack: (() -> Unit)?, onDone: () -> Unit) {
    var postcode by rememberSaveable { mutableStateOf("") }
    var house by rememberSaveable { mutableStateOf("") }
    var suffix by rememberSaveable { mutableStateOf("") }
    var street by rememberSaveable { mutableStateOf("") }
    var streets by rememberSaveable { mutableStateOf(listOf<String>()) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var streetsMenu by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val normalised = postcode.replace(" ", "").uppercase()
    val postcodeValid = POSTCODE.matches(normalised)

    LaunchedEffect(normalised) {
        if (postcodeValid) {
            val found = vm.lookupStreets(normalised)
            streets = found
            street = if (found.size == 1) found.first() else if (street in found) street else ""
        } else {
            streets = emptyList(); street = ""
        }
    }

    val unknownPostcode = stringResource(R.string.error_unknown_postcode)
    val noData = stringResource(R.string.error_no_data)
    val offline = stringResource(R.string.error_offline)
    val generic = stringResource(R.string.error_generic, "%s")

    fun submit() {
        if (!postcodeValid || house.isBlank() || submitting) return
        submitting = true; error = null
        scope.launch {
            val result = vm.submitAddress(Address(normalised, house.trim(), suffix.trim().uppercase(), street))
            submitting = false
            when (result) {
                AddressResult.Success -> onDone()
                AddressResult.UnknownPostcode -> error = unknownPostcode
                AddressResult.NoData -> error = noData
                AddressResult.Offline -> error = offline
                is AddressResult.Error -> error = generic.format(result.message)
            }
        }
    }

    Scaffold(
        topBar = { if (onBack != null) DetailTopBar(stringResource(R.string.change_address), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(if (onBack == null) 72.dp else 8.dp))
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = postcode,
                onValueChange = { postcode = it.uppercase().take(7) },
                label = { Text(stringResource(R.string.postcode)) },
                placeholder = { Text(stringResource(R.string.postcode_hint)) },
                singleLine = true,
                isError = postcode.length >= 6 && !postcodeValid,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = house,
                    onValueChange = { house = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text(stringResource(R.string.house_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1.4f),
                )
                OutlinedTextField(
                    value = suffix,
                    onValueChange = { suffix = it.take(6) },
                    label = { Text(stringResource(R.string.suffix)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                )
            }
            if (streets.size > 1) {
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedTextField(
                        value = street,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.street)) },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(Modifier.matchParentSize().clickable { streetsMenu = true })
                    DropdownMenu(expanded = streetsMenu, onDismissRequest = { streetsMenu = false }) {
                        streets.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { street = s; streetsMenu = false })
                        }
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { submit() },
                enabled = postcodeValid && house.isNotBlank() && !submitting && (streets.size <= 1 || street.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                if (submitting) {
                    LoadingIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.checking))
                } else {
                    Text(stringResource(R.string.continue_), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
            if (onBack == null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { postcode = "5616KA"; house = "10"; suffix = "" },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.demo_address)) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
