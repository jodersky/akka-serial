package com.github.jodersky.flow

import akka.io.PipelineContext
import akka.io.SymmetricPipePair
import akka.io.SymmetricPipelineStage
import akka.util.ByteString
import java.nio.ByteOrder
import scala.annotation.tailrec
import java.nio.ByteBuffer
import akka.io._
import akka.actor.{IO=>_,_}
import Serial._

object test {

  val StartByte = 0: Byte                         //> StartByte  : Byte = 0
  val StopByte = 1: Byte                          //> StopByte  : Byte = 1
  val EscapeByte = 2: Byte                        //> EscapeByte  : Byte = 2
	
	val ctx = new  PipelineContext{}          //> ctx  : akka.io.PipelineContext = com.github.jodersky.flow.test$$anonfun$main
                                                  //| $1$$anon$1@32bf7190

  val stages = new DelimitedFrame(StartByte, StopByte, EscapeByte)
                                                  //> stages  : com.github.jodersky.flow.DelimitedFrame = com.github.jodersky.flow
                                                  //| .DelimitedFrame@36ff057f

  val PipelinePorts(cmd, evt, mgmt) = PipelineFactory.buildFunctionTriple(ctx, stages)
                                                  //> cmd  : akka.util.ByteString => (Iterable[akka.util.ByteString], Iterable[akk
                                                  //| a.util.ByteString]) = <function1>
                                                  //| evt  : akka.util.ByteString => (Iterable[akka.util.ByteString], Iterable[akk
                                                  //| a.util.ByteString]) = <function1>
                                                  //| mgmt  : PartialFunction[AnyRef,(Iterable[akka.util.ByteString], Iterable[akk
                                                  //| a.util.ByteString])] = <function1>
  
  val injector = PipelineFactory.buildWithSinkFunctions(ctx, stages)(
		t => println("sent command: " + t), // will receive messages of type Try[ByteString]
		t => println("got event: " + t) // will receive messages of type Try[Message]
	)                                         //> injector  : akka.io.PipelineInjector[akka.util.ByteString,akka.util.ByteStri
                                                  //| ng] = akka.io.PipelineFactory$$anon$5@70cb6009
 
 
 		val bs = ByteString.fromArray(Array(0,4,2,1,1,6,1))
                                                  //> bs  : akka.util.ByteString = ByteString(0, 4, 2, 1, 1, 6, 1)
                     
		injector.injectCommand(bs)        //> sent command: Success(ByteString(0, 2, 0, 4, 2, 2, 2, 1, 2, 1, 6, 2, 1, 1))
		injector.injectEvent(bs)          //> got event: Success(ByteString(4, 1))
		
		implicit val system = ActorSystem("flow")
                                                  //> system  : akka.actor.ActorSystem = akka://flow|
  //IO(Serial) ! Open("s", 9600)

}