package com.badlogicgames.waranimationmaker

import com.badlogicgames.waranimationmaker.models.Animation
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

    private val animationsFolder by lazy {
        val file = File("animations")
        if (!file.exists()) {
            file.mkdirs()
        }
        file
    }

    fun deleteAnimation(animation: Animation) {
        animations.remove(animation)
        val file = File(animationsFolder, "${animation.name}.json");
        if (file.exists()) {
            file.delete();
        }
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

    fun createNewAnimation(animation: Animation) {
        animations.add(animation)
        save()
    }

    fun load() {
        animationsFolder.listFiles()?.forEach {
            val content = it.readText()
            kotlin.runCatching {
                gson.fromJson(content, Animation::class.java)
                    .apply {
                        if (animations.any { animation -> animation.name == name })
                        {
                            return@apply
                        }

                        animations += this

                        units.forEach { unit -> unit.texture() }

                        // preload image dimensions
                        getImageDimensions()
                    }
            }.onFailure { x ->
                x.printStackTrace()
            }
        }
    }
}