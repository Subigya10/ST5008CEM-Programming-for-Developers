/**
 * Definition for a binary tree node.
 */
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode(int x) { val = x; }
}

public class HydropowerOptimizer {
    private int maxNetGeneration = Integer.MIN_VALUE;

    public int maxPathSum(TreeNode root) {
        calculateMaxGain(root);
        return maxNetGeneration;
    }

    private int calculateMaxGain(TreeNode node) {
        if (node == null) {
            return 0;
        }

        // Recursively find the max gain from left and right tributaries
        // If the gain is negative (environmental cost > power), we ignore it (take 0)
        int leftGain = Math.max(calculateMaxGain(node.left), 0);
        int rightGain = Math.max(calculateMaxGain(node.right), 0);

        // Current net power if this node is the highest point (peak) of the path
        int currentPathSum = node.val + leftGain + rightGain;

        // Update the global maximum if this sequence is better
        maxNetGeneration = Math.max(maxNetGeneration, currentPathSum);

        // Return the best branch to the upstream plant to continue the sequence
        return node.val + Math.max(leftGain, rightGain);
    }

    public static void main(String[] args) {
        HydropowerOptimizer optimizer = new HydropowerOptimizer();

        // Example 1: [1, 2, 3]
        TreeNode ex1 = new TreeNode(1);
        ex1.left = new TreeNode(2);
        ex1.right = new TreeNode(3);
        System.out.println("Example 1 Output: " + optimizer.maxPathSum(ex1)); // Expected: 6

        // Example 2: [-10, 9, 20, null, null, 15, 7]
        TreeNode ex2 = new TreeNode(-10);
        ex2.left = new TreeNode(9);
        ex2.right = new TreeNode(20);
        ex2.right.left = new TreeNode(15);
        ex2.right.right = new TreeNode(7);
        System.out.println("Example 2 Output: " + optimizer.maxPathSum(ex2)); // Expected: 42
    }
}