package com.badlogicgames.superjumper

import com.badlogicgames.superjumper.models.*
import com.badlogicgames.superjumper.models.Animation
import com.badlogicgames.superjumper.models.Unit
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import java.io.File

object FileHandler {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .serializeNulls()
        .create()

    val animations = mutableListOf<Animation>(/*
        Animation(
            path = "C:\\Users\\User\\Documents\\Projects\\war-animation-maker\\assets\\maps\\gaza.png",
            name = "hors",
            area = Line(
                nodes = mutableListOf(
                    Node(
                        position = Coordinate(0.0f, 0.0f),
                        screenPosition = Coordinate(0.0f, 0.0f),
                        movementFrames = mutableListOf(
                            GroupedMovement(
                                frames = mutableMapOf(
                                    400 to Coordinate(400.0f, 500.0f),
                                    600 to Coordinate(800.0f, 1500.0f),
                                    800 to Coordinate(100.0f, 500.0f)
                                )
                            )
                        )
                    )
                )
            ),
            units = mutableListOf(
                Unit(
                    image = "wonky",
                    movementFrames = mutableListOf(
                        GroupedMovement(
                            frames = mutableMapOf(
                                0 to Coordinate(100.0f, 100.0f),
                                200 to Coordinate(400.0f, 500.0f),
                            )
                        ),
                        GroupedMovement(
                            frames = mutableMapOf(
                                200 to Coordinate(400.0f, 500.0f),
                                600 to Coordinate(400.0f, 500.0f),
                            )
                        ),
                        GroupedMovement(
                            frames = mutableMapOf(
                                600 to Coordinate(400.0f, 500.0f),
                                1000 to Coordinate(50.0f, 1000.0f),
                                800 to Coordinate(800.0f, 700.0f),
                            )
                        )
                    ),
                    position = Coordinate(0.0f, 0.0f),
                    screenPosition = Coordinate(0.0f, 0.0f),
                    death = 100
                )
            ),
            lines = mutableListOf(
                Line(
                    nodes = mutableListOf(
                        Node(
                            position = Coordinate(0.0f, 0.0f),
                            screenPosition = Coordinate(0.0f, 0.0f),
                            movementFrames = mutableListOf(
                                GroupedMovement(
                                    frames = mutableMapOf(
                                        0 to Coordinate(400.0f, 500.0f),
                                    )
                                )
                            )
                        ),
                        Node(
                            position = Coordinate(0.0f, 0.0f),
                            screenPosition = Coordinate(0.0f, 0.0f),
                            movementFrames = mutableListOf(
                                GroupedMovement(
                                    frames = mutableMapOf(
                                        0 to Coordinate(500.0f, 600.0f),
                                    )
                                )
                            )
                        ),
                        Node(
                            position = Coordinate(0.0f, 0.0f),
                            screenPosition = Coordinate(0.0f, 0.0f),
                            movementFrames = mutableListOf(
                                GroupedMovement(
                                    frames = mutableMapOf(
                                        0 to Coordinate(300.0f, 250.0f),
                                    )
                                )
                            )
                        ),
                        Node(
                            position = Coordinate(0.0f, 0.0f),
                            screenPosition = Coordinate(0.0f, 0.0f),
                            movementFrames = mutableListOf(
                                GroupedMovement(
                                    frames = mutableMapOf(
                                        0 to Coordinate(50.0f, 900.0f),
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )*/
    )

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