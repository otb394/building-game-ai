package assignment2;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
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
    private List<Action> actionPlan;
    private State initialState;
    private int nextAction;
    private String ownerOfFenrir;
    private int noOfAction;
    private boolean weWin;
//    private int count;
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
        JSONObject json = loadJSONObject(sketchPath("data/piazzaTestMap.json"));
        MyImage knightImg = new MyImage(loadImage(sketchPath("images/knight.png")), pixelLimit);
        Knight knight = new Knight(new PVector(json.getJSONArray("knight_start").getFloat(0),
                json.getJSONArray("knight_start").getFloat(1)), knightImg,
                "Knight", "Starting position");
        this.knight = knight.changePosition(knight.location);
        gameCharacterMap.put("Knight", knight);

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("castle"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/castle.png")), pixelLimit), "King", "Maugrim Castle"))
                .ifPresent(king -> gameCharacterMap.put("King", king));
        kingGold = json.getInt("greet_king");

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tar_pit"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/tarpit.jpg")), pixelLimit), "Rameses", "Tar Pit"))
                .ifPresent(rameses -> gameCharacterMap.put("Rameses", rameses));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tavern"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/tavern.png")), pixelLimit), "Innkeeper", "Tavern"))
                .ifPresent(innkeeper -> gameCharacterMap.put("Innkeeper", innkeeper));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("cave"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/cave.jpg")), pixelLimit), "Lady Lupa", "Ancient Cave"))
                .ifPresent(ladyLupa -> gameCharacterMap.put("Lady Lupa", ladyLupa));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("tree"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos,
                        new MyImage(loadImage(sketchPath("images/tree.png")), pixelLimit), "Tree Spirit", "Supernatural Forest"))
                .ifPresent(treeSpirit -> gameCharacterMap.put("Tree Spirit", treeSpirit));

        Optional.ofNullable(json.getJSONObject("key_locations").getJSONArray("forge"))
                .map(arr -> new PVector(arr.getFloat(0), arr.getFloat(1)))
                .map(pos -> new GameCharacter(pos, new MyImage(loadImage(sketchPath("images/forge.png")), pixelLimit), "Blacksmith", "Forge"))
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
        Set<String> inactive = new HashSet<>();
        for (GameCharacter gc: gameCharacterMap.values()) {
            PVector tarPos = gc.location;
            List<PVector> path = AStar(graph, vertex -> vertex.dist(tarPos), knight.location, tarPos);
            if (path == null) {
//                System.out.println(gc.name + "is unreachable");
                inactive.add(gc.name);
            }
        }
        this.initialState = new State(knight, inactive, false, ownerOfFenrir, false, false);
        this.actionPlan = DFS(initialState);
//        System.out.println("Action plan received = " + actionPlan);
        if (actionPlan == null) {
            this.weWin = false;
            GameCharacter king = gameCharacterMap.get("King");
            actionPlan = new ArrayList<>();
            PVector kingsLocation = king.location;
            if (knight.location.equals(kingsLocation)) {
                Action greetAction = new Action(false, "Greet");
                actionPlan.add(greetAction);
            } else {
                Action moveAction = new Move(false, "Move", knight, king);
                Action greetAction = new Action(false, "Greet");
                actionPlan.add(moveAction);
                actionPlan.add(greetAction);
            }
            GameCharacter demon = gameCharacterMap.get("Rameses");
            PVector demonLocation = demon.location;
            if (knight.location.equals(demonLocation)) {
                Action fightAction = new Action(false, "Fight");
                actionPlan.add(fightAction);
            } else {
                Action moveAction = new Move(false, "Move", king, demon);
                Action fightAction = new Action(false, "Fight");
                actionPlan.add(moveAction);
                actionPlan.add(fightAction);
            }
        } else {
            this.weWin = true;
            Collections.reverse(actionPlan);
        }
        this.nextAction = 0;
        this.noOfAction = (actionPlan == null) ? (0) : actionPlan.size();
    }

    private void readGameState(JSONObject json) {
        JSONObject worldState = json.getJSONObject("state_of_world");
        JSONArray hasArr = worldState.getJSONArray("Has");
        int hasSize = hasArr.size();
        for (int i = 0; i < hasSize; i++) {
            JSONArray elements = hasArr.getJSONArray(i);
            String characterName = elements.getString(0);
            String object = elements.getString(1).replaceAll("\\s", "")
                    .replaceAll("[0-9]", "");
            if (object.equals("Fenrir")) {
                ownerOfFenrir = characterName;
            }
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
//            System.out.println("object = " + object);
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
        public MyImage img;
        public String name;
        public String locationName;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GameCharacter character = (GameCharacter) o;
            return Objects.equals(location, character.location) &&
                    Objects.equals(has, character.has) &&
                    Objects.equals(wants, character.wants) &&
                    Objects.equals(name, character.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, has, wants, name);
        }

        public void print() {
            System.out.println("location = " + location);
            System.out.println("has = " + has);
            System.out.println("wants = " + wants);
            System.out.println("name = " + name);
        }

        public void display() {
            img.display(location);
        }

        public GameCharacter(PVector location,  MyImage img, String name, String locationName) {
            this.location = location;
            this.has = new HashMap<>();
            this.wants = new HashMap<>();
            this.img = img;
            this.name = name;
            this.locationName = locationName;
        }

        public GameCharacter(PVector location, Map<String, Integer> has, Map<String, Integer> wants, MyImage img,
                             String name, String locationName) {
            this.location = location;
            this.has = has;
            this.wants = wants;
            this.img = img;
            this.name = name;
            this.locationName = locationName;
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
        gameCharacterMap.values().stream().filter(gc -> !gc.name.equals("Knight")).forEach(GameCharacter::display);
        obstacleMap.values().forEach(this::shape);
        if (nextAction < noOfAction) {
            Action currentAction = actionPlan.get(nextAction);
            if (!currentAction.isSucceeded) {
                currentAction.perform(knight, graph);
                if (currentAction.isSucceeded) {
                    if (currentAction.type.equals("Fight")) {
                        if (weWin) {
                            System.out.println("Knight defeated Rameses");
                        } else {
                            System.out.println("Knight died in battle");
                        }
                    }
                    nextAction++;
                }
            } else {
                nextAction++;
            }
        }
//        if (mousePressed) {
//            target = new PVector(mouseX, mouseY);
//            knight.plan = AStar(graph, vertex -> vertex.dist(target), knight.currentPos, target);
//            System.out.println("knight.plan = " + knight.plan);
//            knight.currentIndex = -1;
//        }
//        knight.move();
//        knight.display();
    }

    private static class State {
        public Knight knight;
        public Set<String> inactive;
        public boolean isGreeted;
        public String fenrirOwner;
        public boolean hasFought;
        public boolean hasWon;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return isGreeted == state.isGreeted &&
                    hasFought == state.hasFought &&
                    hasWon == state.hasWon &&
                    Objects.equals(knight, state.knight) &&
                    Objects.equals(inactive, state.inactive) &&
                    Objects.equals(fenrirOwner, state.fenrirOwner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(knight, inactive, isGreeted, fenrirOwner, hasFought, hasWon);
        }

        public void print() {
            System.out.println("Knight");
            knight.print();
            System.out.println("inactive = " + inactive);
            System.out.println("isGreeted = " + isGreeted);
            System.out.println("fenrirOwner = " + fenrirOwner);
            System.out.println("hasFought = " + hasFought);
            System.out.println("hasWon = " + hasWon);
        }

        public State(Knight knight, Set<String> inactive, boolean isGreeted, String fenrirOwner, boolean hasFought,
                     boolean hasWon) {
            this.knight = knight;
            this.inactive = inactive;
            this.isGreeted = isGreeted;
            this.fenrirOwner = fenrirOwner;
            this.hasFought = hasFought;
            this.hasWon = hasWon;
        }

        public List<Pair<State, Action>> getNextStates(Map<String, GameCharacter> gameCharacterMap, int kingGold) {
//            System.out.println("====================");
//            System.out.println("Current Knight");
//            knight.print();
            if (hasFought) {
                return new ArrayList<>();
            }
            if (!isGreeted) {
                GameCharacter king = gameCharacterMap.get("King");
                PVector kingsLocation = king.location;
//                System.out.println("kingsLocation = " + kingsLocation);
                if (knight.location.equals(kingsLocation)) {
                    List<Pair<State, Action>> path = new ArrayList<>();
                    Action greetAction = new Action(false, "Greet");
                    path.add(Pair.of(new State(knight.addObject("gold", kingGold), inactive, true,
                            fenrirOwner, hasFought, hasWon), greetAction));
                    return path;
                } else {
                    List<Pair<State, Action>> path = new ArrayList<>();
                    Action moveAction = new Move(false, "Move", knight, king);
                    path.add(Pair.of(new State(knight.changePosition(kingsLocation), inactive, isGreeted, fenrirOwner,
                                    hasFought, hasWon), moveAction));
                    return path;
                }
            }

            List<Pair<State, Action>> plan = new ArrayList<>();
            GameCharacter demon = gameCharacterMap.get("Rameses");
            if (knight.location.equals(demon.location)) {
                Action fightAction = new Action(false, "Fight");
                //Is knight immutable?
                boolean hasWon = false;
                if ((fenrirOwner!= null && fenrirOwner.equals("Knight"))
                        || knight.has.containsKey("Fire")
                        || knight.has.containsKey("Poisoned Sword")) {
                    hasWon = true;
                }
                State newState = new State(knight, inactive, isGreeted, fenrirOwner, true, hasWon);
                if (hasWon) {
                    List<Pair<State, Action>> singlePath = new ArrayList<>();
                    singlePath.add(Pair.of(newState, fightAction));
                    return singlePath;
                } else {
                    plan.add(Pair.of(newState, fightAction));
                }
            }

            GameCharacter tree = gameCharacterMap.get("Tree Spirit");
            if (knight.has.containsKey("Axe") && knight.location.equals(tree.location)
                    && !inactive.contains("Tree Spirit")) {
                Action action = new Use(false, "Use", "Axe", "Tree Spirit", "Wood");
                Set<String> newInactive = new HashSet<>(inactive);
                newInactive.add("Tree Spirit");
                State newState = new State(knight.removeObject("Axe", 1).addObject("Wood", 1),
                        newInactive, isGreeted, fenrirOwner, hasFought, hasWon);
                plan.add(Pair.of(newState, action));
            }

            if (knight.has.containsKey("Blade") && knight.has.containsKey("Wood")) {
                List<String> removals = new ArrayList<>();
                removals.add("Blade");
                removals.add("Wood");
                Knight newKnight = knight.addObject("Sword", 1).removeObjects(removals);
                Action action = new Use(false, "Use", "Blade", "Wood", "Cheap Sword");
                State newState = new State(newKnight, inactive, isGreeted, fenrirOwner, hasFought, hasWon);
                plan.add(Pair.of(newState, action));
            }

            if (knight.has.containsKey("Sword") && knight.has.containsKey("Wolfsbane")) {
                List<String> removals = new ArrayList<>();
                removals.add("Sword");
                removals.add("Wolfsbane");
                Knight newKnight = knight.addObject("Poisoned Sword", 1).removeObjects(removals);
                Action action = new Use(false, "Use", "Sword", "Wolfsbane", "Poisoned Sword");
                State newState = new State(newKnight, inactive, isGreeted, fenrirOwner, hasFought, hasWon);
                plan.add(Pair.of(newState, action));
            }

            if (knight.has.containsKey("Ale") && knight.has.containsKey("Wood")) {
                List<String> removals = new ArrayList<>();
                removals.add("Ale");
                removals.add("Wood");
                Knight newKnight = knight.addObject("Fire", 1).removeObjects(removals);
                Action action = new Use(false, "Use", "Ale", "Wood", "Fire");
                State newState = new State(newKnight, inactive, isGreeted, fenrirOwner, hasFought, hasWon);
                plan.add(Pair.of(newState, action));
            }

            Collection<GameCharacter> gameCharacters = gameCharacterMap.values();
            for (GameCharacter gc: gameCharacters) {
                if (gc.name.equals("Knight") || inactive.contains(gc.name) || gc.name.equals("King")
                        || !gc.location.equals(knight.location)) {
                    continue;
                }
                List<String> wants = new ArrayList<>(gc.wants.keySet());
                List<String> gcHas = new ArrayList<>(gc.has.keySet());
                for (String want: wants) {
                    if (knight.has.containsKey(want)) {
                        for (String hass: gcHas) {
                            if (hass.equals("Fenrir") && !fenrirOwner.equals(gc.name)) {
                                continue;
                            }
                            Knight newKnight = knight.addObject(hass, 1).removeObject(want, 1);
                            Action action = new Exchange(false, "Exchange", want, hass, gc.name);
                            String newFenrirOwner;
                            if (hass.equals("Fenrir")) {
                                newFenrirOwner = "Knight";
                            } else {
                                newFenrirOwner = fenrirOwner;
                            }
                            State newState = new State(newKnight, inactive, isGreeted, newFenrirOwner, hasFought,
                                    hasWon);
                            plan.add(Pair.of(newState, action));
                        }
                    }
                }
            }

            for (GameCharacter gc: gameCharacters) {
                if (gc.name.equals("Knight") || inactive.contains(gc.name) || gc.name.equals("King")
                        || gc.location.equals(knight.location) ) {
                    continue;
                }
                Action moveAction = new Move(false, "Move", knight, gc);
                State newState = new State(knight.changePosition(gc.location), inactive, isGreeted, fenrirOwner,
                        hasFought, hasWon);
                plan.add(Pair.of(newState, moveAction));
            }

            return plan;
        }
    }

    public static class Action {
        public boolean isSucceeded;
        public String type;
//        public boolean inProgress;

        public void perform() {
            if (type.equals("Fight")) {
                System.out.println("Knight fights Rameses");
            } else if (type.equals("Greet")) {
                System.out.println("Knight greets King of Leighra");
            }
            isSucceeded = true;
        }

        public void perform(Knight knight, Graph graph) {
            perform();
        }

        public Action(boolean isSucceeded, String type) {
            this.isSucceeded = isSucceeded;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Action{" +
                    "isSucceeded=" + isSucceeded +
                    ", type='" + type + '\'' +
                    '}';
        }
    }

    public static class Exchange extends Action {
        public String gave;
        public String received;
        public String otherParty;

        public Exchange(boolean isSucceeded, String type, String gave, String received, String otherParty) {
            super(isSucceeded, type);
            this.gave = gave;
            this.received = received;
            this.otherParty = otherParty;
        }

        @Override
        public void perform() {
            System.out.println(otherParty + " wants " + gave);
            System.out.println("Knight wants " + received);
            System.out.println(String.format("Knight exchanges %s for %s", gave, received));
            isSucceeded = true;
        }
    }

    public static class Move extends Action {
        public GameCharacter from;
        public GameCharacter to;

        public Move(boolean isSucceeded, String type, GameCharacter from, GameCharacter to) {
            super(isSucceeded, type);
            this.from = from;
            this.to = to;
        }

        @Override
        public void perform(Knight knight, Graph graph) {
            if (knight.plan.isEmpty()) {
                knight.plan = AStar(graph, vertex -> vertex.dist(to.location), knight.location, to.location);
                knight.currentIndex = -1;
                System.out.println("Knight moves to " + to.locationName);
            }
            PVector newLoc = knight.move();
            knight.display();
            if (newLoc == null || newLoc.equals(to.location)) {
                isSucceeded = true;
                knight.plan = new ArrayList<>();
                knight.currentIndex = -1;
            }
        }
    }

    public static class Use extends Action {
        public String first;
        public String second;
        public String third;

        public Use(boolean isSucceeded, String type, String first, String second, String third) {
            super(isSucceeded, type);
            this.first = first;
            this.second = second;
            this.third = third;
        }

        @Override
        public void perform() {
            System.out.println(String.format("Knight uses %s and %s to get %s", first, second, third));
            isSucceeded = true;
        }
    }

    private static class Knight extends GameCharacter {
        public List<PVector> plan;
        public int currentIndex;

        public Knight addObject(String object, int n) {
            Map<String, Integer> newHas = new HashMap<>();
            for (Map.Entry<String, Integer> entry: has.entrySet()) {
                newHas.put(entry.getKey(), entry.getValue());
            }
            int val = newHas.getOrDefault(object, 0);
            newHas.put(object, val+n);
            //It's okay to reuse wants if it is not modified
            Knight newKnight = new Knight(location, newHas, wants, img, name, locationName);
            return newKnight;
        }

        public Knight removeObjects(List<String> objects) {
            Map<String, Integer> newHas = new HashMap<>();
            for (Map.Entry<String, Integer> entry: has.entrySet()) {
                newHas.put(entry.getKey(), entry.getValue());
            }
            for (String obj: objects) {
                int val = newHas.getOrDefault(obj, 0);
                if (val == 1) {
                    newHas.remove(obj);
                } else {
                    newHas.put(obj, val-1);
                }
            }
            //It's okay to reuse wants if it is not modified
            Knight newKnight = new Knight(location, newHas, wants, img, name, locationName);
            return newKnight;
        }

        public Knight removeObject(String object, int n) {
            Map<String, Integer> newHas = new HashMap<>();
            for (Map.Entry<String, Integer> entry: has.entrySet()) {
                newHas.put(entry.getKey(), entry.getValue());
            }
            int val = newHas.getOrDefault(object, 0);
            if (val > n) {
                newHas.put(object, val - n);
            } else {
                newHas.remove(object);
            }
            //It's okay to reuse wants if it is not modified
            Knight newKnight = new Knight(location, newHas, wants, img, name, locationName);
            return newKnight;
        }

        public Knight changePosition(PVector newLocation) {
            return new Knight(newLocation, has, wants, img, name, locationName);
        }

        public Knight(PVector location, Map<String, Integer> has, Map<String, Integer> wants, MyImage img,
                      String name, String locationName) {
            super(location, has, wants, img, name, locationName);
            this.plan = new ArrayList<>();
            this.currentIndex = -1;
        }

        //TODO: Possible improvement here
        public Knight(PVector location, MyImage img, String name, String locationName) {
            super(location, img, name, locationName);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyImage myImage = (MyImage) o;
            return Float.compare(myImage.width, width) == 0 &&
                    Float.compare(myImage.height, height) == 0 &&
                    Objects.equals(img, myImage.img);
        }

        @Override
        public int hashCode() {
            return Objects.hash(img, width, height);
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

    public List<Action> DFSUtil(State state, Set<State> visited) {
//        System.out.println("Visited");
//        for (State st: visited) {
//            st.print();
//            System.out.println("st.hashCode() = " + st.hashCode());
//        }
//        system.out.println("================");
//        system.out.println("state");
        visited.add(state);
//        initialState.print();
        if (state.hasFought) {
            if (state.hasWon) {
                return new ArrayList<>();
            } else {
                return null;
            }
        }
        List<Pair<State, Action>> children = state.getNextStates(gameCharacterMap, kingGold);
//        System.out.println("Children");
//        System.out.println("children.size() = " + children.size());
//        for (Pair<State, Action> child: children) {
//            System.out.println("child.left.knight.location = " + child.left.knight.location);
//            System.out.println("child.action = " + child.right);
//        }
        for (Pair<State, Action> next: children) {
            if (next.left.hasWon && next.left.hasFought) {
                List<Action> plan = new ArrayList<>();
                plan.add(next.right);
                return plan;
            }
        }
        for (Pair<State, Action> child: children) {
            if (!visited.contains(child.left)) {
//                System.out.println("Chosen action = " + child.right);
                List<Action> nextPlan = DFSUtil(child.left, visited);
//                System.out.println("nextPlan = " + nextPlan);
                if (nextPlan != null) {
//                    System.out.println("next plan not null");
                    nextPlan.add(child.right);
//                    System.out.println("nextPlan after adding child = " + nextPlan);
                    return nextPlan;
                }
            }
        }
        return null;
    }

    public List<Action> DFS(State initialState) {
        Set<State> visited = new HashSet<>();
        return DFSUtil(initialState, visited);
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
            return null;
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


    public static class Pair<L, R> {
        private L left;
        private R right;

        private Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }

        public static <A, B> Pair<A, B> of(A a, B b) {
            return new Pair<>(a, b);
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "left=" + left +
                    ", right=" + right +
                    '}';
        }
    }
}
