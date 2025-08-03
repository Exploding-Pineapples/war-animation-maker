package com.badlogicgames.waranimationmaker.utilities;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;

/**
 * Port of https://github.com/earcut4j/earcut4j for libgdx with 0 allocation
 * Original port of mapbox/earcut
 */
public final class GdxEarcut {

    private GdxEarcut () {
    }

    private static final Pool<Node> nodePool = new Pool<Node>(100, 10000) {
        @Override
        protected Node newObject () {
            return new Node();
        }
    };

    private static final Array<Node> tempQueue = new Array<Node>();
    private static final IntArray triangles = new IntArray();

    public static IntArray earcut (float[] data) {
        return earcut(data, null, 2);
    }

    public static IntArray earcut (float[] data, int[] holeIndices, int dim) {
        boolean hasHoles = holeIndices != null && holeIndices.length > 0;
        int outerLen = hasHoles ? holeIndices[0] * dim : data.length;

        triangles.clear();

        Node outerNode = linkedList(data, 0, outerLen, dim, true);

        if (outerNode == null || outerNode.next == outerNode.prev) {
            if (outerNode != null) {
                freeNode(outerNode);
            }
            return triangles;
        }

        float minX = 0;
        float minY = 0;
        float maxX = 0;
        float maxY = 0;
        float invSize = Float.MIN_VALUE;

        if (hasHoles) {
            outerNode = eliminateHoles(data, holeIndices, outerNode, dim);
        }

        if (data.length > 80 * dim) {
            minX = maxX = data[0];
            minY = maxY = data[1];

            for (int i = dim; i < outerLen; i += dim) {
                float x = data[i];
                float y = data[i + 1];
                if (x < minX) {
                    minX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }

            invSize = Math.max(maxX - minX, maxY - minY);
            invSize = invSize != 0.0f ? 1.0f / invSize : 0.0f;
        }

        earcutLinked(outerNode, triangles, dim, minX, minY, invSize, Integer.MIN_VALUE);
        return triangles;
    }

    private static void earcutLinked (Node ear, IntArray triangles, int dim, float minX, float minY, float invSize, int pass) {
        if (ear == null) {
            return;
        }

        if (pass == Integer.MIN_VALUE && invSize != Float.MIN_VALUE) {
            indexCurve(ear, minX, minY, invSize);
        }

        Node stop = ear;

        while (ear.prev != ear.next) {
            Node prev = ear.prev;
            Node next = ear.next;

            if (invSize != Float.MIN_VALUE ? isEarHashed(ear, minX, minY, invSize) : isEar(ear)) {
                triangles.add(prev.i / dim);
                triangles.add(ear.i / dim);
                triangles.add(next.i / dim);

                removeNode(ear);

                ear = next.next;
                stop = next.next;

                continue;
            }

            ear = next;

            if (ear == stop) {
                if (pass == Integer.MIN_VALUE) {
                    earcutLinked(filterPoints(ear, null), triangles, dim, minX, minY, invSize, 1);

                } else if (pass == 1) {
                    ear = cureLocalIntersections(filterPoints(ear, null), triangles, dim);
                    earcutLinked(ear, triangles, dim, minX, minY, invSize, 2);

                } else if (pass == 2) {
                    splitEarcut(ear, triangles, dim, minX, minY, invSize);
                }

                break;
            }
        }
    }

    private static void splitEarcut (Node start, IntArray triangles, int dim, float minX, float minY, float size) {
        Node a = start;
        do {
            Node b = a.next.next;
            while (b != a.prev) {
                if (a.i != b.i && isValidDiagonal(a, b)) {
                    Node c = splitPolygon(a, b);

                    a = filterPoints(a, a.next);
                    c = filterPoints(c, c.next);

                    earcutLinked(a, triangles, dim, minX, minY, size, Integer.MIN_VALUE);
                    earcutLinked(c, triangles, dim, minX, minY, size, Integer.MIN_VALUE);
                    return;
                }
                b = b.next;
            }
            a = a.next;
        } while (a != start);
    }

    private static boolean isValidDiagonal (Node a, Node b) {
        return a.next.i != b.i && a.prev.i != b.i && !intersectsPolygon(a, b) && (locallyInside(a, b) && locallyInside(b, a) && middleInside(a, b) && (area(a.prev, a, b.prev) != 0 || area(a, b.prev, b) != 0) || equals(a, b) && area(a.prev, a, a.next) > 0 && area(b.prev, b, b.next) > 0);
    }

    private static boolean middleInside (Node a, Node b) {
        Node p = a;
        boolean inside = false;
        float px = (a.x + b.x) * 0.5f;
        float py = (a.y + b.y) * 0.5f;
        do {
            if (((p.y > py) != (p.next.y > py)) && (px < (p.next.x - p.x) * (py - p.y) / (p.next.y - p.y) + p.x)) {
                inside = !inside;
            }
            p = p.next;
        } while (p != a);

        return inside;
    }

    private static boolean intersectsPolygon (Node a, Node b) {
        Node p = a;
        do {
            if (p.i != a.i && p.next.i != a.i && p.i != b.i && p.next.i != b.i && intersects(p, p.next, a, b)) {
                return true;
            }
            p = p.next;
        } while (p != a);

        return false;
    }

    private static boolean intersects (Node p1, Node q1, Node p2, Node q2) {
        if ((equals(p1, p2) && equals(q1, q2)) || (equals(p1, q2) && equals(p2, q1))) {
            return true;
        }

        float o1 = sign(area(p1, q1, p2));
        float o2 = sign(area(p1, q1, q2));
        float o3 = sign(area(p2, q2, p1));
        float o4 = sign(area(p2, q2, q1));

        if (o1 != o2 && o3 != o4) {
            return true;
        }

        if (o1 == 0 && onSegment(p1, p2, q1)) {
            return true;
        }
        if (o2 == 0 && onSegment(p1, q2, q1)) {
            return true;
        }
        if (o3 == 0 && onSegment(p2, p1, q2)) {
            return true;
        }
        if (o4 == 0 && onSegment(p2, q1, q2)) {
            return true;
        }

        return false;
    }

    private static boolean onSegment (Node p, Node q, Node r) {
        return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
    }

    private static float sign (float num) {
        return num > 0 ? 1 : num < 0 ? -1 : 0;
    }

    private static Node cureLocalIntersections (Node start, IntArray triangles, int dim) {
        Node p = start;
        do {
            Node a = p.prev, b = p.next.next;

            if (!equals(a, b) && intersects(a, p, p.next, b) && locallyInside(a, b) && locallyInside(b, a)) {

                triangles.add(a.i / dim);
                triangles.add(p.i / dim);
                triangles.add(b.i / dim);

                removeNode(p);
                removeNode(p.next);

                p = start = b;
            }
            p = p.next;
        } while (p != start);

        return filterPoints(p, null);
    }

    private static boolean isEar (Node ear) {
        Node a = ear.prev, b = ear, c = ear.next;

        if (area(a, b, c) >= 0) {
            return false;
        }

        Node p = ear.next.next;

        while (p != ear.prev) {
            if (pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y) && area(p.prev, p, p.next) >= 0) {
                return false;
            }
            p = p.next;
        }

        return true;
    }

    private static boolean isEarHashed (Node ear, float minX, float minY, float invSize) {
        Node a = ear.prev;
        Node b = ear;
        Node c = ear.next;

        if (area(a, b, c) >= 0) {
            return false;
        }

        float minTX, minTY, maxTX, maxTY;
        if (a.x < b.x) {
            if (a.x < c.x) {
                minTX = a.x;
                maxTX = b.x > c.x ? b.x : c.x;
            } else {
                minTX = c.x;
                maxTX = b.x > a.x ? b.x : a.x;
            }
        } else {
            if (b.x < c.x) {
                minTX = b.x;
                maxTX = a.x > c.x ? a.x : c.x;
            } else {
                minTX = c.x;
                maxTX = a.x > b.x ? a.x : b.x;
            }
        }

        if (a.y < b.y) {
            if (a.y < c.y) {
                minTY = a.y;
                maxTY = b.y > c.y ? b.y : c.y;
            } else {
                minTY = c.y;
                maxTY = b.y > a.y ? b.y : a.y;
            }
        } else {
            if (b.y < c.y) {
                minTY = b.y;
                maxTY = a.y > c.y ? a.y : c.y;
            } else {
                minTY = c.y;
                maxTY = a.y > b.y ? a.y : b.y;
            }
        }

        float minZ = zOrder(minTX, minTY, minX, minY, invSize);
        float maxZ = zOrder(maxTX, maxTY, minX, minY, invSize);

        Node p = ear.prevZ;
        Node n = ear.nextZ;

        while (p != null && p.z >= minZ && n != null && n.z <= maxZ) {
            if (p != ear.prev && p != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y) && area(p.prev, p, p.next) >= 0) {
                return false;
            }
            p = p.prevZ;

            if (n != ear.prev && n != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, n.x, n.y) && area(n.prev, n, n.next) >= 0) {
                return false;
            }
            n = n.nextZ;
        }

        while (p != null && p.z >= minZ) {
            if (p != ear.prev && p != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y) && area(p.prev, p, p.next) >= 0) {
                return false;
            }
            p = p.prevZ;
        }

        while (n != null && n.z <= maxZ) {
            if (n != ear.prev && n != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, n.x, n.y) && area(n.prev, n, n.next) >= 0) {
                return false;
            }
            n = n.nextZ;
        }

        return true;
    }

    private static float zOrder (float x, float y, float minX, float minY, float invSize) {
        int lx = (int)(32767 * (x - minX) * invSize);
        int ly = (int)(32767 * (y - minY) * invSize);

        lx = (lx | (lx << 8)) & 0x00FF00FF;
        lx = (lx | (lx << 4)) & 0x0F0F0F0F;
        lx = (lx | (lx << 2)) & 0x33333333;
        lx = (lx | (lx << 1)) & 0x55555555;

        ly = (ly | (ly << 8)) & 0x00FF00FF;
        ly = (ly | (ly << 4)) & 0x0F0F0F0F;
        ly = (ly | (ly << 2)) & 0x33333333;
        ly = (ly | (ly << 1)) & 0x55555555;

        return lx | (ly << 1);
    }

    private static void indexCurve (Node start, float minX, float minY, float invSize) {
        Node p = start;
        do {
            if (p.z == Float.MIN_VALUE) {
                p.z = zOrder(p.x, p.y, minX, minY, invSize);
            }
            p.prevZ = p.prev;
            p.nextZ = p.next;
            p = p.next;
        } while (p != start);

        p.prevZ.nextZ = null;
        p.prevZ = null;

        sortLinked(p);
    }

    private static Node sortLinked (Node list) {
        int inSize = 1;

        int numMerges;
        do {
            Node p = list;
            list = null;
            Node tail = null;
            numMerges = 0;

            while (p != null) {
                numMerges++;
                Node q = p;
                int pSize = 0;
                for (int i = 0; i < inSize; i++) {
                    pSize++;
                    q = q.nextZ;
                    if (q == null) {
                        break;
                    }
                }

                int qSize = inSize;

                while (pSize > 0 || (qSize > 0 && q != null)) {
                    Node e;
                    if (pSize == 0) {
                        e = q;
                        q = q.nextZ;
                        qSize--;
                    } else if (qSize == 0 || q == null) {
                        e = p;
                        p = p.nextZ;
                        pSize--;
                    } else if (p.z <= q.z) {
                        e = p;
                        p = p.nextZ;
                        pSize--;
                    } else {
                        e = q;
                        q = q.nextZ;
                        qSize--;
                    }

                    if (tail != null) {
                        tail.nextZ = e;
                    } else {
                        list = e;
                    }

                    e.prevZ = tail;
                    tail = e;
                }

                p = q;
            }

            tail.nextZ = null;
            inSize *= 2;

        } while (numMerges > 1);

        return list;
    }

    private static Node eliminateHoles (float[] data, int[] holeIndices, Node outerNode, int dim) {
        tempQueue.clear();

        int len = holeIndices.length;
        for (int i = 0; i < len; i++) {
            int start = holeIndices[i] * dim;
            int end = i < len - 1 ? holeIndices[i + 1] * dim : data.length;
            Node list = linkedList(data, start, end, dim, false);
            if (list == list.next) {
                list.steiner = true;
            }
            tempQueue.add(getLeftmost(list));
        }

        tempQueue.sort((o1, o2) -> Float.compare(o1.x, o2.x));

        for (int i = 0; i < tempQueue.size; i++) {
            eliminateHole(tempQueue.get(i), outerNode);
            outerNode = filterPoints(outerNode, outerNode.next);
        }

        return outerNode;
    }

    private static Node filterPoints (Node start, Node end) {
        if (start == null) {
            return start;
        }
        if (end == null) {
            end = start;
        }

        Node p = start;
        boolean again;

        do {
            again = false;

            if (!p.steiner && (equals(p, p.next) || area(p.prev, p, p.next) == 0)) {
                removeNode(p);
                p = end = p.prev;
                if (p == p.next) {
                    break;
                }
                again = true;
            } else {
                p = p.next;
            }
        } while (again || p != end);

        return end;
    }

    private static boolean equals (Node p1, Node p2) {
        return p1.x == p2.x && p1.y == p2.y;
    }

    private static float area (Node p, Node q, Node r) {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
    }

    private static void eliminateHole (Node hole, Node outerNode) {
        outerNode = findHoleBridge(hole, outerNode);
        if (outerNode != null) {
            Node b = splitPolygon(outerNode, hole);

            filterPoints(outerNode, outerNode.next);
            filterPoints(b, b.next);
        }
    }

    private static Node splitPolygon (Node a, Node b) {
        Node a2 = obtainNode(a.i, a.x, a.y);
        Node b2 = obtainNode(b.i, b.x, b.y);
        Node an = a.next;
        Node bp = b.prev;

        a.next = b;
        b.prev = a;

        a2.next = an;
        an.prev = a2;

        b2.next = a2;
        a2.prev = b2;

        bp.next = b2;
        b2.prev = bp;

        return b2;
    }

    private static Node findHoleBridge (Node hole, Node outerNode) {
        Node p = outerNode;
        float hx = hole.x;
        float hy = hole.y;
        float qx = -Float.MAX_VALUE;
        Node m = null;

        do {
            if (hy <= p.y && hy >= p.next.y && p.next.y != p.y) {
                float x = p.x + (hy - p.y) * (p.next.x - p.x) / (p.next.y - p.y);
                if (x <= hx && x > qx) {
                    qx = x;
                    if (x == hx) {
                        if (hy == p.y) {
                            return p;
                        }
                        if (hy == p.next.y) {
                            return p.next;
                        }
                    }
                    m = p.x < p.next.x ? p : p.next;
                }
            }
            p = p.next;
        } while (p != outerNode);

        if (m == null) {
            return null;
        }

        if (hx == qx) {
            return m;
        }

        Node stop = m;
        float mx = m.x;
        float my = m.y;
        float tanMin = Float.MAX_VALUE;
        float tan;

        p = m;

        do {
            if (hx >= p.x && p.x >= mx && hx != p.x && pointInTriangle(hy < my ? hx : qx, hy, mx, my, hy < my ? qx : hx, hy, p.x, p.y)) {

                tan = Math.abs(hy - p.y) / (hx - p.x);

                if (locallyInside(p, hole) && (tan < tanMin || (tan == tanMin && (p.x > m.x || (p.x == m.x && sectorContainsSector(m, p)))))) {
                    m = p;
                    tanMin = tan;
                }
            }

            p = p.next;
        } while (p != stop);

        return m;
    }

    private static boolean locallyInside (Node a, Node b) {
        if (area(a.prev, a, a.next) < 0) {
            return area(a, b, a.next) >= 0 && area(a, a.prev, b) >= 0;
        } else {
            return area(a, b, a.prev) < 0 || area(a, a.next, b) < 0;
        }
    }

    private static boolean sectorContainsSector (Node m, Node p) {
        return area(m.prev, m, p.prev) < 0 && area(p.next, m, m.next) < 0;
    }

    private static boolean pointInTriangle (float ax, float ay, float bx, float by, float cx, float cy, float px, float py) {
        return (cx - px) * (ay - py) - (ax - px) * (cy - py) >= 0 && (ax - px) * (by - py) - (bx - px) * (ay - py) >= 0 && (bx - px) * (cy - py) - (cx - px) * (by - py) >= 0;
    }

    private static Node getLeftmost (Node start) {
        Node p = start;
        Node leftmost = start;
        do {
            if (p.x < leftmost.x || (p.x == leftmost.x && p.y < leftmost.y)) {
                leftmost = p;
            }
            p = p.next;
        } while (p != start);
        return leftmost;
    }

    private static Node linkedList (float[] data, int start, int end, int dim, boolean clockwise) {
        Node last = null;
        if (clockwise == (signedArea(data, start, end, dim) > 0)) {
            for (int i = start; i < end; i += dim) {
                last = insertNode(i, data[i], data[i + 1], last);
            }
        } else {
            for (int i = (end - dim); i >= start; i -= dim) {
                last = insertNode(i, data[i], data[i + 1], last);
            }
        }

        if (last != null && equals(last, last.next)) {
            removeNode(last);
            last = last.next;
        }
        return last;
    }

    private static void removeNode (Node p) {
        p.next.prev = p.prev;
        p.prev.next = p.next;

        if (p.prevZ != null) {
            p.prevZ.nextZ = p.nextZ;
        }
        if (p.nextZ != null) {
            p.nextZ.prevZ = p.prevZ;
        }

        freeNode(p);
    }

    private static Node insertNode (int i, float x, float y, Node last) {
        Node p = obtainNode(i, x, y);

        if (last == null) {
            p.prev = p;
            p.next = p;
        } else {
            p.next = last.next;
            p.prev = last;
            last.next.prev = p;
            last.next = p;
        }
        return p;
    }

    private static float signedArea (float[] data, int start, int end, int dim) {
        float sum = 0;
        int j = end - dim;
        for (int i = start; i < end; i += dim) {
            sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1]);
            j = i;
        }
        return sum;
    }

    private static Node obtainNode (int i, float x, float y) {
        Node node = nodePool.obtain();
        node.reset(i, x, y);
        return node;
    }

    private static void freeNode (Node node) {

        if (node != null) {
            nodePool.free(node);
        }
    }

    private static void freeLinkedList (Node start) {
        if (start == null) {
            return;
        }

        Node current = start;
        boolean first = true;
        while (current != null && (first || current != start)) {
            first = false;
            Node next = current.next;
            freeNode(current);
            current = next;
        }
    }

    private static class Node implements Pool.Poolable {
        int i;
        float x;
        float y;
        float z;
        boolean steiner;

        Node prev;
        Node next;
        Node prevZ;
        Node nextZ;

        Node () {
        }

        void reset (int i, float x, float y) {
            this.i = i;
            this.x = x;
            this.y = y;
            this.prev = null;
            this.next = null;
            this.z = Float.MIN_VALUE;
            this.prevZ = null;
            this.nextZ = null;
            this.steiner = false;
        }

        @Override
        public void reset () {
            this.i = 0;
            this.x = 0;
            this.y = 0;
            this.z = Float.MIN_VALUE;
            this.steiner = false;
            this.prev = null;
            this.next = null;
            this.prevZ = null;
            this.nextZ = null;
        }
    }
}