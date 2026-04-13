// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tailscale.ipn.R
import com.tailscale.ipn.mdm.MDMSettings
import com.tailscale.ipn.ui.util.Lists
import com.tailscale.ipn.ui.util.LoadingIndicator
import com.tailscale.ipn.ui.util.flag
import com.tailscale.ipn.ui.util.itemsWithDividers
import com.tailscale.ipn.ui.viewModel.ExitNodePickerNav
import com.tailscale.ipn.ui.viewModel.ExitNodePickerViewModel
import com.tailscale.ipn.ui.viewModel.ExitNodePickerViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MullvadExitNodePicker(
    countryCode: String,
    nav: ExitNodePickerNav,
    model: ExitNodePickerViewModel = viewModel(factory = ExitNodePickerViewModelFactory(nav))
) {
  val mullvadExitNodes by model.mullvadExitNodesByCountryCode.collectAsState()
  val mullvadExitNodesByCountryAndCity by model.mullvadExitNodesByCountryAndCity.collectAsState()
  val bestAvailableByCountry by model.mullvadBestAvailableByCountry.collectAsState()
  val isRunningExitNode by model.isRunningExitNode.collectAsState()
  val forcedExitNodeId = MDMSettings.exitNodeID.flow.collectAsState().value.value
  val canSelectServers = !isRunningExitNode && forcedExitNodeId == null
  val chooseServerText = stringResource(R.string.choose_mullvad_server)
  var pickerTitle by remember { mutableStateOf("") }
  var pickerNodes by remember { mutableStateOf<List<ExitNodePickerViewModel.ExitNode>>(emptyList()) }

  mullvadExitNodes[countryCode]?.toList()?.let { nodes ->
    val any = nodes.first()

    LoadingIndicator.Wrap {
      Scaffold(
          topBar = {
            Header(
                title = { Text("${countryCode.flag()} ${any.country}") },
                onBack = nav.onNavigateBackToMullvad)
          }) { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
              if (nodes.size > 1) {
                val bestAvailableNode = bestAvailableByCountry[countryCode]!!
                item {
                  ExitNodeItem(
                      model,
                      ExitNodePickerViewModel.ExitNode(
                          id = bestAvailableNode.id,
                          label = stringResource(R.string.best_available),
                          online = bestAvailableNode.online,
                          selected = false,
                      ))
                  Lists.SectionDivider()
                }
              }

              itemsWithDividers(nodes) { node ->
                ExitNodeItem(
                    viewModel = model,
                    node = node,
                    onLongClick = {
                      val cityNodes =
                          mullvadExitNodesByCountryAndCity[countryCode]?.get(node.city).orEmpty()
                      if (cityNodes.isNotEmpty()) {
                        pickerTitle = "${node.city} - $chooseServerText"
                        pickerNodes = cityNodes
                      }
                    })
              }
            }
            if (pickerNodes.isNotEmpty()) {
              MullvadExitNodeServerPickerSheet(
                  title = pickerTitle,
                  servers = pickerNodes,
                  canSelectServers = canSelectServers,
                  showCityInSubtitle = false,
                  viewModel = model,
                  onDismiss = { pickerNodes = emptyList() })
            }
          }
    }
  }
}
