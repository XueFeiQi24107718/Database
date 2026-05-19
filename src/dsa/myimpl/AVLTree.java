package dsa.myimpl;

import dsa.iface.IPosition;
import dsa.impl.BinarySearchTree;

@SuppressWarnings("unchecked")
public class AVLTree<T extends Comparable<T>> extends BinarySearchTree<T> {

    private int height(BTPosition n) {
        if (n == null || isExternal((IPosition<T>) n)) {
            return -1;
        }
        return ((AVLPosition) n).height;
    }

    private int getBalance(BTPosition n) {
        if (n == null || isExternal((IPosition<T>) n)) {
            return 0;
        }
        return height(n.left) - height(n.right);
    }

    private void refresh(BTPosition n) {
        if (n == null || isExternal((IPosition<T>) n)) {
            return;
        }
        // add the height
        ((AVLPosition) n).height = 1 + Math.max(height(n.left), height(n.right));
    }

    private void rightRotate(BTPosition y) {
        BTPosition p = y.parent;
        BTPosition x = y.left;
        BTPosition b = x.right;
        x.parent = p;
        if (p == null) {
            root = x;
        } else if (y == p.left) {
            p.left = x;
        } else {
            p.right = x;
        }
        x.right = y;
        y.parent = x;
        y.left = b;
        if (b != null) {
            b.parent = y;
        }
        refresh(y);
        refresh(x);
    }

    private void leftRotate(BTPosition x) {
        BTPosition p = x.parent;
        BTPosition y = x.right;
        BTPosition b = y.left;
        y.parent = p;
        if (p == null) {
            root = y;
        } else if (x == p.left) {
            p.left = y;
        } else {
            p.right = y;
        }
        y.left = x;
        x.parent = y;
        x.right = b;
        if (b != null) {
            b.parent = x;
        }
        refresh(x);
        refresh(y);
    }

    /**
     * Walks upward from {@code from}, refreshing heights and applying AVL rotations until the root.
     */
    private void rebalance(BTPosition from) {
        BTPosition z = from;
        while (z != null) {
            BTPosition par = z.parent; // save the parent "reference"
            refresh(z);
            int bf = getBalance(z);
            if (bf > 1) {
                if (getBalance(z.left) < 0) {
                    leftRotate(z.left); // LR(first rotation)
                }
                rightRotate(z); // LR(second rotation) LL(only one rotation)
            } else if (bf < -1) {
                if (getBalance(z.right) > 0) {
                    rightRotate(z.right); // RL(first rotation)
                }
                leftRotate(z); // RL(second rotation) RR(only one rotation)
            }
            z = par;
        }
    }

    /*
     * Implement tri-node restructuring with {@code x}, the parent of {@code x} and the grandparent of {@code x}.
     */
    private void restructure(IPosition<T> x) {
        BTPosition xp = (BTPosition) x;
        BTPosition y = xp.parent;
        if (y == null) {
            return;
        }
        BTPosition z = y.parent;
        if (z == null) {
            return;
        }
        boolean yIsLeft = (y == z.left);
        boolean xIsLeft = (xp == y.left);
        if (yIsLeft && xIsLeft) {
            rightRotate(z);
        } else if (yIsLeft) {
            leftRotate(y);
            rightRotate(z);
        } else if (!xIsLeft) {
            leftRotate(z);
        } else {
            rightRotate(y);
            leftRotate(z);
        }
    }

    @Override
    public boolean contains(T element) {
        if (isEmpty()) {
            return false;
        }
        BTPosition p = (BTPosition) find(root(), element);
        // (defensive coding) double confirm
        return isInternal((IPosition<T>) p) && p.element().compareTo(element) == 0;
    }

    @Override
    // boolean means we can know whether the code "insert" successfully
    public boolean insert(T element) { // can only insert when it's external

        //two special situations:
        if (!isEmpty()) {
            BTPosition pos = (BTPosition) find(root(), element);
            if (isInternal((IPosition<T>) pos)) { //if the element already exits
                return false; // can not insert
            }
        }
        if (isEmpty()) {
            root = newPosition((T) null, null);
            // turn the external node into real node by adding two external nodes
            expandExternal((BTPosition) root, element);
            refresh((BTPosition) root); // update the height
            return true; // insert successfully
        }

        BTPosition leaf = (BTPosition) find(root(), element);
        expandExternal(leaf, element);
        refresh(leaf); // update the height
        rebalance(leaf.parent);
        return true; // insert successfully
    }


    /** Inorder successor: leftmost internal node in {@code z}'s right subtree ( {@code z.right} has external left). */
    private BTPosition rightSubtreeMinimum(BTPosition z) {
        BTPosition w = z.right;
        while (isInternal((IPosition<T>) w.left)) {
            w = w.left;
        }
        return w;
    }

    @Override
    public boolean remove(T element) {
        if (isEmpty()) {
            return false;
        }
        BTPosition target = (BTPosition) find(root(), element);
        if (isExternal((IPosition<T>) target)) {
            return false;
        }

        BTPosition rebalanceFrom;

        if (isInternal((IPosition<T>) target.left) && isInternal((IPosition<T>) target.right)) {
            BTPosition succ = rightSubtreeMinimum(target);
            replace((IPosition<T>) target, succ.element());
            BTPosition succParent = succ.parent;
            remove((IPosition<T>) succ);
            rebalanceFrom = succParent;
        } else {
            rebalanceFrom = target.parent;
            remove((IPosition<T>) target);
        }

        if (rebalanceFrom == null && root != null) {
            rebalanceFrom = (BTPosition) root;
        }
        rebalance(rebalanceFrom);
        return true;
    }

    @Override
    protected BTPosition newPosition(T element, BTPosition parent) {
        return new AVLPosition(element, parent);
    }

    /**
     * Define a subclass of BTPosition so that we can also store the height
     * of each position in its object.
     */
    class AVLPosition extends BTPosition {
        public int height = 0;
        public AVLPosition(T element, BTPosition parent) {
            super(element, parent);
        }
    }
}