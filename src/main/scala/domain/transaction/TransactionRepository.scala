package domain.transaction

import infrastructure.Repository
import cats.effect.IO
import java.util.UUID
import cats.effect.Resource
import java.time.LocalDateTime
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import cats.implicits.*

final case class Transaction(
    transactionId: UUID,
    debitAccountId: Option[UUID],
    creditAccountId: Option[UUID],
    transactionType: TransactionType,
    amount: Int,
    timestamp: LocalDateTime
)

final case class TransactionRepository private (
    pool: Resource[IO, Session[IO]]
) extends Repository[IO, UUID, Transaction] {

  import TransactionRepository.*

  override def find(key: UUID): IO[Option[Transaction]] =
    pool.use(_.option(findQuery)(key))

  override def findMany(
      keys: List[UUID]
  ): IO[Map[UUID, Option[Transaction]]] =
    if keys.isEmpty then Map.empty.pure[IO]
    else
      pool.use(_.execute(findManyQuery(keys.size))(keys)).map { transactions =>
        val found = transactions.map(t => t.transactionId -> t).toMap
        keys.map(k => k -> found.get(k)).toMap
      }

  override def persistMany(
      states: Map[UUID, Option[Transaction]]
  ): IO[Unit] =
    val toUpsert = states.collect { case (_, Some(v)) => v }.toList
    val toDelete = states.collect { case (k, None) => k }.toList
    if toUpsert.isEmpty && toDelete.isEmpty then IO.unit
    else
      pool.use { session =>
        session.transaction.use { _ =>
          val upsertEffect =
            if toUpsert.isEmpty then IO.unit
            else
              toUpsert
                .grouped(MaxUpsertChunkSize)
                .toList
                .traverse_(chunk =>
                  session.execute(upsertManyCommand(chunk.size))(chunk).void
                )
          val deleteEffect =
            if toDelete.isEmpty then IO.unit
            else
              session.execute(deleteManyCommand(toDelete.size))(toDelete).void
          upsertEffect >> deleteEffect
        }
      }

  def findByAccountId(accountId: UUID): IO[List[Transaction]] =
    pool.use(_.execute(findByAccountIdQuery)(accountId))

}

object TransactionRepository:

  private val MaxUpsertChunkSize = 500

  private val transactionCodec: Codec[Transaction] =
    (uuid *: uuid.opt *: uuid.opt *: TransactionType.codec *: int4 *: timestamp)
      .to[Transaction]

  private val findQuery: Query[UUID, Transaction] =
    sql"""
      SELECT transaction_id, debit_account_id, credit_account_id, transaction_type, amount, transaction_timestamp
      FROM transactions
      WHERE transaction_id = $uuid
    """.query(transactionCodec)

  private def findByAccountIdQuery: Query[UUID, Transaction] =
    sql"""
      SELECT transaction_id, debit_account_id, credit_account_id, transaction_type, amount, transaction_timestamp
      FROM transactions
      WHERE debit_account_id = $uuid OR credit_account_id = $uuid

    """
      .query(transactionCodec)
      .contramap[UUID](accountId => accountId *: accountId *: EmptyTuple)

  private def findManyQuery(n: Int): Query[List[UUID], Transaction] =
    sql"""
      SELECT transaction_id, debit_account_id, credit_account_id, transaction_type, amount, transaction_timestamp
      FROM transactions
      WHERE transaction_id = ANY(Array[${uuid.list(n)}])
    """.query(transactionCodec)

  private def upsertManyCommand(n: Int): Command[List[Transaction]] =
    sql"""
      INSERT INTO transactions (transaction_id, debit_account_id, credit_account_id, transaction_type, amount, transaction_timestamp)
      VALUES ${transactionCodec.values.list(n)}
      ON CONFLICT (transaction_id) DO UPDATE SET
        transaction_id = EXCLUDED.transaction_id,
        debit_account_id = EXCLUDED.debit_account_id,
        credit_account_id = EXCLUDED.credit_account_id,
        transaction_type = EXCLUDED.transaction_type,
        amount = EXCLUDED.amount,
        transaction_timestamp = EXCLUDED.transaction_timestamp
    """.command

  private def deleteManyCommand(n: Int): Command[List[UUID]] =
    sql"DELETE FROM transactions WHERE transaction_id = ANY(ARRAY[${uuid.list(n)}])".command

  def make(pool: Resource[IO, Session[IO]]): TransactionRepository =
    new TransactionRepository(pool)
