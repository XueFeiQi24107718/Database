package dsa.testing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import dsa.iface.IBinarySearchTree;
import dsa.iface.IBinaryTree;
import dsa.iface.IPosition;
import dsa.impl.BinarySearchTree;
import dsa.myimpl.AVLTree;
import dsa.testers.BinaryTreeChecker;

/**
 * Structure and consistency tests for the AVL tree implementation.
 *
 * The tests include:
 * - insert, remove, and contains operations
 * - explicit LL, RR, LR, and RL rotation cases
 * - larger insertion/removal sequences
 * - consistency and shape checking using BinaryTreeChecker
 * - stress testing with many operations
 *
 * Expected tree shapes are compared using level-order arrays, following
 * the format described in the assignment handout.
 */
public class AVLTreeStructureTest {

   private static int failureCount; // number of failed checks

    // returns the internal keys in level-order traversal
   private static List<Integer> levelOrderInternalKeys( IBinaryTree<Integer> tree ) {
      List<Integer> out = new ArrayList<>();
      if ( tree.isEmpty() ) {
         return out;
      }
      ArrayDeque<IPosition<Integer>> q = new ArrayDeque<>();
      q.add( tree.root() );
      while ( !q.isEmpty() ) {
         IPosition<Integer> p = q.remove();
         if ( tree.isExternal( p ) ) {
            continue;
         }
         out.add( p.element() );
         if ( tree.hasLeft( p ) ) {
            q.add( tree.left( p ) );
         }
         if ( tree.hasRight( p ) ) {
            q.add( tree.right( p ) );
         }
      }
      return out;
   }

   // isEmpty() was acting weird for me sometimes so i check like this instead
   private static boolean hasNoInternalNodes( IBinarySearchTree<Integer> tree ) {
      return levelOrderInternalKeys( tree ).isEmpty();
   }

   private static void check( String label, IBinarySearchTree<Integer> tree ) {
      check( label, tree, new Integer[0] );
   }

   // helper for testing return values from insert/remove/contains
   private static void assertBoolean( String message, boolean actual, boolean expected ) {
      if ( actual != expected ) {
         failureCount++;
      }
      System.out.println( "   " + message + " -> " + actual + ( actual == expected ? "" : " (expected " + expected + ")" ) );
   }

    // checks tree consistency and compares against the expected structure
   private static void check( String label, IBinarySearchTree<Integer> tree, Integer... levelOrderExpected ) {
      boolean consistent = BinaryTreeChecker.isConsistent( tree );
      BinarySearchTree<Integer> expected = new BinarySearchTree<>( levelOrderExpected );
      boolean sameShape = BinaryTreeChecker.areEqual( tree, expected );
      if ( !consistent || !sameShape ) {
         failureCount++;
      }
      System.out.println( label );
      System.out.println( "   consistency: " + ( consistent ? "OK" : "FAIL" ) );
      System.out.println( "   shape match: " + ( sameShape ? "OK" : "FAIL" ) );
      if ( !sameShape ) {
         System.out.println( "   actual level-order: " + levelOrderInternalKeys( tree ) );
         System.out.println( "   expect level-order: " + java.util.Arrays.asList( levelOrderExpected ) );
      }
   }

    // separate tests for each AVL rotation type
   private static void explicitFourRotationTypes( IBinarySearchTree<Integer> prototype ) {
      System.out.println( "\n--- Explicit rotation types (LL, RR, LR, RL) ---" );
      Integer[] balancedThree = { 20, 10, 30 };

      IBinarySearchTree<Integer> ll = newTreeLike( prototype );
      assertBoolean( "LL insert 30", ll.insert( 30 ), true );
      assertBoolean( "LL insert 20", ll.insert( 20 ), true );
      assertBoolean( "LL insert 10", ll.insert( 10 ), true );
      check( "   LL (30,20,10) -> balanced", ll, balancedThree );

      IBinarySearchTree<Integer> rr = newTreeLike( prototype );
      assertBoolean( "RR insert 10", rr.insert( 10 ), true );
      assertBoolean( "RR insert 20", rr.insert( 20 ), true );
      assertBoolean( "RR insert 30", rr.insert( 30 ), true );
      check( "   RR (10,20,30) -> balanced", rr, balancedThree );

      IBinarySearchTree<Integer> lr = newTreeLike( prototype );
      assertBoolean( "LR insert 30", lr.insert( 30 ), true );
      assertBoolean( "LR insert 10", lr.insert( 10 ), true );
      assertBoolean( "LR insert 20", lr.insert( 20 ), true );
      check( "   LR (30,10,20) -> balanced", lr, balancedThree );

      IBinarySearchTree<Integer> rl = newTreeLike( prototype );
      assertBoolean( "RL insert 10", rl.insert( 10 ), true );
      assertBoolean( "RL insert 30", rl.insert( 30 ), true );
      assertBoolean( "RL insert 20", rl.insert( 20 ), true );
      check( "   RL (10,30,20) -> balanced", rl, balancedThree );
   }

   // make another empty tree of the same type as the one passed in (for the rotation tests)
   @SuppressWarnings( "unchecked" )
   private static IBinarySearchTree<Integer> newTreeLike( IBinarySearchTree<Integer> sample ) {
      try {
         return (IBinarySearchTree<Integer>) sample.getClass().getDeclaredConstructor().newInstance();
      }
      catch ( ReflectiveOperationException e ) {
         throw new IllegalStateException( "Tree class needs a public no-arg constructor.", e );
      }
   }

   // this gets called from main and probably from the autograder too
   public static void runAllTests( IBinarySearchTree<Integer> tree ) {
      failureCount = 0;
      System.out.println( "AVLTreeStructureTest.runAllTests — " + tree.getClass().getName() );

      //tests on empty trees
      System.out.println( "\n--- Empty tree ---" );
      if ( hasNoInternalNodes( tree ) ) {
         assertBoolean( "contains(1) on empty", tree.contains( 1 ), false );
         assertBoolean( "remove(1) on empty", tree.remove( 1 ), false );
         check( "   still empty", tree );
      }
      else {
         System.out.println( "   (skipped: tree already has nodes at start of runAllTests)" );
      }

       // single-node insertion and removal
      System.out.println( "\n--- Single node then clear ---" );
      if ( hasNoInternalNodes( tree ) ) {
         assertBoolean( "insert(100)", tree.insert( 100 ), true );
         check( "   one node", tree, 100 );
         assertBoolean( "contains(100)", tree.contains( 100 ), true );
         assertBoolean( "remove(100)", tree.remove( 100 ), true );
         assertBoolean( "contains(100) after remove", tree.contains( 100 ), false );
         check( "   empty again", tree );
      }
      else {
         System.out.println( "   (skipped: need empty tree)" );
      }

      // make sure insert returns false if already there and remove false if not found
      System.out.println( "\n--- insert / remove boolean results ---" );
      if ( hasNoInternalNodes( tree ) ) {
         assertBoolean( "insert(3)", tree.insert( 3 ), true );
         assertBoolean( "insert(3) duplicate", tree.insert( 3 ), false );
         check( "   unchanged", tree, 3 );
         assertBoolean( "remove(999) missing", tree.remove( 999 ), false );
         check( "   still {3}", tree, 3 );
         assertBoolean( "remove(3)", tree.remove( 3 ), true );
      }
      else {
         System.out.println( "   (skipped: need empty tree)" );
      }

      explicitFourRotationTypes( tree );

       // Insert a larger batch of keys so that deeper AVL rebalancing cases occur
      System.out.println( "\n--- Bulk insert (23 keys) ---" );
      int[] bulk = { 50, 25, 75, 12, 37, 62, 87, 6, 18, 30, 40, 55, 68, 80, 90, 5, 15, 28, 35, 22, 32, 38, 42 };
      for ( int k : bulk ) {
         assertBoolean( "insert(" + k + ")", tree.insert( k ), true );
      }
      check( "   after bulk insert", tree,
            35, 25, 50, 12, 30, 38, 75, 6, 18, 28, 32, 37, 40, 62, 87, 5, 15, 22, 42, 55, 68, 80, 90 );

      System.out.println( "\n--- contains ---" ); // search for something thats there and something thats not
      assertBoolean( "contains(35)", tree.contains( 35 ), true );
      assertBoolean( "contains(999)", tree.contains( 999 ), false );

      // delete a few different cases and see if shape still matches what i worked out
      System.out.println( "\n--- Removals with shape checks ---" );
      assertBoolean( "remove(5)", tree.remove( 5 ), true );
      check( "   after remove 5", tree,
            35, 25, 50, 12, 30, 38, 75, 6, 18, 28, 32, 37, 40, 62, 87, 15, 22, 42, 55, 68, 80, 90 );
      assertBoolean( "remove(35)", tree.remove( 35 ), true );
      check( "   after remove 35", tree,
            37, 25, 50, 12, 30, 40, 75, 6, 18, 28, 32, 38, 42, 62, 87, 15, 22, 55, 68, 80, 90 );
      assertBoolean( "remove(37)", tree.remove( 37 ), true );
      check( "   after remove 37", tree,
            38, 25, 50, 12, 30, 40, 75, 6, 18, 28, 32, 42, 62, 87, 15, 22, 55, 68, 80, 90 );

      // more inserts - skipped 32 and 50 cos they were already in the tree from the bulk part
      System.out.println( "\n--- Further inserts (44, 17, 78, 88, 48, 54, 29, 41, 19) ---" );
      assertBoolean( "insert(44)", tree.insert( 44 ), true );
      assertBoolean( "insert(17)", tree.insert( 17 ), true );
      assertBoolean( "insert(78)", tree.insert( 78 ), true );
      assertBoolean( "insert(88)", tree.insert( 88 ), true );
      assertBoolean( "insert(48)", tree.insert( 48 ), true );
      check( "   after … 48", tree,
            38, 25, 75, 15, 30, 50, 87, 12, 18, 28, 32, 42, 62, 80, 90, 6, 17, 22, 40, 44, 55, 68, 78, 88, 48 );
      assertBoolean( "insert(54)", tree.insert( 54 ), true );
      check( "   after 54", tree,
            38, 25, 75, 15, 30, 50, 87, 12, 18, 28, 32, 42, 62, 80, 90, 6, 17, 22, 40, 44, 55, 68, 78, 88, 48, 54 );
      assertBoolean( "insert(29)", tree.insert( 29 ), true );
      check( "   after 29", tree,
            38, 25, 75, 15, 30, 50, 87, 12, 18, 28, 32, 42, 62, 80, 90, 6, 17, 22, 29, 40, 44, 55, 68, 78, 88, 48, 54 );
      assertBoolean( "insert(41)", tree.insert( 41 ), true );
      check( "   after 41", tree,
            38, 25, 75, 15, 30, 50, 87, 12, 18, 28, 32, 42, 62, 80, 90, 6, 17, 22, 29, 40, 44, 55, 68, 78, 88, 41, 48, 54 );
      assertBoolean( "insert(19)", tree.insert( 19 ), true );
      check( "   after 19", tree,
            38, 25, 75, 15, 30, 50, 87, 12, 18, 28, 32, 42, 62, 80, 90, 6, 17, 22, 29, 40, 44, 55, 68, 78, 88, 19, 41, 48, 54 );

      // last thing: lots of insert/remove on a new tree, only check links are ok
      System.out.println( "\n--- Stress (consistency only) ---" );
      IBinarySearchTree<Integer> extra = newTreeLike( tree );
      for ( int i = 0; i < 400; i++ ) {
         if ( !extra.insert( i ) ) {
            failureCount++;
            System.out.println( "   stress insert(" + i + ") expected true" );
         }
      }
      for ( int i = 0; i < 400; i += 3 ) {
         if ( !extra.remove( i ) ) {
            failureCount++;
            System.out.println( "   stress remove(" + i + ") expected true" );
         }
      }
      boolean stressOk = BinaryTreeChecker.isConsistent( extra );
      if ( !stressOk ) {
         failureCount++;
      }
      System.out.println( "   consistency after stress: " + ( stressOk ? "OK" : "FAIL" ) );

      System.out.println( "\n=== " + ( failureCount == 0 ? "All checks passed." : ( failureCount + " failed check(s)." ) ) + " ===" );
   }

   public static void main( String[] args ) {
      runAllTests( new AVLTree<>() ); // change this if you want to test the library version instead
   }
}
