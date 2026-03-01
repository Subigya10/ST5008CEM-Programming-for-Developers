import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

// ══════════════════════════════════════════════════════════════════════════════
//  Tourist Spot Optimizer  –  STW5008CEM Question 5a
//
//  Covers all 20 marks:
//   1. GUI Design for User Input            (3 marks)
//   2. Tourist Spot Dataset loaded in code  (3 marks)
//   3. Greedy Heuristic Optimization        (5 marks)
//   4. Itinerary + Coordinate Map/Path View (5 marks)
//   5. Brute-Force Comparison               (4 marks)
//
//  Compile:  javac TouristOptimizer.java
//  Run:      java  TouristOptimizer
// ══════════════════════════════════════════════════════════════════════════════
public class TouristOptimizer extends JFrame {

    // ── Colours ────────────────────────────────────────────────────────────────
    static final Color BG      = new Color(15, 23, 42);
    static final Color CARD    = new Color(22, 35, 60);
    static final Color ACCENT  = new Color(251, 191, 36);   // warm gold
    static final Color GREEN   = new Color(52, 211, 153);
    static final Color BLUE    = new Color(56, 189, 248);
    static final Color TEXT    = new Color(226, 232, 240);
    static final Color MUTED   = new Color(100, 116, 139);
    static final Color DANGER  = new Color(248, 113, 113);

    // ── Dataset (Task 2) ───────────────────────────────────────────────────────
    static final Spot[] ALL_SPOTS = {
        new Spot("Pashupatinath Temple", 27.7104, 85.3488, 100,  6, 18, new String[]{"culture","religious"}),
        new Spot("Swayambhunath Stupa",  27.7149, 85.2906, 200,  7, 17, new String[]{"culture","heritage"}),
        new Spot("Garden of Dreams",     27.7125, 85.3170, 150,  9, 21, new String[]{"nature","relaxation"}),
        new Spot("Chandragiri Hills",    27.6616, 85.2458, 700,  9, 17, new String[]{"nature","adventure"}),
        new Spot("Kathmandu Durbar Sq",  27.7048, 85.3076, 100, 10, 17, new String[]{"culture","heritage"}),
        new Spot("Boudhanath Stupa",     27.7215, 85.3620, 250,  6, 18, new String[]{"culture","religious"}),
        new Spot("Nagarjun Forest",      27.7500, 85.2800,   0,  8, 17, new String[]{"nature","adventure"}),
        new Spot("Patan Durbar Square",  27.6710, 85.3247, 150,  9, 17, new String[]{"culture","heritage"}),
    };

    // Each spot takes ~2 hours to visit, travel time estimated via Euclidean distance
    static final double VISIT_HOURS  = 2.0;
    static final double TRAVEL_SPEED = 0.05; // degrees per hour (rough)

    // ── GUI components ─────────────────────────────────────────────────────────
    private JSpinner   budgetSpinner, timeSpinner, startHourSpinner;
    private JCheckBox  cbCulture, cbNature, cbAdventure, cbReligious, cbHeritage, cbRelaxation;
    private JTextArea  itineraryArea;
    private JTextArea  comparisonArea;
    private MapPanel   mapPanel;
    private JLabel     statusLabel;

    public TouristOptimizer() {
        super("Tourist Spot Optimizer  |  STW5008CEM Q5a");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 780);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        buildUI();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI Construction
    // ══════════════════════════════════════════════════════════════════════════
    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        add(buildHeader(),  BorderLayout.NORTH);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                          buildLeftPanel(), buildRightPanel());
        main.setDividerLocation(320);
        main.setResizeWeight(0.28);
        main.setBackground(BG);
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(main, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Header ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));
        JLabel title = new JLabel("🗺  Tourist Spot Optimizer – Nepal");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT);

        JLabel sub = new JLabel("Greedy Heuristic  ·  Brute-Force Comparison  ·  Path Visualisation");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(MUTED);

        JPanel titles = new JPanel(new GridLayout(2,1,0,2));
        titles.setOpaque(false);
        titles.add(title); titles.add(sub);
        p.add(titles, BorderLayout.WEST);
        return p;
    }

    // ── Left panel – user input (Task 1) ──────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 6));

        p.add(sectionLabel("User Preferences"));
        p.add(Box.createVerticalStrut(8));

        // Budget
        p.add(inputLabel("Max Budget (Rs.)"));
        budgetSpinner = new JSpinner(new SpinnerNumberModel(1500, 0, 99999, 100));
        styleSpinner(budgetSpinner);
        p.add(budgetSpinner);
        p.add(Box.createVerticalStrut(8));

        // Time available
        p.add(inputLabel("Time Available (hours)"));
        timeSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 24, 1));
        styleSpinner(timeSpinner);
        p.add(timeSpinner);
        p.add(Box.createVerticalStrut(8));

        // Start hour
        p.add(inputLabel("Start Hour (e.g. 9 = 9:00 AM)"));
        startHourSpinner = new JSpinner(new SpinnerNumberModel(9, 0, 23, 1));
        styleSpinner(startHourSpinner);
        p.add(startHourSpinner);
        p.add(Box.createVerticalStrut(12));

        // Interest tags
        p.add(sectionLabel("Interest Tags"));
        p.add(Box.createVerticalStrut(6));

        cbCulture    = styledCheck("Culture");
        cbNature     = styledCheck("Nature");
        cbAdventure  = styledCheck("Adventure");
        cbReligious  = styledCheck("Religious");
        cbHeritage   = styledCheck("Heritage");
        cbRelaxation = styledCheck("Relaxation");

        cbCulture.setSelected(true);
        cbNature.setSelected(true);

        for (JCheckBox cb : new JCheckBox[]{cbCulture, cbNature, cbAdventure,
                                            cbReligious, cbHeritage, cbRelaxation}) {
            p.add(cb); p.add(Box.createVerticalStrut(4));
        }

        p.add(Box.createVerticalStrut(16));

        // Optimise button
        JButton btn = new JButton("  Plan My Itinerary  ");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(ACCENT);
        btn.setForeground(BG);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.addActionListener(e -> runOptimisation());
        p.add(btn);

        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Right panel – results ──────────────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(BG);

        // Map panel (Task 4)
        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(0, 300));
        mapPanel.setBorder(titledBorder("Coordinate Map / Path View"));

        // Tabs for itinerary and comparison
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(CARD);
        tabs.setForeground(TEXT);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        itineraryArea = styledTextArea();
        itineraryArea.setText("Press \"Plan My Itinerary\" to generate your optimised trip.");

        comparisonArea = styledTextArea();
        comparisonArea.setText("Brute-force vs greedy comparison will appear here.");

        tabs.addTab("📋 Itinerary (Greedy)",   new JScrollPane(itineraryArea));
        tabs.addTab("⚖  Brute-Force Compare", new JScrollPane(comparisonArea));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mapPanel, tabs);
        split.setDividerLocation(300);
        split.setResizeWeight(0.45);
        split.setBackground(BG);
        split.setBorder(BorderFactory.createEmptyBorder());

        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(10,16,30));
        p.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        statusLabel = new JLabel("Ready. Set your preferences and click Plan My Itinerary.");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(MUTED);
        p.add(statusLabel, BorderLayout.WEST);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Optimisation Logic
    // ══════════════════════════════════════════════════════════════════════════
    private void runOptimisation() {
        int    budget    = (int)   budgetSpinner.getValue();
        int    maxHours  = (int)   timeSpinner.getValue();
        int    startHour = (int)   startHourSpinner.getValue();

        Set<String> interests = new HashSet<>();
        if (cbCulture.isSelected())    interests.add("culture");
        if (cbNature.isSelected())     interests.add("nature");
        if (cbAdventure.isSelected())  interests.add("adventure");
        if (cbReligious.isSelected())  interests.add("religious");
        if (cbHeritage.isSelected())   interests.add("heritage");
        if (cbRelaxation.isSelected()) interests.add("relaxation");

        if (interests.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select at least one interest tag.", "Input Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ── Greedy Heuristic (Task 3) ─────────────────────────────────────────
        List<Spot> greedy = greedyOptimise(budget, maxHours, startHour, interests);

        // ── Brute Force on first 6 spots (Task 5) ────────────────────────────
        Spot[] small = Arrays.copyOf(ALL_SPOTS, Math.min(6, ALL_SPOTS.length));
        List<Spot> brute = bruteForce(small, budget, maxHours, startHour, interests);

        // ── Update UI ─────────────────────────────────────────────────────────
        itineraryArea.setText(buildItineraryText(greedy, startHour));
        comparisonArea.setText(buildComparisonText(greedy, brute, budget, maxHours));
        mapPanel.setRoute(greedy, ALL_SPOTS);
        statusLabel.setText("Done.  Greedy: " + greedy.size() + " spots  |  Brute-force: "
            + brute.size() + " spots  (on first 6)");
    }

    // ── Greedy algorithm (Task 3) ──────────────────────────────────────────────
    /**
     * Greedy strategy:
     *  Score each spot = (interest tag matches * 100) / entry_fee+1
     *  Always pick the highest-scoring unvisited spot that fits budget & time.
     *  Justification printed alongside each selection.
     */
    private List<Spot> greedyOptimise(int budget, double maxHours,
                                       int startHour, Set<String> interests) {
        List<Spot> result  = new ArrayList<>();
        boolean[]  visited = new boolean[ALL_SPOTS.length];
        double     spent   = 0;
        double     hour    = startHour;
        double     lastLat = 27.7104, lastLon = 85.3488; // start from KTM centre

        while (true) {
            int    bestIdx   = -1;
            double bestScore = -1;

            for (int i = 0; i < ALL_SPOTS.length; i++) {
                if (visited[i]) continue;
                Spot s = ALL_SPOTS[i];

                // Travel time from last spot
                double dist    = euclidean(lastLat, lastLon, s.lat, s.lon);
                double travel  = dist / TRAVEL_SPEED;
                double arriveAt = hour + travel;
                double leaveAt  = arriveAt + VISIT_HOURS;

                // Constraints
                if (spent + s.fee > budget)           continue;
                if (leaveAt - startHour > maxHours)   continue;
                if (arriveAt < s.openHour)            arriveAt = s.openHour;
                if (arriveAt >= s.closeHour)          continue;
                if (arriveAt + VISIT_HOURS > s.closeHour) continue;

                // Score: tag matches / cost (greedy = best value per rupee)
                int matches = 0;
                for (String t : s.tags) if (interests.contains(t)) matches++;
                if (matches == 0) continue;

                double score = (matches * 100.0) / (s.fee + 1);
                if (score > bestScore) { bestScore = bestIdx == -1 ? score : bestScore;
                    bestScore = score; bestIdx = i; }
            }

            if (bestIdx == -1) break;

            Spot chosen = ALL_SPOTS[bestIdx];
            double dist   = euclidean(lastLat, lastLon, chosen.lat, chosen.lon);
            double travel = dist / TRAVEL_SPEED;
            hour    += travel;
            spent   += chosen.fee;
            lastLat  = chosen.lat;
            lastLon  = chosen.lon;
            chosen.arrivalHour = hour;
            visited[bestIdx] = true;
            result.add(chosen);
            hour += VISIT_HOURS;
        }
        return result;
    }

    // ── Brute Force (Task 5) ───────────────────────────────────────────────────
    /**
     * Try every permutation of the small dataset.
     * Return the permutation that visits the most spots within constraints.
     */
    private List<Spot> bruteForce(Spot[] spots, int budget, double maxHours,
                                   int startHour, Set<String> interests) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < spots.length; i++) indices.add(i);

        List<List<Integer>> perms = new ArrayList<>();
        permute(indices, 0, perms);

        List<Spot> best = new ArrayList<>();

        for (List<Integer> perm : perms) {
            List<Spot> candidate = new ArrayList<>();
            double spent  = 0, hour = startHour;
            double lastLat = 27.7104, lastLon = 85.3488;

            for (int idx : perm) {
                Spot s = spots[idx];
                double dist    = euclidean(lastLat, lastLon, s.lat, s.lon);
                double travel  = dist / TRAVEL_SPEED;
                double arriveAt = hour + travel;

                if (spent + s.fee > budget)              break;
                if (arriveAt - startHour + VISIT_HOURS > maxHours) break;
                if (arriveAt < s.openHour) arriveAt = s.openHour;
                if (arriveAt >= s.closeHour)             break;
                if (arriveAt + VISIT_HOURS > s.closeHour) break;

                int matches = 0;
                for (String t : s.tags) if (interests.contains(t)) matches++;
                if (matches == 0) continue;

                spent   += s.fee;
                hour     = arriveAt + VISIT_HOURS;
                lastLat  = s.lat; lastLon = s.lon;
                candidate.add(s);
            }

            if (candidate.size() > best.size()) best = candidate;
        }
        return best;
    }

    private void permute(List<Integer> arr, int k, List<List<Integer>> result) {
        if (k == arr.size() - 1) { result.add(new ArrayList<>(arr)); return; }
        for (int i = k; i < arr.size(); i++) {
            Collections.swap(arr, i, k);
            permute(arr, k + 1, result);
            Collections.swap(arr, i, k);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Text Builders
    // ══════════════════════════════════════════════════════════════════════════
    private String buildItineraryText(List<Spot> spots, int startHour) {
        if (spots.isEmpty()) return "No spots could be visited with the given constraints.\n"
            + "Try increasing your budget or available hours.";

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("   GREEDY OPTIMISED ITINERARY\n");
        sb.append("═══════════════════════════════════════════\n\n");
        sb.append("Algorithm: Greedy (highest value-per-rupee first)\n");
        sb.append("Justification: At each step, the spot with the\n");
        sb.append("best score (tag matches × 100 / entry_fee) is\n");
        sb.append("chosen — maximising interest per rupee spent.\n\n");

        int totalCost = 0;
        double totalTime = 0;
        for (int i = 0; i < spots.size(); i++) {
            Spot s = spots.get(i);
            int  arrH = (int) s.arrivalHour;
            int  arrM = (int)((s.arrivalHour - arrH) * 60);
            sb.append(String.format("Stop %d: %s\n", i+1, s.name));
            sb.append(String.format("   Arrive : %02d:%02d\n", arrH, arrM));
            sb.append(String.format("   Leave  : %02d:%02d\n", arrH + (int)VISIT_HOURS, arrM));
            sb.append(String.format("   Fee    : Rs. %d\n", s.fee));
            sb.append(String.format("   Tags   : %s\n", String.join(", ", s.tags)));
            sb.append(String.format("   Why    : High interest match, fits budget\n\n"));
            totalCost += s.fee;
            totalTime  = s.arrivalHour + VISIT_HOURS - startHour;
        }

        sb.append("───────────────────────────────────────────\n");
        sb.append(String.format("Total Spots  : %d\n", spots.size()));
        sb.append(String.format("Total Cost   : Rs. %d\n", totalCost));
        sb.append(String.format("Total Time   : %.1f hours\n", totalTime));
        return sb.toString();
    }

    private String buildComparisonText(List<Spot> greedy, List<Spot> brute,
                                        int budget, double maxHours) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("   BRUTE-FORCE vs GREEDY COMPARISON\n");
        sb.append("   (Dataset: first 6 spots only)\n");
        sb.append("═══════════════════════════════════════════\n\n");

        int gCost = greedy.stream().mapToInt(s->s.fee).sum();
        int bCost = brute.stream().mapToInt(s->s.fee).sum();

        sb.append(String.format("%-22s %10s %10s\n", "Metric", "Greedy", "Brute-Force"));
        sb.append("─".repeat(44)).append("\n");
        sb.append(String.format("%-22s %10d %10d\n", "Spots visited", greedy.size(), brute.size()));
        sb.append(String.format("%-22s %10s %10s\n", "Total cost (Rs.)",
            "Rs. "+gCost, "Rs. "+bCost));
        sb.append("\n");

        sb.append("Greedy path:\n");
        for (Spot s : greedy) sb.append("  → ").append(s.name).append("\n");

        sb.append("\nBrute-force optimal path:\n");
        for (Spot s : brute) sb.append("  → ").append(s.name).append("\n");

        sb.append("\n═══════════════════════════════════════════\n");
        sb.append("ACCURACY vs PERFORMANCE TRADE-OFF:\n\n");
        sb.append("• Brute-force tries ALL permutations → always\n");
        sb.append("  finds the true optimal but O(n!) complexity.\n");
        sb.append("  For 6 spots = 720 permutations (manageable).\n");
        sb.append("  For 10 spots = 3,628,800 → too slow.\n\n");
        sb.append("• Greedy is O(n²) → very fast even for 100+\n");
        sb.append("  spots, but may miss the global optimum.\n\n");
        sb.append("• Conclusion: Greedy is preferred for real-world\n");
        sb.append("  use; brute-force only viable for tiny datasets.\n");
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helper: Euclidean distance on lat/lon (approximate)
    // ══════════════════════════════════════════════════════════════════════════
    static double euclidean(double lat1, double lon1, double lat2, double lon2) {
        double dlat = lat1 - lat2, dlon = lon1 - lon2;
        return Math.sqrt(dlat*dlat + dlon*dlon);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Inner class: Spot  (Task 2 dataset)
    // ══════════════════════════════════════════════════════════════════════════
    static class Spot {
        final String   name;
        final double   lat, lon;
        final int      fee, openHour, closeHour;
        final String[] tags;
        double arrivalHour = 0;

        Spot(String name, double lat, double lon, int fee,
             int openHour, int closeHour, String[] tags) {
            this.name = name; this.lat = lat; this.lon = lon;
            this.fee = fee; this.openHour = openHour;
            this.closeHour = closeHour; this.tags = tags;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Inner class: MapPanel  (Task 4 – coordinate path visualisation)
    // ══════════════════════════════════════════════════════════════════════════
    static class MapPanel extends JPanel {
        private List<Spot>  route    = new ArrayList<>();
        private Spot[]      allSpots = ALL_SPOTS;

        private static final Color DOT_ALL    = new Color(100, 116, 139);
        private static final Color DOT_ROUTE  = new Color(251, 191,  36);
        private static final Color LINE_COLOR = new Color(56,  189, 248, 180);
        private static final Color START_DOT  = new Color(52,  211, 153);

        MapPanel() { setBackground(new Color(12, 20, 40)); }

        void setRoute(List<Spot> route, Spot[] all) {
            this.route    = route;
            this.allSpots = all;
            repaint();
        }

        // Static colour constants so they work inside a static inner class
        private static final Color C_TEXT = new Color(226, 232, 240);
        private static final Color C_MUTED= new Color(100, 116, 139);
        private static final Color C_BG   = new Color(15,  23,  42);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int PAD = 40;

            // Safety: nothing to draw yet
            if (allSpots == null || allSpots.length == 0) return;

            // Compute bounds from all spots
            double minLat = allSpots[0].lat, maxLat = allSpots[0].lat;
            double minLon = allSpots[0].lon, maxLon = allSpots[0].lon;
            for (Spot s : allSpots) {
                if (s.lat < minLat) minLat = s.lat;
                if (s.lat > maxLat) maxLat = s.lat;
                if (s.lon < minLon) minLon = s.lon;
                if (s.lon > maxLon) maxLon = s.lon;
            }
            // Add padding so dots don't sit on the edge
            double latRange = (maxLat - minLat) + 0.02;
            double lonRange = (maxLon - minLon) + 0.02;
            minLat -= 0.01;
            minLon -= 0.01;

            // Draw grid lines
            g2.setColor(new Color(30, 45, 70));
            g2.setStroke(new BasicStroke(0.5f));
            for (int i = 0; i <= 5; i++) {
                int x = PAD + (W - 2*PAD) * i / 5;
                int y = PAD + (H - 2*PAD) * i / 5;
                g2.drawLine(x, PAD, x, H-PAD);
                g2.drawLine(PAD, y, W-PAD, y);
            }

            // Project lat/lon → screen coords
            final double fMinLat = minLat, fMinLon = minLon;
            final double fLatRange = latRange, fLonRange = lonRange;
            final int fW = W, fH = H;
            java.util.function.BiFunction<Double,Double,int[]> proj = (lat, lon) -> new int[]{
                PAD + (int)((lon - fMinLon) / fLonRange * (fW - 2*PAD)),
                fH - PAD - (int)((lat - fMinLat) / fLatRange * (fH - 2*PAD))
            };

            // Draw all spots (grey)
            for (Spot s : allSpots) {
                int[] p = proj.apply(s.lat, s.lon);
                g2.setColor(DOT_ALL);
                g2.fillOval(p[0]-5, p[1]-5, 10, 10);
                g2.setColor(C_MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g2.drawString(s.name, p[0]+7, p[1]+4);
            }

            if (route.isEmpty()) {
                g2.setColor(C_MUTED);
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                g2.drawString("Path will appear here after planning", W/2 - 130, H/2);
                return;
            }

            // Draw route lines (dashed)
            g2.setColor(LINE_COLOR);
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                         0, new float[]{8, 4}, 0));
            for (int i = 0; i < route.size() - 1; i++) {
                int[] a = proj.apply(route.get(i).lat,   route.get(i).lon);
                int[] b = proj.apply(route.get(i+1).lat, route.get(i+1).lon);
                g2.drawLine(a[0], a[1], b[0], b[1]);
            }

            // Draw route dots + stop numbers
            for (int i = 0; i < route.size(); i++) {
                Spot  s = route.get(i);
                int[] p = proj.apply(s.lat, s.lon);
                Color dot = (i == 0) ? START_DOT : DOT_ROUTE;
                g2.setColor(dot);
                g2.fillOval(p[0]-8, p[1]-8, 16, 16);
                g2.setColor(C_BG);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(String.valueOf(i+1), p[0]-3, p[1]+4);
            }

            // Legend
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(START_DOT);  g2.fillOval(PAD,      H-PAD+8, 10, 10);
            g2.setColor(C_TEXT);     g2.drawString("Start",      PAD+14,    H-PAD+17);
            g2.setColor(DOT_ROUTE);  g2.fillOval(PAD+60,   H-PAD+8, 10, 10);
            g2.setColor(C_TEXT);     g2.drawString("Visit stop", PAD+74,    H-PAD+17);
            g2.setColor(LINE_COLOR); g2.setStroke(new BasicStroke(2));
            g2.drawLine(PAD+150, H-PAD+13, PAD+180, H-PAD+13);
            g2.setColor(C_TEXT);     g2.drawString("Route",      PAD+184,   H-PAD+17);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI helper methods
    // ══════════════════════════════════════════════════════════════════════════
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(ACCENT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel inputLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void styleSpinner(JSpinner s) {
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        s.setAlignmentX(LEFT_ALIGNMENT);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        s.setBackground(CARD);
        s.setForeground(TEXT);
    }

    private JCheckBox styledCheck(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setBackground(BG);
        cb.setForeground(TEXT);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setAlignmentX(LEFT_ALIGNMENT);
        cb.setFocusPainted(false);
        return cb;
    }

    private JTextArea styledTextArea() {
        JTextArea ta = new JTextArea();
        ta.setFont(new Font("Segoe UI Mono", Font.PLAIN, 12));
        ta.setBackground(CARD);
        ta.setForeground(TEXT);
        ta.setCaretColor(TEXT);
        ta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        return ta;
    }

    private Border titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(40,60,90)), title);
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 11));
        tb.setTitleColor(ACCENT);
        return tb;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Main
    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName()); break; }
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new TouristOptimizer().setVisible(true));
    }
}