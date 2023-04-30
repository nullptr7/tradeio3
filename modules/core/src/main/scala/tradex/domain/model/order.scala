package tradex.domain
package model

import zio.prelude._

import instrument._
import account._
import java.time.LocalDateTime

object order {
  object OrderNo extends Newtype[String]:
    given Equal[OrderNo] = Equal.default

  type OrderNo = OrderNo.Type

  extension (ono: OrderNo)
    def validateNo: Validation[String, OrderNo] =
      if (OrderNo.unwrap(ono).size > 12 || OrderNo.unwrap(ono).size < 5)
        Validation.fail(s"OrderNo cannot be more than 12 characters or less than 5 characters long")
      else Validation.succeed(ono)

  object Quantity extends Subtype[BigDecimal]:
    override inline def assertion = Assertion.greaterThan(BigDecimal(0))

  type Quantity = Quantity.Type

  enum BuySell(val entryName: NonEmptyString):
    case Buy extends BuySell(NonEmptyString("buy"))
    case Sell extends BuySell(NonEmptyString("sell"))

  final case class LineItem private (
      orderNo: OrderNo,
      isin: ISINCode,
      quantity: Quantity,
      unitPrice: UnitPrice,
      buySell: BuySell
  )

  final case class Order private (
      no: OrderNo,
      date: LocalDateTime,
      accountNo: AccountNo,
      items: NonEmptyList[LineItem]
  )

  object Order:
    def make(
        no: OrderNo,
        orderDate: LocalDateTime,
        accountNo: AccountNo,
        items: NonEmptyList[LineItem]
    ): Order = Order(no, orderDate, accountNo, items)

  object LineItem:
    def make(
        orderNo: OrderNo,
        isin: ISINCode,
        quantity: Quantity,
        unitPrice: UnitPrice,
        buySell: BuySell
    ): LineItem = LineItem(orderNo, isin, quantity, unitPrice, buySell)
}
