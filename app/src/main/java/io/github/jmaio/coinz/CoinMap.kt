package io.github.jmaio.coinz

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.protobuf.Internal
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.json.JSONException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

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
    val lng = geometry.coordinates[0]
    val lat = geometry.coordinates[1]

    fun asLatLng(): LatLng {
        return LatLng(lat, lng)
    }
}

@Parcelize
data class CoinMap(var coins: MutableList<WildCoin>,
                   var rates: Rates): Parcelable {

//    constructor(parcel: Parcel) : this(
//            parcel.readList(),
//            parcel.readMap()
//    )
//
//    override fun writeToParcel(dest: Parcel, flags: Int) {
//        dest.writeList(coins)
//        dest.writeMap(rates)
//    }
//
//    override fun describeContents(): Int {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

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

//    companion object CREATOR : Parcelable.Creator<CoinMap> {
//        override fun createFromParcel(parcel: Parcel): CoinMap {
//            return CoinMap(parcel)
//        }
//
//        override fun newArray(size: Int): Array<CoinMap?> {
//            return arrayOfNulls(size)
//        }
//    }

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
            val j = JsonParser().parse(s).asJsonObject

            info("[loadMapFromFile] : GeoJSON parse OK -- $j")

            coinMap = CoinMap(
                    mutableListOf(),
                    gson.fromJson(j.get("rates"), Rates::class.java)
            )

                    val features = j.get("features").asJsonArray

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
