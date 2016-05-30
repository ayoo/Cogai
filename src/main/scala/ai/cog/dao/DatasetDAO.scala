package ai.cog.dao

import ai.cog.models.{Dataset, DatasetColumn}
import ai.cog.models.Tables.SlickTableDefinitions
import ai.cog.utils.{DBConfig, MySqlConfig}

import scala.concurrent.Future

/**
  * Created by andyyoo on 16/05/16.
  */

trait DatasetDAO extends SlickTableDefinitions {

  this: DBConfig =>

  import driver.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    *
    * @return
    */
  def findAllDatasets(): Future[Seq[Dataset]] = db.run(datasetQuery.result)

  /**
    *
    * @param id
    * @return
    */
  def findDatasetById(id: Long): Future[Option[Dataset]] = db.run(datasetQuery.filter(_.id === id).result.headOption)

  /**
    *
    * @param dataset
    * @return
    */
  def createDataset(dataset: Dataset): Future[Option[Dataset]] =
    db.run(datasetQuery returning datasetQuery.map(_.id) += dataset).flatMap { id => findDatasetById(id.get) }

  /**
    *
    * @param id
    * @param updated
    * @return
    */
  def updateDataset(id: Long, updated: Dataset): Future[Int] = db.run(datasetQuery.filter(_.id === id).update(updated))

  /**
    *
    * @param id
    * @param columns
    * @return
    */
  def updateDatasetWithColumns(id: Long, dataset: Dataset, columns: Seq[DatasetColumn]) = {
    val updateActions = DBIO.seq(
      datasetQuery.filter(_.id === id).update(dataset) +:
      columns.map { column =>
        datasetColumnQuery.filter(_.id === column.id).update(column)
      } : _*
    )
    db.run(updateActions.transactionally).map(result => 1) // to return Int
  }

  /**
    *
    * @param id
    * @return
    */
  def deleteDataset(id: Long): Future[Int] = db.run(datasetQuery.filter(_.id === id).delete)
}

// MySQL providers
trait DatasetMySqlDAO extends DatasetDAO with MySqlConfig
class DatasetMySqlDAOImpl extends DatasetMySqlDAO

// Postgres providers

// H2 provicers
