package com.badlogicgames.superjumper

import com.badlogicgames.superjumper.models.Object

val actions = mutableListOf<Action>()

class Action private constructor(builder: Builder) {
    val actionKeys: List<Int> = builder.actionKeys
    val actionName: String = builder.actionName
    val description: String = builder.description
    val requiresShift: Boolean = builder.requiresShift
    val requiresControl: Boolean = builder.requiresControl
    val requiresSelected: Boolean = builder.requiresSelected
    val requiredSelectedTypes: List<Class<*>> = builder.requiredSelectedTypes
    val requiredInputModes: List<InputMode> = builder.requiredInputModes
    val exludedInputModes: List<InputMode> = builder.excludedInputModes
    val action: () -> Unit = builder.action

    //returns if the action could be executed by key press given the current conditions
    fun couldExecute(shiftPressed: Boolean, controlPressed: Boolean, selected: Object?, inputMode: InputMode): Boolean {
        if (shiftPressed != requiresShift) {
            return false
        }
        if (controlPressed != requiresControl) {
            return false
        }
        if ((selected == null) == requiresSelected) {
            return false
        }

        if (requiresSelected) {
            var correctType = false;
            if (selected != null) {
                for (type in requiredSelectedTypes) {
                    if (type.isAssignableFrom(selected.javaClass)) {
                        correctType = true
                        break
                    }
                }
            }
            if (!correctType) {
                return false
            }
        }

        if (inputMode !in requiredInputModes) {
            return false
        }
        if (inputMode in exludedInputModes) {
            return false
        }
        return true
    }
    //returns if the action should be executed given the current conditions
    fun shouldExecute(keyPressed: Int, shiftPressed: Boolean, controlPressed: Boolean, selected: Object?, inputMode: InputMode): Boolean {
        return couldExecute(shiftPressed, controlPressed, selected, inputMode) && (keyPressed in actionKeys)
    }

    fun execute() {
        action.invoke()
    }

    companion object {
        @JvmStatic
        fun createBuilder(action: () -> Unit, actionKeys: List<Int>, actionName: String) = Builder(action, actionKeys.toMutableList(), actionName)
        class Builder(var action: () -> Unit, var actionKeys: MutableList<Int>, var actionName: String) {
            var description: String = ""
            var requiresShift: Boolean = false
            var requiresControl: Boolean = false
            var requiresSelected: Boolean = false
            var requiredSelectedTypes: MutableList<Class<*>> = mutableListOf(Object::class.java)
            var requiredInputModes: MutableList<InputMode> = mutableListOf(InputMode.NONE)
            var excludedInputModes: MutableList<InputMode> = mutableListOf()
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
            fun requiresSelected(requiresSelected: Boolean): Builder {
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
            fun requiredInputModes(vararg requiredInputModes: InputMode): Builder {
                this.requiredInputModes += requiredInputModes
                return this
            }
            fun excludedInputModes(vararg excludedInputModes: InputMode): Builder {
                this.excludedInputModes += excludedInputModes
                return this
            }
            fun build(): Action {
                return Action(this).apply { actions += this }
            }
        }
    }
}