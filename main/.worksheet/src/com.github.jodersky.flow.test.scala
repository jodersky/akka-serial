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

object test {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(351); 

  val StartByte = 0: Byte;System.out.println("""StartByte  : Byte = """ + $show(StartByte ));$skip(25); 
  val StopByte = 1: Byte;System.out.println("""StopByte  : Byte = """ + $show(StopByte ));$skip(27); 
  val EscapeByte = 2: Byte;System.out.println("""EscapeByte  : Byte = """ + $show(EscapeByte ));$skip(36); 
	
	val ctx = new  PipelineContext{};System.out.println("""ctx  : akka.io.PipelineContext = """ + $show(ctx ));$skip(68); 

  val stages = new DelimitedFrame(StartByte, StopByte, EscapeByte);System.out.println("""stages  : com.github.jodersky.flow.DelimitedFrame = """ + $show(stages ));$skip(88); 

  val PipelinePorts(cmd, evt, mgmt) = PipelineFactory.buildFunctionTriple(ctx, stages);System.out.println("""cmd  : akka.util.ByteString => (Iterable[akka.util.ByteString], Iterable[akka.util.ByteString]) = """ + $show(cmd ));System.out.println("""evt  : akka.util.ByteString => (Iterable[akka.util.ByteString], Iterable[akka.util.ByteString]) = """ + $show(evt ));System.out.println("""mgmt  : PartialFunction[AnyRef,(Iterable[akka.util.ByteString], Iterable[akka.util.ByteString])] = """ + $show(mgmt ));$skip(243); 
  
  val injector = PipelineFactory.buildWithSinkFunctions(ctx, stages)(
		t => println("sent command: " + t), // will receive messages of type Try[ByteString]
		t => println("got event: " + t) // will receive messages of type Try[Message]
	);System.out.println("""injector  : akka.io.PipelineInjector[akka.util.ByteString,akka.util.ByteString] = """ + $show(injector ));$skip(59); 
 
 
 		val bs = ByteString.fromArray(Array(0,4,2,1,1,6,1));System.out.println("""bs  : akka.util.ByteString = """ + $show(bs ));$skip(51); 
                     
		injector.injectCommand(bs);$skip(27); 
		injector.injectEvent(bs);$skip(47); 
		
		implicit val system = ActorSystem("flow");System.out.println("""system  : akka.actor.ActorSystem = """ + $show(system ))}
  //IO(Serial) ! Open("s", 9600)

}
