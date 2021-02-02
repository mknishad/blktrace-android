package com.androidflash.blocktrace

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class PeriodicTask(private val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        Log.e("Hello",runAsRoot()!!)
        return Result.success()
    }
    fun runAsRoot(): String? {
        return "Hello World"
        /*return try {
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
            Log.e("output","OUT "+output.toString()).toString()
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }*/
    }
}