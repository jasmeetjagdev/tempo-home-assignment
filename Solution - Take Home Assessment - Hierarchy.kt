import kotlin.test.*

// The task:
// 1. Read and understand the Hierarchy data structure described in this file.
// 2. Implement filter() function.
// 3. Implement more test cases.
//
// The task should take 30-90 minutes.
//
// When assessing the submission, we will pay attention to:
// - correctness, efficiency, and clarity of the code;
// - the test cases.

/**
 * A `Hierarchy` stores an arbitrary _forest_ (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * Parent-child relationships are identified by the position in the array and the associated depth.
 * Each tree root has depth 0, its children have depth 1 and follow it in the array, their children have depth 2 and follow them, etc.
 *
 * Example:
 * ```
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths:  0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * ```
 *
 * the forest can be visualized as follows:
 * ```
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 *```
 * 1 is a parent of 2 and 5, 2 is a parent of 3, etc. Note that depth is equal to the number of hyphens for each node.
 *
 * Invariants on the depths array:
 *  * Depth of the first element is 0.
 *  * If the depth of a node is `D`, the depth of the next node in the array can be:
 *      * `D + 1` if the next node is a child of this node;
 *      * `D` if the next node is a sibling of this node;
 *      * `d < D` - in this case the next node is not related to this node.
 */
interface Hierarchy {
  /** The number of nodes in the hierarchy. */
  val size: Int

  /**
   * Returns the unique ID of the node identified by the hierarchy index. The depth for this node will be `depth(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun nodeId(index: Int): Int

  /**
   * Returns the depth of the node identified by the hierarchy index. The unique ID for this node will be `nodeId(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun depth(index: Int): Int

  fun formatString(): String {
    return (0 until size).joinToString(
      separator = ", ",
      prefix = "[",
      postfix = "]"
    ) { i -> "${nodeId(i)}:${depth(i)}" }
  }
}

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate and all of its ancestors pass it as well.
 */
fun Hierarchy.filter(nodeIdPredicate: (Int) -> Boolean): Hierarchy {
	// Initialize the filtered node ids and depths list
    val filteredNodeIds: MutableList<Int> = mutableListOf()
	val filteredDepths: MutableList<Int> = mutableListOf()

    // Example Data
    //intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
    //intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))

    //intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
    //intArrayOf(0, 1, 2, 3, 3, 4, 5, 4, 5, 1, 2))

    // Iterate through the hierarchy and apply the filter
    var i = 0
    while (i < size){
      // Check if the predicate is satisfied then add the value to the filtered lists
    if (nodeIdPredicate(nodeId(i))){
        filteredNodeIds.add(nodeId(i))
        filteredDepths.add(depth(i))

    }else {
        // if the predicate is not satisfied, then we skip all the children of that node.
        // The identification of the childer is by comparing the depth value.
        // If the depth value is greater than the depth of the current node, then it is a child of that node.
        // We keep skipping until we find a node with depth less than or equal to the current node.
        // This works because the list already has the elements in DFS order,
        // so all the children of a node will be right next to each other.
        val root: Int = i
        while( i+1 < size && depth(i+1)>depth(root)){
            i++; // skip all the children, their siblings etc.
        }
    }
      // Increment the loop counter
      i++;
  }

  return ArrayBasedHierarchy(filteredNodeIds.toIntArray(),
      filteredDepths.toIntArray())
	  
}

// Dry run to understand the filter function with the example data.

// Example Data
//intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
//intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))

//1 - add to filtered
//2 - add to filtered
//3 - do not add
//	go to its children, keep checking with filter condition
//	4 - skip
//5 - add to filtered
//6 - do not add
//	7 - skip
//8 - add to filtered
//9 - do not add
//	no children
//10 - add to filtered
//11 - add to filtered

class ArrayBasedHierarchy(
  private val myNodeIds: IntArray,
  private val myDepths: IntArray,
) : Hierarchy {
  override val size: Int = myDepths.size

  override fun nodeId(index: Int): Int = myNodeIds[index]

  override fun depth(index: Int): Int = myDepths[index]
}

class FilterTest {
  @Test
  fun testFilter() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
    val filteredExpected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 5, 8, 10, 11),
      intArrayOf(0, 1, 1, 0, 1, 2))
    assertEquals(filteredExpected.formatString(), filteredActual.formatString())
  }
  
	/*
     * 1
     * - 2
     * - - 3
     * - - - 4
     * - - - 5
     * - - - 6
     */
    @Test
    fun testFilter_with_siblings_of_children_all_excluded() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3, 4, 5, 6),
            intArrayOf(0, 1, 2, 3, 3, 3))
        val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
        val filteredExpected: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2),
            intArrayOf(0, 1))
        assertEquals(filteredExpected.formatString(), filteredActual.formatString())
    }

    /*
     * 1
     * - 2
     * - - 3
     * - - 4
     * - - 5
     * - - 6
     */
    @Test
    fun testFilter_with_siblings_of_ancestors_selectively_included() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 3, 4, 5, 6),
            intArrayOf(0, 1, 2, 2, 2, 2))
        val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
        val filteredExpected: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(1, 2, 4, 5),
            intArrayOf(0, 1, 2, 2))
        assertEquals(filteredExpected.formatString(), filteredActual.formatString())
    }

    @Test
    fun testFilter_with_empty_tree() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(),
            intArrayOf())
        val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
        val filteredExpected: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(),
            intArrayOf())
        assertEquals(filteredExpected.formatString(), filteredActual.formatString())
    }

	/*
     * 3
	 */
    @Test
    fun testFilter_with_root_tree_not_included) {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(3),
            intArrayOf(0))
        val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
        val filteredExpected: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(),
            intArrayOf())
        assertEquals(filteredExpected.formatString(), filteredActual.formatString())
    }

	/*
     * 2
	 */
    @Test
    fun testFilter_with_root_tree_included() {
        val unfiltered: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(2),
            intArrayOf(0))
        val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
        val filteredExpected: Hierarchy = ArrayBasedHierarchy(
            intArrayOf(2),
            intArrayOf(0))
        assertEquals(filteredExpected.formatString(), filteredActual.formatString())
    }
}

