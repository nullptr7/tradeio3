package tradex.domain
package transport

import kantan.csv.RowDecoder
import kantan.csv.java8.*
import model.frontOfficeOrder.FrontOfficeOrder
import cellCodecs.{ given, * }
import kantan.csv.HeaderCodec
import tradex.domain.model.account.AccountNo
import java.time.Instant
import tradex.domain.model.instrument.ISINCode
import tradex.domain.model.order.Quantity
import tradex.domain.model.instrument.UnitPrice
import tradex.domain.model.order.BuySell

object frontOfficeOrderT:
  given RowDecoder[FrontOfficeOrder] = RowDecoder.decoder(0, 1, 2, 3, 4, 5)(FrontOfficeOrder.apply)
  given HeaderCodec[FrontOfficeOrder] =
    HeaderCodec.codec[AccountNo, Instant, ISINCode, Quantity, UnitPrice, BuySell, FrontOfficeOrder](
      "Account No",
      "Order Date",
      "ISIN Code",
      "Quantity",
      "Unit Price",
      "Buy/Sell"
    )(FrontOfficeOrder(_, _, _, _, _, _))(fo => (fo.accountNo, fo.date, fo.isin, fo.qty, fo.unitPrice, fo.buySell))
