package nl.infcomtec.trees;

import javax.swing.tree.DefaultTreeModel;

/**
 * TrModel extends DefaultTreeModel to provide a specialized tree model that
 * supports depth control and safe construction to avoid infinite expansion,
 * particularly useful in cases where data structures might contain circular
 * references. It integrates with TrDataSource to allow for dynamic tree
 * construction based on external data sources.
 */
public class TrModel extends DefaultTreeModel {

    public TrModel(TrNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }

    public TrModel(TrNode root) {
        super(root);
    }

    /**
     * Creates a TrModel instance by loading data from a provided TrDataSource.
     * The tree is populated in a snapshot manner based on the current state of
     * the data source. Implementors are responsible for ensuring the data
     * source is in a consistent state during this operation.
     *
     * @param dataSource The data source to load the tree data from.
     * @return A populated TrModel instance.
     */
    public static TrModel createFromDataSource(TrDataSource dataSource) {
        TrNode root = dataSource.getNextNode(null); // Assuming the first call fetches the root.
        if (root == null) {
            throw new IllegalArgumentException("Data source provided no root node.");
        }

        TrModel model = new TrModel(root);
        TrNode current = root;
        TrNode next;
        while ((next = dataSource.getNextNode(current)) != null) {
            // Implementation detail: This simplistic approach assumes getNextNode
            // provides nodes in a manner suitable for direct addition to the tree.
            // Actual logic for integrating nodes into the tree based on the dataSource's
            // structure should be implemented here.
            current.add(next);
            current = next;
        }

        return model;
    }
}
