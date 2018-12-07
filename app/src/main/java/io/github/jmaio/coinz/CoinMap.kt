package io.github.jmaio.coinz

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.protobuf.Internal
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.GeoJson
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

@Parcelize
data class Rates(
        @SerializedName("SHIL") val SHIL: Double,
        @SerializedName("DOLR") val DOLR: Double,
        @SerializedName("QUID") val QUID: Double,
        @SerializedName("PENY") val PENY: Double
): Parcelable
{
    fun toMap(): Map<String, Double> {
        return mapOf(
                Pair("SHIL", SHIL),
                Pair("DOLR", DOLR),
                Pair("QUID", QUID),
                Pair("PENY", PENY)
        )
    }
}

@Parcelize
data class Properties(
        @SerializedName("id") val id: String,
        @SerializedName("value") val value: Double,
        @SerializedName("currency") val currency: String,
        @SerializedName("marker-symbol") val markerSymbol: Int,
        @SerializedName("marker-color") val markerColor: String
): Parcelable

@Parcelize
data class Geometry(
        @SerializedName("type") val type: String,
        @SerializedName("coordinates") val coordinates: List<Double>
): Parcelable

@Parcelize
data class WildCoin(
        @SerializedName("properties") val properties: Properties,
        @SerializedName("geometry") val geometry: Geometry
): Parcelable {
    fun asLatLng(): LatLng {
        return LatLng(geometry.coordinates[0], geometry.coordinates[1])
    }
}

@Parcelize
data class CoinMap(var coins: MutableList<WildCoin>,
                   var rates: Rates,
                   var features: String): Parcelable, AnkoLogger {

    fun collectCoin(id: String) {
        // find the coin by id
        val wildCoin = coins.find { coin ->
            coin.properties.id == id
        }
        if (wildCoin != null) {
            val collectedCoin = Coin(
                    wildCoin.properties.id,
                    wildCoin.properties.currency,
                    wildCoin.properties.value
            )
            // add to firebase wallet as collected
            //

            coins.remove(wildCoin)
        }
    }

    fun isEmpty(): Boolean {
        return coins.isEmpty()
    }

    fun toGeoJson(c: String): GeoJsonSource {
        var coin = JsonParser().parse(features).asJsonArray.filter { f ->
            f.asJsonObject["properties"].asJsonObject["currency"].asString == c }
//        [0]
//                .asJsonObject["properties"]
//                .asJsonObject["currency"]
        info("[toGeoJson] : $coin")
//        return GeoJsonSource("hi")
        val fs = arrayListOf<Feature>()
        JsonParser().parse(features).asJsonArray.filter { f ->
            f.asJsonObject["properties"].asJsonObject["currency"].asString == c }.forEach { jf ->
            jf.asJsonObject["properties"].asJsonObject.addProperty(
                    "icon-opacity",
                    ceil((jf.asJsonObject["properties"].asJsonObject["marker-symbol"].asDouble + 1) / 5.0) / 2 )
            info(jf)
            fs.add(Feature.fromJson(jf.toString())) }
        return GeoJsonSource(
                "${c.toLowerCase()}_source",
                FeatureCollection.fromFeatures(fs)
        )
    }
}

// class to keep maps of coins per day
class CoinMapMaker : AnkoLogger {
    private val gson = Gson()

    fun loadMapFromFile(file: String): CoinMap? {
        info("[loadMapFromFile]: loading...")

        var coinMap: CoinMap? = null

        try {
            val s = File(file).inputStream().readBytes().toString(Charsets.UTF_8)
            info("[loadMapFromFile] : read file OK ($file)")
            val j = JsonParser().parse(s)

            info("[loadMapFromFile] : GeoJSON parse OK -- $j")

            val features = j.asJsonObject.get("features").asJsonArray

            coinMap = CoinMap(
                    mutableListOf(),
                    gson.fromJson(j.asJsonObject.get("rates"), Rates::class.java),
                    features.toString()
            )


            info("[loadMapFromFile] : GeoJSON contains ${features.size()} features")


            for (i in 0 until features.size()) {
                val f = features.get(i).asJsonObject
                val props = gson.fromJson(f.get("properties").asJsonObject, Properties::class.java)
                val geometry = gson.fromJson(f.get("geometry").asJsonObject, Geometry::class.java)

                // don't add this coin if has already been collected!!
                coinMap.coins.add(WildCoin(props, geometry))
            }

            info("[loadMapFromFile] : contains ${coinMap.coins.size} coins")
        } catch (e: FileNotFoundException) {
            info("[loadMapFromFile]: file not found!")
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        } finally {
            return coinMap
        }
    }
}
