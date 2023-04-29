package tradex.domain
package service
package live

import java.time.LocalDate
import zio.{ Chunk, Task, ZIO, ZLayer }
import zio.stream.{ ZPipeline, ZStream }
import zio.interop.catz.*
import zio.stream.interop.fs2z.*
import skunk.Session

import model.account.*
import model.trade.*
import model.execution.*
import model.order.*
import model.user.*
import repository.{ ExecutionRepository, OrderRepository }
import repository.live.ExecutionRepositorySQL
import resources.AppResources

final case class TradingServiceLive(session: Session[Task], orderRepository: OrderRepository) extends TradingService:

  override def generateTrades(date: LocalDate, userId: UserId): ZStream[Any, Throwable, Trade] =
    ZStream.fromZIO(session.prepare(ExecutionRepositorySQL.selectByExecutionDate)).flatMap { pq =>
      pq.stream(date, 512)
        .toZStream()
        .groupByKey(_.orderNo) { case (orderNo, executions) =>
          executions
            .via(executionsWithAccountNo)
            .via(trades(userId))
        }
    }

  private val executionsWithAccountNo: ZPipeline[Any, Throwable, Execution, (Execution, AccountNo)] =
    ZPipeline.mapChunksZIO((inputs: Chunk[Execution]) =>
      ZIO.foreach(inputs) { case exe =>
        orderRepository
          .query(exe.orderNo)
          .someOrFail(new Throwable(s"Order not found for order no ${exe.orderNo}"))
          .map(order => (exe, order.accountNo))
      }
    )

  private def trades(userId: UserId): ZPipeline[Any, Throwable, (Execution, AccountNo), Trade] =
    ZPipeline.mapChunksZIO((inputs: Chunk[(Execution, AccountNo)]) =>
      ZIO.foreach(inputs) { case (exe, accountNo) =>
        Trade
          .trade(
            accountNo,
            exe.isin,
            exe.market,
            exe.buySell,
            exe.unitPrice,
            exe.quantity,
            exe.dateOfExecution,
            valueDate = None,
            userId = Some(userId)
          )
          .map(Trade.withTaxFee)
      }
    )

object TradingServiceLive:
  val layer =
    ZLayer.scoped(for
      orderRepository <- ZIO.service[OrderRepository]
      appResources    <- ZIO.service[AppResources]
      session         <- appResources.postgres.toScopedZIO
    yield TradingServiceLive(session, orderRepository))
