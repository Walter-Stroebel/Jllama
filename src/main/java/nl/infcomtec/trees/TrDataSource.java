package nl.infcomtec.trees;

/**
 * TrDataSource defines the interface for fetching nodes from a data source to
 * populate a tree structure. Implementations of this interface should provide a
 * mechanism to return the next TrNode in a sequence, potentially in
 * breadth-first order, to ensure a balanced tree construction. The specific
 * nature of the data source and the strategy for fetching and converting data
 * into TrNode instances are left to the implementer.
 */
public interface TrDataSource {

    /**
     * Fetches the next node from the data source based on the current node.
     *
     * @param current The current node from which the next node is to be
     * determined. May be null if fetching the first node or if the data source
     * does not require a current node context.
     * @return The next TrNode in the sequence, or null if there are no more
     * nodes.
     */
    TrNode getNextNode(TrNode current);
}
