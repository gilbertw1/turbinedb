package com.sparcedge.turbine.blade.query

import net.liftweb.json._
import com.mongodb.casbah.query.Imports._
import com.sparcedge.turbine.blade.query.cache.Event

object Reduce {
	def reReduce(results: Iterable[ReducedResult]): ReducedResult = {
		results.head.reducer match {
			case "max" => ReducerFunctions.MAX_REREDUCE(results)
			case "min" => ReducerFunctions.MIN_REREDUCE(results)
			case "avg" => ReducerFunctions.AVG_REREDUCE(results)
			case "sum" => ReducerFunctions.SUM_REREDUCE(results)
			case "count" => ReducerFunctions.COUNT_REREDUCE(results)
		}
	}
}

class Reduce (reducers: Option[List[Reducer]], filter: Option[Map[String,JObject]]) {

	implicit val formats = Serialization.formats(NoTypeHints)
	val filters = filter.getOrElse(Map[String,JValue]()) map { case (segment, value) => 
		new Match(segment, value.extract[Map[String,JValue]])
	}
	val reducerList = reducers.getOrElse(List[Reducer]())
}

class Reducer (val propertyName: String, val reducer: String, val segment: String) {

	def createReduceFunction(): (Iterable[Event]) => ReducedResult = {
	    reducer match {
			case "max" => ReducerFunctions.MAX(segment, _:Iterable[Event])
			case "min" => ReducerFunctions.MIN(segment, _:Iterable[Event])
			case "avg" => ReducerFunctions.AVG(segment, _:Iterable[Event])
			case "sum" => ReducerFunctions.SUM(segment, _:Iterable[Event])
			case "count" => ReducerFunctions.COUNT(segment, _:Iterable[Event])
	    }
	}

	def createReducedResult(): ReducedResult = {
		new ReducedResult(segment, reducer, Some(propertyName))
	}

	val reduceFunction = createReduceFunction()
}

class ReducedResult (val segment: String, val reducer: String, var output: Option[String], var value: Double = 0.0, var count: Int = 0) {
	val streamingReduceFunction = reducer match {
		case "max" => ReducerFunctions.MAX_STREAMING(_:Double, _:Int, _:Option[Any])
		case "min" => ReducerFunctions.MIN_STREAMING(_:Double, _:Int, _:Option[Any])
		case "avg" => ReducerFunctions.AVG_STREAMING(_:Double, _:Int, _:Option[Any])
		case "sum" => ReducerFunctions.SUM_STREAMING(_:Double, _:Int, _:Option[Any])
		case "count" => ReducerFunctions.COUNT_STREAMING(_:Double, _:Int, _:Option[Any])
	}

	def apply(event: Event) {
		val (newValue,newCount) = streamingReduceFunction(value, count, event(segment))
		value = newValue
		count = newCount
	}
}