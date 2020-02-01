import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

public class ArriveSteeringV2 extends PApplet {
    Boid boid;
    public static void main(String[] args) {
        PApplet.main("ArriveSteeringV2", args);
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
    }

    public class Boid {
        float objectRadius;
        float padding;
        PVector target;
        float MAX_VELOCITY;
        float MAX_ACCELERATION;
        float DRAG;
        PVector position;
        PVector velocity;
        float orientation;
        boolean stopMotion;
        List<Pair<PVector, Float>> breadcrumbs;
        int counter;
        int rateOfLeavingCrumbs;
        float timeToTarget;
        float slowDownRadius;

        public Boid() {
            objectRadius = 5;
            padding = 10.0f;
            position = new PVector(width / 2.0f, height - padding);
            velocity = new PVector(2, 0);
            orientation = 0.0f;
            stopMotion = false;
            breadcrumbs = new ArrayList<>();
            counter = 0;
            MAX_VELOCITY = 8;
            MAX_ACCELERATION = 0.1f;
            DRAG = 0.05f;
            target = new PVector(width / 2.0f, height / 2.0f);
            rateOfLeavingCrumbs = 1;
            slowDownRadius = 10;
            timeToTarget = 0.25f;
        }

        private void render() {
            renderShape(position, orientation);
            for (Pair<PVector, Float> crumb : breadcrumbs) {
                renderBreadCrumb(crumb.getLeft());
            }
            if (counter % rateOfLeavingCrumbs == 0) {
                breadcrumbs.add(Pair.of(new PVector(position.x, position.y), orientation));
            }
        }

        private void renderBreadCrumb(PVector pos) {
            pushMatrix();
            translate(pos.x, pos.y);
            fill(0);
            stroke(0);
            ellipse(0, 0, 2, 2);
            popMatrix();
        }

        private void renderShape(PVector pos, float orientation) {
            pushMatrix();
            translate(pos.x, pos.y);
            rotate(orientation);
            PShape shape = createShape(GROUP);
            float radius = 7;
            PShape circle = createShape(ELLIPSE, 0, 0, radius, radius);
            circle.setFill(0);
            circle.setStroke(0);
            PShape triangle = createShape(TRIANGLE, 0, -radius / 2.0f, 0, radius / 2.0f, radius, 0);
            triangle.setFill(0);
            triangle.setStroke(0);
            shape.addChild(circle);
            shape.addChild(triangle);
            shape(shape);
            popMatrix();
        }

        private void update() {
            position.add(velocity);
            orientation = velocity.heading();
        }

        private PVector getVelocity(PVector acc) {
            PVector newVelocity = PVector.add(velocity, acc).limit(MAX_VELOCITY);
            float mag = newVelocity.mag();
            newVelocity.setMag(Math.max(mag - DRAG, 0));
            return newVelocity;
        }

        private void applyForce(PVector acc) {
            velocity = getVelocity(acc);
        }

        public void run() {
            counter++;
            if (mousePressed) {
                target = new PVector(mouseX, mouseY);
            }
            arrive(target);
        }

        public void arrive(PVector target) {
            PVector distance = PVector.sub(target, position);
            if (distance.mag() > objectRadius) {
                float targetSpeed = (distance.mag() > slowDownRadius) ? (MAX_VELOCITY)
                        : Math.min(distance.mag() / timeToTarget, MAX_VELOCITY);
                PVector targetVelocity = distance.copy().setMag(targetSpeed);
                PVector deltaVelocity = PVector.sub(targetVelocity, velocity);

                PVector acc = deltaVelocity.limit(MAX_ACCELERATION);
                applyForce(acc);
                update();
                checkBoundaries();
            }
            render();
        }

        private boolean checkBoundaries() {
            boolean flag = false;
            if (position.x < 0) {
                position.x += width;
                flag = true;
            }
            if (position.y < 0) {
                position.y += height;
                flag = true;
            }
            if (position.x > width) {
                position.x -= width;
                flag = true;
            }
            if (position.y > height) {
                position.y -= height;
                flag = true;
            }
            return flag;
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
