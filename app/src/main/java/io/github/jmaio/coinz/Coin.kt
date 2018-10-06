package io.github.jmaio.coinz

import com.mapbox.mapboxsdk.geometry.LatLng

class Coin(val id: String, val currency: Currency, val value: Double, val location: LatLng) {

}