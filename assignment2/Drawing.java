package assignment2;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Drawing extends PApplet {
    private MyImage knight;
    private MyImage castle;
    private MyImage tarPit;
    private MyImage tavern;
    private MyImage tree;
    private MyImage cave;
    private MyImage forge;
    private Map<String, PShape> obstacleMap;
    private int[] OBSTACLE_COLORS;

    public static void main(String[] args) {
        PApplet.main("assignment2.Drawing", args);
    }

    public void settings() {
        size(640, 480);
    }

    @Override
    public void setup() {
        int pixelLimit = 50;
        OBSTACLE_COLORS = new int[]{color(84,134,214), color(42, 176, 109), color(189, 109, 87)};
        JSONObject json = loadJSONObject(sketchPath("data/map.json"));
        knight = new MyImage(loadImage(sketchPath("images/knight.png")),
                new Vertex(json.getJSONArray("knight_start").getFloat(0),
                        json.getJSONArray("knight_start").getFloat(1)), pixelLimit);
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
        Optional.ofNullable(knight).ifPresent(MyImage::display);
        Optional.ofNullable(castle).ifPresent(MyImage::display);
        Optional.ofNullable(tarPit).ifPresent(MyImage::display);
        Optional.ofNullable(tavern).ifPresent(MyImage::display);
        Optional.ofNullable(cave).ifPresent(MyImage::display);
        obstacleMap.values().forEach(this::shape);
        Optional.ofNullable(tree).ifPresent(MyImage::display);
        Optional.ofNullable(forge).ifPresent(MyImage::display);
    }

    private static class Vertex {
        public float xCoord;
        public float yCoord;

        public Vertex(float xCoord, float yCoord) {
            this.xCoord = xCoord;
            this.yCoord = yCoord;
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
}
