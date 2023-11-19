package nl.infcomtec.simpleimage;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Tiny class to hold pixel membership. Incomparably faster then Area.
 *
 * @author Walter Stroebel
 */
public class BitShape implements Iterable<Point> {

    private final BitSet bs = new BitSet();
    private final int W;

    public BitShape(int W) {
        this.W = W;
    }

    /**
     * @param p The point, java.awt.Point is also a Point2D.
     * @return true if p is inside the shape.
     */
    public final boolean contains(Point2D p) {
        int x = (int) (Math.round(p.getX()));
        int y = (int) (Math.round(p.getY()));
        return contains(x, y);
    }

    /**
     * @param x
     * @param y
     * @return true if (x,y) is inside the shape.
     */
    public final boolean contains(int x, int y) {
        return bs.get(y * W + x);
    }

    /**
     * Just for semantics == contains().
     *
     * @param p The point, java.awt.Point is also a Point2D.
     * @return true if p is inside the shape.
     */
    public final boolean get(Point2D p) {
        return contains(p);
    }

    /**
     * Add a point.
     *
     * @param x
     * @param y
     */
    public final void set(int x, int y) {
        bs.set(y * W + x);
    }

    /**
     * Remove a point.
     *
     * @param x
     * @param y
     */
    public final void reset(int x, int y) {
        bs.clear(y * W + x);
    }

    /**
     * Add a point.
     *
     * @param p The point, java.awt.Point is also a Point2D.
     */
    public final void set(Point2D p) {
        int x = (int) (Math.round(p.getX()));
        int y = (int) (Math.round(p.getY()));
        bs.set(y * W + x);
    }

    /**
     * Remove a point.
     *
     * @param p The point, java.awt.Point is also a Point2D.
     */
    public final void reset(Point2D p) {
        int x = (int) (Math.round(p.getX()));
        int y = (int) (Math.round(p.getY()));
        bs.clear(y * W + x);
    }

    /**
     * Just for semantics == reset().
     *
     * @param p The point, java.awt.Point is also a Point2D.
     */
    public final void clear(Point2D p) {
        reset(p);
    }

    @Override
    public Iterator<Point> iterator() {
        return new Iterator<Point>() {
            int index = bs.nextSetBit(0);
            int lastIndex = -1;  // Added to track the last index accessed by 'next()'

            @Override
            public boolean hasNext() {
                return index >= 0 && index < bs.size();
            }

            @Override
            public Point next() {
                if (index < 0 || index >= bs.size()) {
                    throw new NoSuchElementException();  // Throwing an exception if 'next()' is called without a 'hasNext()'
                }
                Point ret = new Point(index % W, index / W);
                lastIndex = index;  // Update the lastIndex
                index = bs.nextSetBit(index + 1);
                return ret;
            }

            @Override
            public void remove() {
                if (lastIndex == -1) {
                    throw new IllegalStateException("The next method has not yet been called, or the remove method has already been called after the last call to the next method");
                }
                bs.clear(lastIndex);
                lastIndex = -1;  // Reset lastIndex to indicate that the last element has been removed
            }
        };
    }
}
