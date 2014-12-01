package de.kp.spark.recom.api
/* Copyright (c) 2014 Dr. Krusche & Partner PartG
* 
* This file is part of the Spark-Recom project
* (https://github.com/skrusche63/spark-recom).
* 
* Spark-Recom is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* Spark-Recom is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* Spark-Recom. 
* 
* If not, see <http://www.gnu.org/licenses/>.
*/

import org.apache.spark.SparkContext

import akka.actor.{ActorRef,ActorSystem,Props}
import akka.pattern.ask

import akka.util.Timeout

import spray.http.StatusCodes._

import spray.routing.{Directives,HttpService,RequestContext,Route}

import scala.concurrent.{ExecutionContext}
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt

import scala.util.parsing.json._

import de.kp.spark.core.model._
import de.kp.spark.core.rest.RestService

import de.kp.spark.recom.Configuration
import de.kp.spark.recom.model._

import de.kp.spark.recom.actor.{RecomMaster}

class RestApi(host:String,port:Int,system:ActorSystem,@transient val sc:SparkContext) extends HttpService with Directives {

  implicit val ec:ExecutionContext = system.dispatcher  
  import de.kp.spark.core.rest.RestJsonSupport._
  
  override def actorRefFactory:ActorSystem = system
  
  val (duration,retries,time) = Configuration.actor   
  val master = system.actorOf(Props(new RecomMaster(sc)), name="recom-master")
 
  def start() {
    RestService.start(routes,system,host,port)
  }

  private def routes:Route = {

    path("build" / Segment) {subject =>
	  post {
	    respondWithStatus(OK) {
	      ctx => doBuild(ctx,subject)
	    }
	  }
    }  ~ 
    path("index" / Segment) {subject =>  
	  post {
	    respondWithStatus(OK) {
	      ctx => doIndex(ctx,subject)
	    }
	  }
    }  ~ 
    path("predict" / Segment) {subject => 
	  post {
	    respondWithStatus(OK) {
	      ctx => doPredict(ctx,subject)
	    }
	  }
    }  ~ 
    path("recommend" / Segment) {subject => 
	  post {
	    respondWithStatus(OK) {
	      ctx => doRecommend(ctx,subject)
	    }
	  }
    }  ~ 
    path("register" / Segment) {subject =>  
	  post {
	    respondWithStatus(OK) {
	      ctx => doRegister(ctx,subject)
	    }
	  }
    }  ~ 
    path("status") {
	  post {
	    respondWithStatus(OK) {
	      ctx => doStatus(ctx)
	    }
	  }
    }  ~ 
    path("track" / Segment) {subject => 
	  post {
	    respondWithStatus(OK) {
	      ctx => doTrack(ctx,subject)
	    }
	  }
    }  ~  
    pathPrefix("web") {
      /*
       * 'web' is the prefix for static public content that is
       * served from a web browser and provides a minimalistic
       * web UI for this prediction server
       */
      implicit val actorContext = actorRefFactory
      get {
	    respondWithStatus(OK) {
	      getFromResourceDirectory("public")
	    }
      }
    }
  }

  /**
   * 'build' describes the initial step of creating a recommender model;
   * the subsequent step (not invoked by the REST API) comprises training 
   * with previously prepared data. Training is initiated through the Akka
   * remote service that interacts with the user preference service
   */
  private def doBuild[T](ctx:RequestContext,subject:String) = {
    
    val service = "rating"
      
    subject match {
      /* 
       * Build a recommender model based on an event related
       * data source
       */
      case Topics.EVENT => doRequest(ctx,service,"build:event")
      /*
       * Build a recommender model based on an item related
       * data source
       */
      case Topics.ITEM => doRequest(ctx,service,"build:item")
      
      case _ => {/* do nothing */}
      
    }
    
  }

  /**
   * 'index' describes an administration request to create an
   * Elasticsearch index either for events or items; this request
   * must be performed before tracking of events or items can be
   * started
   */
  private def doIndex[T](ctx:RequestContext,subject:String) = {
    
    val service = ""
      
    subject match {
      /* 
       * Prepare an Elasticsearch index to describe events;
       * an event specifies a certain user engagemen event
       * and is used to compute an implicit user rating 
       * before any ALS or CAR based model building can
       * be started
       */
      case Topics.EVENT => doRequest(ctx,service,"index:event")
      /* 
       * Prepare an Elasticsearch index to describe items;
       * an item is a certain order o purchase item and is 
       * used to discover association rules or ALS based
       * model building
       */
      case Topics.ITEM => doRequest(ctx,service,"index:item")
      
      case _ => {/* do nothing */}
      
    }
  }
  
  /**
   * 'predict' describes requests to retrieve predictions either 
   * from event or item based models
   */
  private def doPredict[T](ctx:RequestContext,subject:String) = {
    
    val service = ""
      
    subject match {
      /* 
       * Get recommendations (predictions) from event-based
       * recommender models
       */
      case Topics.EVENT => doRequest(ctx,service,"predict:event")
      /* 
       * Get recommendations (predictions) from item-based
       * recommender models
       */
      case Topics.ITEM => doRequest(ctx,service,"predict:item")
      
      case _ => {/* do nothing */}
      
    }
    
  }
  /**
   * 'recommend' describes requests to retrieve recommendations 
   * either from event or item based models
   */
  private def doRecommend[T](ctx:RequestContext,subject:String) = {
    
    val service = ""
      
    subject match {
      /* 
       * Get recommendations (predictions) from event-based
       * recommender models
       */
      case Topics.EVENT => doRequest(ctx,service,"recommend:event")
      /* 
       * Get recommendations (predictions) from item-based
       * recommender models
       */
      case Topics.ITEM => doRequest(ctx,service,"recommend:item")
      
      case _ => {/* do nothing */}
      
    }
    
  }
  
  /**
   * 'register' describes an administration request to persist
   * meta data descriptions either for event based or item based
   * data sources. These metadata descriptions are used to map
   * internal and external field specifications
   */
  private def doRegister[T](ctx:RequestContext,subject:String) = {
    
    val service = ""
      
    subject match {
      /* 
       * Register the metadata specification for an event based
       * data source; the specification is persisted in a Redis
       * instance and will be used to adequately access a data
       * soucre
       */
      case Topics.EVENT => doRequest(ctx,service,"register:event")
      /*
       * Register the metadata specification for an item based
       * data source; the specification is persisted in a Redis
       * instance and will be used to adequately access a data
       * soucre
       */
      case Topics.ITEM => doRequest(ctx,service,"register:item")
      
      case _ => {/* do nothing */}
      
    }
  }
  /**
   * 'status' is an administration request to determine whether
   * a certain data mining or knowledge building task has been
   * finished or not; the only parameter required for status
   * requests is the unique identifier of a certain task
   */
  private def doStatus[T](ctx:RequestContext) = {
    
    val service = ""
    doRequest(ctx,service,"status")
  
  }
  
  /**
   * 'track' describes a request to register a single 'event' 
   * or 'item' for later used; the indexing functionality is
   * done by the recommender and NOT delegated to the respective
   * service
   */
  private def doTrack[T](ctx:RequestContext,subject:String) = {
    
    val service = ""
      
    subject match {
      /* 
       * Track a single event; this event is registered in an 
       * Elasticsearch index and will be used for preference
       * building as well as for training factorization models
       */
      case Topics.EVENT => doRequest(ctx,service,"track:event")
      /*
       * Track a single item; this item is registered in an 
       * Elasticsearch index and will used for preference
       * building as well as for data mining (association
       * rules) and training and ALS model
       */
      case Topics.ITEM => doRequest(ctx,service,"track:item")
      
      case _ => {/* do nothing */}
      
    }
    
  }
  
  private def doRequest[T](ctx:RequestContext,service:String,task:String) = {
     
    val request = new ServiceRequest(service,task,getRequest(ctx))
    implicit val timeout:Timeout = DurationInt(time).second
    
    val response = ask(master,request) 
      response.onSuccess {
        case result => {
          
          if (result.isInstanceOf[Preferences]) {
            /*
             * This is the response type used for 'predict' and 
             * also 'recommend' requests that refer to the ALS 
             * or ASR algorithms 
             */
            ctx.complete(result.asInstanceOf[Preferences])
          
          } else if (result.isInstanceOf[TargetedPoint]) {
            /*
             * This is the response type used for 'predict'
             * requests that refer to the CAR algorithm
             */
            ctx.complete(result.asInstanceOf[ServiceResponse])
            
          } else if (result.isInstanceOf[ServiceResponse]) {
            /*
             * This is the common response type used for almost
             * all requests
             */
            ctx.complete(result.asInstanceOf[ServiceResponse])
            
          }
          
        }
      
      }

      response.onFailure {
        case throwable => ctx.complete(throwable.getMessage)
      }
    
    ctx.complete(response)
    
  }

  private def getHeaders(ctx:RequestContext):Map[String,String] = {
    
    val httpRequest = ctx.request
    
    /* HTTP header to Map[String,String] */
    val httpHeaders = httpRequest.headers
    
    Map() ++ httpHeaders.map(
      header => (header.name,header.value)
    )
    
  }
 
  private def getBodyAsMap(ctx:RequestContext):Map[String,String] = {
   
    val httpRequest = ctx.request
    val httpEntity  = httpRequest.entity    

    val body = JSON.parseFull(httpEntity.data.asString) match {
      case Some(map) => map
      case None => Map.empty[String,String]
    }
      
    body.asInstanceOf[Map[String,String]]
    
  }
  
  private def getRequest(ctx:RequestContext):Map[String,String] = {

    val headers = getHeaders(ctx)
    val body = getBodyAsMap(ctx)
    
    headers ++ body
    
  }

}