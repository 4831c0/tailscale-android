package com.tailscale.ipn.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailscale.ipn.App
import com.tailscale.ipn.ui.util.InputStreamAdapter
import com.tailscale.ipn.ui.util.flag
import java.io.ByteArrayInputStream
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MullvadStatusJson(
    val loggedIn: Boolean = false,
    val account: String = "",
    val wantTunnel: Boolean = false,
    val tunnelState: String = "",
    val tunnelError: String = "",
    val warnings: List<String> = emptyList(),
    val activeFeatures: List<String> = emptyList(),
    val stubbedRequested: List<String> = emptyList(),
    val location: String = "",
    val city: String = "",
    val relayHostname: String = "",
    val deviceName: String = "",
    val wgPublicKey: String = "",
)

@Serializable
data class MullvadSettingsJson(
    val accountNumber: String = "",
    val wantTunnel: Boolean = false,
    val location: String = "",
    val city: String = "",
    val relayHostname: String = "",
    val useMultihop: Boolean = false,
    val entryLocation: String = "",
    val quantumResistant: Boolean = false,
    val daita: Boolean = false,
    val obfuscation: String = "",
    val allowLAN: Boolean = false,
    val lockdown: Boolean = false,
    val autoConnect: Boolean = false,
)

@Serializable data class CapabilityJson(val feature: String, val level: Int, val label: String = "")

@Serializable
data class RelayCountryJson(
    val code: String = "",
    val name: String = "",
    val cities: List<RelayCityJson> = emptyList(),
)

@Serializable
data class RelayCityJson(
    val code: String = "",
    val name: String = "",
    val relays: List<WireguardRelayJson> = emptyList(),
)

@Serializable
data class WireguardRelayJson(
    val hostname: String = "",
    val active: Boolean = false,
)

data class MullvadNav(
    val onNavigateBackToExitNodes: () -> Unit,
    val onNavigateToClientLocation: () -> Unit,
    val onNavigateBackToAccount: () -> Unit,
    val onNavigateToClientCountry: (String) -> Unit,
)

class MullvadViewModel : ViewModel() {
  private val json = Json { ignoreUnknownKeys = true }

  private val _loggedIn = MutableStateFlow(false)
  val loggedIn: StateFlow<Boolean> = _loggedIn

  private val _wantTunnel = MutableStateFlow(false)
  val wantTunnel: StateFlow<Boolean> = _wantTunnel

  private val _statusText = MutableStateFlow("")
  val statusText: StateFlow<String> = _statusText

  private val _settings = MutableStateFlow(MullvadSettingsJson())
  val settings: StateFlow<MullvadSettingsJson> = _settings

  private val _capabilities = MutableStateFlow<Map<String, Int>>(emptyMap())
  val capabilities: StateFlow<Map<String, Int>> = _capabilities

  private val _relays = MutableStateFlow<List<RelayCountryJson>>(emptyList())
  val relays: StateFlow<List<RelayCountryJson>> = _relays

  private val _deviceName = MutableStateFlow("")
  val deviceName: StateFlow<String> = _deviceName

  private val _wgPublicKey = MutableStateFlow("")
  val wgPublicKey: StateFlow<String> = _wgPublicKey

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading

  private val _tunnelState = MutableStateFlow("")
  val tunnelState: StateFlow<String> = _tunnelState

  private val _statusLocation = MutableStateFlow("")
  val statusLocation: StateFlow<String> = _statusLocation

  private val _statusCity = MutableStateFlow("")
  val statusCity: StateFlow<String> = _statusCity

  private val _statusRelayHostname = MutableStateFlow("")
  val statusRelayHostname: StateFlow<String> = _statusRelayHostname

  val isMullvadActive: Boolean
    get() = _wantTunnel.value && _tunnelState.value == "connected"

  fun mullvadExitNodeLabel(): String {
    val loc = _statusLocation.value
    val city = _statusCity.value
    val relay = _statusRelayHostname.value
    if (loc.isEmpty()) return ""
    val countryName =
        _relays.value.find { it.code.equals(loc, ignoreCase = true) }?.name ?: loc.uppercase()
    val serverId = relay.substringAfterLast("-", "")
    val flag = loc.flag()
    return buildString {
      append("$flag $countryName/$city")
      if (serverId.isNotEmpty()) append(" ($serverId)")
    }
  }

  private val obfuscationModes =
      listOf("auto", "off", "udp2tcp", "shadowsocks", "wireguard_port", "quic", "lwo")

  init {
    refresh()
    fetchRelays()
  }

  fun capabilityLevel(feature: String): Int = _capabilities.value[feature] ?: 2

  fun isStub(feature: String): Boolean = capabilityLevel(feature) == 1

  fun isImplemented(feature: String): Boolean = capabilityLevel(feature) == 0

  fun implementedObfuscationModes(): List<String> =
      obfuscationModes.filter { mode ->
        when (mode) {
          "off" -> true
          "auto" -> isImplemented("obfs_auto")
          "udp2tcp" -> isImplemented("obfs_udp2tcp")
          "shadowsocks" -> isImplemented("obfs_shadowsocks")
          "wireguard_port" -> isImplemented("obfs_wg_port")
          "quic" -> isImplemented("obfs_quic")
          "lwo" -> isImplemented("obfs_lwo")
          else -> false
        }
      }

  fun refresh() {
    loadCapabilities()
    mullvadGet("status") { result ->
      result
          .onSuccess { body ->
            val st = json.decodeFromString<MullvadStatusJson>(body)
            _loggedIn.value = st.loggedIn
            _wantTunnel.value = st.wantTunnel
            _deviceName.value = st.deviceName
            _wgPublicKey.value = st.wgPublicKey
            _tunnelState.value = st.tunnelState
            _statusLocation.value = st.location
            _statusCity.value = st.city
            _statusRelayHostname.value = st.relayHostname
            val lines = mutableListOf<String>()
            if (st.account.isNotEmpty()) lines += "Account: ${redactAccount(st.account)}"
            if (st.deviceName.isNotEmpty()) lines += "Device: ${st.deviceName}"
            if (st.wgPublicKey.isNotEmpty()) lines += "WG key: ${st.wgPublicKey}"
            if (st.tunnelState.isNotEmpty()) lines += "Tunnel: ${st.tunnelState}"
            if (st.location.isNotEmpty()) {
              val loc = buildString {
                append(st.location.uppercase())
                if (st.city.isNotEmpty()) append(" / ${st.city}")
                if (st.relayHostname.isNotEmpty()) append(" / ${st.relayHostname}")
              }
              lines += "Location: $loc"
            }
            st.warnings.forEach { lines += "Warning: $it" }
            st.stubbedRequested.forEach { lines += "Stubbed: $it" }
            if (st.tunnelError.isNotEmpty()) lines += "Error: ${st.tunnelError}"
            _statusText.value = lines.joinToString("\n")
          }
          .onFailure { _statusText.value = it.message ?: "error" }
    }
    mullvadGet("settings") { result ->
      result.onSuccess { body ->
        _settings.value = json.decodeFromString<MullvadSettingsJson>(body)
      }
    }
  }

  fun fetchRelays(onDone: (() -> Unit)? = null) {
    mullvadGet("relays") { result ->
      result.onSuccess { body ->
        _relays.value = json.decodeFromString<List<RelayCountryJson>>(body)
      }
      onDone?.invoke()
    }
  }

  fun login(account: String) {
    val payload =
        """{"accountNumber":"${account.replace(" ", "")}"}""".toByteArray(Charsets.UTF_8)
    mullvadPost("login", payload) { refresh() }
  }

  fun logout() {
    mullvadPost("logout", null) { refresh() }
  }

  fun connect(onDone: (() -> Unit)? = null) {
    _loading.value = true
    mullvadPost("connect", null) {
      _loading.value = false
      refresh()
      onDone?.invoke()
    }
  }

  fun disconnect(onDone: (() -> Unit)? = null) {
    _loading.value = true
    mullvadPost("disconnect", null) {
      _loading.value = false
      refresh()
      onDone?.invoke()
    }
  }

  fun updateSettings(
      transform: (MullvadSettingsJson) -> MullvadSettingsJson,
      onDone: (() -> Unit)? = null,
  ) {
    val updated = transform(_settings.value)
    mullvadPut("settings", json.encodeToString(updated).toByteArray()) {
      refresh()
      onDone?.invoke()
    }
  }

  fun setQuantum(on: Boolean) {
    updateSettings(transform = { it.copy(quantumResistant = on) })
  }

  fun setAllowLAN(on: Boolean) {
    updateSettings(transform = { it.copy(allowLAN = on) })
  }

  fun setObfuscation(mode: String) {
    updateSettings(transform = { it.copy(obfuscation = mode) })
  }

  fun setLocation(countryCode: String, cityCode: String, connectIfWanted: Boolean = false) {
    updateSettings(
        transform = { it.copy(location = countryCode, city = cityCode, relayHostname = "") },
    ) {
      if (connectIfWanted && (_wantTunnel.value || _settings.value.wantTunnel)) {
        connect()
      }
    }
  }

  fun setRelay(
      countryCode: String,
      cityCode: String,
      hostname: String,
      connectIfWanted: Boolean = false,
  ) {
    updateSettings(
        transform = {
          it.copy(location = countryCode, city = cityCode, relayHostname = hostname)
        },
    ) {
      if (connectIfWanted && (_wantTunnel.value || _settings.value.wantTunnel)) {
        connect()
      }
    }
  }

  private fun redactAccount(account: String): String {
    val digits = account.filter { it.isDigit() }
    if (digits.length <= 4) return "****"
    return "****${digits.takeLast(4)}"
  }

  private fun loadCapabilities() {
    mullvadGet("capabilities") { result ->
      result.onSuccess { body ->
        val caps = json.decodeFromString<List<CapabilityJson>>(body)
        _capabilities.value = caps.associate { it.feature to it.level }
      }
    }
  }

  private fun mullvadGet(suffix: String, handler: (Result<String>) -> Unit) {
    mullvadRaw("GET", "mullvad/$suffix", null, handler)
  }

  private fun mullvadPost(suffix: String, body: ByteArray?, onDone: () -> Unit) {
    mullvadRaw("POST", "mullvad/$suffix", body) {
      onDone()
    }
  }

  private fun mullvadPut(suffix: String, body: ByteArray?, onDone: () -> Unit) {
    mullvadRaw("PUT", "mullvad/$suffix", body) {
      onDone()
    }
  }

  private fun mullvadRaw(
      method: String,
      endpoint: String,
      body: ByteArray?,
      handler: (Result<String>) -> Unit,
  ) {
    viewModelScope.launch {
      try {
        val app = App.get().getLibtailscaleApp()
        val stream = body?.let { InputStreamAdapter(ByteArrayInputStream(it)) }
        val resp = app.callLocalAPI(180000, method, "/localapi/v0/$endpoint", stream)
        val text = resp.bodyBytes()?.decodeToString() ?: ""
        handler(Result.success(text))
      } catch (e: Exception) {
        handler(Result.failure(e))
      }
    }
  }
}
