package assignment2;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PathFinding extends PApplet {
    private static final long INF = 100000000000000L;
    private static final int[] xMod = new int[]{-1,0,1,1,1,0,-1,-1};
    private static final int[] yMod = new int[]{-1,-1,-1,0,1,1,1,0};
    private Knight knight;
    private MyImage castle;
    private MyImage tarPit;
    private MyImage tavern;
    private MyImage tree;
    private MyImage cave;
    private MyImage forge;
    private Map<String, PShape> obstacleMap;
    private int[] OBSTACLE_COLORS;
    private Graph graph;
    private Map<Vertex, Boolean> blockedCache;
    private PVector target;

    public static void main(String[] args) {
        PApplet.main("assignment2.PathFinding", args);
    }

    public void settings() {
        size(640, 480);
    }

    @Override
    public void setup() {
        frameRate(200.0f);
        blockedCache = new HashMap<>();
        target = null;
        int pixelLimit = 50;
        OBSTACLE_COLORS = new int[]{color(84,134,214), color(42, 176, 109), color(189, 109, 87)};
        JSONObject json = loadJSONObject(sketchPath("data/map3.json"));
        MyImage knightImg = new MyImage(loadImage(sketchPath("images/knight.png")),
                new Vertex(json.getJSONArray("knight_start").getFloat(0),
                        json.getJSONArray("knight_start").getFloat(1)), pixelLimit);
        knight = new Knight(knightImg);
        castle = Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("castle"))
                .map(arr -> new Vertex(arr.getFloat(0), arr.getFloat(1)))
                .map(forgV -> new MyImage(loadImage(sketchPath("images/castle.png")),
                        forgV, pixelLimit)).orElse(null);
        tarPit = Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tar_pit"))
                .map(arr -> new Vertex(arr.getFloat(0), arr.getFloat(1)))
                .map(forgV -> new MyImage(loadImage(sketchPath("images/tarpit.jpg")),
                        forgV, pixelLimit)).orElse(null);
        tavern = Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tavern"))
                .map(arr -> new Vertex(arr.getFloat(0), arr.getFloat(1)))
                .map(forgV -> new MyImage(loadImage(sketchPath("images/tavern.png")),
                        forgV, pixelLimit)).orElse(null);
        cave = Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("cave"))
                .map(arr -> new Vertex(arr.getFloat(0), arr.getFloat(1)))
                .map(forgV -> new MyImage(loadImage(sketchPath("images/cave.jpg")),
                        forgV, pixelLimit)).orElse(null);
        tree = Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tree"))
                .map(arr -> new Vertex(arr.getFloat(0), arr.getFloat(1)))
                .map(forgV -> new MyImage(loadImage(sketchPath("images/tree.png")),
                        forgV, pixelLimit)).orElse(null);
        forge = Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("forge"))
                .map(arr -> new Vertex(arr.getFloat(0), arr.getFloat(1)))
                .map(forgV -> new MyImage(loadImage(sketchPath("images/forge.png")),
                        forgV, pixelLimit)).orElse(null);

        JSONObject obstacles = json.getJSONObject("obstacles");
        Set<String> obstacleNameSet = ((Set<Object>)obstacles.keys()).stream().map(Object::toString)
                .collect(Collectors.toSet());
        List<String> obstacleNames = new ArrayList<>(obstacleNameSet);
        int noOfObstacles = obstacleNames.size();
        obstacleMap = IntStream.range(0, noOfObstacles).boxed()
                .collect(Collectors.toMap(obstacleNames::get,
                        ind -> getObstacle(obstacles, obstacleNames.get(ind), OBSTACLE_COLORS[ind % 3])));
        this.graph = getGraph();
    }

    private Graph getGraph() {
        Graph g = new Graph();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                g.addVertex(new Vertex(x,y));
            }
        }
        Set<Vertex> vertices = g.adjList.keySet();
        for (Vertex v: vertices) {
            for (int i = 0; i < 8; i++) {
                float neighX = v.xCoord + xMod[i];
                float neighY = v.yCoord + yMod[i];
                if (isValid(neighX, neighY) && !isBlocked(neighX, neighY)) {
                    g.addEdge(new Edge(v, new Vertex(neighX, neighY), 1));
                }
            }
        }
        return g;
    }

    private boolean isValid(float x, float y) {
        return  (x >= 0 && x < width && y >= 0 && y < height);
    }

    private boolean isBlocked(float x, float y) {
        Vertex v = new Vertex(x,y);
        return Optional.ofNullable(blockedCache.get(v)).orElseGet(() -> {
            boolean ans = obstacleMap.values().stream()
                    .anyMatch(shp -> isInsidePolygon(new PVector(x, y), getVertices(shp)));
            blockedCache.put(v, ans);
            return ans;
        });
    }

    private List<PVector> getVertices(PShape pShape) {
        int vertexCount = pShape.getVertexCount();
        List<PVector> vertices = new ArrayList<>();
        for (int i = 0; i < vertexCount; i++) {
            vertices.add(pShape.getVertex(i));
        }
        return vertices;
    }

    private boolean isInsidePolygon(PVector pos, List<PVector> vertices) {
        int j = vertices.size()-1;
        int sides = vertices.size();
        boolean oddNodes = false;
        for (int i = 0; i < sides; i++) {
            if ((vertices.get(i).y < pos.y && vertices.get(j).y >= pos.y || vertices.get(j).y < pos.y && vertices.get(i).y >= pos.y)
                    && (vertices.get(i).x <= pos.x || vertices.get(j).x <= pos.x)) {
                oddNodes^=(vertices.get(i).x
                        + (pos.y-vertices.get(i).y)/(vertices.get(j).y - vertices.get(i).y)
                        *(vertices.get(j).x-vertices.get(i).x) < pos.x);
            }
            j=i;
        }
        return oddNodes;
    }

    private PShape getObstacle(JSONObject obstacles, String key, int col) {
        JSONArray vertices = obstacles.getJSONArray(key);
        PShape ret = createShape();
        ret.setStroke(0);
        ret.setFill(col);
        ret.beginShape();
        int len = vertices.size();
        for (int i = 0; i < len; i++) {
            JSONArray coords = vertices.getJSONArray(i);
            ret.vertex(coords.getFloat(0), coords.getFloat(1));
        }
        ret.endShape(CLOSE);
        return ret;
    }

    @Override
    public void draw() {
        background(255);
        Optional.ofNullable(castle).ifPresent(MyImage::display);
        Optional.ofNullable(tarPit).ifPresent(MyImage::display);
        Optional.ofNullable(tavern).ifPresent(MyImage::display);
        Optional.ofNullable(cave).ifPresent(MyImage::display);
        obstacleMap.values().forEach(this::shape);
        Optional.ofNullable(tree).ifPresent(MyImage::display);
        Optional.ofNullable(forge).ifPresent(MyImage::display);
        if (mousePressed) {
            target = new PVector(mouseX, mouseY);
            knight.plan = AStar(graph, vertex -> vectorFrom(vertex).dist(target), vertexFrom(knight.currentPos), vertexFrom(target)).stream()
                    .map(PathFinding::vectorFrom).collect(Collectors.toList());
            System.out.println("knight.plan = " + knight.plan);
            knight.currentIndex = -1;
        }
        knight.move();
        knight.display();
    }

    private static Vertex vertexFrom(PVector pv) {
        return new Vertex(pv.x, pv.y);
    }

    private static PVector vectorFrom(Vertex v) {
        return new PVector(v.xCoord, v.yCoord);
    }

    private class Knight {
        public PVector currentPos;
        public List<PVector> plan;
        public int currentIndex;
        public MyImage img;

        public Knight(MyImage img) {
            this.currentPos = new PVector(img.position.xCoord, img.position.yCoord);
            this.plan = new ArrayList<>();
            this.currentIndex = -1;
            this.img = img;
        }

        public Knight(PVector currentPos, MyImage img) {
            this.currentPos = currentPos;
            this.plan = new ArrayList<>();
            this.currentIndex = -1;
            this.img = img;
        }

        public PVector move() {
            if (currentIndex == plan.size() - 1) {
                return null;
            } else {
                currentIndex++;
                currentPos = plan.get(currentIndex);
                return currentPos;
            }
        }

        public void display() {
            img.display(currentPos);
        }
    }


    private class MyImage {
        public PImage img;
        public Vertex position;
        public float width;
        public float height;

        public void display() {
            imageMode(CENTER);
            image(this.img, position.xCoord, position.yCoord, width, height);
        }

        public void display(PVector pos) {
            imageMode(CENTER);
            image(this.img, pos.x, pos.y, width, height);
        }

        public MyImage(PImage img, Vertex position) {
            this.img = img;
            this.position = position;
            this.width = img.width;
            this.height = img.height;
        }

        public MyImage(PImage img, Vertex position, float scaleFactor) {
            this.img = img;
            this.position = position;
            this.width = img.width*scaleFactor;
            this.height = img.height*scaleFactor;
        }

        public MyImage(PImage img, Vertex position, int limit) {
            this.img = img;
            this.position = position;
            float biggerSide = Math.max(img.width, img.height);
//            System.out.println("biggerSide = " + biggerSide);
//            System.out.println("limit = " + limit);
            if (biggerSide <= limit) {
                this.width = img.width;
                this.height = img.height;
            } else {
                float ratio = limit / biggerSide;
                this.width = img.width*ratio;
                this.height = img.height*ratio;
            }
        }
    }

    private static class Graph {
        public Map<Vertex, Set<Edge>> adjList;

        public Graph() {
            this.adjList = new HashMap<>();
        }

        public Graph addVertex(Vertex v) {
            Set<Edge> edges = Optional.ofNullable(adjList.get(v)).orElseGet(HashSet::new);
            adjList.put(v, edges);
            return this;
        }

        public Graph addEdge(Edge e) {
            Vertex v1 = e.first;
            Vertex v2 = e.second;
            Set<Edge> edges1 = Optional.ofNullable(adjList.get(v1)).orElseGet(HashSet::new);
            Set<Edge> edges2 = Optional.ofNullable(adjList.get(v2)).orElseGet(HashSet::new);
            edges1.add(e);
            Edge reverseEdge = new Edge(v2, v1, e.distance);
            edges2.add(reverseEdge);
            adjList.put(v1, edges1);
            adjList.put(v2, edges2);
            return this;
        }
    }

    private static class Edge {
        public Vertex first;
        public Vertex second;
        public int distance;

        public Edge(Vertex first, Vertex second, int distance) {
            this.first = first;
            this.second = second;
            this.distance = distance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return distance == edge.distance &&
                    Objects.equals(first, edge.first) &&
                    Objects.equals(second, edge.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second, distance);
        }
    }

    private static class Vertex {
        public float xCoord;
        public float yCoord;

        public Vertex(float xCoord, float yCoord) {
            this.xCoord = xCoord;
            this.yCoord = yCoord;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return Float.compare(vertex.xCoord, xCoord) == 0 &&
                    Float.compare(vertex.yCoord, yCoord) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(xCoord, yCoord);
        }
    }


    public static List<Vertex> AStar(Graph g, Function<Vertex, Float> hFun, Vertex sourceV, Vertex destV) {
        Set<Vertex> expanded = new HashSet<>();
        Map<Vertex, Long> gFun = g.adjList.keySet().stream().collect(Collectors.toMap(v -> v, v -> INF));
        gFun.put(sourceV, 0L);

        Function<Vertex, Float> f = v -> gFun.get(v) + hFun.apply(v);
        PriorityQueue<Vertex> pq = new PriorityQueue<>(Comparator.comparing(f));
        pq.add(sourceV);
        int maxQueueSize = 1;
        int effectiveQueueSize = 1;
        Map<Vertex, Vertex> previousVertexInShortestPath = new HashMap<>();
        previousVertexInShortestPath.put(sourceV, sourceV);
        while (!pq.isEmpty()) {
            Vertex head = pq.poll();
            if (expanded.contains(head)) {
                continue;
            } else {
                effectiveQueueSize--;
            }
            if (head.equals(destV)) {
                break;
            }
            expanded.add(head);
            Set<Edge> incidentEdges = g.adjList.get(head);
            long pathFromSourceToHead = gFun.get(head);
            for (Edge e: incidentEdges) {
                Vertex otherV = e.second;
                long pathFromSourceToOtherV = gFun.get(otherV);
                long newPathLength = pathFromSourceToHead + e.distance;
                if (pathFromSourceToOtherV == INF) {
                    gFun.put(otherV, newPathLength);
                    pq.add(otherV);
                    effectiveQueueSize++;
                    maxQueueSize = Math.max(maxQueueSize, effectiveQueueSize);
                    previousVertexInShortestPath.put(otherV, head);
                } else if (pathFromSourceToOtherV > newPathLength) {
                    gFun.put(otherV, newPathLength);
                    previousVertexInShortestPath.put(otherV, head);
                    if (!expanded.contains(otherV)) {
                        pq.add(otherV);
                    }
                }
            }
        }
        List<Vertex> finalPath = new ArrayList<>();
        finalPath.add(destV);
        Vertex v = destV;
        while(true) {
            Vertex parent = previousVertexInShortestPath.get(v);
            if (parent == null || parent.equals(v)) {
                break;
            }
            finalPath.add(parent);
            v = parent;
        }
        Collections.reverse(finalPath);
        if (!finalPath.get(0).equals(sourceV)) {
            return new ArrayList<>();
        }
        /*
        long temppl = 0L;
        for (int i = 0; i < finalPath.size() - 1; i++) {
            Vertex curr = finalPath.get(i);
            Vertex next = finalPath.get(i+1);
            Set<Edge> edges = g.adjList.get(curr);
            for (Edge e: edges) {
                if (e.second.equals(next)) {
                    temppl += e.distance;
                    break;
                }
            }
        }
        System.out.println("temppl = " + temppl);
        */
//        System.out.println("A*");
//        System.out.println("No. of nodes expanded = " + expanded.size());
//        System.out.println("Max. length of queue during search = " + maxQueueSize);
//        System.out.println("Final path length = " + gFun.get(destV));
        return finalPath;
    }
}
