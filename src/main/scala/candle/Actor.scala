package candle

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString
import org.joda.time.Instant

import scala.concurrent.duration._

/**
  * Tcp client actor
  *
  * @param remote   socker address
  * @param listener listener actor
  */
class Client(remote: InetSocketAddress, listener: ActorRef) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive: Receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self

    case c@Connected(remote, local) =>
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! "write failed"
        case Received(data) =>
          listener ! data
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! "connection closed"
          context stop self
      }
  }
}

/**
  * State actor, contains candles (history data)
  */
class State extends Actor {
  def add(values: List[Order]): Receive = {
    case m: Order =>
      context.become(add(m +: values))
    case ("last", instant: Instant, actor: ActorRef) => sender ! (actor, JsonFormatters.format(Aggregate.init(values, instant)))
    case ("min", instant: Instant) => sender ! JsonFormatters.format(Aggregate.lastMinute(values, instant))
  }

  def receive: Receive = add(List())
}

/**
  * Client registry actor, contains actorRefs of all clients
  *
  * @param state state actorRef
  */
class ClientRegistry(state: ActorRef) extends Actor {
  def add(values: List[ActorRef], instant: Instant): Receive = {
    case m: ActorRef =>
      context.become(add(m +: values, instant))
    case "tick" =>
      val current = new Instant()
      val minute = DateUtils.truncateToMinute(current)
      if (minute != DateUtils.truncateToMinute(instant)) {
        state ! ("min", minute)
      }

      context.become(add(values, current))
    case res: String =>
      values.foreach(x => x ! Write(ByteString(res)))
  }

  def receive: Receive = add(List(), new Instant())
}

/**
  * Server actor
  *
  * @param address        address
  * @param port           port
  * @param clientRegistry clientRegistry actor
  * @param state          state actor
  */
class Server(address: String, port: Int, clientRegistry: ActorRef, state: ActorRef) extends Actor {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress(address, port))

  def receive: Receive = {
    case (a: ActorRef, s: String) => a ! Write(ByteString(s))

    case b@Bound(localAddress) =>

    case CommandFailed(_: Bind) => context stop self

    case c@Connected(remote, local) =>
      val handler: ActorRef = ActorSystem().actorOf(Props(new SimpleClient()))
      val connection = sender()
      connection ! Register(handler)
      state ! ("last", DateUtils.truncateToMinute(new Instant()), connection)
      clientRegistry ! connection
  }
}

/**
  * Ticker client actor
  *
  * @param address address
  * @param port    port
  * @param state   state actorRef
  */
class TickerClient(address: String, port: Int, state: ActorRef) extends Actor {

  val client = context.actorOf(Props(new Client(new InetSocketAddress(address, port), self)))

  def receive: Receive = {
    case byteString: ByteString =>
      val model: Option[Order] = BinaryConverter.convert(byteString.toArray)
      model.foreach(x => state ! x)
  }
}

/**
  * Simple tcp client
  */
class SimpleClient() extends Actor {

  import Tcp._

  def receive: Receive = {
    case Received(data) => sender() ! {
      Write(data)
    }

    case PeerClosed => context stop self
  }
}

/**
  * App
  */
object Main extends App {
  val port = ConfigUtils.config().getInt("application.port")
  val address = ConfigUtils.config().getString("application.address")

  val state = ActorSystem().actorOf(Props(new State()))

  val clientRegistry = ActorSystem().actorOf(Props(new ClientRegistry(state)))

  ActorSystem().actorOf(Props(new Server(address, port, clientRegistry, state)))

  val tickerPort = ConfigUtils.config().getInt("ticker.port")
  val tickerAddress = ConfigUtils.config().getString("ticker.address")

  ActorSystem().actorOf(Props(new TickerClient(tickerAddress, tickerPort, state)))

  ActorSystem().scheduler.schedule(1 second, 1 seconds, clientRegistry, "tick")(ExecutionPool.createExecutionContext())
}
