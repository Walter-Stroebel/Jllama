package nl.infcomtec.simpleimage;

import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Label in 2D space.
 *
 * @author Walter Stroebel
 */
public class LabelledPoint extends Point implements Comparable<LabelledPoint> {

    public static Shape circle(double cx, double cy, double radius) {
        return circle(new Point2D.Double(cx, cy), radius);
    }

    public static Shape circle(int cx, int cy, int radius) {
        return circle(new Point2D.Double(cx, cy), radius);
    }

    public static Shape circle(Point p, int radius) {
        return circle(new Point2D.Double(p.x, p.y), radius);
    }

    /**
     * Just a circle.
     *
     * @param center
     * @param radius
     * @return
     */
    public static Shape circle(Point2D center, double radius) {
        double x = center.getX() - radius;
        double y = center.getY() - radius;
        return new Ellipse2D.Double(x, y, 2 * radius, 2 * radius);
    }

    /**
     * Basic atan2 calculation.
     *
     * @param p1 Point 1
     * @param p2 Point 2
     * @return Angle
     */
    public static double getAngle(Point2D p1, Point2D p2) {
        if (p1.getX() == p2.getX()) {
            return Math.PI / 2;
        }
        return Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX());
    }

    /**
     * The label
     */
    public final String label;
    private Shape shape = null;

    /**
     * Fetch from input stream (UTF, Y, X).
     *
     * @param in Input stream.
     * @throws java.io.IOException Whenever.
     */
    public LabelledPoint(DataInput in) throws IOException {
        this.label = in.readUTF();
        this.y = in.readShort();
        this.x = in.readShort();
    }

    /**
     * Point is (0,0).
     *
     * @param label The label.
     */
    public LabelledPoint(String label) {
        this.label = label;
    }

    /**
     * Create.
     *
     * @param p Point.
     * @param label Label.
     */
    public LabelledPoint(Point p, String label) {
        super(p);
        this.label = label;
    }

    /**
     * Create.
     *
     * @param p Point.
     * @param label Label.
     */
    public LabelledPoint(Point2D p, String label) {
        super((int) Math.round(p.getX()), (int) Math.round(p.getY()));
        this.label = label;
    }

    /**
     * Clone.
     *
     * @param p Source.
     */
    public LabelledPoint(LabelledPoint p) {
        super(p);
        this.label = p.label;
    }

    /**
     * Create.
     *
     * @param x Point x.
     * @param y Point y.
     * @param label Label.
     */
    public LabelledPoint(int x, int y, String label) {
        super(x, y);
        this.label = label;
    }

    /**
     * Return a small circle around the point.
     *
     * @return circle with radius 15 around the point.
     */
    public synchronized final Shape getShape() {
        if (null == shape) {
            shape = circle(this, 15);
        }
        return shape;
    }

    /**
     * Write to output stream (UTF, short Y, short X).
     *
     * @param out Data output stream.
     * @throws IOException Whenever.
     */
    public void write(DataOutput out) throws IOException {
        out.writeUTF(label);
        out.writeShort(y);
        out.writeShort(x);
    }

    /**
     * Allows to select a mark by clicking inside its default circle shape.
     *
     * @param p Point to test (usually from the mouse).
     * @return true if p is close to this marker.
     */
    public boolean selects(Point2D p) {
        return getShape().contains(p);
    }

    /**
     * Allows to select a mark by clicking inside its default circle shape.
     *
     * @param x
     * @param y
     * @return true if p is close to this marker.
     */
    public boolean selects(int x, int y) {
        return getShape().contains(x, y);
    }

    /**
     * Allows to select a mark by clicking inside its default circle shape.
     *
     * @param ev Mouse event.
     * @return true if p is close to this marker.
     */
    public boolean selects(MouseEvent ev) {
        return getShape().contains(ev.getX(), ev.getY());
    }

    /**
     * Simple calculation of the angle of the line between two points and the
     * X-axis.
     *
     * @param other Some other point.
     * @return The angle.
     */
    public double angle(Point other) {
        return getAngle(this, other);
    }

    /**
     * Length of the vector to this point.
     *
     * @return The length.
     */
    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    @Override
    public String toString() {
        return String.format("%s : (%d, %d)", label, x, y);
    }

    @Override
    public int compareTo(LabelledPoint t) {
        return label.compareTo(t.label);
    }

}
