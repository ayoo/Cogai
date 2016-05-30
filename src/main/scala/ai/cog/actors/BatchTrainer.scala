package ai.cog.actors

import ai.cog.actors.BatchTrainer.StartBatchTrain
import ai.cog.dao.{DatasetColumnDAO, DatasetDAO, JobDAO}
import ai.cog.ml.features.DataFrameHelper
import ai.cog.models.{Dataset, Job}
import ai.cog.utils.AppConfig
import akka.actor.{Actor, ActorRef, Props}
import org.apache.spark.SparkContext
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.SQLContext

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by andyyoo on 25/05/16.
  */
object BatchTrainer {
  def props(supervisor: ActorRef,
            sparkContext: SparkContext,
            jobDAO: JobDAO,
            datasetDAO: DatasetDAO with DatasetColumnDAO) = {
    Props(new BatchTrainer(supervisor, sparkContext, jobDAO, datasetDAO))
  }

  case class StartBatchTrain(job: Job, datasets: Seq[Dataset])

}

class BatchTrainer(supervisor: ActorRef,
                   sparkContext: SparkContext,
                   jobDAO: JobDAO,
                   datasetDAO: DatasetDAO with DatasetColumnDAO)
  extends Actor with DataFrameHelper{

  override def receive: Receive = {
    case StartBatchTrain(job, datasets) =>
      println("received StartBatchTrain message")
      startBatch(job, datasets)
  }

  /**
    *
    * @param job
    * @param datasets
    * @return
    */
  private def startBatch(job: Job, datasets: Seq[Dataset]) = {
    val datasetColumsMap = datasets.flatMap { dataset =>
      val columns = datasetDAO.findAllDatasetColumnsByDatasetId(dataset.id)
      Map(dataset -> columns)
    }

    val dataset = datasetColumsMap.head._1
    val datasetColumnsF = datasetColumsMap.head._2

    datasetColumnsF.map { datasetColumns =>
      val filename = dataset.filename
      val format = dataset.fileType
      val header = dataset.hasHeader.toString
      val s3AppKey = AppConfig.awsAccessKey
      val s3SecretKey = AppConfig.awsSecretKey
      val s3Bucket = AppConfig.s3Bucket
      val s3Path = s"s3n://$s3Bucket/$filename"

      sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", s3AppKey)
      sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", s3SecretKey)
      //val rdd = sparkContext.textFile(s"s3n://$s3Bucket/$filename")
      val sqlContext = new SQLContext(sparkContext)
      //import sqlContext.implicits._

      val dfReader = sqlContext.read.option("header", s"$header").option("inferSchema", "true")
      val df = format match {
        case "csv" => dfReader.format("com.databricks.spark.csv").load(s3Path)
        case "json" => dfReader.json(s3Path)
        case _ => throw new Exception(s"Not supported file format detected $format")
      }
      df.cache()
      df.printSchema()
      //df.show(20)
      df.describe().show()

      val selected = selectColumns(df, job)
      var newdf = createFeaturesAndLabel(selected, job.targetColumn, datasetColumns, needToScale = false)
      newdf.printSchema()

      val Array(trainingData, testData) = newdf.randomSplit(Array(0.8, 0.2))
      trainingData.cache()
      testData.cache()

      val rf = new RandomForestClassifier() // or DecisionTreeClassifier
        .setLabelCol("label")
        .setFeaturesCol("features")
        .setImpurity("gini")
        .setMaxBins(30)
        .setMaxDepth(10)
        .setNumTrees(20)

      /*
        val model = rf.fit(trainingData)
        val predictions = model.transform(testData)
        predictions.show(100)
        */

      val estimator = new Pipeline().setStages(Array(rf))
      val model = estimator.fit(trainingData)
      val predictions = model.transform(testData)

      //ROC
      val rocEval = new BinaryClassificationEvaluator().setLabelCol("label")
      val rocScore = rocEval.evaluate(predictions)
      println(s"Metric Name: area under ROC, score: $rocScore")

      //PR
      val prEval = new BinaryClassificationEvaluator().setLabelCol("label").setMetricName("areaUnderPR")
      val prScore = prEval.evaluate(predictions)
      println(s"Metric Name: area under PR, score: $prScore")

      /*
      val cv = new CrossValidator().setEstimator(estimator).setEvaluator(rocEval).setNumFolds(3)
      val paramGrid = new ParamGridBuilder()
        .addGrid(rf.maxDepth, Array(5,10,15,20))
        .addGrid(rf.numTrees, Array(5,10,15,20))
        .addGrid(rf.maxBins, Array(10,20,30,40))
        .build()
      cv.setEstimatorParamMaps(paramGrid)

      val cvModel = cv.fit(trainingData)

      val cvEval = new BinaryClassificationEvaluator().setLabelCol("label")
      val cvScore = cvEval.evaluate(cvModel.bestModel.transform(testData))
      val theClassifier = cvModel.bestModel.parent.asInstanceOf[Pipeline].getStages(0).asInstanceOf[RandomForestClassifier]
      val bestMaxDepth = theClassifier.getMaxDepth
      val bestNumTrees = theClassifier.getNumTrees
      val bestMaxBins = theClassifier.getMaxBins

      println(s"Metric Name: Cross Validated ROC, score: $cvScore")
      println(s"Max Dept: $bestMaxDepth, MaxBins: $bestMaxBins, NumTrees: $bestNumTrees")
      */
    }
  }

}