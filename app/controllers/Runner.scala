package controllers

import java.util.UUID

import com.mle.cert.{CertMaker, CertRequestInfo}
import com.mle.concurrent.ExecutionContexts.cached
import com.mle.play.json.JsonStrings.EVENT
import com.mle.play.ws.{JsonWebSockets, SyncAuth, WebSocketClient}
import com.mle.util.Log
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Call, Controller, RequestHeader}
import rx.lang.scala.Subscription

import scala.collection.concurrent.TrieMap

/**
 * @author Michael
 */
object Runner extends Controller with JsonWebSockets with SyncAuth with Log {
  val tasks = TrieMap.empty[UUID, CertRequestInfo]
  val CMD = "cmd"
  val UUID = "uuid"
  val LINE = "line"
  val RESULT = "result"
  val ERROR = "error"
  override type Client = WebSocketClient
  override type AuthSuccess = String
  val subscriptions = TrieMap.empty[WebSocketClient, Subscription]

  def run = Action(implicit req => Ok(views.html.run(None, Seq.empty)))

  override def clients: Seq[Client] = subscriptions.keys.toSeq

  override def authenticate(implicit req: RequestHeader): Option[AuthSuccess] = Some("test")

  override def openSocketCall: Call = routes.Runner.openSocket

  override def onMessage(msg: Message, client: Client): Boolean = {
    def send(msg: Message) = client.channel push msg
    (msg \ CMD).validate[String].flatMap {
      case UUID =>
        (msg \ UUID).validate[UUID].map(uuid => {
          val task = tasks remove uuid
          task.fold(log.warn(s"Unknown UUID: $uuid"))(req => {
            val progress = CertMaker.run(req)
            val sub = progress.output.subscribe(next => send(lineJson(next)))
            progress.result.map(r => if (r.isSuccess) filesJson else errorJson(r.exitValue)).foreach(json => {
              if (!sub.isUnsubscribed) {
                send(json)
              }
            })
            subscriptions += client -> sub
            writeLog(client, s"subscribed. Subscriptions in total: ${subscriptions.size}")
          })
        })
    }.isSuccess
  }

  def lineJson(line: String): JsValue = Json.obj(EVENT -> LINE, LINE -> line)

  def filesJson = Json.obj(EVENT -> RESULT, RESULT -> CertMaker.meta)

  def errorJson(exitValue: Int) = Json.obj(EVENT -> ERROR, ERROR -> s"Abnormal exit: $exitValue.")

  override def newClient(authResult: AuthSuccess, channel: Channel[Message])(implicit request: RequestHeader): Client =
    WebSocketClient(authResult, channel, request)

  override def onConnect(client: Client): Unit =
    writeLog(client, "connected")

  override def onDisconnect(client: Client): Unit = {
    subscriptions.get(client).foreach(_.unsubscribe())
    subscriptions -= client
    writeLog(client, "disconnected")
  }

  protected def writeLog(client: Client, suffix: String): Unit =
    log.info(s"User: ${client.user} from: ${client.request.remoteAddress} $suffix.")
}
