package com.tailscale.ipn.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.util.Lists
import com.tailscale.ipn.ui.util.LoadingIndicator
import com.tailscale.ipn.ui.viewModel.MullvadNav
import com.tailscale.ipn.ui.viewModel.MullvadViewModel

@Composable
fun MullvadSettingsView(nav: MullvadNav, model: MullvadViewModel) {
  val loggedIn by model.loggedIn.collectAsState()
  val wantTunnel by model.wantTunnel.collectAsState()
  val statusText by model.statusText.collectAsState()
  val settings by model.settings.collectAsState()
  val loading by model.loading.collectAsState()
  val capabilities by model.capabilities.collectAsState()
  var account by remember { mutableStateOf("") }
  val obfsModes = remember(capabilities) { model.implementedObfuscationModes() }

  LoadingIndicator.Wrap {
    Scaffold(topBar = { Header(R.string.mullvad_settings_title, onBack = nav.onNavigateBackToExitNodes) }) {
        innerPadding ->
      Column(
          modifier =
              Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (statusText.isNotEmpty()) {
              Text(statusText, style = MaterialTheme.typography.bodyMedium)
            }

            if (!loggedIn) {
              OutlinedTextField(
                  value = account,
                  onValueChange = { account = it },
                  label = { Text(stringResource(R.string.mullvad_account_number)) },
                  modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
              Button(
                  onClick = { model.login(account) },
                  modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                  enabled = !loading) {
                Text(stringResource(R.string.mullvad_login))
              }
            } else {
              if (wantTunnel) {
                Button(
                    onClick = { model.disconnect() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !loading) {
                  Text(stringResource(R.string.mullvad_disconnect))
                }
              } else {
                Button(
                    onClick = { model.connect() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !loading) {
                  Text(stringResource(R.string.mullvad_connect))
                }
              }
              Button(
                  onClick = { model.logout() },
                  modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                  enabled = !loading) {
                Text(stringResource(R.string.mullvad_logout))
              }

              Lists.SectionDivider()
              ListItem(
                  modifier = Modifier.clickable { nav.onNavigateToClientLocation() },
                  headlineContent = { Text(stringResource(R.string.mullvad_location)) },
                  supportingContent = {
                    val loc =
                        if (settings.location.isNotEmpty()) {
                          buildString {
                            append(settings.location.uppercase())
                            if (settings.city.isNotEmpty()) append(" / ${settings.city}")
                            if (settings.relayHostname.isNotEmpty()) {
                              append(" / ${settings.relayHostname}")
                            }
                          }
                        } else {
                          stringResource(R.string.mullvad_location_default)
                        }
                    Text(loc)
                  },
              )

              Lists.SectionDivider()
              Text(
                  stringResource(R.string.mullvad_quantum_resistance),
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.padding(top = 8.dp))
              Switch(
                  checked = settings.quantumResistant,
                  onCheckedChange = { model.setQuantum(it) },
                  enabled = !loading && model.isImplemented("pq"),
              )

              Text(
                  stringResource(R.string.allow_lan_access),
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.padding(top = 8.dp))
              Switch(
                  checked = settings.allowLAN,
                  onCheckedChange = { model.setAllowLAN(it) },
                  enabled = !loading && model.isImplemented("allow_lan"),
              )

              Text(
                  stringResource(R.string.mullvad_obfuscation),
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.padding(top = 8.dp))
              val currentObfs = settings.obfuscation.ifEmpty { "auto" }
              obfsModes.forEach { mode ->
                ListItem(
                    modifier = Modifier.clickable(enabled = !loading) { model.setObfuscation(mode) },
                    headlineContent = { Text(mode) },
                    trailingContent = {
                      if (currentObfs == mode) {
                        Text("✓", style = MaterialTheme.typography.bodyLarge)
                      }
                    },
                )
              }

              Lists.SectionDivider()
              Text(
                  stringResource(R.string.mullvad_advanced_stubbed),
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.padding(top = 8.dp))
              StubFeatureRow(stringResource(R.string.mullvad_daita), model.isStub("daita"))
              StubFeatureRow(stringResource(R.string.mullvad_multihop), model.isStub("multihop"))
              StubFeatureRow(stringResource(R.string.mullvad_lockdown), model.isStub("lockdown"))
            }
          }
    }
  }
}

@Composable
private fun StubFeatureRow(label: String, stubbed: Boolean) {
  ListItem(
      headlineContent = { Text(label) },
      supportingContent = {
        if (stubbed) Text(stringResource(R.string.mullvad_feature_unavailable))
      },
      trailingContent = {
        Switch(checked = false, onCheckedChange = {}, enabled = !stubbed)
      },
  )
}
