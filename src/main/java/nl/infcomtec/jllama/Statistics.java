/*
 * Copyright (c) 2017 by Walter Stroebel and InfComTec.
 * All rights reserved.
 */
package nl.infcomtec.jllama;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Mostly just your standard statistics stuff, mean, standard deviation.
 *
 * @author walter
 */
public class Statistics {

    private boolean calcNeeded = true;
    /**
     * Number of values as a double.
     */
    private double count;
    /**
     * Data. Stored as an ArrayList.
     */
    private ArrayList<Double> data = new ArrayList<>();
    /**
     * Largest value.
     */
    private double max;
    /**
     * Average.
     */
    private double mean;
    /**
     * Smallest value.
     */
    private double min;
    /**
     * max - min
     */
    private double range;
    /**
     * Standard deviation.
     */
    private double stdDev;
    /**
     * Sum of all values.
     */
    private double sum;
    /**
     * Sum of all values squared.
     */
    private double sum2;

    /**
     * Constructor for an outside array. Values are copied.
     *
     * @param vals The values to calculate the statistics for.
     */
    public Statistics(final double... vals) {
        if (null != vals) {
            for (double d : vals) {
                data.add(d);
            }
        }
    }

    /**
     * Collections based constructor. This makes a copy of the values.
     *
     * @param vals The values to calculate the statistics for.
     */
    public Statistics(Collection<Double> vals) {
        if (null != vals) {
            for (double d : vals) {
                data.add(d);
            }
        }
    }

    /**
     * Empty constructor.
     */
    public Statistics() {
    }

    /**
     * Return the values as a List.
     *
     * @return The values as a List.
     */
    public List<Double> asList() {
        return new LinkedList<>(data);
    }

    /**
     * Break down the values in N buckets, calculating the statistics of each
     * subset. Note that this may result in "empty" buckets or buckets of one
     * value.
     *
     * @param bucketCount
     * @return
     */
    public Statistics[] buckets(final int bucketCount) {
        Statistics[] ret = new Statistics[bucketCount];
        double step = range / bucketCount;
        double start = min;
        for (int i = 0; i < bucketCount; i++) {
            ArrayList<Double> match = new ArrayList<>();
            for (double d : data) {
                if (d >= start && d < start + step) {
                    match.add(d);
                }
            }
            start += step;
            ret[i] = new Statistics(match);
        }
        return ret;
    }

    /**
     * Number of values.
     *
     * @return the count
     */
    public int getCount() {
        recalculate();
        return (int) Math.round(count);
    }

    /**
     * Largest value.
     *
     * @return the max
     */
    public double getMax() {
        recalculate();
        return max;
    }

    /**
     * Average.
     *
     * @return the mean
     */
    public double getMean() {
        recalculate();
        return mean;
    }

    /**
     * Smallest value.
     *
     * @return the min
     */
    public double getMin() {
        recalculate();
        return min;
    }

    /**
     * max - min
     *
     * @return the range
     */
    public double getRange() {
        recalculate();
        return range;
    }

    /**
     * Standard deviation.
     *
     * @return the stdDev
     */
    public double getStdDev() {
        recalculate();
        return stdDev;
    }

    /**
     * Sum of all values.
     *
     * @return the sum
     */
    public double getSum() {
        recalculate();
        return sum;
    }

    /**
     * Map a value to this range of values.
     *
     * @param targetMin Target range start.
     * @param targetMax Target range end.
     * @param value Value to map.
     * @return Mapped value.
     */
    public double map(final double targetMin, final double targetMax, final double value) {
        recalculate();
        double xf = (max - min) / (targetMax - targetMin);
        double ret = ((value - min) / xf) + targetMin;
        ret = Math.max(targetMin, ret);
        return Math.min(ret, targetMax);
    }

    /**
     * Map a value to this range of values (integers).
     *
     * @param targetMin Target range start.
     * @param targetMax Target range end.
     * @param value Value to map.
     * @return Mapped value.
     */
    public int iMap(final double targetMin, final double targetMax, final double value) {
        recalculate();
        double xf = (max - min) / (targetMax - targetMin);
        double ret = ((value - min) / xf) + targetMin;
        ret = Math.max(targetMin, ret);
        return (int) Math.round(Math.min(ret, targetMax));
    }

    /**
     * Calculate the median of the values.
     *
     * @return The median value.
     */
    public double median() {
        if (data.size() < 1) {
            return Double.NaN;
        }
        List<Double> l = asList();
        Collections.sort(l);
        return l.get(l.size() / 2);
    }

    /**
     * (Re)calculation done only when results are requested.
     */
    private synchronized void recalculate() {
        if (calcNeeded) {
            sum = 0;
            sum2 = 0;
            min = Long.MAX_VALUE;
            max = Long.MIN_VALUE;
            count = data.size();
            for (double d : data) {
                sum += d;
                sum2 += (d * d);
                min = Math.min(min, d);
                max = Math.max(max, d);
            }
            mean = sum / count;
            if (count > 1) {
                stdDev = Math.sqrt((sum2 - ((sum * sum) / count)) / (count - 1));
            } else {
                stdDev = 0;
            }
            range = max - min;
            calcNeeded = false;
        }
    }

    /**
     * Rolling update.
     *
     * @param newVal New value to add.
     */
    public void rolling(double newVal) {
        rolling(newVal, Integer.MAX_VALUE);
    }

    /**
     * Rolling update with a limit for the number of values.
     *
     * @param newVal New value to add.
     * @param limit Maximum number of values to keep (FIFO).
     */
    public void rolling(double newVal, int limit) {
        data.add(newVal);
        if (data.size() > limit) {
            for (Iterator<Double> it = data.iterator(); it.hasNext();) {
                it.next();
                if (data.size() > limit) {
                    it.remove();
                }
            }
        }
        calcNeeded = true;
    }

    @Override
    public String toString() {
        return "mean=" + getMean() + ", stdDev=" + getStdDev() + ", sum=" + getSum() + ", count=" + getCount() + ", min=" + getMin() + ", max=" + getMax() + ", range=" + getRange();
    }
}
