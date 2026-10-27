package ca.gbc.comp3074.onofua_phillip.lab5

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.material3.Button


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.White
                ) { innerPadding ->
                    LocationComponent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationComponent(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val fusedClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val status = remember { mutableStateOf("Tap to find Location") }
    val startListenerAfterPermission = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationCallback = remember {
        mutableStateOf<LocationCallback?>(null)
    }

    fun stopLocationListener() {
        val callback = locationCallback.value ?: return
        locationCallback.value = null
        fusedClient.removeLocationUpdates(callback)
    }

    DisposableEffect(fusedClient, lifecycleOwner, locationCallback) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                stopLocationListener()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopLocationListener()
        }
    }

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return coarse || fine
    }

    fun fetchLocation() {
        if (!hasPermission()) {
            Toast.makeText(context, "Permission is required", Toast.LENGTH_LONG).show()
            return
        }

        status.value = "Finding location..."
        val tokenSource = CancellationTokenSource()
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener { location ->
            if (location == null) {
                status.value = "Location not available"
            } else {
                status.value = "Location: %.5f, %.5f".format(
                    location.latitude, location.longitude
                )
            }
        }.addOnFailureListener { error ->
            status.value = "Error getting location: ${error.localizedMessage ?: "Unknown error"}"
        }
    }

    fun startLocationListener() {
        if (!hasPermission()) {
            Toast.makeText(context, "Permission is required", Toast.LENGTH_LONG).show()
            return
        }
        if (locationCallback.value != null) return

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (locationCallback.value !== this) return
                for (location in result.locations) {
                    status.value = "Location: %.5f, %.5f".format(
                        location.latitude, location.longitude
                    )
                }
            }
        }
        locationCallback.value = callback

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5_000L
        ).build()
        status.value = "Finding location..."
        fusedClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { error ->
            if (locationCallback.value === callback) {
                locationCallback.value = null
                status.value = "Error getting location: ${error.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            if (startListenerAfterPermission.value) startLocationListener()
            else fetchLocation()
        } else {
            status.value = "Permission denied"
        }
        startListenerAfterPermission.value = false
    }

    LocationContent(
        status = status.value,
        onFindLocation = {
            startListenerAfterPermission.value = false
            if (hasPermission()) fetchLocation()
            else permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onStartListener = {
            if (hasPermission()) startLocationListener()
            else {
                startListenerAfterPermission.value = true
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onStopListener = { stopLocationListener() },
        modifier = modifier
    )
}

@Composable
private fun LocationContent(
    status: String,
    onFindLocation: () -> Unit,
    onStartListener: () -> Unit,
    onStopListener: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 12.dp)
    ) {
        Text(
            text = "Hello $status",
            color = Color(0xFF5F6368),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onFindLocation,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5),
                contentColor = Color.White
            )
        ) {
            Text("Tap to get location")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStartListener,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5),
                contentColor = Color.White
            )
        ) {
            Text("Start Listener")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStopListener,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5),
                contentColor = Color.White
            )
        ) {
            Text("Stop Listener")
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun LocationComponentPreview() {
    MaterialTheme {
        LocationContent(
            status = "Tap to find Location",
            onFindLocation = {},
            onStartListener = {},
            onStopListener = {}
        )
    }
}
