import java.util.*;

class Edge {
    String destination;
    double probability;

    public Edge(String destination, double probability) {
        this.destination = destination;
        this.probability = probability;
    }
}

class Node implements Comparable<Node> {
    String name;
    double distance;

    public Node(String name, double distance) {
        this.name = name;
        this.distance = distance;
    }

    @Override
    public int compareTo(Node other) {
        return Double.compare(this.distance, other.distance);
    }
}

public class SafestPath {

    public static Map<String, Double> dijkstra(Map<String, List<Edge>> graph, String source) {
        
        Map<String, Double> dist = new HashMap<>();
        
        // Initialize distances
        for (String node : graph.keySet()) {
            dist.put(node, Double.POSITIVE_INFINITY);
        }
        dist.put(source, 0.0);

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(source, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String u = current.name;

            if (current.distance > dist.get(u)) continue;

            for (Edge edge : graph.get(u)) {
                String v = edge.destination;
                
                // Transform probability using -log
                double weight = -Math.log(edge.probability);

                if (dist.get(u) + weight < dist.get(v)) {
                    dist.put(v, dist.get(u) + weight);
                    pq.add(new Node(v, dist.get(v)));
                }
            }
        }

        return dist;
    }

    public static void main(String[] args) {

        // Create graph
        Map<String, List<Edge>> graph = new HashMap<>();

        graph.put("KTM", new ArrayList<>());
        graph.put("JA", new ArrayList<>());
        graph.put("JB", new ArrayList<>());
        graph.put("PH", new ArrayList<>());
        graph.put("BS", new ArrayList<>());

        // Add edges with safety probabilities
        graph.get("KTM").add(new Edge("JA", 0.9));
        graph.get("KTM").add(new Edge("JB", 0.85));
        graph.get("JA").add(new Edge("PH", 0.95));
        graph.get("JB").add(new Edge("BS", 0.8));
        graph.get("JA").add(new Edge("BS", 0.7));

        Map<String, Double> result = dijkstra(graph, "KTM");

        System.out.println("Safest Path Probabilities from KTM:");
        for (String node : result.keySet()) {
            double probability = Math.exp(-result.get(node)); // Convert back
            System.out.println(node + " : " + probability);
        }
    }
}