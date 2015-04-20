package edu.berkeley.cs.succinct.sql

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{OneToOneDependency, Partition, TaskContext}

/**
 * A container RDD for results of a range search operation on [[SuccinctTableRDD]].
 *
 * @constructor Creates a [[RangeSearchResultsRDD]] from the underlying [[SuccinctTableRDD]], query, list of separators
 *              and the target storage level for the RDD.
 * @param succinctTableRDD The underlying [[SuccinctTableRDD]].
 * @param queryBegin The beginning of the query range.
 * @param queryEnd The end of the query range.
 * @param separators The list of separators.
 * @param schema The schema for the table.
 * @param targetStorageLevel The target storage level for the RDD.
 */
class RangeSearchResultsRDD(val succinctTableRDD: SuccinctTableRDD,
    val queryBegin: Array[Byte],
    val queryEnd: Array[Byte],
    val separators: Array[Byte],
    val schema: StructType,
    val targetStorageLevel: StorageLevel = StorageLevel.MEMORY_ONLY)
  extends RDD[Row](succinctTableRDD.context, List(new OneToOneDependency(succinctTableRDD))) {

  /** Overrides the compute method in RDD to return an iterator over the search results. */
  override def compute(split: Partition, context: TaskContext): Iterator[Row] = {
    succinctTableRDD.getFirstParent
      .iterator(split, context)
      .next
      .recordRangeSearch(queryBegin, queryEnd)
      .asInstanceOf[Array[Array[Byte]]]
      .iterator
      .map(SuccinctSerializer.deserializeRow(_, separators, schema))
  }

  /**
   * Returns the partitions for the RDD.
   *
   * @return The array of partitions.
   */
  override def getPartitions: Array[Partition] = succinctTableRDD.partitions
}