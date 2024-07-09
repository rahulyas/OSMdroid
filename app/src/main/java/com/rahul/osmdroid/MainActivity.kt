package com.rahul.osmdroid

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint

class MainActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Location permission is granted. Proceed with displaying the map.
            setContent {
                OSMMapScreen()
            }
        } else {
            // Permission is denied. Handle the case accordingly.
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            // Permissions are already granted
            setContent {
                OSMMapScreen()
            }
        }
    }
}

@Suppress("NAME_SHADOWING")
@Preview
@Composable
fun OSMMapScreen() {
    val mapView = rememberMapViewWithLifecycle()

    val pointsList = listOf(
        SurveyDetailModel(1, "Point1", "northing1", "easting1", "elevation1", "zone1", "26.905734", "75.733757", "red"),
        SurveyDetailModel(2, "Point2", "northing2", "easting2", "elevation2", "zone2", "26.905964", "75.735688", "blue"),
        SurveyDetailModel(3, "Point3", "northing3", "easting3", "elevation3", "zone3", "26.905619", "75.742855", "red")
    )

    val points = pointsList.map { LabelledGeoPoint(it.latitude.toDoubleOrNull() ?: 0.0, it.longitude.toDoubleOrNull() ?: 0.0,it.pointName) }


    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }
    val pointPlot by lazy {
        PointPlot { surveyDetailModel ->
            Log.i("MapScreen", "Point clicked: ${surveyDetailModel.pointName}")

        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            createMapView(context).apply {
                locationOverlay = createLocationOverlay(this) { geoPoint ->
                    updateCurrentLocationMarker(this, geoPoint, currentLocationMarker) {
                        currentLocationMarker = it
                    }
                }
                overlays.add(locationOverlay)

                val overlay = pointPlot.plotPointOnMap(context, mapView, points, pointsList, isClick = true)
                if (overlay == null) {
                    Log.e("MapScreen", "Failed to create overlay")
                } else {
                    overlays.add(overlay)
                    Log.i("MapScreen", "Overlay added successfully")
                }
                invalidate()
            }
        },
        update = { mapView ->
            locationOverlay?.runOnFirstFix {
                updateMarkerPosition(mapView, locationOverlay, currentLocationMarker)
            }
        }
    )

    DisposableEffect(mapView) {
        onDispose {
            locationOverlay?.disableMyLocation()
        }
    }
}




