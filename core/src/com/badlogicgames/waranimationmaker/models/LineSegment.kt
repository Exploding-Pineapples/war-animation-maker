package com.badlogicgames.waranimationmaker.models

class LineSegment(val lineID: LineID, val segment: Pair<NodeID, NodeID>) {
    override fun toString(): String {
        return "LineSegment of line with id ${lineID.value}, from node ${segment.first.value} to node ${segment.second.value})"
    }
    fun contains(nodeID: NodeID, line: Line): Boolean {
        val ids = line.nodeIDs
        return (ids.indexOf(nodeID) in ids.indexOf(segment.first)..ids.indexOf(segment.second))
    }
}