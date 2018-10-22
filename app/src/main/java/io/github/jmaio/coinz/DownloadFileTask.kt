package io.github.jmaio.coinz

import android.os.AsyncTask
import android.os.Environment
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.act
import org.jetbrains.anko.info
import kotlin.math.min

// implementation of DownloadFileTask from lecture 5
interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

object DownloadCompleteRunner: DownloadCompleteListener, AnkoLogger {
    var result: String? = null
    override fun downloadComplete(result: String) {
        this.result = result
        info("[result] size=${result.length}")
        info("[result] ${result.substring(0, min(result.length, 200))}...")
    }
}

class DownloadFileTask(private val caller: DownloadCompleteListener):
        AsyncTask<String, Void, String>(), AnkoLogger {

    override fun doInBackground(vararg args: String): String = try {
        info("[downloadUrl] executing in background")
        loadFileFromNetwork(args[0])
    } catch (e: IOException) {
        "Unable to load content. Check your network connection"
    }

    private fun loadFileFromNetwork(urlString: String): String {
        // Read input from stream, build result as a string
        val stream: InputStream = downloadUrl(urlString)
        // buffer stream and return as string
        return stream.bufferedReader().use { it.readText() }
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect()
//        val stream = conn.inputStream
        info("[downloadUrl] connection executed, response=${conn.responseCode}")
        return conn.inputStream
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

}

