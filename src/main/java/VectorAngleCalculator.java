import java.util.Vector;

public class VectorAngleCalculator {
    public static double calculateAngle(Vector<Double> vector1, Vector<Double> vector2) {
        // Check if the vectors have the same dimension
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        // Calculate the dot product of the vectors
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            magnitude1 += Math.pow(vector1.get(i), 2);
            magnitude2 += Math.pow(vector2.get(i), 2);
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        // Calculate the angle in radians
        double radians = Math.acos(dotProduct / (magnitude1 * magnitude2));

        // Convert radians to degrees
        double degrees = Math.toDegrees(radians);

        return degrees;
    }

    public static void main(String[] args) {
        Vector<Double> vector1 = new Vector<>();
        vector1.add(1.0);
        vector1.add(0.0);

        Vector<Double> vector2 = new Vector<>();
        vector2.add(1.0);
        vector2.add(1.0);

        double angleDegrees = calculateAngle(vector1, vector2);
        System.out.println("Angle between vectors: " + angleDegrees + " degrees");
    }
}
