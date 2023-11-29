package com.badlogicgames.superjumper;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolygonUtils {
    public static List<Integer> findAdjacentVertices(double[] flatPolygon, double[] targetPoint) {
        double minDistance = Double.POSITIVE_INFINITY;
        List<Integer> closestEdge = null;

        // Iterate through the edges of the polygon
        for (int i = 0; i < flatPolygon.length; i += 2) {
            double x1 = flatPolygon[i];
            double y1 = flatPolygon[i + 1];

            double x2 = flatPolygon[(i + 2) % flatPolygon.length];
            double y2 = flatPolygon[(i + 3) % flatPolygon.length];

            // Calculate the vector representing the edge
            double edgeVectorX = x2 - x1;
            double edgeVectorY = y2 - y1;

            if (edgeVectorX == 0 && edgeVectorY == 0) {
                continue;
            }

            // Calculate the vector from the starting point of the edge to the target point
            double targetVectorX = targetPoint[0] - x1;
            double targetVectorY = targetPoint[1] - y1;

            // Calculate the projection of the target point onto the edge
            double t = (targetVectorX * edgeVectorX + targetVectorY * edgeVectorY) /
                    (edgeVectorX * edgeVectorX + edgeVectorY * edgeVectorY);

            // Clamp the parameter t to the range [0, 1]
            t = Math.max(0, Math.min(t, 1));

            // Calculate the closest point on the edge
            double closestX = x1 + t * (x2 - x1);
            double closestY = y1 + t * (y2 - y1);

            // Calculate the distance between the closest point and the target point
            double distance = Math.sqrt(Math.pow(closestX - targetPoint[0], 2) +
                    Math.pow(closestY - targetPoint[1], 2));

            // Update the closest edge if the current distance is smaller
            if (distance < minDistance) {
                minDistance = distance;
                closestEdge = List.of(i / 2, (i / 2 + 1) % (flatPolygon.length / 2));
            }
        }

        if (closestEdge != null) {
            return closestEdge;
        } else {
            return new ArrayList<>();
        }
    }

    public static double[] polygon_points(double[] polygon, int[] indexes) {
        if (indexes[0] < indexes[1]) {
            return Arrays.copyOfRange(polygon, indexes[0] * 2, indexes[1] * 2);
        }
        if (indexes[1] < indexes[0]) {
            double[] list1 = Arrays.copyOfRange(polygon, indexes[0] * 2, polygon.length);
            double[] list2 = Arrays.copyOfRange(polygon, 0, indexes[1] * 2 + 2);

            return ArrayUtils.addAll(list1, list2);
        }
        return new double[] {polygon[indexes[0] * 2], polygon[indexes[0] * 2 + 1]};
    }
}
