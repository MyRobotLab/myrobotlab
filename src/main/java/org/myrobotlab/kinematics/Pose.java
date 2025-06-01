package org.myrobotlab.kinematics;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;

public class Pose {

    protected Map<String, PoseMove> moves = new TreeMap<>();

    public Map<String, PoseMove> getMoves() {
        return moves;
    }

    public void setMoves(Map<String, PoseMove> moves) {
        this.moves = moves;
    }

    @Override
    public String toString() {
        return "Pose{" +
                "moves=" + moves +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pose pose = (Pose) o;
        return Objects.equals(moves, pose.moves);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moves);
    }
}
