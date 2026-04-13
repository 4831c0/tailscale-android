// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.theme.disabledListItem
import com.tailscale.ipn.ui.theme.listItem
import com.tailscale.ipn.ui.viewModel.ExitNodePickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MullvadExitNodeServerPickerSheet(
    title: String,
    servers: List<ExitNodePickerViewModel.ExitNode>,
    canSelectServers: Boolean,
    showCityInSubtitle: Boolean,
    viewModel: ExitNodePickerViewModel,
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
      items(servers, key = { it.id ?: it.label }) { server ->
        val online by server.online.collectAsState()
        val canSelect = canSelectServers && online
        ListItem(
            modifier =
                if (canSelect) {
                  Modifier.clickable { viewModel.setExitNode(server) }
                } else {
                  Modifier
                },
            colors =
                if (canSelectServers && online) {
                  MaterialTheme.colorScheme.listItem
                } else {
                  MaterialTheme.colorScheme.disabledListItem
                },
            headlineContent = { Text(server.label, style = MaterialTheme.typography.bodyMedium) },
            supportingContent = {
              when {
                !online ->
                    Text(
                        text = stringResource(R.string.offline),
                        style = MaterialTheme.typography.bodyMedium)
                showCityInSubtitle ->
                    Text(text = server.city, style = MaterialTheme.typography.bodyMedium)
              }
            },
            trailingContent = {
              Row {
                if (server.selected) {
                  Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.selected))
                }
              }
            })
      }
    }
  }
}
