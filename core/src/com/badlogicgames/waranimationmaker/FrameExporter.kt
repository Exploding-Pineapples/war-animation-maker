package com.badlogicgames.waranimationmaker

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.ScreenUtils
import java.io.File
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

object FrameExporter {
    private const val FRAME_WIDTH = 1920  // Set your game's resolution here
    private const val FRAME_HEIGHT = 1080
    private const val PIXEL_FORMAT = "rgb24"
    private const val BYTES_PER_PIXEL = 3

    private val frameQueue: BlockingQueue<Pair<String, ByteArray>> = LinkedBlockingQueue()
    private val exportDir = File(Gdx.files.internal("exports").toString())
    private var frameCount = 0
    private var initialized = false

    private fun init() {
        if (initialized) return
        initialized = true

        if (!exportDir.exists()) exportDir.mkdirs()

        Thread {
            while (true) {
                val (filename, data) = frameQueue.take()
                File(exportDir, filename).writeBytes(data)
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun captureFrame() {
        init()

        val width = Gdx.graphics.width
        val height = Gdx.graphics.height

        // Safety check — must match dimensions you'll tell FFmpeg
        require(width == FRAME_WIDTH && height == FRAME_HEIGHT) {
            "Capture size must be $FRAME_WIDTH x $FRAME_HEIGHT"
        }

        val pixmap: Pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, width, height)

        val bytes = ByteArray(width * height * BYTES_PER_PIXEL)
        var i = 0

        // Flip vertically and extract RGB bytes
        for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                val color = pixmap.getPixel(x, y)
                bytes[i++] = ((color shr 24) and 0xFF).toByte() // R
                bytes[i++] = ((color shr 16) and 0xFF).toByte() // G
                bytes[i++] = ((color shr 8) and 0xFF).toByte()  // B
            }
        }

        val filename = "frame%05d.rgb".format(frameCount++)
        frameQueue.put(filename to bytes)

        pixmap.dispose()
    }

    fun frameCount(): Int = frameCount
}