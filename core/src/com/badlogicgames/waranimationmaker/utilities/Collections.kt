package com.badlogicgames.waranimationmaker.utilities

fun <K : Number> Collection<K>.toDoubleArray() = map { it.toDouble() }.toTypedArray()