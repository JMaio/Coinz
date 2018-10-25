package io.github.jmaio.coinz

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.json.JSONException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

data class Rates(
        @SerializedName("SHIL") val shilRate: Double,
        @SerializedName("DOLR") val dolrRate: Double,
        @SerializedName("QUID") val quidRate: Double,
        @SerializedName("PENY") val penyRate: Double
)

data class Properties(
        @SerializedName("id") val id: String,
        @SerializedName("value") val value: Double,
        @SerializedName("currency") val currency: String,
        @SerializedName("marker-symbol") val markerSymbol: Int,
        @SerializedName("marker-color") val markerColor: String
)

data class Geometry(
        @SerializedName("type") val type: String,
        @SerializedName("coordinates") val coordinates: List<Double>
)

class WildCoin(
        @SerializedName("properties") val properties: Properties,
        @SerializedName("geometry") val geometry: Geometry
) {
    val lng = geometry.coordinates[0]
    val lat = geometry.coordinates[1]
    fun asFeature(): Feature {
        return Feature.fromGeometry(Point.fromLngLat(lat, lng))
    }

    fun asLatLng(): LatLng {
        return LatLng(lat, lng)
    }
}

// class to keep maps of coins per day
class CoinMap : AnkoLogger {
    lateinit var day: Calendar

    var coins: List<WildCoin> = ArrayList()

    val gson = Gson()

    lateinit var rates: Rates

    fun loadMapFromFile(file: String) {
        info("[loadMapFromFile]: loading...")

        try {
            val s = File(file).inputStream().readBytes().toString(Charsets.UTF_8)
            info("[loadMapFromFile] : read file OK ($file)")
            val j = JsonParser().parse(s).asJsonObject

            info("[loadMapFromFile] : GeoJSON parse OK -- $j")

            rates = gson.fromJson(j.get("rates"), Rates::class.java)
            val features = j.get("features").asJsonArray

            for (i in 0 until features.size()) {
                val f = features.get(i).asJsonObject
                val props = gson.fromJson(f.get("properties").asJsonObject, Properties::class.java)
                val geometry = gson.fromJson(f.get("geometry").asJsonObject, Geometry::class.java)

                coins += WildCoin(props, geometry)
            }

            info("[loadMapFromFile] : coins : $coins")
        } catch (e: FileNotFoundException) {
            info("[loadMapFromFile]: file not found!")
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        } finally {

        }
    }

    fun isEmpty(): Boolean {
        return coins.isEmpty()
    }

}
