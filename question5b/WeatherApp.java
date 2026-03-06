import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import javax.swing.table.*;

// ──────────────────────────────────────────────────────────────────────────────
//  Multi-threaded Weather Data Collector  –  STW5008CEM Question 5b
//
//  Demonstrates:
//   1. Swing GUI that never freezes  (SwingWorker keeps EDT free)
//   2. Weather data for 5 Nepali cities via OpenWeatherMap free API
//   3. 5 threads – one per city – using ExecutorService + CountDownLatch
//   4. Thread-safe GUI updates via SwingUtilities.invokeLater()
//   5. Sequential vs parallel latency comparison + live bar chart
//
//  Dependencies:
//   • org.json (https://mvnrepository.com/artifact/org.json/json)
//     Download: org.json-20231013.jar  – put it in the same folder.
//
//  Compile:
//   javac -cp .:org.json-20231013.jar WeatherApp.java
//
//  Run:
//   java -cp .:org.json-20231013.jar WeatherApp
//
//  IMPORTANT: Replace "YOUR_API_KEY_HERE" with a free key from
//   https://openweathermap.org/api  (Current Weather Data, free tier)
// ──────────────────────────────────────────────────────────────────────────────
public class WeatherApp extends JFrame {

    // ── Configuration ─────────────────────────────────────────────────────────
    private static final String API_KEY  = "a4f3701c5a7a9d5d1447f8cb9b439dd2";

    private static final String BASE_URL =
        "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";

    private static final String[] CITIES =
        {"Kathmandu", "Pokhara", "Biratnagar", "Nepalgunj", "Dhangadhi"};

    // ── GUI Components ─────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable            weatherTable;
    private JButton           fetchBtn;
    private JLabel            statusLabel;
    private JLabel            seqTimeLabel;
    private JLabel            parTimeLabel;
    private BarChartPanel     chartPanel;
    private JProgressBar      progressBar;

    // ── Timing state ───────────────────────────────────────────────────────────
    private final AtomicInteger finishedCount = new AtomicInteger(0);
    private long seqMs = 0;
    private long parMs = 0;

    // ── Colours ────────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(12,  18,  35);
    private static final Color CARD    = new Color(20,  30,  55);
    private static final Color ACCENT  = new Color(56, 189, 248);
    private static final Color TEXT    = new Color(226, 232, 240);
    private static final Color MUTED   = new Color(100, 116, 139);
    private static final Color SUCCESS = new Color(52,  211, 153);
    private static final Color DANGER  = new Color(248, 113, 113);
    private static final Color WARN    = new Color(251, 191,  36);

    // ── Constructor ────────────────────────────────────────────────────────────
    public WeatherApp() {
        super("Weather Data Collector  |  STW5008CEM Q5b");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 700);
        setMinimumSize(new Dimension(780, 560));
        setLocationRelativeTo(null);
        buildUI();
    }

    // ── Build UI ───────────────────────────────────────────────────────────────
    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildBottomPanel(),BorderLayout.SOUTH);
    }

    // ── Header panel ──────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT),
            BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));

        JLabel icon  = new JLabel("⛅");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);

        JLabel title = new JLabel("Nepal Multi-City Weather Collector");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT);

        JLabel sub = new JLabel("OpenWeatherMap API  ·  5 threads  ·  Thread-safe EDT updates");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(MUTED);

        titles.add(title);
        titles.add(sub);

        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);
        left.add(icon,   BorderLayout.WEST);
        left.add(titles, BorderLayout.CENTER);

        fetchBtn = new JButton("  Fetch Weather  ");
        fetchBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fetchBtn.setBackground(ACCENT);
        fetchBtn.setForeground(BG);
        fetchBtn.setFocusPainted(false);
        fetchBtn.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        fetchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fetchBtn.addActionListener(e -> startFetch());

        p.add(left,     BorderLayout.WEST);
        p.add(fetchBtn, BorderLayout.EAST);
        return p;
    }

    // ── Table panel ───────────────────────────────────────────────────────────
    private JScrollPane buildTablePanel() {
        String[] cols = {
            "City", "Temp (°C)", "Feels Like (°C)",
            "Humidity (%)", "Pressure (hPa)", "Condition", "Status"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        weatherTable = new JTable(tableModel);
        weatherTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        weatherTable.setRowHeight(34);
        weatherTable.setBackground(CARD);
        weatherTable.setForeground(TEXT);
        weatherTable.setGridColor(new Color(35, 50, 80));
        weatherTable.setSelectionBackground(new Color(56, 189, 248, 50));
        weatherTable.setShowHorizontalLines(true);
        weatherTable.setShowVerticalLines(false);
        weatherTable.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader th = weatherTable.getTableHeader();
        th.setBackground(new Color(28, 42, 72));
        th.setForeground(ACCENT);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setPreferredSize(new Dimension(0, 36));

        // Default centre renderer
        DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
        cr.setHorizontalAlignment(SwingConstants.CENTER);
        cr.setBackground(CARD);
        cr.setForeground(TEXT);
        for (int i = 0; i < cols.length - 1; i++)
            weatherTable.getColumnModel().getColumn(i).setCellRenderer(cr);

        // Status column – coloured renderer
        weatherTable.getColumnModel().getColumn(6)
            .setCellRenderer(new StatusRenderer());

        // Column widths
        int[] widths = {130, 90, 100, 100, 110, 170, 100};
        for (int i = 0; i < widths.length; i++)
            weatherTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(weatherTable);
        sp.getViewport().setBackground(CARD);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        return sp;
    }

    // ── Bottom panel ──────────────────────────────────────────────────────────
    private JPanel buildBottomPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 8));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        // --- Latency card ---
        JPanel latCard = new JPanel();
        latCard.setLayout(new BoxLayout(latCard, BoxLayout.Y_AXIS));
        latCard.setBackground(CARD);
        latCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 60, 90)),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JLabel latTitle = new JLabel("Latency Comparison");
        latTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        latTitle.setForeground(ACCENT);
        latTitle.setAlignmentX(LEFT_ALIGNMENT);

        seqTimeLabel = styledLabel("Sequential :  — ms", TEXT);
        parTimeLabel = styledLabel("Parallel     :  — ms", SUCCESS);

        JLabel hint = styledLabel("(Sequential runs first to measure baseline)", MUTED);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 10));

        latCard.add(latTitle);
        latCard.add(Box.createVerticalStrut(8));
        latCard.add(seqTimeLabel);
        latCard.add(Box.createVerticalStrut(4));
        latCard.add(parTimeLabel);
        latCard.add(Box.createVerticalStrut(6));
        latCard.add(hint);

        // --- Bar chart ---
        chartPanel = new BarChartPanel();
        chartPanel.setPreferredSize(new Dimension(320, 130));

        JPanel middle = new JPanel(new BorderLayout(12, 0));
        middle.setBackground(BG);
        middle.add(latCard,    BorderLayout.WEST);
        middle.add(chartPanel, BorderLayout.CENTER);

        // --- Status bar ---
        progressBar = new JProgressBar(0, CITIES.length);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setBackground(CARD);
        progressBar.setForeground(ACCENT);
        progressBar.setPreferredSize(new Dimension(0, 20));

        statusLabel = styledLabel(" Press \"Fetch Weather\" to start.", MUTED);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        JPanel statusRow = new JPanel(new BorderLayout(4, 0));
        statusRow.setBackground(BG);
        statusRow.add(progressBar, BorderLayout.CENTER);
        statusRow.add(statusLabel, BorderLayout.SOUTH);

        p.add(middle,    BorderLayout.CENTER);
        p.add(statusRow, BorderLayout.SOUTH);
        return p;
    }

    private JLabel styledLabel(String text, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI Mono", Font.PLAIN, 12));
        l.setForeground(fg);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    // ── Fetch orchestration ───────────────────────────────────────────────────

    /**
     * Entry point for the fetch button (runs on EDT).
     * A SwingWorker keeps all blocking work off the EDT.
     */
    private void startFetch() {
        fetchBtn.setEnabled(false);
        tableModel.setRowCount(0);
        finishedCount.set(0);
        progressBar.setValue(0);
        progressBar.setString("0 / " + CITIES.length);
        statusLabel.setText(" Starting sequential fetch…");

        for (String city : CITIES)
            tableModel.addRow(new Object[]{city,"—","—","—","—","—","Waiting…"});

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                // ══ PASS 1: SEQUENTIAL ════════════════════════════════════════
                long seqStart = System.currentTimeMillis();
                for (int i = 0; i < CITIES.length; i++) {
                    WeatherData wd = fetchCity(CITIES[i]);
                    final int row = i;
                    SwingUtilities.invokeLater(() -> fillRow(row, wd));
                    Thread.sleep(50);   // tiny yield so you can see rows appear
                }
                seqMs = System.currentTimeMillis() - seqStart;

                // Reset table for parallel pass
                SwingUtilities.invokeLater(() -> {
                    for (int i = 0; i < CITIES.length; i++) {
                        tableModel.setValueAt(CITIES[i], i, 0);
                        for (int c = 1; c <= 5; c++) tableModel.setValueAt("—", i, c);
                        tableModel.setValueAt("Waiting…", i, 6);
                    }
                    finishedCount.set(0);
                    progressBar.setValue(0);
                    progressBar.setString("0 / " + CITIES.length);
                    statusLabel.setText(String.format(
                        " Sequential done in %d ms. Now fetching in parallel…", seqMs));
                });

                Thread.sleep(300);

                // ══ PASS 2: PARALLEL (5 threads) ═════════════════════════════
                ExecutorService pool = Executors.newFixedThreadPool(CITIES.length);
                CountDownLatch latch = new CountDownLatch(CITIES.length);
                long parStart = System.currentTimeMillis();

                for (int i = 0; i < CITIES.length; i++) {
                    final int row = i;
                    pool.submit(() -> {
                        WeatherData wd = fetchCity(CITIES[row]);
                        // Thread-safe update – only SwingUtilities.invokeLater touches GUI
                        SwingUtilities.invokeLater(() -> {
                            fillRow(row, wd);
                            int done = finishedCount.incrementAndGet();
                            progressBar.setValue(done);
                            progressBar.setString(done + " / " + CITIES.length);
                        });
                        latch.countDown();
                    });
                }

                latch.await();
                pool.shutdown();
                
                parMs = System.currentTimeMillis() - parStart;
                return null;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    seqTimeLabel.setText(String.format("Sequential :  %d ms", seqMs));
                    parTimeLabel.setText(String.format("Parallel     :  %d ms", parMs));
                    chartPanel.setValues(seqMs, parMs);

                    double speedup = parMs > 0 ? (double) seqMs / parMs : 0;
                    statusLabel.setText(String.format(
                        "  Done.  Sequential: %d ms  |  Parallel: %d ms  |  Speedup: %.2fx",
                        seqMs, parMs, speedup));

                    progressBar.setValue(CITIES.length);
                    progressBar.setString("Complete ✓");
                    fetchBtn.setEnabled(true);
                });
            }
        }.execute();
    }

    // ── HTTP Fetch ────────────────────────────────────────────────────────────

    /**
     * Blocking HTTP call – called only from worker/pool threads, never the EDT.
     */
    private WeatherData fetchCity(String city) {
        WeatherData wd = new WeatherData(city);
        try {
            String encoded = URLEncoder.encode(city, "UTF-8");
            String urlStr  = String.format(BASE_URL, encoded, API_KEY);

            HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                // Manual JSON parsing (avoids needing external library at runtime)
                // Alternatively: JSONObject json = new JSONObject(sb.toString());
                String body = sb.toString();
                wd.temp      = extractDouble(body, "\"temp\":");
                wd.feelsLike = extractDouble(body, "\"feels_like\":");
                wd.humidity  = (int) extractDouble(body, "\"humidity\":");
                wd.pressure  = (int) extractDouble(body, "\"pressure\":");
                wd.condition = extractString(body, "\"description\":\"", "\"");
                wd.status    = "OK";
                wd.success   = true;

            } else if (responseCode == 401) {
                wd.status = "ERR: Invalid API key";
            } else if (responseCode == 404) {
                wd.status = "ERR: City not found";
            } else {
                wd.status = "ERR: HTTP " + responseCode;
            }
        } catch (java.net.SocketTimeoutException e) {
            wd.status = "ERR: Timeout";
        } catch (java.net.UnknownHostException e) {
            wd.status = "ERR: No internet";
        } catch (Exception e) {
            wd.status = "ERR: " + e.getClass().getSimpleName();
        }
        return wd;
    }

    // Minimal JSON value extractors (no external lib needed)
    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        int end   = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
               || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private String extractString(String json, String startKey, String endDelim) {
        int idx = json.indexOf(startKey);
        if (idx == -1) return "N/A";
        int start = idx + startKey.length();
        int end   = json.indexOf(endDelim, start);
        if (end == -1) return "N/A";
        return json.substring(start, end);
    }

    // ── Fill table row ────────────────────────────────────────────────────────
    // Called on EDT only
    private void fillRow(int row, WeatherData wd) {
        if (wd.success) {
            tableModel.setValueAt(String.format("%.1f",  wd.temp),      row, 1);
            tableModel.setValueAt(String.format("%.1f",  wd.feelsLike), row, 2);
            tableModel.setValueAt(wd.humidity,                           row, 3);
            tableModel.setValueAt(wd.pressure,                           row, 4);
            tableModel.setValueAt(capitalise(wd.condition),              row, 5);
            tableModel.setValueAt("OK",                                  row, 6);
        } else {
            for (int c = 1; c <= 5; c++) tableModel.setValueAt("—", row, c);
            tableModel.setValueAt(wd.status, row, 6);
        }
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner class: WeatherData
    // ─────────────────────────────────────────────────────────────────────────
    static class WeatherData {
        final String city;
        String  status = "Pending";
        String  condition = "";
        double  temp, feelsLike;
        int     humidity, pressure;
        boolean success = false;

        WeatherData(String city) { this.city = city; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner class: StatusRenderer  (colours the Status column)
    // ─────────────────────────────────────────────────────────────────────────
    class StatusRenderer extends DefaultTableCellRenderer {
        StatusRenderer() { setHorizontalAlignment(SwingConstants.CENTER); }

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            String val = v == null ? "" : v.toString();
            setBackground(CARD);
            if (val.equals("OK"))              setForeground(SUCCESS);
            else if (val.startsWith("ERR"))    setForeground(DANGER);
            else if (val.startsWith("Waiting"))setForeground(WARN);
            else                               setForeground(MUTED);
            return this;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner class: BarChartPanel  (Java2D bar chart for latency comparison)
    // ─────────────────────────────────────────────────────────────────────────
    class BarChartPanel extends JPanel {
        private long seq = 0, par = 0;

        BarChartPanel() { setBackground(BG); }

        void setValues(long seq, long par) { this.seq = seq; this.par = par; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int PL = 14, PR = 70, PT = 28, PB = 36;
            int cW = W - PL - PR;
            int cH = H - PT - PB;

            // Title
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(ACCENT);
            g2.drawString("Latency Bar Chart", PL, PT - 8);

            if (seq == 0 && par == 0) {
                g2.setColor(MUTED);
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                g2.drawString("No data yet – click Fetch Weather", PL + 4, PT + cH / 2);
                return;
            }

            long maxV  = Math.max(seq, Math.max(par, 1));
            int  barW  = Math.max((cW - 24) / 2, 20);

            // Sequential bar
            int h1 = (int)((double) seq / maxV * cH);
            int x1 = PL + 8;
            g2.setColor(ACCENT);
            g2.fillRoundRect(x1, PT + cH - h1, barW, h1, 6, 6);
            // Value label
            g2.setColor(TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString(seq + "ms", x1 + barW / 2 - 14, PT + cH - h1 - 4);
            // Axis label
            g2.setColor(MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString("Sequential", x1, H - 4);

            // Parallel bar
            int h2 = (int)((double) par / maxV * cH);
            int x2 = x1 + barW + 20;
            g2.setColor(SUCCESS);
            g2.fillRoundRect(x2, PT + cH - h2, barW, h2, 6, 6);
            g2.setColor(TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString(par + "ms", x2 + barW / 2 - 14, PT + cH - h2 - 4);
            g2.setColor(MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString("Parallel", x2 + 4, H - 4);

            // Speedup annotation
            if (par > 0) {
                double sp = (double) seq / par;
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(WARN);
                g2.drawString(String.format("%.2fx", sp), x2 + barW + 6, PT + cH / 2);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(MUTED);
                g2.drawString("faster", x2 + barW + 6, PT + cH / 2 + 14);
            }

            // Baseline
            g2.setColor(new Color(40, 60, 90));
            g2.drawLine(PL, PT + cH, W - PR, PT + cH);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Main
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Use Nimbus for a cleaner look
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new WeatherApp().setVisible(true));
    }
}