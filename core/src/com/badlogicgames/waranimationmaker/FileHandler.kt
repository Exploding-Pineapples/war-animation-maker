package com.badlogicgames.waranimationmaker

import com.badlogicgames.waranimationmaker.models.*
import com.google.gson.*
import java.io.File
import java.lang.reflect.Type

object FileHandler {
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .serializeNulls()
        .registerTypeAdapter(ID::class.java, AbstractTypeSerializer<ID>())
        .registerTypeAdapter(AnyEdgeCollectionStrategy::class.java, AbstractTypeSerializer<AnyEdgeCollectionStrategy>())
        .registerTypeAdapter(AnyEdgeCollectionContext::class.java, AbstractTypeSerializer<AnyEdgeCollectionContext>())
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

                        units.forEach { unit -> unit.typeTexture() }
                    }
            }.onFailure { x ->
                x.printStackTrace()
            }
        }
    }
}

interface AbstractTypeSerializable {

    fun getAbstractType(): Type {
        throw IllegalStateException("Serializable abstract type has not been setup")
    }

}

class AbstractTypeSerializer<T : AbstractTypeSerializable> : JsonSerializer<T>, JsonDeserializer<T> {

    override fun serialize(src: T, typeOf: Type, context: JsonSerializationContext): JsonElement {
        val abstractType = src.getAbstractType() as Class<*>
        val json = JsonObject()
        json.addProperty("type", abstractType.name)
        json.add("properties", context.serialize(src, src.getAbstractType()))
        return json
    }

    override fun deserialize(json: JsonElement, typeOf: Type, context: JsonDeserializationContext): T {
        val type = json.asJsonObject.get("type").asString
        val properties = json.asJsonObject.get("properties")

        try {
            return context.deserialize(properties, Class.forName(type))
        } catch (e: ClassNotFoundException) {
            throw JsonParseException("Unknown type: $type", e)
        }
    }

}