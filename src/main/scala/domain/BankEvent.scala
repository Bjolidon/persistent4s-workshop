package domain

import persistent4s.Event
import java.util.UUID
import io.circe.Encoder
import io.circe.Decoder
import java.time.LocalDateTime
import java.time.LocalDate

sealed trait BankEvent extends Event

// ----------------------------------------------------------------------
//  Member Events
// ----------------------------------------------------------------------

sealed trait MemberEvents extends BankEvent

final case class MemberRegistered(
    memberId: UUID,
    name: String,
    birthDate: LocalDate
) extends MemberEvents derives Encoder, Decoder

// ----------------------------------------------------------------------
//  Account Events
// ----------------------------------------------------------------------

sealed trait AccountEvents extends BankEvent

final case class AccountCreated(
    accountId: UUID,
    memberId: UUID,
    name: String,
    createdAt: LocalDateTime
) extends AccountEvents derives Encoder, Decoder

final case class AccountClosed(
    accountId: UUID,
    closedAt: LocalDateTime
) extends AccountEvents derives Encoder, Decoder

// ----------------------------------------------------------------------
//  Transaction Events
// ----------------------------------------------------------------------

sealed trait TransactionEvents extends BankEvent

final case class Deposited(
    transactionId: UUID,
    accountId: UUID,
    amount: Int,
    timestamp: LocalDateTime
) extends TransactionEvents derives Encoder, Decoder

final case class Withdrawn(
    transactionId: UUID,
    accountId: UUID,
    amount: Int,
    timestamp: LocalDateTime
) extends TransactionEvents derives Encoder, Decoder

final case class Transferred(
    transactionId: UUID,
    debitAccountId: UUID,
    creditAccountId: UUID,
    amount: Int,
    timestamp: LocalDateTime
) extends TransactionEvents derives Encoder, Decoder
