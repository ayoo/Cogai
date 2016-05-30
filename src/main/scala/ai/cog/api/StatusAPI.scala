package ai.cog.api
/**
  * Created by andyyoo on 30/04/16.
  */
trait StatusAPI extends BaseAPI {
  val statusRoutes =
    path("status") {
      get {
        complete("OK")
      }
  }
}
