package io.github.jmaio.coinz

import android.os.AsyncTask
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.info
import org.jetbrains.anko.uiThread
import kotlin.math.min

// implementation of DownloadFileTask from lecture 5
//interface DownloadCompleteListener {
//    fun downloadComplete(result: String, saveSuccess: Boolean)
//}
//
//object DownloadCompleteRunner: DownloadCompleteListener, AnkoLogger {
//    private var result: String? = null
//    private var saveSuccess: Boolean = false
//    override fun downloadComplete(result: String, saveSuccess: Boolean) {
//        this.result = result
//        this.saveSuccess = saveSuccess
//        info("[result] size=${result.length}")
//        info("[result] ${result.substring(0, min(result.length, 200))}...")
//    }
//}

class DownloadFileTask(val url: String, val filename: String) : AnkoLogger {

    var result: String? = null

    fun execute() {
        if (doAsync {
                    result = try {
                        info("[downloadUrl] executing in background")
                        loadFileFromNetwork(url)
                    } catch (e: IOException) {
                        null
                    }
                }.isDone) {
            saveAsFile()
        }

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

    fun saveAsFile(): Boolean {
        info("[saveStringAsFile]: $filename")
        if (result == null) {
            info("[saveStringAsFile]: failed! result = null")
            return false
        }
        File(filename).writeText(result!!)
        return true
    }

}
//
//class DownloadFileTask(private val caller: DownloadCompleteListener):
//        AsyncTask<String, Void, String>(), AnkoLogger {
//
//    private var filename = ""
//
//    override fun doInBackground(vararg args: String): String {
//        var r = ""
//        try {
//            info("[downloadUrl] executing in background")
//            loadFileFromNetwork(args[0])
//            filename = args[1]
//        } catch (e: IOException) {
//            r = "Unable to load content. Check your network connection"
//        }
//        return r
//    }
//
//    private fun loadFileFromNetwork(urlString: String): String {
//        // Read input from stream, build result as a string
//        val stream: InputStream = downloadUrl(urlString)
//        // buffer stream and return as string
//        return stream.bufferedReader().use { it.readText() }
//    }
//
//    @Throws(IOException::class)
//    private fun downloadUrl(urlString: String): InputStream {
//        val url = URL(urlString)
//        val conn = url.openConnection() as HttpURLConnection
//        conn.readTimeout = 10000
//        conn.connectTimeout = 15000
//        conn.requestMethod = "GET"
//        conn.doInput = true
//        conn.connect()
////        val stream = conn.inputStream
//        info("[downloadUrl] connection executed, response=${conn.responseCode}")
//        return conn.inputStream
//    }
//
//    private fun saveStringAsFile(result: String?, f: String) : Boolean {
//        info("[saveStringAsFile]: $f")
//        if (result != null) {
//            File(f).writeText(result)
//        } else {
//            return false
//        }
//        return true
//    }
//
//    override fun onPostExecute(result: String) {
//        super.onPostExecute(result)
//        val s = saveStringAsFile(result, filename)
//        caller.downloadComplete(result, s)
//    }
//
//}

