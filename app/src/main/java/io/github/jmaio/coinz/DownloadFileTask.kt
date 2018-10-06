package io.github.jmaio.coinz

import android.os.AsyncTask
import android.os.Environment
import io.github.jmaio.coinz.DownloadCompleteRunner.result
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// implementation of DownloadFileTask from lecture 5
interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

object DownloadCompleteRunner: DownloadCompleteListener {
    var result: String? = null
    override fun downloadComplete(result: String) {
        this.result = result
    }
}

class DownloadFileTask(private val caller: DownloadCompleteListener):
        AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg urls: String): String = try {
        loadFileFromNetwork(urls[0])
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
        return conn.inputStream
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)

        caller.downloadComplete(result)

    }

}