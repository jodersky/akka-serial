package com.github.jodersky.flow

import akka.io.PipelineContext
import akka.io.SymmetricPipePair
import akka.io.SymmetricPipelineStage
import akka.util.ByteString
import java.nio.ByteOrder
import scala.annotation.tailrec
import java.nio.ByteBuffer

class DelimitedFrame(
  StartByte: Byte,
  StopByte: Byte,
  EscapeByte: Byte)
  //byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN)
  extends SymmetricPipelineStage[PipelineContext, ByteString, ByteString] {

  // range checks omitted ...

  override def apply(ctx: PipelineContext) =
    new SymmetricPipePair[ByteString, ByteString] {
      var buffer = ByteString.empty
      //implicit val byteOrder = DelimitedFrame.this.byteOrder

      sealed trait State
      case object Waiting extends State
      case object Accepting extends State
      case object Escaping extends State

      def extractFrame(bs: ByteString, accepted: ByteString, state: State): (ByteString, Option[ByteString]) = { //(remaining, frame))
        if (bs.isEmpty && state == Waiting) (ByteString.empty, None)
        else if (bs.isEmpty) (accepted, None)
        else {
          val in = bs.head

          state match {
            case Waiting if (in == StartByte) => extractFrame(bs.tail, accepted, Accepting)
            case Escaping => extractFrame(bs.tail, accepted ++ ByteString(in), Accepting)
            case Accepting => in match {
              case EscapeByte => extractFrame(bs.tail, accepted, Escaping)
              case StartByte => extractFrame(bs.tail, ByteString.empty, Accepting)
              case StopByte => (bs.tail, Some(accepted))
              case other => extractFrame(bs.tail, accepted ++ ByteString(other), Accepting)
            }
            case _ => extractFrame(bs.tail, accepted, state)
          }
        }
      }

      def extractFrames(bs: ByteString, accepted: List[ByteString]): (ByteString, List[ByteString]) = {
        val (remainder, frame) = extractFrame(bs, ByteString.empty, Waiting)

        frame match {
          case None => (remainder, accepted)
          case Some(data) => extractFrames(remainder, data :: accepted)
        }
      }

      /*
       * This is how commands (writes) are transformed: calculate length
       * including header, write that to a ByteStringBuilder and append the
       * payload data. The result is a single command (i.e. `Right(...)`).
       */
      override def commandPipeline = { bs: ByteString =>
        val bb = ByteString.newBuilder

        def escape(b: Byte) = {
          bb += EscapeByte
          bb += b
        }

        bb += StartByte
        for (b <- bs) {
          b match {
            case StartByte => escape(b)
            case StopByte => escape(b)
            case EscapeByte => escape(b)
            case _ => bb += b
          }
        }
        bb += StopByte

        ctx.singleCommand(bb.result)
      }

      /*
       * This is how events (reads) are transformed: append the received
       * ByteString to the buffer (if any) and extract the frames from the
       * result. In the end store the new buffer contents and return the
       * list of events (i.e. `Left(...)`).
       */
      override def eventPipeline = { bs: ByteString =>
        val data = buffer ++ bs
        val (remainder, frames) = extractFrames(data, Nil)
        buffer = remainder

        frames match {
          case Nil => Nil
          case one :: Nil ⇒ ctx.singleEvent(one)
          case many ⇒ many reverseMap (Left(_))
        }
      }
    }
}