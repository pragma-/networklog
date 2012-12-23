/**
 * Implements an AA-tree.
 * @author Based upon implemention by Mark Allen Weiss
 */

// Utility class to pool Strings without expensive allocations

package com.googlecode.networklog;

public class CharArrayStringAATree
{
  /**
   * Construct the tree.
   */
  public CharArrayStringAATree( )
  {
    nullNode = new AANode( null, null, null );
    nullNode.left = nullNode.right = nullNode;
    nullNode.level = 0;
    root = nullNode;
  }

  public int size = 0;

  /**
   * Insert into the tree.
   * @param x the item to insert.
   * @return the item inserted or the value of the existing item
   */
  public String insert( String x )
  {
    root = insert( x, root );
    return result;
  }

  /**
   * Insert into the tree.
   * @param x the item to insert.
   * @return the item inserted or the value of the existing item
   */
  public String insert( CharArray x )
  {
    root = insert( x, root );
    return result;
  }

  /**
   * Remove from the tree.
   * @param x the item to remove.
   * @throws ItemNotFoundException if x is not found.
   */
  public void remove( String x ) throws ItemNotFoundException
  {
    deletedNode = nullNode;
    root = remove( x, root );
  }

  /**
   * Find an item in the tree.
   * @param x the item to search for.
   * @return the matching item of null if not found.
   */
  public String find( String x )
  {
    AANode current = root;
    nullNode.element = x;

    for( ; ; )
    {
      if( x.compareTo( current.element ) < 0 )
        current = current.left;
      else if( x.compareTo( current.element ) > 0 ) 
        current = current.right;
      else if( current != nullNode )
        return current.element;
      else
        return null;
    }
  }

  /**
   * Make the tree logically empty.
   */
  public void clear( )
  {
    root = nullNode;
    size = 0;
  }

  /**
   * Test if the tree is logically empty.
   * @return true if empty, false otherwise.
   */
  public boolean isEmpty( )
  {
    return root == nullNode;
  }

  /**
   * Internal method to insert into a subtree.
   * Sets {@link result} to the value of the existing or newly inserted object
   * @param x the item to insert.
   * @param t the node that roots the tree.
   * @return the new root.
   */
  private AANode insert( CharArray x, AANode t )
  {
    if( t == nullNode ) {
      size++;
      t = new AANode( x.toString(), nullNode, nullNode );
      result = t.element;
    } else if( x.compareTo( t.element ) < 0 ) {
      t.left = insert( x, t.left );
    } else if( x.compareTo( t.element ) > 0 ) {
      t.right = insert( x, t.right );
    } else {
      result = t.element;
      return t;
    }
    t = skew( t );
    t = split( t );
    return t;
  }

  /**
   * Internal method to insert into a subtree.
   * Sets {@link result} to the value of the existing or newly inserted object
   * @param x the item to insert.
   * @param t the node that roots the tree.
   * @return the new root.
   */
  private AANode insert( String x, AANode t )
  {
    if( t == nullNode ) {
      t = new AANode( x, nullNode, nullNode );
      result = t.element;
    } else if( x.compareTo( t.element ) < 0 ) {
      t.left = insert( x, t.left );
    } else if( x.compareTo( t.element ) > 0 ) {
      t.right = insert( x, t.right );
    } else {
      result = t.element;
      return t;
    }
    t = skew( t );
    t = split( t );
    return t;
  }

  /**
   * Internal method to remove from a subtree.
   * @param x the item to remove.
   * @param t the node that roots the tree.
   * @return the new root.
   * @throws ItemNotFoundException if x is not found.
   */
  private AANode remove( String x, AANode t ) throws ItemNotFoundException
  {
    if( t != nullNode )
    {
      // Step 1: Search down the tree and set lastNode and deletedNode
      lastNode = t;
      if( x.compareTo( t.element ) < 0 )
        t.left = remove( x, t.left );
      else
      {
        deletedNode = t;
        t.right = remove( x, t.right );
      }

      // Step 2: If at the bottom of the tree and
      //         x is present, we remove it
      if( t == lastNode )
      {
        if( deletedNode == nullNode || x.compareTo( deletedNode.element ) != 0 ) {
          throw new ItemNotFoundException( x.toString( ) );
        } else {
          deletedNode.element = t.element;
          t = t.right;
        }
      }

      // Step 3: Otherwise, we are not at the bottom; rebalance
      else
        if( t.left.level < t.level - 1 || t.right.level < t.level - 1 )
        {
          if( t.right.level > --t.level )
            t.right.level = t.level;
          t = skew( t );
          t.right = skew( t.right );
          t.right.right = skew( t.right.right );
          t = split( t );
          t.right = split( t.right );
        }
    }
    return t;
  }

  /**
   * Skew primitive for AA-trees.
   * @param t the node that roots the tree.
   * @return the new root after the rotation.
   */
  private static  AANode skew( AANode t )
  {
    if( t.left.level == t.level )
      t = rotateWithLeftChild( t );
    return t;
  }

  /**
   * Split primitive for AA-trees.
   * @param t the node that roots the tree.
   * @return the new root after the rotation.
   */
  private static  AANode split( AANode t )
  {
    if( t.right.right.level == t.level )
    {
      t = rotateWithRightChild( t );
      t.level++;
    }
    return t;
  }

  /**
   * Rotate binary tree node with left child.
   */
  private static  AANode rotateWithLeftChild( AANode k2 )
  {
    AANode k1 = k2.left;
    k2.left = k1.right;
    k1.right = k2;
    return k1;
  }

  /**
   * Rotate binary tree node with right child.
   */
  private static  AANode rotateWithRightChild( AANode k1 )
  {
    AANode k2 = k1.right;
    k1.right = k2.left;
    k2.left = k1;
    return k2;
  }

  private static class AANode
  {
    // Constructors
    AANode( String theElement, AANode lt, AANode rt )
    {
      element = theElement;
      left    = lt;
      right   = rt;
      level   = 1;
    }

    String element; // The data in the node
    AANode left;    // Left child
    AANode right;   // Right child
    int level;      // Level
  }

  private AANode root;
  private AANode nullNode;

  private AANode deletedNode;
  private AANode lastNode;
  public String result;
}
