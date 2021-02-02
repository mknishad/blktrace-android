package com.androidflash.blocktrace

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.work.Constraints
import com.androidflash.blocktrace.network.response.FileResponse
import com.androidflash.blocktrace.network.retrofit.Api
import com.androidflash.blocktrace.network.retrofit.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class MainActivity : AppCompatActivity() {

    lateinit var runButton: Button
    lateinit var serviceRunButton: Button

    lateinit var dialog: ProgressDialog

    private var serverResponseCode = 0

    private var havePermission = true
    private val PERMISSION_REQUEST_CODE = 1111
    private val wantedPermission = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runButton = findViewById(R.id.btnNormalRun)
        serviceRunButton = findViewById(R.id.btnServiceRun)
        runButton.setOnClickListener {
            //  runScript()
            runAsRoot()
        }
        serviceRunButton.setOnClickListener {
            /*val onetimeWork = OneTimeWorkRequest.Builder(PeriodicTask::class.java)
            onetimeWork.setConstraints(constraints)
            val data = Data.Builder()
            data.putString("ONETIME_WORK_DESCRIPTION","testing")
            onetimeWork.setInputData(data.build())

            WorkManager.getInstance(this).enqueue(onetimeWork.build())*/
            //  createNetworkCall()
            //putFileCall()

            dialog = ProgressDialog(this)
            dialog.setMessage("Uploading file...")

            try {
                havePermission = hasPermissions(this@MainActivity, wantedPermission)
                if (!havePermission) {
                    requestPermission(wantedPermission)
                } else {
                    putFileCall()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                dialog.dismiss()
            }
        }
    }

    private fun putFileCall() {
        ApiClient.instance.putFiles("20013fea6bcc82fd", "blocktrace", "2021-01-20T05:12:40.000Z")
            .enqueue(
                object : Callback<FileResponse> {
                    override fun onFailure(call: Call<FileResponse>, t: Throwable) {
                        //TODO("Not yet implemented")
                        Log.e("FileFailure", "Failure")
                        Log.e("FileFailure", t.message.toString())
                        //Log.e("FileFailure", t.localizedMessage)
                    }

                    override fun onResponse(
                        call: Call<FileResponse>,
                        response: Response<FileResponse>
                    ) {
                        Log.e("FileResponse", "Success")
                        Log.e("FileResponseM", response.message())
                        Log.e("FileResponse", response.body()!!.presignedUrl)
                        //uploadFile(response.body()!!.presignedUrl.removePrefix("http://167.71.193.87:9000/blocktrace/"))

                        Thread {
                            uploadFile(response.body()!!.presignedUrl)
                        }.start()
                    }
                })
    }

    fun uploadFile(presignedUrl: String): Int {
        runOnUiThread {
            dialog.show()
        }

        var conn: HttpURLConnection?
        var dos: DataOutputStream?
        val boundary = "*****"
        var bytesRead: Int
        var bytesAvailable: Int
        var bufferSize: Int
        val buffer: ByteArray
        val maxBufferSize = 1 * 1024 * 1024
        val sourceFile = File("/storage/emulated/0/Download/sample1.txt")
        return if (!sourceFile.isFile) {
            dialog.dismiss()
            Log.e(TAG, "uploadFile: Source File not exist: " + sourceFile.absolutePath)
            runOnUiThread {
                Toast.makeText(
                    this, "uploadFile: Source File not exist: " +
                            sourceFile.absolutePath, Toast.LENGTH_SHORT
                ).show()
            }
            0
        } else {
            try {
                val fileInputStream = FileInputStream(sourceFile)
                val url = URL(presignedUrl)

                // Open a HTTP  connection to  the URL
                conn = url.openConnection() as HttpURLConnection
                conn.doInput = true // Allow Inputs
                conn.doOutput = true // Allow Outputs
                conn.useCaches = false // Don't use a Cached Copy
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Connection", "Keep-Alive")
                conn.setRequestProperty("ENCTYPE", "multipart/form-data")
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
                conn.setRequestProperty("uploaded_file", sourceFile.path)
                dos = DataOutputStream(conn.outputStream)

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available()
                bufferSize = bytesAvailable.coerceAtMost(maxBufferSize)
                buffer = ByteArray(bufferSize)

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize)
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize)
                    bytesAvailable = fileInputStream.available()
                    bufferSize = bytesAvailable.coerceAtMost(maxBufferSize)
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize)
                }

                // Responses from the server (code and message)
                serverResponseCode = conn.responseCode
                val serverResponseMessage = conn.responseMessage
                Log.i(TAG, "HTTP Response is : $serverResponseMessage: $serverResponseCode")
                if (serverResponseCode == 200) {
                    runOnUiThread {
                        Toast.makeText(
                            this, "File Upload Complete.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                //close the stream
                fileInputStream.close()
                dos.flush()
                dos.close()
            } catch (ex: MalformedURLException) {
                ex.printStackTrace()
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "MalformedURLException", Toast.LENGTH_SHORT)
                        .show()
                }
                Log.e("Upload file to server", "error: " + ex.message, ex)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(
                        this, "Got Exception : see logcat ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Log.e(TAG, "Upload file to server Exception Exception : " + e.message, e)
            }
            runOnUiThread {
                dialog.dismiss()
            }
            serverResponseCode
        } // End else block
    }

    private fun requestPermission(argPermissions: Array<String>) {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            argPermissions,
            PERMISSION_REQUEST_CODE
        )
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission!!
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

fun createNetworkCall() {
    // Create Retrofit
    ApiClient.instance.createBuckets("blocktrace").enqueue(object : Callback<String> {
        override fun onFailure(call: Call<String>, t: Throwable) {
            Log.e("NETWORKE", t.message.toString())
            Log.e("NETWORKE", call.toString())
        }

        override fun onResponse(call: Call<String>, response: Response<String>) {
            Log.e("NETWORK", response.code().toString())
            Log.e("NETWORK-M", response.message().toString())
            Log.e("NETWORK-B", response.body().toString())
        }
    })
}

fun uploadFile(url: String) {
    //val file = RequestBody.create("*/*".toMediaTypeOrNull(), "/storage/emulated/0/Download/Gabweek4 (280).jpg")

    var file = File("/storage/emulated/0/Download/Gabweek4 (280).jpg")

    var part: MultipartBody.Part? = null
    try {
        val fileBody: RequestBody = RequestBody.create("*/*".toMediaTypeOrNull(), file)
        part = MultipartBody.Part.createFormData("doc", file.name, fileBody)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val retrofit = Retrofit.Builder()
        .baseUrl("http://167.71.193.87:9000/blocktrace/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val service = retrofit.create(Api::class.java)

    if (part != null) {
        service.uploadFile(url, part).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e("UF", "Fail")
                Log.e("UFM", t.message.toString())
                //Log.e("UFM", t.localizedMessage)
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                Log.e("UR", "Success")
                Log.e("URM", response.message())
            }
        })
    }
}

fun runAsRoot(): String? {
    return try {
        // Executes the command.
        val process = Runtime.getRuntime().exec("/data/app/fisrt.sh")
        // Reads stdout.
        // NOTE: You can write to stdin of the command using
        //       process.getOutputStream().
        val reader = BufferedReader(
            InputStreamReader(process.inputStream)
        )
        var read: Int
        val buffer = CharArray(4096)
        val output = StringBuffer()
        while (reader.read(buffer).also { read = it } > 0) {
            output.append(buffer, 0, read)
        }
        reader.close()

        // Waits for the command to finish.
        process.waitFor()
        output.toString()
        Log.e("output", "OUT $output").toString()
    } catch (e: IOException) {
        throw RuntimeException(e)
    } catch (e: InterruptedException) {
        throw RuntimeException(e)
    }
}
