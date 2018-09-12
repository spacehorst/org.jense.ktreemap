package org.jense.ktreemap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Node of a KTreeMap.<BR>
 * If the node is a branch, only the label is set.<BR>
 * If the node is a leaf, we need a label, a weight and a value.
 * <p>
 * You can also use a TreeMapNode in a JTree.
 *
 * @author Laurent Dutheil
 */

public class TreeMapNode {

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 742372833853976103L;
    // max border between two nodes of the same level
    private static int border = 3;
    private double weight = 0.0;
    private Rectangle bounds = new Rectangle(0, 0, 0, 0);
    private Object value;
    private final ArrayList<TreeMapNode> children = new ArrayList<>();
    private TreeMapNode parent;

    /**
     * Constructor for a branch
     *
     * @param value value of the TreeMapNode
     */
    public TreeMapNode(Object value) {
        this.value = value;
    }

    /**
     * Constructor for a leaf.
     *
     * @param weight weight of the leaf (if negative, we take the absolute value).
     * @param value Value associ�e � la feuille
     */
    public TreeMapNode(Object value, double weight) {
        this.value = value;
        // the weight must be positive
        this.weight = Math.abs(weight);
    }

    /**
     * Get the max border between two nodes of the same level.
     *
     * @return Returns the border.
     */
    public static int getBorder() {
        return TreeMapNode.border;
    }

    /**
     * Set the max border between two nodes of the same level.
     *
     * @param border The border to set.
     */
    public static void setBorder(int border) {
        TreeMapNode.border = border;
    }

    /**
     * add a new child to the node.
     *
     * @param newChild new child
     */
    public void add(TreeMapNode newChild) {
        children.add(newChild);
        newChild.setParent(this);
        setWeight(weight + newChild.getWeight());
    }

    /**
     * get the active leaf.<BR>
     * null if the passed position is not in this tree.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return active leaf
     */
    public TreeMapNode getActiveLeaf(int x, int y) {

        if (isLeaf()) {
            if (x >= getX() && x <= getX() + getWidth()
                    && y >= getY() && y <= getY() + getHeight()) {
                return this;
            }
        } else {
            for (TreeMapNode node : children) {
                if (x >= node.getX() && x <= node.getX() + node.getWidth()
                        && y >= node.getY() && y <= node.getY() + node.getHeight()) {
                    return node.getActiveLeaf(x, y);
                }
            }
        }
        return null;
    }

    /**
     * get the active leaf.<BR>
     * null if the passed position is not in this tree.
     *
     * @param position position
     * @return active leaf
     */
    public TreeMapNode getActiveLeaf(Point position) {
        if (position != null) {
            return getActiveLeaf(position.x, position.y);
        }
        return null;
    }

    /**
     * @return the bounds of the KTreeMap
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * get the first child which fits the position.<BR>
     * null if the passed position is not in this tree.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return the first child which fits the position.
     */
    public TreeMapNode getChild(int x, int y) {
        if ( !isLeaf()) {
            for (TreeMapNode node : children) {
                if (x >= node.getX() && x <= node.getX() + node.getWidth()
                        && y >= node.getY() && y <= node.getY() + node.getHeight()) {
                    return node;
                }
            }

        }
        return null;
    }

    /**
     * get the first child which fits the position.<BR>
     * null if the passed position is not in this tree.
     *
     * @param position position
     * @return the first child which fits the position.
     */
    public TreeMapNode getChild(Point position) {
        if (position != null) {
            return getChild(position.x, position.y);
        }
        return null;
    }

    /**
     * get a List with the children.
     *
     * @return List with the children
     */
    public List<TreeMapNode> getChildren() {
        return children;
    }

    /**
     * get the height.
     *
     * @return the height
     */
    public int getHeight() {
        return bounds.height;
    }

    /**
     * get the Value.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * get the weight.
     *
     * @return the weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * get the width.
     *
     * @return the width
     */
    public int getWidth() {
        return bounds.width;
    }

    /**
     * get the x-coordinate.
     *
     * @return the x-coordinate
     */
    public int getX() {
        return bounds.x;
    }

    /**
     * get the y-coordinate.
     *
     * @return the y-coordinate
     */
    public int getY() {
        return bounds.y;
    }

    /**
     * @return true if the TreeMapNode is a leaf
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * set the position and the size.
     *
     * @param bounds bounds
     */
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    /**
     * set the height.
     *
     * @param height la nouvelle valeur de height
     */
    public void setHeight(int height) {
        bounds.height = height;
    }

    /**
     * set the position.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public void setPosition(int x, int y) {
        bounds.x = x;
        bounds.y = y;
    }

    /**
     * set size.
     *
     * @param width the new width
     * @param height the new height
     */
    public void setSize(int width, int height) {
        bounds.width = width;
        bounds.height = height;
    }

    /**
     * set the Value.
     *
     * @param value the new value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * set the weight of the node and update the parents.
     *
     * @param weight the new weight
     */
    public void setWeight(double weight) {
        double newWeight = Math.abs(weight);
        if (parent != null) {
            parent.setWeight(parent.weight - this.weight + newWeight);
        }
        this.weight = newWeight;
    }

    /**
     * set the width.
     *
     * @param width la nouvelle valeur de width
     */
    public void setWidth(int width) {
        bounds.width = width;
    }

    /**
     * set the x-coordinate.
     *
     * @param x the new x-coordinate
     */
    public void setX(int x) {
        bounds.x = x;
    }

    /**
     * set the y-coordinate.
     *
     * @param y the new y-coordinate
     */
    public void setY(int y) {
        bounds.y = y;
    }

    /**
     * @return the parent
     */
    public TreeMapNode getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    protected void setParent(TreeMapNode parent) {
        this.parent = parent;
    }
}
