package com.lasertrac.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.lasertrac.app.db.SavedSnapLocationEntity
import com.lasertrac.app.db.SnapLocationDao
import com.lasertrac.app.ui.theme.Lasertac2Theme
import com.lasertrac.app.ui.theme.TextColorLight
import com.lasertrac.app.ui.theme.TopBarColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Locale

// --- Updated Data structures for cascading dropdowns ---
data class District(val name: String, val primaryZip: String)
data class City(val name: String, val districts: List<District>)
data class StateData(val name: String, val cities: List<City>)

val stateCityDistrictDataSource = listOf(
    StateData("Delhi", listOf(
        City("Delhi Central", listOf(
            District("Connaught Place", "110001"),
            District("Karol Bagh", "110005")
        )),
        City("Delhi South", listOf(
            District("Saket", "110017"),
            District("Hauz Khas", "110016")
        ))
    )),
    StateData("Maharashtra", listOf(
        City("Mumbai", listOf(
            District("Mumbai City South", "400001"),
            District("Bandra", "400050")
        )),
        City("Pune", listOf(
            District("Pune City", "411001"),
            District("Hinjewadi", "411057")
        ))
    ))
)

val allPoliceStationsSource = listOf(
    "Connaught Place PS", "Karol Bagh PS", "Saket PS", "Hauz Khas PS",
    "Colaba PS", "Bandra PS", "Shivaji Nagar PS", "Hinjewadi PS"
)
// --- End of data structures ---

data class CurrentLocationDetails(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressLine: String = "N/A",
    val city: String = "N/A",        // From Address.locality
    val district: String = "N/A",    // From Address.subLocality or subAdminArea
    val state: String = "N/A",       // From Address.adminArea
    val country: String = "N/A",
    val postalCode: String = "N/A"
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onNavigateBack: () -> Unit,
    snapId: String,
    snapLocationDao: SnapLocationDao
) {
    val context = LocalContext.current
    var locationDetails by remember { mutableStateOf<CurrentLocationDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorStateMessage by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    var manualState by remember { mutableStateOf("") }
    var manualCity by remember { mutableStateOf("") }
    var manualDistrict by remember { mutableStateOf("") }
    var manualPoliceStation by remember { mutableStateOf("") }
    var manualZipCode by remember { mutableStateOf("") }

    var citiesInSelectedState by remember { mutableStateOf<List<City>>(emptyList()) }
    var districtsInSelectedCity by remember { mutableStateOf<List<District>>(emptyList()) }

    // State to hold the existing entity to prevent data loss on update
    var existingSnapEntity by remember { mutableStateOf<SavedSnapLocationEntity?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val isPreview = LocalContext.current.javaClass.simpleName.contains("Preview")

    // Load existing entity to preserve fields like imageUri on save
    LaunchedEffect(snapId) {
        if (!isPreview) {
            existingSnapEntity = snapLocationDao.getSnapLocationById(snapId).firstOrNull()
        }
    }

    LaunchedEffect(locationDetails) {
        locationDetails?.let { details ->
            if (manualState.isEmpty() && details.state != "N/A") {
                val matchedStateData = stateCityDistrictDataSource.firstOrNull { it.name.equals(details.state, ignoreCase = true) }
                if (matchedStateData != null) {
                    manualState = matchedStateData.name
                    citiesInSelectedState = matchedStateData.cities
                    if (manualCity.isEmpty() && details.city != "N/A") {
                        val matchedCityData = matchedStateData.cities.firstOrNull { it.name.equals(details.city, ignoreCase = true) }
                        if (matchedCityData != null) {
                            manualCity = matchedCityData.name
                            districtsInSelectedCity = matchedCityData.districts
                            if (manualDistrict.isEmpty() && details.district != "N/A") {
                                val matchedDistrictData = matchedCityData.districts.firstOrNull { it.name.equals(details.district, ignoreCase = true) }
                                if (matchedDistrictData != null) {
                                    manualDistrict = matchedDistrictData.name
                                    manualZipCode = matchedDistrictData.primaryZip
                                }
                            }
                        }
                    }
                }
            }
            if (manualZipCode.isEmpty() && details.postalCode != "N/A") {
                manualZipCode = details.postalCode
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            hasPermission = isGranted
            if (isGranted) {
                isLoading = true; errorStateMessage = null; saveMessage = null
                fetchDeviceLocation(context, fusedLocationClient, geocoder) { result ->
                    isLoading = false
                    result.fold(
                        onSuccess = { locDetails -> locationDetails = locDetails },
                        onFailure = { exception -> errorStateMessage = exception.message ?: "Error fetching location." }
                    )
                }
            } else {
                isLoading = false; errorStateMessage = "Location permission denied."
            }
        }
    )

    LaunchedEffect(Unit) {
        if (isPreview) {
            isLoading = false
            locationDetails = CurrentLocationDetails(28.6139, 77.2090, "Connaught Place, Delhi Central, Delhi, India 110001", "Delhi Central", "Connaught Place", "Delhi", "India", "110001")
            manualState = "Delhi"
            citiesInSelectedState = stateCityDistrictDataSource.firstOrNull { it.name == "Delhi" }?.cities ?: emptyList()
            manualCity = "Delhi Central"
            districtsInSelectedCity = citiesInSelectedState.firstOrNull { it.name == "Delhi Central" }?.districts ?: emptyList()
            manualDistrict = "Connaught Place"
            manualPoliceStation = "Connaught Place PS"
            manualZipCode = "110001"
        } else {
            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true
                fetchDeviceLocation(context, fusedLocationClient, geocoder) { result ->
                    isLoading = false
                    result.fold(
                        onSuccess = { locDetails -> locationDetails = locDetails },
                        onFailure = { exception -> errorStateMessage = exception.message ?: "Error fetching location initially." }
                    )
                }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Current Location", color = TextColorLight, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextColorLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TopBarColor.copy(alpha = 0.9f))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 50.dp))
                    Text("Fetching location data...", modifier = Modifier.padding(top = 8.dp))
                }
                errorStateMessage != null -> {
                    LocationDisplayErrorCard(
                        errorMessage = errorStateMessage!!,
                        onRetry = {
                            if (!hasPermission) {
                                if (isPreview) { errorStateMessage = "Retry in Preview not supported."; return@LocationDisplayErrorCard }
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                isLoading = true; errorStateMessage = null; saveMessage = null
                                fetchDeviceLocation(context, fusedLocationClient, geocoder) { result ->
                                    isLoading = false
                                    result.fold(
                                        onSuccess = { details -> locationDetails = details },
                                        onFailure = { exception -> errorStateMessage = exception.message ?: "Error on retry." }
                                    )
                                }
                            }
                        }
                    )
                }
                locationDetails != null -> {
                    CurrentLocationInfoCard(details = locationDetails!!)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { // Update GPS Location
                            if (isPreview) {
                                isLoading = true; saveMessage = null
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(500)
                                    locationDetails = CurrentLocationDetails(28.7041, 77.1025, "456 Updated Preview Ave", "Gurugram", "Sector 29", "Haryana", "India", "122001")
                                    isLoading = false; saveMessage = "Preview location refreshed."
                                }
                                return@Button
                            }
                            if (!hasPermission) { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                            else { isLoading = true; errorStateMessage = null; saveMessage = null
                                fetchDeviceLocation(context, fusedLocationClient, geocoder) { result -> isLoading = false
                                    result.fold( onSuccess = { details -> locationDetails = details; saveMessage = "Location Updated Successfully!" }, onFailure = { exception -> errorStateMessage = exception.message ?: "Error updating location." } )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Current Location (GPS)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { // Save GPS Location
                            locationDetails?.let { currentDetails ->
                                coroutineScope.launch {
                                    val entityToSave = existingSnapEntity?.copy(
                                        latitude = currentDetails.latitude,
                                        longitude = currentDetails.longitude,
                                        fullAddress = currentDetails.addressLine,
                                        district = currentDetails.district,
                                        country = currentDetails.country,
                                        selectedCity = currentDetails.city,
                                        selectedState = currentDetails.state
                                    ) ?: return@launch // Should not happen if snapId is valid

                                    snapLocationDao.insertOrUpdateSnapLocation(entityToSave)
                                    saveMessage = "GPS Location Saved! (City: ${entityToSave.selectedCity}, Dist: ${entityToSave.district})"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Current Location (GPS)")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    ManualLocationEntryCard(
                        manualState = manualState,
                        onManualStateChange = {
                            manualState = it
                            citiesInSelectedState = stateCityDistrictDataSource.firstOrNull { s -> s.name == it }?.cities ?: emptyList()
                            manualCity = ""; districtsInSelectedCity = emptyList()
                            manualDistrict = ""; manualZipCode = ""
                        },
                        availableStates = stateCityDistrictDataSource.map { it.name },
                        manualCity = manualCity,
                        onManualCityChange = {
                            manualCity = it
                            districtsInSelectedCity = citiesInSelectedState.firstOrNull { c -> c.name == it }?.districts ?: emptyList()
                            manualDistrict = ""; manualZipCode = ""
                        },
                        availableCities = citiesInSelectedState,
                        manualDistrict = manualDistrict,
                        onManualDistrictChange = {
                            manualDistrict = it
                            manualZipCode = districtsInSelectedCity.firstOrNull { d -> d.name == it }?.primaryZip ?: ""
                        },
                        availableDistricts = districtsInSelectedCity,
                        manualPoliceStation = manualPoliceStation,
                        onManualPoliceStationChange = { manualPoliceStation = it },
                        availablePoliceStations = allPoliceStationsSource,
                        manualZipCode = manualZipCode,
                        onManualZipCodeChange = { manualZipCode = it },
                        onSaveManualLocation = {
                            coroutineScope.launch {
                                val entityToSave = existingSnapEntity?.copy(
                                    fullAddress = "$manualDistrict, $manualCity, $manualState $manualZipCode".trim().replaceFirst("^N/A, ", "").replaceFirst(", N/A", ""),
                                    selectedState = manualState.ifBlank { locationDetails?.state ?: "N/A" },
                                    selectedCity = manualCity.ifBlank { locationDetails?.city ?: "N/A" },
                                    district = manualDistrict.ifBlank { locationDetails?.district ?: "N/A" },
                                    selectedPoliceArea = manualPoliceStation.ifBlank { "N/A" }
                                ) ?: return@launch // Should not happen if snapId is valid

                                snapLocationDao.insertOrUpdateSnapLocation(entityToSave)
                                saveMessage = "Manual Location Saved!"
                                Log.d("LocationScreen", "Manual location saved for $snapId as $entityToSave")

                                locationDetails = locationDetails?.copy(
                                    addressLine = entityToSave.fullAddress, city = entityToSave.selectedCity, state = entityToSave.selectedState,
                                    postalCode = manualZipCode, district = entityToSave.district
                                )
                            }
                        }
                    )
                    saveMessage?.let {
                        Text(
                            text = it,
                            color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error
                            else if (it.contains("Saved")) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                else -> {
                    Text("Acquiring location... Ensure services and permissions are enabled.", textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 50.dp))
                    Button(onClick = {
                        if (isPreview) { saveMessage = "Permission check N/A in Preview." }
                        else { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                    }) {
                        Text("Check Permissions")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationEntryCard(
    manualState: String, onManualStateChange: (String) -> Unit, availableStates: List<String>,
    manualCity: String, onManualCityChange: (String) -> Unit, availableCities: List<City>,
    manualDistrict: String, onManualDistrictChange: (String) -> Unit, availableDistricts: List<District>,
    manualPoliceStation: String, onManualPoliceStationChange: (String) -> Unit, availablePoliceStations: List<String>,
    manualZipCode: String, onManualZipCodeChange: (String) -> Unit,
    onSaveManualLocation: () -> Unit
) {
    var stateExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var policeStationExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EditLocation, "Manual Entry", Modifier.size(24.dp).padding(end = 8.dp))
                Text("Select Location Manually", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = stateExpanded, onExpandedChange = { stateExpanded = !stateExpanded }, modifier = Modifier.fillMaxWidth()) {
                TextField(value = manualState, onValueChange = {}, readOnly = true, label = { Text("State") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
                    availableStates.forEach { stateName -> DropdownMenuItem(text = { Text(stateName) }, onClick = { onManualStateChange(stateName); stateExpanded = false }) }
                }
            }
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { if (availableCities.isNotEmpty()) cityExpanded = !cityExpanded }, modifier = Modifier.fillMaxWidth()) {
                TextField(value = manualCity, onValueChange = {}, readOnly = true, label = { Text("City") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), enabled = availableCities.isNotEmpty())
                ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                    availableCities.forEach { city -> DropdownMenuItem(text = { Text(city.name) }, onClick = { onManualCityChange(city.name); cityExpanded = false }) }
                }
            }
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = { if (availableDistricts.isNotEmpty()) districtExpanded = !districtExpanded }, modifier = Modifier.fillMaxWidth()) {
                TextField(value = manualDistrict, onValueChange = {}, readOnly = true, label = { Text("District") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), enabled = availableDistricts.isNotEmpty())
                ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                    availableDistricts.forEach { district -> DropdownMenuItem(text = { Text(district.name) }, onClick = { onManualDistrictChange(district.name); districtExpanded = false }) }
                }
            }
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = policeStationExpanded, onExpandedChange = { policeStationExpanded = !policeStationExpanded }, modifier = Modifier.fillMaxWidth()) {
                TextField(value = manualPoliceStation, onValueChange = {}, readOnly = true, label = { Text("Police Station Area") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = policeStationExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = policeStationExpanded, onDismissRequest = { policeStationExpanded = false }) {
                    availablePoliceStations.forEach { psName -> DropdownMenuItem(text = { Text(psName) }, onClick = { onManualPoliceStationChange(psName); policeStationExpanded = false }) }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = manualZipCode, onValueChange = onManualZipCodeChange, label = { Text("Zip Code (Auto-filled, Editable)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSaveManualLocation, modifier = Modifier.fillMaxWidth()) {
                Text("Save Manual Details")
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: Context, client: FusedLocationProviderClient, geocoder: Geocoder, callback: (Result<CurrentLocationDetails>) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        callback(Result.failure(Exception("GPS/Network disabled."))); return
    }
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { location ->
            if (location == null) { callback(Result.failure(Exception("Failed to get current location (null)."))); return@addOnSuccessListener }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        addresses.firstOrNull()?.let { callback(Result.success(mapAndroidAddressToDetails(it, location.latitude, location.longitude))) } ?: callback(Result.success(CurrentLocationDetails(latitude = location.latitude, longitude = location.longitude, addressLine = "-")))
                    }
                } else {
                    @Suppress("DEPRECATION") val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.let { callback(Result.success(mapAndroidAddressToDetails(it, location.latitude, location.longitude))) } ?: callback(Result.success(CurrentLocationDetails(latitude = location.latitude, longitude = location.longitude, addressLine = "-")))
                }
            } catch (e: Exception) { callback(Result.failure(Exception("Could not determine address: ${e.message}"))) }
        }
        .addOnFailureListener { e -> callback(Result.failure(Exception("Could not fetch location: ${e.message}"))) }
}

private fun mapAndroidAddressToDetails(address: Address, lat: Double, lon: Double): CurrentLocationDetails {
    return CurrentLocationDetails(
        latitude = lat, longitude = lon,
        addressLine = address.getAddressLine(0) ?: "N/A",
        city = address.locality ?: "N/A",
        district = address.subLocality ?: address.subAdminArea ?: "N/A",
        state = address.adminArea ?: "N/A",
        country = address.countryName ?: "N/A",
        postalCode = address.postalCode ?: "N/A"
    )
}

@Composable
fun CurrentLocationInfoCard(details: CurrentLocationDetails) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Device Location", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            LocationDetailRow(label = "Lat/Lon:", value = String.format(Locale.US, "%.5f, %.5f", details.latitude, details.longitude))
            LocationDetailRow(label = "Address:", value = details.addressLine)
            LocationDetailRow(label = "City:", value = details.city)
            LocationDetailRow(label = "District:", value = details.district)
            LocationDetailRow(label = "State:", value = details.state)
            LocationDetailRow(label = "Postal Code:", value = details.postalCode)
            LocationDetailRow(label = "Country:", value = details.country)
        }
    }
}

@Composable
fun LocationDisplayErrorCard(errorMessage: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Location Data Error", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(errorMessage, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(if (errorMessage.contains("permission denied")) "Grant Permission" else "Retry")
            }
        }
    }
}

@Composable
private fun LocationDetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}

class LocationScreenMockDao : SnapLocationDao {
    private val mockStorage = mutableMapOf<String, SavedSnapLocationEntity>()
    override suspend fun insertOrUpdateSnapLocation(snapLocation: SavedSnapLocationEntity) { mockStorage[snapLocation.snapId] = snapLocation }
    override fun getSnapLocationById(snapId: String): Flow<SavedSnapLocationEntity?> = flowOf(mockStorage[snapId])
    override fun getAllSnapLocations(): Flow<List<SavedSnapLocationEntity>> = flowOf(mockStorage.values.toList())
    override suspend fun deleteSnapsByIds(snapIds: List<String>) {
        snapIds.forEach { mockStorage.remove(it) }
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun LocationScreenPreview() {
    Lasertac2Theme { LocationScreen(onNavigateBack = {}, snapId = "preview_snap_id", snapLocationDao = LocationScreenMockDao()) }
}