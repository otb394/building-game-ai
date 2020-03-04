package assignment1;

import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

public class FlockingBehavior extends PApplet {
    Flock flock;
    public static void main(String[] args) {
        PApplet.main("assignment1.FlockingBehavior", args);
    }

    public void settings() {
        size(500, 400);
    }

    public void setup() {
        flock = new Flock();
    }

    public void draw() {
        background(255);
        flock.run();
    }

    public void mousePressed() {
        flock.addBoid(new Boid(mouseX, mouseY));
    }

    public class Flock {
        List<Boid> boids;

        Flock() {
            boids = new ArrayList<>();
        }

        void run() {
            for (Boid boid : boids) {
                boid.run(boids);
            }
        }

        void addBoid(Boid boid) {
            boids.add(boid);
        }
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
        float separationRadius;
        float neighborRadius;

        public Boid(float x, float y) {
            objectRadius = 5;
            padding = 10.0f;
            position = new PVector(x,y);
            velocity = PVector.random2D();
            orientation = velocity.heading();
            stopMotion = false;
            breadcrumbs = new ArrayList<>();
            counter = 0;
            MAX_VELOCITY = 2;
            MAX_ACCELERATION = 0.3f;
            DRAG = 0.05f;
            target = new PVector(width / 2.0f, height / 2.0f);
            rateOfLeavingCrumbs = 1;
            slowDownRadius = 10;
            timeToTarget = 0.25f;
            separationRadius = 20;
            neighborRadius = 50;
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
            return;
//            pushMatrix();
//            translate(pos.x, pos.y);
//            fill(0);
//            stroke(0);
//            ellipse(0, 0, 2, 2);
//            popMatrix();
        }

        private void renderShape(PVector pos, float orientation) {
            pushMatrix();
            translate(pos.x, pos.y);
            rotate(orientation);
            PShape shape = createShape(GROUP);
            float radius = 5;
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

        public void run(List<Boid> boids) {
            PVector totalForce = getCohesiveForce(boids).add(getAlignmentForce(boids)).add(getSeparationForce(boids));
            applyForce(totalForce);
            update();
            checkBoundaries();
            render();
            counter++;
        }

        private PVector getCohesiveForce(List<Boid> boids) {
            PVector center = new PVector(0,0);
            int count = 0;
            for (Boid boid : boids) {
                PVector dis = PVector.sub(boid.position, position);
                float disMag = dis.mag();
                if (disMag > 0 && disMag <= neighborRadius) {
                    center.add(boid.position);
                    count++;
                }
            }

            if (count > 0) {
                center.div(count);
                return seek(center);
            } else {
                return new PVector(0,0);
            }
        }

        private PVector seek(PVector target) {
            PVector targetVelocity = PVector.sub(target, position).setMag(MAX_VELOCITY);
            PVector delta = PVector.sub(targetVelocity, velocity);
            return delta.limit(MAX_ACCELERATION);
        }

        private PVector getAlignmentForce(List<Boid> boids) {
            PVector avgVel = new PVector(0,0);
            int count = 0;
            for (Boid boid : boids) {
                PVector dis = PVector.sub(boid.position, position);
                float disMag = dis.mag();
                if (disMag > 0 && disMag <= neighborRadius) {
                    avgVel.add(boid.velocity);
                    count++;
                }
            }

            if (count > 0) {
                avgVel.div(count);
                avgVel.setMag(MAX_VELOCITY);
                PVector delta = PVector.sub(avgVel, velocity);
                return delta.limit(MAX_ACCELERATION);
            } else {
                return new PVector(0,0);
            }
        }

        private PVector getSeparationForce(List<Boid> boids) {
            PVector force = new PVector(0,0);
            int count = 0;
            for (Boid bd : boids) {
                PVector dis = PVector.sub(bd.position, this.position);
                float disMag = dis.mag();
                if (disMag > 0 && disMag < separationRadius) {
                    PVector away = dis.copy().mult(-1);
                    away.normalize().div(disMag);
                    force.add(away);
                    count++;
                }
            }
            if (count > 0) {
                force.div(count);
            }
            if (force.mag() > 0) {
                PVector targetVel = force.setMag(MAX_VELOCITY);
                force = PVector.sub(targetVel, velocity).limit(MAX_ACCELERATION);
            }
            return force;
        }

        public void arrive(PVector target) {
            PVector distance = PVector.sub(target, position);
            if (distance.mag() > objectRadius) {
                float targetSpeed = (distance.mag() > slowDownRadius) ? (MAX_VELOCITY)
                        : (MAX_VELOCITY * (distance.mag() / slowDownRadius));
                PVector targetVelocity = distance.copy().setMag(targetSpeed);
                PVector deltaVelocity = PVector.sub(targetVelocity, velocity);

                PVector acc = deltaVelocity.div(timeToTarget).limit(MAX_ACCELERATION);
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
