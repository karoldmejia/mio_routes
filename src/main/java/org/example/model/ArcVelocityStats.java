package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class ArcVelocityStats {
    private List<Double> samples = new ArrayList<>();
    private double sum = 0;
    private int count = 0;

    public void addSample(double velocity) {
        samples.add(velocity);
        sum += velocity;
        count++;
    }

    public double getAverageVelocity() {
        return count > 0 ? sum / count : 0;
    }

    public int getSampleCount() {
        return count;
    }

    public double getMinVelocity() {
        return samples.stream().min(Double::compare).orElse(0.0);
    }

    public double getMaxVelocity() {
        return samples.stream().max(Double::compare).orElse(0.0);
    }
}