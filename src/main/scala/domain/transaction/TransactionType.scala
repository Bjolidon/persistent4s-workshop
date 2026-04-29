package domain.transaction

import io.circe.{Decoder, Encoder}
import skunk.Codec
import skunk.codec.`enum`.*
import skunk.data.Type

enum TransactionType derives Encoder, Decoder {
  case Deposit, Withdrawal, Transfer
}

object TransactionType:

  def label(t: TransactionType): String = t match
    case Deposit    => "deposit"
    case Withdrawal => "withdrawal"
    case Transfer   => "transfer"

  def fromLabel(label: String): Option[TransactionType] = label match
    case "deposit"    => Some(Deposit)
    case "withdrawal" => Some(Withdrawal)
    case "transfer"   => Some(Transfer)
    case _            => None

  val codec: Codec[TransactionType] =
    `enum`[TransactionType](label, fromLabel, Type("transaction_type"))
