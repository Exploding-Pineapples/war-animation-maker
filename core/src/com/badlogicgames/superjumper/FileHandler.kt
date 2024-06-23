package com.badlogicgames.superjumper

import com.badlogicgames.superjumper.models.Animation
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import java.io.File

object FileHandler {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .serializeNulls()
        .create()

    val animations = mutableListOf<Animation>()

    var currentAnimation: Animation? = null

    private val animationsFolder by lazy {
        val file = File("animations")
        if (!file.exists()) {
            file.mkdirs()
        }
        file
    }

    fun save() {
        animations.forEach {
            val fileName = "${it.name}.json"
            val file = File(animationsFolder, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }

            kotlin.runCatching {
                file.writeText(
                    gson.toJson(it)
                )
            }.onFailure { x ->
                x.printStackTrace()
                println("couldnt save ${it.name} rip")
            }
        }
    }

    fun load() {
        animationsFolder

        animationsFolder.listFiles()?.forEach {
            val content = it.readText()
            kotlin.runCatching {
                animations += gson.fromJson(content, Animation::class.java)
                    .apply {
                        // preload image dimensions
                        getImageDimensions()
                    }
            }.onFailure { x ->
                x.printStackTrace()
            }
        }
    }
}