package ai.cog.ml.features

import ai.cog.models.{DatasetColumn, Job}
import org.apache.spark.ml.feature.{OneHotEncoder, StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.mllib.linalg.{DenseVector, SparseVector}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{TimestampType, _}

/**
  * Created by andyyoo on 29/05/16.
  */
trait DataFrameHelper {

  /**
    *
    * @param df
    * @param cols
    * @return
    */
  def countUniqueValues(df: DataFrame, cols: Seq[String]): Seq[Long] = {
    cols.map(df.select(_).distinct().count())
  }

  /**
    *
    * @param columns
    * @return
    */
  def buildDataSchema(columns: Seq[DatasetColumn]): StructType = {
    StructType(columns.map { column =>
      StructField(column.name, resolveDataType(column.dataType), true)
    })
  }

  /**
    *
    * @param df
    * @param columns
    * @return
    */
  def buildColumnsAndLabel(df: DataFrame, labelColumn: String, columns: Seq[DatasetColumn]): DataFrame = {
    val nameColumnMap: Map[String, DatasetColumn] = columns.map(d => d.name -> d).toMap
    df.schema.foldLeft(df)((prevDf, field) => {
      val name = field.name
      val col = nameColumnMap.get(name).get
      if (name == labelColumn) {
        createLabel(prevDf, name)
      } else if (col.isCategorical.getOrElse(false)) {
        val uniqCnt = countUniqueValues(prevDf, Seq(name)).head
        val indexedDf = indexStringColumns(prevDf, Seq(name))
        if(uniqCnt > 2) oneHotEncodeColumns(indexedDf, Seq(name)) else indexedDf
      } else
        toNewDataType(prevDf, field, col.dataType)
    })
  }

  /**
    *
    * @param df
    * @param job
    * @return
    */
  def selectColumns(df: DataFrame, job: Job): DataFrame = {
    val colsToDrop = job.excludedColumns.getOrElse("").split(",") //:+ job.targetColumn
    colsToDrop.foldLeft(df)((prevDf, colName) => prevDf.drop(colName))
  }

  /**
    *
    * @param df
    * @param columns
    * @param needToScale
    * @return
    */
  def createFeaturesAndLabel(df: DataFrame, labelCol: String, columns: Seq[DatasetColumn], needToScale: Boolean = true): DataFrame = {
    var newdf = df
    try {
      newdf = buildColumnsAndLabel(newdf, labelCol, columns)
      newdf = assembleFeatures(newdf, excludedCols = Seq(labelCol, "label"))
      newdf = if (needToScale) scaleFeatures(newdf) else newdf
    } catch { case (ex) => println(ex)}

    newdf
  }

  /**
    *
    * @param df
    * @param labelCol
    * @return
    */
  def createLabel(df: DataFrame, labelCol: String): DataFrame = {
    indexStringColumns(df, Seq(labelCol), Some("label"))
  }

  /**
    *
    * @param df
    * @param current
    * @param desired
    * @return
    */
  def toNewDataType(df: DataFrame, current: StructField, desired: String) = {
    var newdf = df
    val name = current.name
    val desiredType = resolveDataType(desired)
    if (current.dataType != desiredType) {
      newdf = newdf.withColumn(name + "-tmp", newdf.col(name).cast(desiredType))
        .drop(name)
        .withColumnRenamed(name+"-tmp", name)
    }
    newdf
  }

  /**
    *
    * @param df
    * @param cols
    * @return
    */
  def indexStringColumns(df: DataFrame, cols: Seq[String], newName: Option[String] = None): DataFrame = {
    cols.foldLeft(df)((prevDf, col)=> {
      val si = new StringIndexer().setInputCol(col).setOutputCol(col+"-idx")
      val model = si.fit(prevDf)
      if(newName.isDefined) {
        model.transform(prevDf).withColumnRenamed(col+"-idx", newName.get)
      } else {
        model.transform(prevDf).drop(col).withColumnRenamed(col+"-idx", col)
      }
    })
  }

  /**
    *
    * @param df
    * @param cols
    * @param dropLast
    * @return
    */
  def oneHotEncodeColumns(df: DataFrame, cols: Seq[String], dropLast: Boolean = true): DataFrame = {
    var newdf = df
    cols.foreach { col =>
      val oh = new OneHotEncoder().setInputCol(col).setOutputCol(col+"-ohe").setDropLast(dropLast)
      newdf = oh.transform(newdf).drop(col).withColumnRenamed(col+"-ohe", col)
    }
    newdf
  }

  /**
    *
    * @param df
    * @param excludedCols
    * @return
    */
  def assembleFeatures(df: DataFrame, excludedCols:Seq[String]=Seq[String]("label")): DataFrame = {
    var newdf = df
    val featureCols = df.columns.diff(excludedCols)
    val assembler = new VectorAssembler().setInputCols(featureCols).setOutputCol("features")
    val assembledDf = assembler.transform(newdf)
    assembledDf
  }

  /**
    *
    * @param df
    * @param withMean
    * @param withStd
    * @param useDenseVector
    * @return
    */
  def scaleFeatures(df: DataFrame, withMean: Boolean=true, withStd: Boolean=true, useDenseVector: Boolean=true): DataFrame = {
    var newdf = df
    if (useDenseVector) {
      val toDenseVector = udf[DenseVector, SparseVector](_.toDense)
      val denseDf = newdf.withColumn("features-dense", toDenseVector(newdf("features")))
      newdf = denseDf.drop("features").withColumnRenamed("features-dense", "features")
    }
    val scaler = new StandardScaler().setWithMean(withMean).setWithStd(withStd).setInputCol("features").setOutputCol("features-scaled")
    val model = scaler.fit(newdf)
    newdf = model.transform(newdf).drop("features")
    newdf = newdf.withColumnRenamed("features-scaled", "features")
    newdf
  }

  /**
    *
    * @param origin
    * @return
    */
  def toReadableCsvHeader(origin: String): String = {
    origin.split("_").map(_.capitalize).mkString(" ")
  }

  /**
    *
    * @param dataType
    * @return
    */
  def resolveDataType(dataType: String): DataType = {
    dataType match {
      case "StringType" => StringType
      case "DoubleType" => DoubleType
      case "BooleanType" => BooleanType
      case "IntegerType" => IntegerType
      case "FloatType" => FloatType
      case "LongType" => LongType
      case "NulllType" => NullType
      case "DateType" => DateType
      case "TimestampType" => TimestampType
      case _ => throw new Exception(s"Not supported Data type, $dataType")
    }
  }

}
