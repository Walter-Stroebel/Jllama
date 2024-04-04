package nl.infcomtec.trees;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * TrNode extends DefaultMutableTreeNode to add depth tracking and enforce a
 * maximum depth. This class is designed to prevent unbounded tree growth, which
 * can be particularly useful in preventing circular references in tree creation
 * logic. Public access to depth and maxDepth is intentional to avoid
 * unnecessary getter methods, given these fields are immutable and their direct
 * access does not compromise the design's integrity.
 */
public class TrNode extends DefaultMutableTreeNode {

    /**
     * The depth of this node within the tree.
     */
    public final int depth;

    /**
     * The maximum depth allowed for this tree.
     */
    public final int maxDepth;

    /**
     * Constructs a root TrNode with a specified maximum depth and user object.
     *
     * @param maxDepth The maximum depth the tree can grow to.
     * @param userObject The user object to store in this node.
     * @throws IllegalArgumentException if maxDepth is less than 1.
     */
    public TrNode(int maxDepth, Object userObject) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("Depth must be >0");
        }
        this.depth = 0;
        this.maxDepth = maxDepth;
        setUserObject(userObject);
    }

    /**
     * Constructs a TrNode that is a child of the specified parent node.
     *
     * @param parent The parent node of this node.
     * @param userObject The user object to store in this node.
     * @throws RuntimeException if adding this node would exceed the parent's
     * maxDepth.
     */
    public TrNode(TrNode parent, Object userObject) {
        if (parent.depth >= parent.maxDepth) {
            throw new RuntimeException("Internal logic error, max depth exceeded.");
        }
        this.depth = parent.depth + 1;
        this.maxDepth = parent.maxDepth;
        setUserObject(userObject);
    }

    @Override
    public boolean isLeaf() {
        return !getAllowsChildren();
    }

    @Override
    public boolean getAllowsChildren() {
        return super.getAllowsChildren() && depth < maxDepth;
    }
}
