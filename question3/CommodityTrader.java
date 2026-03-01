public class CommodityTrader {
    public static int maxProfit(int max_trades, int[] daily_prices) {
        int n = daily_prices.length;
        if (n <= 1 || max_trades == 0) return 0;

        // If max_trades is very high, it becomes an infinite transactions problem
        if (max_trades >= n / 2) {
            int profit = 0;
            for (int i = 1; i < n; i++) {
                if (daily_prices[i] > daily_prices[i - 1]) {
                    profit += daily_prices[i] - daily_prices[i - 1];
                }
            }
            return profit;
        }

        int[][] dp = new int[max_trades + 1][n];

        for (int i = 1; i <= max_trades; i++) {
            int localMax = -daily_prices[0];
            for (int j = 1; j < n; j++) {
                // dp[i][j] is the max of:
                // 1. Not doing a trade on day j
                // 2. Selling on day j (price + max profit from previous day with one less trade)
                dp[i][j] = Math.max(dp[i][j - 1], daily_prices[j] + localMax);
                localMax = Math.max(localMax, dp[i - 1][j - 1] - daily_prices[j]);
            }
        }
        return dp[max_trades][n - 1];
    }

    public static void main(String[] args) {
        int max_trades = 2;
        int[] prices = {2000, 4000, 1000};
        System.out.println("Maximum Profit: " + maxProfit(max_trades, prices)); 
        // Output: 2000
    }
}