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

public class Decisions extends PApplet {
    private static final long INF = 100000000000000L;
    private static final int[] xMod = new int[]{-1,0,1,1,1,0,-1,-1};
    private static final int[] yMod = new int[]{-1,-1,-1,0,1,1,1,0};
    private Knight knight;
    private Map<String, GameCharacter> gameCharacterMap;
    private Map<String, PShape> obstacleMap;
    private int[] OBSTACLE_COLORS;
    private Graph graph;
    private Map<PVector, Boolean> blockedCache;
    private int kingGold;
//    private PVector target;

    public static void main(String[] args) {
        PApplet.main("assignment2.Decisions", args);
    }

    public void settings() {
        size(640, 480);
    }

    @Override
    public void setup() {
        frameRate(200.0f);
        blockedCache = new HashMap<>();
        gameCharacterMap = new HashMap<>();
//        target = null;
        int pixelLimit = 50;
        OBSTACLE_COLORS = new int[]{color(84,134,214), color(42, 176, 109), color(189, 109, 87)};
        JSONObject json = loadJSONObject(sketchPath("data/mapTest3.json"));
        MyImage knightImg = new MyImage(loadImage(sketchPath("images/knight.png")), pixelLimit);
        Knight knight = new Knight(new PVector(json.getJSONArray("knight_start").getFloat(0),
                json.getJSONArray("knight_start").getFloat(1)), knightImg,
                "Knight");
        this.knight = knight;
        gameCharacterMap.put("Knight", knight);

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("castle"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/castle.png")), pixelLimit), "King"))
                .ifPresent(king -> gameCharacterMap.put("King", king));
        kingGold = json.getInt("greet_king");

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tar_pit"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/tarpit.jpg")), pixelLimit), "Rameses"))
                .ifPresent(rameses -> gameCharacterMap.put("Rameses", rameses));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tavern"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/tavern.png")), pixelLimit), "Innkeeper"))
                .ifPresent(innkeeper -> gameCharacterMap.put("Innkeeper", innkeeper));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("cave"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/cave.jpg")), pixelLimit), "Lady Lupa"))
                .ifPresent(ladyLupa -> gameCharacterMap.put("Lady Lupa", ladyLupa));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tree"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/tree.png")), pixelLimit), "Tree Spirit"))
                .ifPresent(treeSpirit -> gameCharacterMap.put("Tree Spirit", treeSpirit));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("forge"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos, new MyImage(loadImage(sketchPath("images/forge.png")), pixelLimit), "Blacksmith"))
                .ifPresent(blacksmith -> gameCharacterMap.put("Blacksmith", blacksmith));
        readGameState(json);

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

    private void readGameState(JSONObject json) {
        JSONObject worldState = json.getJSONObject("state_of_world");
        JSONArray hasArr = worldState.getJSONArray("Has");
        int hasSize = hasArr.size();
        for (int i = 0; i < hasSize; i++) {
            JSONArray elements = hasArr.getJSONArray(i);
            String characterName = elements.getString(0);
            String object = elements.getString(1);
            GameCharacter character = gameCharacterMap.get(characterName);
            if (character != null) {
                int value = character.has.getOrDefault(object, 0);
                character.has.put(object, value+1);
            }
        }
        JSONArray wantArr = worldState.getJSONArray("Wants");
        int wantSize = wantArr.size();
        for (int i = 0; i < wantSize; i++) {
            JSONArray elements = wantArr.getJSONArray(i);
            String characterName = elements.getString(0);
            String object = elements.getString(1).replaceAll("\\s", "")
                    .replaceAll("[0-9]", "");
            System.out.println("object = " + object);
            GameCharacter character = gameCharacterMap.get(characterName);
            if (character != null) {
                int value = character.wants.getOrDefault(object, 0);
                character.wants.put(object, value+1);
            }
        }
    }

    private static class GameCharacter {
        public PVector location;
        public Map<String, Integer> has;
        public Map<String, Integer> wants;
        public boolean isActive;
        public MyImage img;
        public String name;

        public void display() {
            img.display(location);
        }

        public GameCharacter(PVector location,  MyImage img, String name) {
            this.location = location;
            this.has = new HashMap<>();
            this.wants = new HashMap<>();
            this.img = img;
            this.name = name;
        }

        public GameCharacter(PVector location, Map<String, Integer> has, Map<String, Integer> wants, MyImage img,
                             String name) {
            this.location = location;
            this.has = has;
            this.wants = wants;
            this.img = img;
            this.name = name;
        }
    }

    private Graph getGraph() {
        Graph g = new Graph();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                g.addPVector(new PVector(x,y));
            }
        }
        Set<PVector> vertices = g.adjList.keySet();
        for (PVector v: vertices) {
            for (int i = 0; i < 8; i++) {
                float neighX = v.x + xMod[i];
                float neighY = v.y + yMod[i];
                if (isValid(neighX, neighY) && !isBlocked(neighX, neighY)) {
                    g.addEdge(new Edge(v, new PVector(neighX, neighY), 1));
                }
            }
        }
        return g;
    }

    private boolean isValid(float x, float y) {
        return  (x >= 0 && x < width && y >= 0 && y < height);
    }

    private boolean isBlocked(float x, float y) {
        PVector v = new PVector(x,y);
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
        gameCharacterMap.values().forEach(GameCharacter::display);
        obstacleMap.values().forEach(this::shape);
//        if (mousePressed) {
//            target = new PVector(mouseX, mouseY);
//            knight.plan = AStar(graph, vertex -> vertex.dist(target), knight.currentPos, target);
//            System.out.println("knight.plan = " + knight.plan);
//            knight.currentIndex = -1;
//        }
//        knight.move();
//        knight.display();
    }

    private class Knight extends GameCharacter {
        public List<PVector> plan;
        public int currentIndex;

        public Knight(PVector location, Map<String, Integer> has, Map<String, Integer> wants, MyImage img,
                      String name) {
            super(location, has, wants, img, name);
            this.plan = new ArrayList<>();
            this.currentIndex = -1;
        }

        public Knight(PVector location, MyImage img, String name) {
            super(location, img, name);
            this.plan = new ArrayList<>();
            this.currentIndex = -1;
        }

        public PVector move() {
            if (currentIndex == plan.size() - 1) {
                return null;
            } else {
                currentIndex++;
                location = plan.get(currentIndex);
                return location;
            }
        }
    }


    private class MyImage {
        public PImage img;
        public float width;
        public float height;

        public void display(PVector pos) {
            imageMode(CENTER);
            image(this.img, pos.x, pos.y, width, height);
        }

        public MyImage(PImage img) {
            this.img = img;
            this.width = img.width;
            this.height = img.height;
        }

        public MyImage(PImage img, float scaleFactor) {
            this.img = img;
            this.width = img.width*scaleFactor;
            this.height = img.height*scaleFactor;
        }

        public MyImage(PImage img, int limit) {
            this.img = img;
            float biggerSide = Math.max(img.width, img.height);
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
        public Map<PVector, Set<Edge>> adjList;

        public Graph() {
            this.adjList = new HashMap<>();
        }

        public Graph addPVector(PVector v) {
            Set<Edge> edges = Optional.ofNullable(adjList.get(v)).orElseGet(HashSet::new);
            adjList.put(v, edges);
            return this;
        }

        public Graph addEdge(Edge e) {
            PVector v1 = e.first;
            PVector v2 = e.second;
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
        public PVector first;
        public PVector second;
        public int distance;

        public Edge(PVector first, PVector second, int distance) {
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

    public static List<PVector> AStar(Graph g, Function<PVector, Float> hFun, PVector sourceV, PVector destV) {
        Set<PVector> expanded = new HashSet<>();
        Map<PVector, Long> gFun = g.adjList.keySet().stream().collect(Collectors.toMap(v -> v, v -> INF));
        gFun.put(sourceV, 0L);

        Function<PVector, Float> f = v -> gFun.get(v) + hFun.apply(v);
        PriorityQueue<PVector> pq = new PriorityQueue<>(Comparator.comparing(f));
        pq.add(sourceV);
        int maxQueueSize = 1;
        int effectiveQueueSize = 1;
        Map<PVector, PVector> previousPVectorInShortestPath = new HashMap<>();
        previousPVectorInShortestPath.put(sourceV, sourceV);
        while (!pq.isEmpty()) {
            PVector head = pq.poll();
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
                PVector otherV = e.second;
                long pathFromSourceToOtherV = gFun.get(otherV);
                long newPathLength = pathFromSourceToHead + e.distance;
                if (pathFromSourceToOtherV == INF) {
                    gFun.put(otherV, newPathLength);
                    pq.add(otherV);
                    effectiveQueueSize++;
                    maxQueueSize = Math.max(maxQueueSize, effectiveQueueSize);
                    previousPVectorInShortestPath.put(otherV, head);
                } else if (pathFromSourceToOtherV > newPathLength) {
                    gFun.put(otherV, newPathLength);
                    previousPVectorInShortestPath.put(otherV, head);
                    if (!expanded.contains(otherV)) {
                        pq.add(otherV);
                    }
                }
            }
        }
        List<PVector> finalPath = new ArrayList<>();
        finalPath.add(destV);
        PVector v = destV;
        while(true) {
            PVector parent = previousPVectorInShortestPath.get(v);
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
            PVector curr = finalPath.get(i);
            PVector next = finalPath.get(i+1);
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
