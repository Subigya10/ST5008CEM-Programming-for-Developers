import java.util.*;

class EnergySource {
    String id;
    String type;
    int capacity;
    int startHour;
    int endHour;
    double costPerKwh;

    public EnergySource(String id, String type, int capacity, int startHour, int endHour, double costPerKwh) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.startHour = startHour;
        this.endHour = endHour;
        this.costPerKwh = costPerKwh;
    }

    public boolean isAvailable(int hour) {
        return hour >= startHour && hour <= endHour;
    }
}

public class task4{

    public static void main(String[] args) {

        // Hour 06 Demand
        Map<String, Integer> demand = new HashMap<>();
        demand.put("District A", 20);
        demand.put("District B", 15);
        demand.put("District C", 25);

        int hour = 6;

        // Energy Sources
        List<EnergySource> sources = new ArrayList<>();
        sources.add(new EnergySource("S1", "Solar", 50, 6, 18, 1.0));
        sources.add(new EnergySource("S2", "Hydro", 40, 0, 24, 1.5));
        sources.add(new EnergySource("S3", "Diesel", 60, 17, 23, 3.0));

        allocateEnergy(hour, demand, sources);
    }

    public static void allocateEnergy(int hour, Map<String, Integer> demand, List<EnergySource> sources) {

        int totalDemand = demand.values().stream().mapToInt(Integer::intValue).sum();
        double minDemand = totalDemand * 0.9;
        double maxDemand = totalDemand * 1.1;

        // Filter available sources
        List<EnergySource> available = new ArrayList<>();
        for (EnergySource s : sources) {
            if (s.isAvailable(hour)) {
                available.add(s);
            }
        }

        // Sort by cost (Greedy Strategy)
        available.sort(Comparator.comparingDouble(s -> s.costPerKwh));

        int remaining = totalDemand;
        double totalCost = 0;

        System.out.println("Hour: " + hour);
        System.out.println("Total Demand: " + totalDemand + " kWh\n");

        for (EnergySource s : available) {
            if (remaining <= 0)
                break;

            int used = Math.min(s.capacity, remaining);
            remaining -= used;
            totalCost += used * s.costPerKwh;

            System.out.println(s.type + " used: " + used + " kWh");
        }

        int supplied = totalDemand - remaining;

        if (supplied >= minDemand && supplied <= maxDemand) {
            System.out.println("\nTotal Supplied: " + supplied + " kWh");
            System.out.println("Total Cost: Rs. " + totalCost);
            System.out.println("Demand Fulfilled Successfully (within ±10%)");
        } else {
            System.out.println("Demand could not be satisfied within limits.");
        }
    }
}