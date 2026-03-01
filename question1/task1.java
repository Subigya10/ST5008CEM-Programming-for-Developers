import java.util.*;

public class task1 {

    private static int gcd(int a, int b) {
        if (b == 0) return a;
        return gcd(b, a % b);
    }

    public static int maxPoints(int[][] points) {
        if (points == null || points.length == 0) return 0;
        int n = points.length;
        int maxPoints = 1;

        for (int i = 0; i < n; i++) {
            Map<String, Integer> slopes = new HashMap<>();
            int duplicates = 0;
            int currMax = 0;

            int x1 = points[i][0];
            int y1 = points[i][1];

            for (int j = i + 1; j < n; j++) {
                int x2 = points[j][0];
                int y2 = points[j][1];

                int dx = x2 - x1;
                int dy = y2 - y1;

                if (dx == 0 && dy == 0) {
                    duplicates++;
                    continue;
                }

               
                int g = gcd(dy, dx);
                if (g != 0) {
                    dy /= g;
                    dx /= g;
                }

                
                if (dx < 0) {
                    dx *= -1;
                    dy *= -1;
                }

                String slopeKey = dy + "/" + dx;  
                slopes.put(slopeKey, slopes.getOrDefault(slopeKey, 0) + 1);
                currMax = Math.max(currMax, slopes.get(slopeKey));
            }

            maxPoints = Math.max(maxPoints, currMax + duplicates + 1); 
        }

        return maxPoints;
    }

    public static void main(String[] args) {
        int[][] customerLocations = {
            {1, 1}, {2, 2}, {3, 3}, {4, 4}, {0, 2}
        };

        System.out.println("Maximum points on a line: " + maxPoints(customerLocations));  // Output: 4
    }
}
