package com.tailscale.ipn.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.theme.disabledListItem
import com.tailscale.ipn.ui.theme.listItem
import com.tailscale.ipn.ui.viewModel.WireguardRelayJson

data class ClientRelayPick(
    val countryCode: String,
    val cityCode: String,
    val relay: WireguardRelayJson,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MullvadClientRelayPickerSheet(
    title: String,
    relays: List<ClientRelayPick>,
    selectedHostname: String,
    onSelect: (ClientRelayPick) -> Unit,
    onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
      item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth())
      }
      items(relays, key = { it.relay.hostname }) { pick ->
        val active = pick.relay.active
        val selected = pick.relay.hostname.equals(selectedHostname, ignoreCase = true)
        ListItem(
            modifier =
                if (active) {
                  Modifier.clickable { onSelect(pick) }
                } else {
                  Modifier
                },
            colors =
                if (active) {
                  MaterialTheme.colorScheme.listItem
                } else {
                  MaterialTheme.colorScheme.disabledListItem
                },
            headlineContent = {
              Text(pick.relay.hostname, style = MaterialTheme.typography.bodyMedium)
            },
            supportingContent = {
              if (!active) {
                Text(
                    text = stringResource(R.string.offline),
                    style = MaterialTheme.typography.bodyMedium)
              }
            },
            trailingContent = {
              if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.selected))
              }
            })
      }
    }
  }
}
