package domain

import persistent4s.Event
import java.util.UUID
import io.circe.Encoder
import io.circe.Decoder
import java.time.LocalDateTime
import java.time.LocalDate

sealed trait BankEvent extends Event

// Step 1: define your event sub-hierarchies and case classes here.
//
// You need three sealed sub-traits: MemberEvents, AccountEvents, TransactionEvents
//
// And six case classes (each deriving Encoder and Decoder):
//   MemberRegistered    (memberId: UUID, name: String, birthDate: LocalDate)
//   AccountCreated      (accountId: UUID, memberId: UUID, name: String, createdAt: LocalDateTime)
//   AccountClosed       (accountId: UUID, closedAt: LocalDateTime)
//   Deposited           (transactionId: UUID, accountId: UUID, amount: Int, timestamp: LocalDateTime)
//   Withdrawn           (transactionId: UUID, accountId: UUID, amount: Int, timestamp: LocalDateTime)
//   Transferred         (transactionId: UUID, debitAccountId: UUID, creditAccountId: UUID, amount: Int, timestamp: LocalDateTime)
