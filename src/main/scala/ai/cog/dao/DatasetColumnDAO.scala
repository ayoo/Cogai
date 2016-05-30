package ai.cog.dao

import ai.cog.models.DatasetColumn
import ai.cog.models.Tables.SlickTableDefinitions
import ai.cog.utils.{DBConfig, MySqlConfig}

import scala.concurrent.Future
/**
  * Created by andyyoo on 19/05/16.
  */
trait DatasetColumnDAO extends SlickTableDefinitions{

  this: DBConfig =>
  import driver.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    *
    * @param did
    * @return
    */
  def findAllDatasetColumnsByDatasetId(did: Option[Long]) : Future[Seq[DatasetColumn]] = {
    db.run(datasetColumnQuery.filter(_.datasetId === did).result)
  }

  /**
    *
    * @param id
    * @return
    */
  def findDatasetById(id: Option[Long]) : Future[Option[DatasetColumn]] = {
    db.run(datasetColumnQuery.filter(_.id === id).result.headOption)
  }

  /**
    *
    * @param column
    * @return
    */
  def createDatasetColumn(column: DatasetColumn) : Future[Option[Long]] = {
    db.run(datasetColumnQuery returning datasetColumnQuery.map(_.id) += column)
  }

  /**
    *
    * @param columns
    * @param did
    * @return
    */
  def createDatasetColumns(did: Option[Long], columns: Seq[DatasetColumn]): Future[Option[Int]] = {
    if (columns.isEmpty) {
      Future(None)
    } else {
      val columsWithDatasetId = columns.zipWithIndex.map {
        case (column, index) => column.copy(datasetId=did, sortOrder=Some(index))
      }
      db.run(datasetColumnQuery ++= columsWithDatasetId)
    }

  }

  /**
    *
    * @param id
    * @param updatingColumn
    * @return
    */
  def updateDatasetColumn(id: Option[Long], updatingColumn: DatasetColumn): Future[Int] = {
    db.run(datasetColumnQuery.filter(_.id === id).update(updatingColumn))
  }


  /**
    *
    * @param id
    * @return
    */
  def deleteDatasetColumn(id: Option[Long]) : Future[Int] = {
    db.run(datasetColumnQuery.filter(_.id === id).delete)
  }
}

//Mysql providers
trait DatasetColumnMySqlDAO extends DatasetColumnDAO with MySqlConfig
class DatasetColumnMySqlDAOImpl extends DatasetColumnMySqlDAO


// Postgres providers

// H2 provicers