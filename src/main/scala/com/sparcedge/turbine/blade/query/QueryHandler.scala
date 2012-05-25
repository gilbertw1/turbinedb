package com.sparcedge.turbine.blade.query

import akka.actor.Actor
import com.mongodb.casbah.query.Imports._
import net.liftweb.json.JsonDSL._

import net.liftweb.json._

import com.sparcedge.turbine.blade.mongo.MongoDBConnection

class QueryHandler(mongoConnection: MongoDBConnection) extends Actor {

    implicit val formats = Serialization.formats(NoTypeHints)
    
	var eventCache: EventCache = null

	def populateEventCache(query: TurbineAnalyticsQuery) = {
		val collection = mongoConnection.collection
		val q: MongoDBObject = 
			("ts" $gte query.blade.periodStart.getMillis $lt query.blade.periodEnd.getMillis) ++ 
			("d" -> new ObjectId(query.blade.domain)) ++
			("t" -> new ObjectId(query.blade.tenant)) ++
			("c" -> query.blade.category) 
		val fields = query.query.requiredFields.map(("dat." + _ -> 1)).foldLeft(MongoDBObject())(_ ++ _) ++ ("r" -> 1) ++ ("ts" -> 1)
		val cursor = collection.find(q, fields)
		cursor.batchSize(5000)
		eventCache = EventCache(cursor, query.blade.periodStart.getMillis, query.blade.periodEnd.getMillis)
		cursor.close()
	}

	def receive = {
		case HandleQuery(query) =>
			if(eventCache == null) {
				populateEventCache(query)
			}
			//println("EventCache: " + eventCache)
			val results = eventCache.applyQuery(query)
			val json = Map[String,Any](
				"results" -> results, 
				"qid" -> query.qid
			)
			println(Serialization.write(json))
		case _ =>
	}
}

case class Result (
	results: Iterable[String]
)

case class HandleQuery(query: TurbineAnalyticsQuery)

