package org.example.model;

import java.util.Objects;

public class Arc {
    private Stop from;
    private Stop to;
    private int routeId;
    private int orientation;
    private int variant;


    public Arc(Stop from, Stop to, int routeId, Integer orientation, int variant) {
        this.from=from;
        this.to=to;
        this.routeId=routeId;
        this.orientation = orientation;
        this.variant = variant;
    }

    public Stop getFrom() {
        return from;
    }

    public Stop getTo() {
        return to;
    }

    public int getRouteId() {
        return routeId;
    }

    public int getVariant() {
        return variant;
    }

    public int getOrientation() {
        return orientation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Arc)) return false;
        Arc arc = (Arc) o;
        return routeId == arc.routeId &&
                orientation == arc.orientation &&
                from.getId() == arc.from.getId() &&
                to.getId() == arc.to.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(from.getId(), to.getId(), routeId);
    }

}

