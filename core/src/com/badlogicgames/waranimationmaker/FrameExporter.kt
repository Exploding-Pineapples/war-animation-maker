package com.badlogicgames.waranimationmaker
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import java.io.BufferedOutputStream

class FrameExporter(
    val width: Int,
    val height: Int,
    name: String = "output.mp4"
) {
    val done = mutableListOf<Int>()

    init {
        Thread {
            ffmpegProcess.errorStream.bufferedReader().forEachLine {
                println("[FFmpeg] $it")
            }
        }.start()
    }

    val ffmpegProcess = ProcessBuilder(
        "ffmpeg",
        "-y", // overwrite output file
        "-f", "rawvideo",
        "-pixel_format", "rgba",
        "-video_size", "${width}x$height",
        "-framerate", "60",
        "-i", "-", // read from stdin
        "-c:v", "libx264",
        "-pix_fmt", "yuv420p",
        "-preset", "ultrafast",
        "exports/$name.mp4"
    )
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .start()

    val ffmpegStdin = BufferedOutputStream(ffmpegProcess.outputStream)

    fun captureFrame(index: Int) {
        if (index !in done) {
            val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
            Gdx.gl.glReadPixels(0, 0, width, height, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixmap.pixels)

            val buffer = pixmap.pixels
            buffer.rewind()
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            flipVertically(bytes, width, height)

            ffmpegStdin.write(bytes)

            pixmap.dispose()
            done.add(index)
        }
    }

    fun flipVertically(data: ByteArray, width: Int, height: Int) {
        val rowSize = width * 4
        val temp = ByteArray(rowSize)
        for (y in 0 until height / 2) {
            val top = y * rowSize
            val bottom = (height - 1 - y) * rowSize

            // swap rows
            System.arraycopy(data, top, temp, 0, rowSize)
            System.arraycopy(data, bottom, data, top, rowSize)
            System.arraycopy(temp, 0, data, bottom, rowSize)
        }
    }

    fun dispose() {
        ffmpegStdin.flush()
        ffmpegStdin.close()
        ffmpegProcess.waitFor()
    }
}