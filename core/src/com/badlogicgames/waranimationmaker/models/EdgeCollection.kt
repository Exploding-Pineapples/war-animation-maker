package com.badlogicgames.waranimationmaker.models

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Array
import com.badlogicgames.waranimationmaker.AreaColor
import com.badlogicgames.waranimationmaker.InputElement
import com.badlogicgames.waranimationmaker.SelectBoxInput
import com.badlogicgames.waranimationmaker.TextInput

open class EdgeCollection(override val id: EdgeCollectionID) : HasInputs, HasID {
    @Transient var edges: MutableList<Edge> = mutableListOf()
    var edgeCollectionStrategy: AnyEdgeCollectionStrategy = EdgeCollectionStrategy<EdgeCollectionContext>()
    var edgeCollectionContext: AnyEdgeCollectionContext = EdgeCollectionContext(edges, AreaColor.RED)
    @Transient override var inputElements: MutableList<InputElement<*>> = mutableListOf()

    override fun buildInputs() {
        super.buildInputs()

        inputElements.add(
            TextInput(null, { input ->
                if (input != null) {
                    for (color in AreaColor.entries) {
                        if (input == color.name) {
                            this.edgeCollectionContext?.color = color
                        }
                    }
                }
            }, label@{
                return@label edgeCollectionContext?.color?.name
            }, String::class.java, "Set color")
        )
        inputElements.add(
            SelectBoxInput(null, { input ->
                if (input == "Area") {
                    edgeCollectionContext = AreaContext()
                    (edgeCollectionContext as AreaContext).edges = edges
                    edgeCollectionStrategy = AreaStrategy()
                }
                if (input == "Line") {
                    edgeCollectionContext = LineContext()
                    (edgeCollectionContext as LineContext).edges = edges
                    edgeCollectionStrategy = LineStrategy()
                }
                edgeCollectionContext.init()
            }, label@{
                return@label edgeCollectionStrategy.javaClass.simpleName.substring(0, edgeCollectionStrategy.javaClass.simpleName.length - "Strategy".length )
            }, String::class.java, "Set edge collection type", Array<String>().apply { add("Area", "Line") })
        )
        edgeCollectionContext.init()
    }

    override fun showInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        uiVisitor.show(verticalGroup, this)
        edgeCollectionContext.showInputs(verticalGroup, uiVisitor)
    }

    override fun hideInputs(verticalGroup: VerticalGroup, uiVisitor: UIVisitor) {
        super.hideInputs(verticalGroup, uiVisitor)
        edgeCollectionContext.hideInputs(verticalGroup, uiVisitor)
    }

    fun prepare() {
        if (edges == null) {
            edges = mutableListOf()
        }
        edges.clear()
        edgeCollectionContext.edges = mutableListOf()
    }

    fun update(time: Int, animation: Animation, paused: Boolean) {
        edgeCollectionContext.edges.addAll(edges)
        edgeCollectionStrategy.updateAny(time, animation, paused, edgeCollectionContext)
    }

    fun draw(shapeRenderer: ShapeRenderer) {
        if (edgeCollectionStrategy == null) {
            edgeCollectionStrategy = EdgeCollectionStrategy<EdgeCollectionContext>()
            //println("Edge collection strategy not set for update")
        }
        if (edgeCollectionContext == null) {
            edgeCollectionContext = EdgeCollectionContext(edges, AreaColor.RED)
        }
        edgeCollectionStrategy.drawAny(shapeRenderer, edgeCollectionContext)
    }
}