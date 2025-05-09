package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.OrthographicCamera

data class Animation @JvmOverloads constructor(
    var name: String = "My Animation",
    var countries: List<String> = mutableListOf(),
    val units: MutableList<Unit> = mutableListOf(),
    private var camera: Camera? = null,
    val nodes: MutableList<Node> = mutableListOf(),
    val edgeCollections: MutableList<EdgeCollection> = mutableListOf(),
    val arrows: MutableList<Arrow> = mutableListOf(),
    val mapLabels: MutableList<MapLabel> = mutableListOf(),
    var images: MutableList<Image> = mutableListOf(),
    var unitId: Int = 0,
    private var edgeCollectionId: Int = 0,
    var nodeId: Int = 0,
    val linesPerNode: Int = 12,
    var initTime: Int = 0
)
{
    @Transient var unitHandler = UnitHandler(this)
    @Transient var nodeHandler = NodeHandler(this)

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
        unitHandler = UnitHandler(this)
        nodeHandler = NodeHandler(this)
        unitHandler.init()
        mapLabels.forEach { it.alpha.update(initTime) }
        arrows.forEach { it.alpha.update(initTime) }
        images.forEach { it.alpha.update(initTime) }
        images.forEach { it.loadTexture() }
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

    fun deleteObject(obj: Object): Boolean {
        if (obj.javaClass == Node::class.java) {
            return nodeHandler.remove(obj as Node)
        }
        if (obj.javaClass == Image::class.java) {
            return images.remove(obj as Image)
        }
        if (obj.javaClass == MapLabel::class.java) {
            return mapLabels.remove(obj as MapLabel)
        }
        if (obj.javaClass == Arrow::class.java) {
            return arrows.remove(obj as Arrow)
        }
        if (obj.javaClass == Unit::class.java) {
            return unitHandler.remove(obj)
        }
        return false
    }

    fun getEdgeCollectionByID(id: EdgeCollectionID): EdgeCollection? {
        return edgeCollections.find { it.id.value == id.value }
    }

    fun getNodeByID(id: NodeID): Node? = nodes.firstOrNull { it.id.value == id.value }

    fun newNode(x: Float, y: Float, time: Int): Node {
        val node = Node(Coordinate(x, y), time, NodeID(nodeId))
        node.buildInputs()
        nodes.add(node)
        nodeId++
        return node
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

    fun newImage(x: Float, y: Float, time: Int): Image {
        val new = Image(x, y, time, "")
        new.buildInputs()
        images.add(new)
        return new
    }

    fun createObjectAtPosition(time: Int, x: Float, y: Float, type: String, country: String = ""): ScreenObject? {
        if (type == "Unit" && country != "") {
            return unitHandler.newUnit(Coordinate(x, y), time, country)
        }

        val objectDictionary = mapOf(
            "Node" to ::newNode,
            "Unit" to unitHandler::newUnit,
            "Arrow" to ::newArrow,
            "Map Label" to ::newMapLabel,
            "Image" to ::newImage
        )
        return objectDictionary[type]?.invoke(x, y, time)
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

        if (type.isAssignableFrom(Image::class.java)) {
            for (image in images) {
                if (image.clicked(x, y)) {
                    objects.add(image as T)
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
        images.forEach { it.buildInputs() }
    }

    fun update(time: Int, orthographicCamera: OrthographicCamera, paused: Boolean) {
        nodeHandler.update(time, orthographicCamera, paused)
        unitHandler.update(time, orthographicCamera, paused)
        images.forEach { it.goToTime(time, orthographicCamera.zoom, orthographicCamera.position.x, orthographicCamera.position.y, paused) }
        arrows.forEach { it.goToTime(time, orthographicCamera.zoom, orthographicCamera.position.x, orthographicCamera.position.y, paused) }
        mapLabels.forEach { it.goToTime(time, orthographicCamera.zoom, orthographicCamera.position.x, orthographicCamera.position.y, paused) }
    }

    fun draw(drawer: Drawer) {
        drawer.draw(this)
    }
}