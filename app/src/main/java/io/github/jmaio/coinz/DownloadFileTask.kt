package io.github.jmaio.coinz

import org.jetbrains.anko.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


class DownloadFileTask(private val url: String, private val filename: String) : AnkoLogger {

    private var result: String? = null

    fun execute(): Boolean {
        result = try {
            info("[downloadUrl] executing in background")
            loadFileFromNetwork(url)
        } catch (e: IOException) {
            null
        }
        return saveAsFile()
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
        info("[downloadUrl] connection executed, response=${conn.responseCode}")
        return conn.inputStream
    }

    private fun saveAsFile(): Boolean {
        info("[saveStringAsFile]: $filename")
        if (result == null) {
            info("[saveStringAsFile]: failed! result = null")
            return false
        }
        File(filename).writeText(result!!)
        return true
    }

}
