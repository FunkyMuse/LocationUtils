package com.crazylegend.locationhelpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*


/**
 * Created by crazy on 4/28/20 to long live and prosper !
 */

/**
 * Initialize location request
 * usually
 *  locationRequest.interval = LOCATION_REQ_TIME
 *  locationRequest.fastestInterval = LOCATION_REQ_TIME
 *  locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
 */
fun initFusedLocationRequest(request: LocationRequest.() -> Unit): LocationRequest {
    val locationRequest = LocationRequest()
    locationRequest.request()
    return locationRequest
}


val Context.fusedLocationProvider get() = FusedLocationProviderClient(this)

val Activity.fusedLocationProvider get() = FusedLocationProviderClient(this)

inline fun fusedLocationCallback(crossinline callbackResult: (locationResult: LocationResult, lastLocation: Location) -> Unit = { _, _ -> }): LocationCallback =
    object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val lastLocation = locationResult.lastLocation
            callbackResult(locationResult, lastLocation)
        }
    }


inline fun LocationRequest.setup(
        context: Context,
        crossinline onError: (Exception) -> Unit = {},
        crossinline resolvableApiException: (ResolvableApiException) -> Unit = {},
        crossinline onSuccess: (response: LocationSettingsResponse) -> Unit = {}
) {
    val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(this)

    val client = LocationServices.getSettingsClient(context)
    val task = client.checkLocationSettings(builder.build())

    task.addOnSuccessListener {
        onSuccess(it)
    }
    task.addOnFailureListener { e ->
        onError(e)
        if (e is ResolvableApiException) {
            // Location settings are not satisfied, but this can be fixed
            // by showing the user a dialog.
            try {
                // Show the dialog by calling startResolutionForResult(),
                // and check the data in onActivityResult().
                /*e.startResolutionForResult(activity,REQUEST_CHECK_SETTINGS)*/
                resolvableApiException(e)
            } catch (sendEx: IntentSender.SendIntentException) {
                // Ignore the error.
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun FusedLocationProviderClient.startLocationUpdates(
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    requestLocationUpdates(locationRequest, locationCallback, null)
}

fun FusedLocationProviderClient.stopLocationUpdates(locationCallback: LocationCallback) {
    removeLocationUpdates(locationCallback)
}

@SuppressLint("MissingPermission")
inline fun FusedLocationProviderClient.lastLocation(crossinline callback:(Location)->Unit){
    lastLocation.addOnCompleteListener {
        val result = it.result
        result?.apply {
            callback(this)
        }
    }
}

fun AppCompatActivity.enableGPS(requestCode: Int? = null) {
    if (!isLocationEnabled) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        if (requestCode == null) {
            startActivity(intent)
        } else {
            startActivityForResult(intent, requestCode)
        }
    }
}

fun enableGPSIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)


fun Context.enableGPS() {
    if (!isLocationEnabled) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }
}

val Context.isLocationEnabled: Boolean
    get() = (getSystemService(Context.LOCATION_SERVICE) as LocationManager?)?.isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
    ) ?: false


inline fun fusedLocationResultCallback(crossinline locResult: (locRes: LocationResult) -> Unit = { _ -> }): LocationCallback? {
    return object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locResult(locationResult)
            super.onLocationResult(locationResult)
        }
    }
}