package com.dmdirc

import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Duration

class ImageLoader(url: String) : Pane() {
    init {
        val http = OkHttpClient().newBuilder()
            .callTimeout(Duration.ofSeconds(2))
            .build()
        val request = Request.Builder().url(url).build()
        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // NOOP
            }

            override fun onResponse(call: Call, response: Response) {
                runLater {
                    val stream = response.body()?.byteStream()
                    if (response.isSuccessful && stream != null) {
                        GlobalScope.launch {
                            val image = Image(stream)
                            runLater {
                                children.add(ImageView(image).apply {
                                    fitWidth = 250.0
                                    fitHeight = 250.0
                                    isPreserveRatio = true
                                    isSmooth = true
                                })
                            }
                        }
                    }
                }
            }
        })
    }
}
