package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogicgames.waranimationmaker.WarAnimationMaker
import java.io.File

data class Animation @JvmOverloads constructor(
    var mapPath: String,
    var name: String = "My Animation",
    var countries: List<String> = mutableListOf(),
    val units: MutableList<Unit> = mutableListOf(),
    private var camera: Camera? = null,
    val nodes: MutableList<Node> = mutableListOf(),
    val edgeCollections: MutableList<EdgeCollection> = mutableListOf(),
    val arrows: MutableList<Arrow> = mutableListOf(),
    var mapLabels: MutableList<MapLabel> = mutableListOf(),
    var unitId: Int = 0,
    private var edgeCollectionId: Int = 0,
    var nodeId: Int = 0,
    val linesPerNode: Int = 12,
    var initTime: Int = 0
)
{
    private var cachedImageDimensions: Pair<Int, Int>? = null

    fun getEdgeCollectionId(): Int {
        edgeCollectionId++
        return edgeCollectionId
    }

    fun getEdgeCollectionId(noIncrement: Boolean): Int {
        if (!noIncrement) {
            edgeCollectionId++
        }
        return edgeCollectionId
    }

    fun init() {
        if (mapLabels == null) {
            mapLabels = mutableListOf()
        }
        if (initTime == null) {
            initTime = 0
        }
        unitHandler = UnitHandler(this)
        nodeHandler = NodeHandler(this)
        unitHandler.init()
        mapLabels.forEach { it.alpha.update(initTime) }
        arrows.forEach { it.alpha.update(initTime) }
        buildInputs()
    }

    fun camera(): Camera
    {
        if (camera == null)
        {
            camera = Camera(Coordinate(960f, 540f),1.0f, 0)
        }

        return camera!!
    }

    fun getImageDimensions(): Pair<Int, Int> {
        if (cachedImageDimensions == null) {
            cachedImageDimensions = File(mapPath)
                .getImageDimensions()
        }

        return cachedImageDimensions!!
    }

    fun deleteObject(obj: Object): Boolean {
        if (obj.javaClass == Node::class.java) {
            return nodeHandler.remove(obj as Node)
        }
        if (unitHandler.remove(obj)) {
            return true
        }
        return false
    }

    fun getEdgeCollectionByID(id: EdgeCollectionID): EdgeCollection? {
        return edgeCollections.find { it.id.value == id.value }
    }

    fun getNodeByID(id: NodeID): Node? = nodes.firstOrNull { it.id.value == id.value }

    fun createNodeAtPosition(time: Int, x: Float, y: Float): Node {
        val node = Node(Coordinate(x, y), time, NodeID(nodeId))
        node.buildInputs()
        nodes.add(node)
        nodeId++
        return node
    }

    fun selectObject(x: Float, y: Float): ArrayList<ScreenObject> {
        return selectObjectWithType(x, y, ScreenObject::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ObjectClickable> selectObjectWithType(x: Float, y: Float, type: Class<out ObjectClickable>): ArrayList<T> {
        val objects = ArrayList<T>()

        if (type.isAssignableFrom(Unit::class.java)) {
            val selectedUnit = unitHandler.clicked(x, y)
            if (selectedUnit != null) {
                objects.add(selectedUnit as T)
            }
        }

        if (type.isAssignableFrom(Node::class.java)) {
            objects.addAll(nodes.filter { it.clicked(x, y) }.map {it as T})
        }

        if (type.isAssignableFrom(Edge::class.java)) {
            for (node in nodes) {
                for (edge in node.edges) {
                    if (edge.clicked(x, y)) {
                        objects.add(edge as T)
                    }
                }
            }
        }

        if (type.isAssignableFrom(Arrow::class.java)) {
            for (arrow in arrows) {
                if (arrow.clicked(x, y)) {
                    objects.add(arrow as T);
                }
            }
        }

        if (type.isAssignableFrom(MapLabel::class.java)) {
            for (mapLabel in mapLabels) {
                if (mapLabel.clicked(x, y)) {
                    objects.add(mapLabel as T);
                }
            }
        }

        return objects
    }

    fun getParents(node: Object) : List<EdgeCollection> {
        return if (node.javaClass == Node::class.java) {
            edgeCollections.filter {
                    edgeCollection -> (edgeCollection.edges.find { it.contains((node as Node).id) } != null)
            }
        } else {
            mutableListOf()
        }
    }

    fun buildInputs() {
        nodeHandler.buildInputs()
        unitHandler.buildInputs()
        arrows.forEach { it.buildInputs() }
        edgeCollections.forEach { it.buildInputs() }
        mapLabels.forEach { it.buildInputs() }
    }

    fun update(time: Int, orthographicCamera: OrthographicCamera, paused: Boolean) {
        nodeHandler.update(time, orthographicCamera)
        unitHandler.update(time, orthographicCamera, paused)
        arrows.forEach { it.goToTime(time, orthographicCamera.zoom, orthographicCamera.position.x, orthographicCamera.position.y, paused) }
        mapLabels.forEach { it.goToTime(time, orthographicCamera.zoom, orthographicCamera.position.x, orthographicCamera.position.y, paused) }
    }

    fun draw(game: WarAnimationMaker, colorLayer: FrameBuffer, animationMode: Boolean, zoomFactor: Float, time: Int, camera: OrthographicCamera) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        arrows.forEach { it.draw(game.shapeRenderer, camera, time) }
        game.shapeRenderer.end()

        nodeHandler.draw(game.batcher, game.shapeRenderer, colorLayer, animationMode)
        unitHandler.draw(game, game.shapeRenderer, zoomFactor)
        mapLabels.forEach { it.draw(game.batcher, game.shapeRenderer, zoomFactor, game.bitmapFont, game.fontShader, game.layout) }
    }

    fun newArrow(x: Float, y: Float, time: Int): Arrow {
        val new = Arrow(x, y, time)
        new.buildInputs()
        arrows.add(new)
        return new
    }

    fun newMapLabel(x: Float, y: Float, time: Int): MapLabel {
        val new = MapLabel(x, y, time)
        new.buildInputs()
        mapLabels.add(new)
        return new
    }

    @Transient var unitHandler = UnitHandler(this)
    @Transient var nodeHandler = NodeHandler(this)
}