package ai.cog.dao

import ai.cog.BaseDaoSpec
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by andyyoo on 19/05/16.
  */
class DatasetColumnDAOSpec extends BaseDaoSpec {

  before {
    datasetDao.reloadSchema("datasets")
    datasetColumnDao.reloadSchema("dataset_columns")
  }

  "DatasetColumnDAO" should {
    "insert a list of dataset columns with dataset_id" in {
      val dataset = createNewDataset(buildNewDataset(name="new test dataset"))
      val colList = List(
        buildNewDatasetColumn(name="col1", dataType = "StrinngType", datasetId = dataset.id),
        buildNewDatasetColumn(name="col2", dataType = "FloatType", datasetId = dataset.id),
        buildNewDatasetColumn(name="col3", dataType = "StrinngType", datasetId = dataset.id),
        buildNewDatasetColumn(name="col4", dataType = "FloatType", datasetId = dataset.id)
      )
      val result = Await.result(datasetColumnDao.createDatasetColumns(dataset.id, colList), 1.seconds).getOrElse(assert(false))
      result should be === 4
    }
    "find a list of dataset columns with the given dataset id " in {
      val dataset = createNewDataset(buildNewDataset(name="new test dataset"))
      createNewDatasetColumns(dataset.id, List(
                buildNewDatasetColumn(name="col1", dataType = "StrinngType", datasetId = dataset.id),
                buildNewDatasetColumn(name="col2", dataType = "FloatType", datasetId = dataset.id),
                buildNewDatasetColumn(name="col3", dataType = "BooleanType", datasetId = dataset.id)
              ))

      val result = Await.result(datasetColumnDao.findAllDatasetColumnsByDatasetId(dataset.id), 1.seconds)
      result.head.name should be === "col1"
      result.tail.head.dataType should be === "FloatType"
      result.last.sortOrder.get should be === 2
    }
    "find a single dataset column with dataset column id" in {
      val dataset = createNewDataset(buildNewDataset(name="new test dataset"))
      val datasetColumnIdOpt = createNewDatasetColumn(buildNewDatasetColumn(name="colA", dataType = "StrinngType", datasetId = dataset.id))
      val result = Await.result(datasetColumnDao.findDatasetById(datasetColumnIdOpt), 1.seconds).get
      result.name should be === "colA"
    }
    "update a single dataset column with dataset column id" in {
      val dataset = createNewDataset(buildNewDataset(name="new test dataset"))
      val theColumn = buildNewDatasetColumn(name="colA", dataType = "StrinngType", datasetId = dataset.id)
      val datasetColumnIdOpt = createNewDatasetColumn(theColumn)
      val updatingColumn = theColumn.copy(id=datasetColumnIdOpt, name="updated colA")
      Await.result(datasetColumnDao.updateDatasetColumn(datasetColumnIdOpt, updatingColumn), 1.seconds)
      val result = Await.result(datasetColumnDao.findDatasetById(datasetColumnIdOpt), 1.seconds).get
      result.name should be === "updated colA"
    }
    "delete a single dataset column with dataset column id " in {
      val dataset = createNewDataset(buildNewDataset(name="new test dataset"))
      val datasetColumnIdOpt = createNewDatasetColumn(buildNewDatasetColumn(name="colA", dataType = "StringType", datasetId = dataset.id))
      Await.result(datasetColumnDao.deleteDatasetColumn(datasetColumnIdOpt), 1.seconds)
      val result = Await.result(datasetColumnDao.findDatasetById(datasetColumnIdOpt), 1.seconds)
      result should be === None
    }
  }
}
