import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

public class BasicMotion extends PApplet {
    Boid boid;
    public static void main(String[] args) {
        PApplet.main("BasicMotion", args);
    }

    public void settings() {
        size(500, 400);
    }

    public void setup() {
        boid = new Boid();
    }

    public void draw() {
        background(255);
        boid.run();
//        stroke(0);
//        fill(0);
//        ellipse(mouseX, mouseY, 5, 5);
//        triangle(mouseX, mouseY-2.5f, mouseX, mouseY+2.5f, mouseX+5, mouseY);
    }

    public class Boid {
        PVector initialPosition;
        PVector position;
        PVector velocity;
        float orientation;
        boolean stopMotion;
        List<Pair<PVector, Float>> breadcrumbs;
        int counter;

        public Boid() {
            initialPosition = new PVector(10, 390);
            position = new PVector(10, 390);
            velocity = new PVector(2, 0);
            orientation = 0.0f;
            stopMotion = false;
            breadcrumbs = new ArrayList<>();
            counter = 0;
        }

        public void render() {
            renderShape(position, orientation);
            for (Pair<PVector, Float> crumb : breadcrumbs) {
                renderBreadCrumb(crumb.getLeft());
                //renderShape(crumb.getLeft(), crumb.getRight());
            }
            if (counter % 3 == 0) {
                breadcrumbs.add(Pair.of(new PVector(position.x, position.y), orientation));
            }
        }

        public void renderBreadCrumb(PVector pos) {
            pushMatrix();
            translate(pos.x, pos.y);
            fill(0);
            stroke(0);
            ellipse(0, 0, 2, 2);
            popMatrix();
        }

        public void renderShape(PVector pos, float orientation) {
            pushMatrix();
            translate(pos.x, pos.y);
            rotate(orientation);
            PShape shape = createShape(GROUP);
            float radius = 7;
            PShape circle = createShape(ELLIPSE, 0, 0, radius, radius);
            circle.setFill(0);
            circle.setStroke(0);
            PShape triangle = createShape(TRIANGLE, 0, -radius/2.0f, 0, radius/2.0f, radius, 0);
            triangle.setFill(0);
            triangle.setStroke(0);
            shape.addChild(circle);
            shape.addChild(triangle);
            shape(shape);
            popMatrix();
        }

        public void update() {
            position.add(velocity);
            orientation = velocity.heading();
        }

        public PVector getNextPosition() {
            return PVector.add(position, velocity);
        }

        public void run() {
            counter++;
            if (!stopMotion) {
                PVector nextPos = getNextPosition();
                checkBoundaries(nextPos);
                update();
            }
            render();
            if (checkStopping()) {
                stopMotion = true;
            }
        }

        public void checkBoundaries(PVector nextPosition) {
            if (nextPosition.x < 10 || nextPosition.x > 490 || nextPosition.y < 10 || nextPosition.y > 390) {
                velocity.rotate(-HALF_PI);
            }
        }

        public boolean checkStopping() {
            return position.equals(initialPosition);
        }
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
