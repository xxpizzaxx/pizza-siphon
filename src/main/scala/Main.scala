import com.beimin.eveapi.core.ApiAuthorization
import com.beimin.eveapi.corporation.assetlist.AssetListParser
import com.beimin.eveapi.corporation.locations.LocationsParser
import com.beimin.eveapi.eve.character.CharacterLookupParser
import com.typesafe.config.ConfigFactory
import dispatch._, Defaults._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val conf = ConfigFactory.load()
  val keyid = conf.getInt("keyId")
  val vcode = conf.getString("vCode")
  val authapikey = conf.getString("pingauthkey")

  val auth = new ApiAuthorization(keyid, vcode)
  val assetparser = AssetListParser.getInstance()
  val locationparser = LocationsParser.getInstance()
  val characternameparser = CharacterLookupParser.getId2NameInstance()
  val assets = assetparser.getResponse(auth).getAll.asScala

  // filter out silos
  val SILO_ID = 14343
  val COUPLING_ARRAY_ID = 17982
  val ids = Seq(SILO_ID, COUPLING_ARRAY_ID)
  // get towers with siphons on
  val (siphoned, notSiphoned) = assets.filter{a => ids.contains(a.getTypeID)}.partition{_.getAssets.asScala.exists(_.getQuantity%100!=0)}
  // grab their locations
  val siphonedLocations = siphoned.map{_.getLocationID}
  // look up the locations
  val systemsWithSiphonsIn = characternameparser.getResponse(siphonedLocations.mkString(",")).getAll.asScala.map{_.getName}
  // report
  val report = "There may be siphons present in the following systems: %s".format(systemsWithSiphonsIn.mkString(", "))
  val ping = url("https://auth.pizza.moe/apiv1/ping/siphons")
    .POST
    .setParameters(Map("message" -> Seq(report), "sender" -> Seq("siphon_bot")))
    .setHeader("apikey", authapikey)
  val response = Http(ping OK as.String).either
  Await.result(response, 10.seconds)
  println(response.either)
  sys.exit(0)
}
