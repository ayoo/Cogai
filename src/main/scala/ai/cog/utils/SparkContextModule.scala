package ai.cog.utils
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by andyyoo on 21/05/16.
  */
trait SparkContextModule {
  this: Config =>

  val sparkConf = new SparkConf().setMaster(sparkMaster).setAppName(sparkAppName)
  val sparkContext = new SparkContext(sparkConf)
}
