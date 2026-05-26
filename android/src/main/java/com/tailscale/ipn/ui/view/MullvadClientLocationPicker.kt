package com.tailscale.ipn.ui.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.util.LoadingIndicator
import com.tailscale.ipn.ui.util.flag
import com.tailscale.ipn.ui.util.itemsWithDividers
import com.tailscale.ipn.ui.viewModel.MullvadNav
import com.tailscale.ipn.ui.viewModel.MullvadViewModel
import com.tailscale.ipn.ui.viewModel.RelayCityJson
import com.tailscale.ipn.ui.viewModel.RelayCountryJson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MullvadClientLocationPickerList(
    nav: MullvadNav,
    parentEntry: NavBackStackEntry,
    model: MullvadViewModel = viewModel(parentEntry),
) {
  val relays by model.relays.collectAsState()
  val settings by model.settings.collectAsState()
  val chooseServerText = stringResource(R.string.choose_mullvad_server)
  var pickerTitle by remember { mutableStateOf("") }
  var pickerRelays by remember { mutableStateOf<List<ClientRelayPick>>(emptyList()) }

  LaunchedEffect(Unit) {
    if (relays.isEmpty()) model.fetchRelays()
  }

  LoadingIndicator.Wrap {
    Scaffold(
        topBar = {
          Header(R.string.mullvad_choose_location, onBack = nav.onNavigateBackToAccount)
        }) { innerPadding ->
          LazyColumn(modifier = Modifier.padding(innerPadding)) {
            val sorted = relays.sortedBy { it.name.lowercase() }
            itemsWithDividers(sorted, key = { it.code }) { country ->
              val selected =
                  settings.location.equals(country.code, ignoreCase = true) &&
                      settings.city.isEmpty() &&
                      settings.relayHostname.isEmpty()
              MullvadClientCountryItem(
                  country = country,
                  selected = selected,
                  onClick = {
                    if (country.cities.size == 1 && country.cities.first().relays.isNotEmpty()) {
                      val city = country.cities.first()
                      model.setLocation(
                          country.code,
                          city.code,
                          connectIfWanted = settings.wantTunnel,
                      )
                      nav.onNavigateBackToAccount()
                    } else {
                      nav.onNavigateToClientCountry(country.code)
                    }
                  },
                  onLongClick = {
                    val picks =
                        country.cities
                            .sortedBy { it.name.lowercase() }
                            .flatMap { city ->
                              city.relays.map { relay ->
                                ClientRelayPick(country.code, city.code, relay)
                              }
                            }
                    if (picks.isNotEmpty()) {
                      pickerTitle = "${country.code.flag()} ${country.name} - $chooseServerText"
                      pickerRelays = picks
                    }
                  },
              )
            }
          }
          if (pickerRelays.isNotEmpty()) {
            MullvadClientRelayPickerSheet(
                title = pickerTitle,
                relays = pickerRelays,
                selectedHostname = settings.relayHostname,
                onSelect = { pick ->
                  model.setRelay(
                      pick.countryCode,
                      pick.cityCode,
                      pick.relay.hostname,
                      connectIfWanted = settings.wantTunnel,
                  )
                  pickerRelays = emptyList()
                  nav.onNavigateBackToAccount()
                },
                onDismiss = { pickerRelays = emptyList() },
            )
          }
        }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MullvadClientLocationPicker(
    countryCode: String,
    nav: MullvadNav,
    parentEntry: NavBackStackEntry,
    model: MullvadViewModel = viewModel(parentEntry),
) {
  val relays by model.relays.collectAsState()
  val settings by model.settings.collectAsState()
  val country = relays.find { it.code.equals(countryCode, ignoreCase = true) }
  val chooseServerText = stringResource(R.string.choose_mullvad_server)
  var pickerTitle by remember { mutableStateOf("") }
  var pickerRelays by remember { mutableStateOf<List<ClientRelayPick>>(emptyList()) }

  LoadingIndicator.Wrap {
    Scaffold(
        topBar = {
          Header(
              title = { Text("${countryCode.flag()} ${country?.name ?: countryCode}") },
              onBack = nav.onNavigateToClientLocation,
          )
        }) { innerPadding ->
          LazyColumn(modifier = Modifier.padding(innerPadding)) {
            country?.cities?.sortedBy { it.name.lowercase() }?.let { cities ->
              itemsWithDividers(cities, key = { it.code }) { city ->
                val selected =
                    settings.location.equals(countryCode, ignoreCase = true) &&
                        settings.city.equals(city.code, ignoreCase = true) &&
                        settings.relayHostname.isEmpty()
                MullvadClientCityItem(
                    city = city,
                    selected = selected,
                    onClick = {
                      model.setLocation(
                          countryCode,
                          city.code,
                          connectIfWanted = settings.wantTunnel,
                      )
                      nav.onNavigateBackToAccount()
                    },
                    onLongClick = {
                      val picks =
                          city.relays.map { relay ->
                            ClientRelayPick(countryCode, city.code, relay)
                          }
                      if (picks.isNotEmpty()) {
                        pickerTitle = "${city.name} - $chooseServerText"
                        pickerRelays = picks
                      }
                    },
                )
              }
            }
          }
          if (pickerRelays.isNotEmpty()) {
            MullvadClientRelayPickerSheet(
                title = pickerTitle,
                relays = pickerRelays,
                selectedHostname = settings.relayHostname,
                onSelect = { pick ->
                  model.setRelay(
                      pick.countryCode,
                      pick.cityCode,
                      pick.relay.hostname,
                      connectIfWanted = settings.wantTunnel,
                  )
                  pickerRelays = emptyList()
                  nav.onNavigateBackToAccount()
                },
                onDismiss = { pickerRelays = emptyList() },
            )
          }
        }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MullvadClientCountryItem(
    country: RelayCountryJson,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
  Box {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
          Text("${country.code.flag()} ${country.name}", style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
          val n = country.cities.sumOf { it.relays.count { r -> r.active } }
          Text("$n ${stringResource(R.string.servers)}", style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
          if (selected) Icon(Icons.Outlined.Check, contentDescription = null)
        },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MullvadClientCityItem(
    city: RelayCityJson,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
  Box {
    ListItem(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = { Text(city.name, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = {
          val n = city.relays.count { it.active }
          Text("$n ${stringResource(R.string.servers)}", style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
          if (selected) Icon(Icons.Outlined.Check, contentDescription = null)
        },
    )
  }
}
