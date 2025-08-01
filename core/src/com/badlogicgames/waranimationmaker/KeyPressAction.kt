package com.badlogicgames.waranimationmaker

import com.badlogicgames.waranimationmaker.models.AnyObject

class Action private constructor(builder: Builder) {
    val actionKeys = builder.actionKeys
    val actionName: String = builder.actionName
    val description: String = builder.description
    val requiresShift: Boolean = builder.requiresShift
    val requiresControl: Boolean = builder.requiresControl
    val requiresSelected: Requirement = builder.requiresSelected
    val requiredSelectedTypes: List<Class<*>> = builder.requiredSelectedTypes
    val requiredTouchModes: List<TouchMode> = builder.requiredTouchModes
    val excludedTouchModes: List<TouchMode> = builder.excludedTouchModes
    val action: () -> Unit = builder.action
    val debug: Boolean = builder.debug

    //returns if the action could be executed by key press given the current conditions
    fun couldExecute(shiftPressed: Boolean, controlPressed: Boolean, selected: ArrayList<AnyObject>, touchMode: TouchMode): Boolean {
        if (debug) {
            println("Shift pressed: $shiftPressed" + "requires shift: $requiresShift")
            println("Control pressed: $controlPressed" + "requires control: $requiresControl")
            println("Selected requirement: " + requiresSelected.name)
            selected.forEach { println("Selected: " + it.javaClass.name + "required selected type: $requiredSelectedTypes") }
        }

        if (shiftPressed != requiresShift) {
            return false
        }
        if (controlPressed != requiresControl) {
            return false
        }

        if (requiresSelected == Requirement.REQUIRES_NOT && selected.isNotEmpty()) {
            return false
        }

        if (requiresSelected == Requirement.REQUIRES) {
            if (selected.isEmpty()) {
                return false
            }
            selected.forEach { // Every selected object must conform to the required types
                var correctType = false
                for (type in requiredSelectedTypes) {
                    if (type.isAssignableFrom(it.javaClass)) {
                        correctType = true
                        break
                    }
                }
                if (!correctType) {
                    return false
                }
            }
        }

        if (requiredTouchModes.isNotEmpty()) {
            if (touchMode !in requiredTouchModes) {
                return false
            }
        }
        if (excludedTouchModes.isNotEmpty()) {
            if (touchMode in excludedTouchModes) {
                return false
            }
        }

        return true
    }
    //returns if the action should be executed given the current conditions
    fun shouldExecute(keyPressed: Int, shiftPressed: Boolean, controlPressed: Boolean, selected: ArrayList<AnyObject>, touchMode: TouchMode): Boolean {
        return couldExecute(shiftPressed, controlPressed, selected, touchMode) && (keyPressed in actionKeys)
    }

    fun execute() {
        action.invoke()
    }

    companion object {
        @JvmStatic
        fun createBuilder(action: () -> Unit, actionName: String, vararg actionKeys: Int) = Builder(action, actionName, actionKeys)
        class Builder(var action: () -> Unit, var actionName: String, actionKeys: IntArray) {
            val actionKeys: MutableList<Int> = MutableList(actionKeys.size){actionKeys[it]}
            var description: String = ""
            var requiresShift: Boolean = false
            var requiresControl: Boolean = false
            var requiresSelected: Requirement = Requirement.ANY
            var requiredSelectedTypes: MutableList<Class<*>> = mutableListOf(AnyObject::class.java)
            var requiredTouchModes: MutableList<TouchMode> = mutableListOf()
            var excludedTouchModes: MutableList<TouchMode> = mutableListOf()
            var debug: Boolean = false

            fun description(description: String): Builder {
                this.description = description
                return this
            }
            fun requiresShift (requiresShift: Boolean): Builder {
                this.requiresShift = requiresShift
                return this
            }
            fun requiresControl (requiresControl: Boolean): Builder {
                this.requiresControl = requiresControl
                return this
            }
            fun requiresSelected(requiresSelected: Requirement): Builder {
                this.requiresSelected = requiresSelected
                return this
            }
            fun clearRequiredSelectedTypes(): Builder {
                requiredSelectedTypes.clear()
                return this
            }
            fun requiredSelectedTypes(vararg types: Class<*>): Builder {
                this.requiredSelectedTypes += types
                return this
            }
            fun requiredTouchModes(vararg requiredTouchModes: TouchMode): Builder {
                this.requiredTouchModes += requiredTouchModes
                return this
            }
            fun excludedTouchModes(vararg excludedTouchModes: TouchMode): Builder {
                this.excludedTouchModes += excludedTouchModes
                return this
            }
            fun debug(): Builder {
                debug = true
                return this
            }
            fun build(): Action {
                return Action(this)
            }
        }
    }
}