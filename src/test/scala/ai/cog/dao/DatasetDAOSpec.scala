package ai.cog.dao

import ai.cog.BaseDaoSpec

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by andyyoo on 17/05/16.
  */
class DatasetDAOSpec extends BaseDaoSpec {

  before {
    datasetDao.reloadSchema("datasets")
  }

  "DatasetDao" should {
    "insert a new dataset" in {
      val created = createNewDataset(buildNewDataset(name="new dataset"))
      created.name should be === "new dataset"
    }
    "update the existing dataset" in {
      val created = createNewDataset(buildNewDataset(name="another dataset"))
      val updated = buildNewDataset(id=created.id, name="updated dataset")
      val result = Await.result(datasetDao.updateDataset(created.id.get, updated), 1.seconds)
      result should be === 1
    }
    "update the existing dataset with columns" in {
      val created = createNewDataset(buildNewDataset(name="another dataset"))
      createNewDatasetColumns(created.id, List(buildNewDatasetColumn(name="new col1")))
      val updated = buildNewDataset(id=created.id, name="updated dataset")
      val createdCols= Await.result(datasetColumnDao.findAllDatasetColumnsByDatasetId(created.id), 1.seconds)
      val updatedCols = createdCols.map { col => col.copy(name="updated col")}
      val result = Await.result(datasetDao.updateDatasetWithColumns(created.id.get, updated, updatedCols), 1.seconds)
    }
    "retreive the exiting dataset" in {
      val created = createNewDataset(buildNewDataset(name="exiting dataset"))
      val found = Await.result(datasetDao.findDatasetById(created.id.get), 1.seconds).getOrElse(throw new Exception("Something went wrong"))
      found.name should be === "exiting dataset"
    }
    "delete the existing dataset" in {
      val created = createNewDataset(buildNewDataset(name="existing dataset"))
      val result = Await.result(datasetDao.deleteDataset(created.id.get), 1.seconds)
      result should be === 1
    }
  }

  after {
    //datasetServiceDao.db.close()
  }
}
