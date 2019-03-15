package com.dmdirc

import javafx.scene.control.ProgressBar
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane

class ImageThingy(private val url: String) : Pane() {
    private val image: Image = Image(url, true)
    private val progress = ProgressBar(0.toDouble())
    init {
        children.add(progress)
        image.progressProperty().addListener { _, _, newValue ->
            progress.progressProperty().value = newValue.toDouble()
            if (newValue.toDouble() >= 1.toDouble()) {
                children.remove(progress)
                children.add(ImageView(image).apply {
                    fitWidth = 250.0
                    fitHeight = 250.0
                    isPreserveRatio = true
                    isSmooth = true
                })
            }
        }
        image.errorProperty().addListener { _, _, newValue ->
            this@ImageThingy.children.remove(progress)
        }
    }
}
