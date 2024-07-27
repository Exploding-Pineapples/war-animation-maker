package com.badlogicgames.waranimationmaker.models

class LineSegment(val lineID: LineID, val segment: Pair<NodeID, NodeID>) {
    override fun toString(): String {
        return "LineSegment of line with id ${lineID.value}, from node ${segment.first.value} to node ${segment.second.value})"
    }
}