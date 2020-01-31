import processing.core.PApplet;

public class SayHelloToProcessing extends PApplet {
    public static void main(String[] args) {
        PApplet.main("SayHelloToProcessing", args);
    }

    public void draw() {
        background(255);
        stroke(0);
        fill(0);
        ellipse(10, 90, 5, 5);
        triangle(10, 87.5f, 10, 92.5f, 15, 90);
    }
}
