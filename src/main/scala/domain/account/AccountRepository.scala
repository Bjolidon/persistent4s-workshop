package domain.account

import persistent4s.Repository
import cats.effect.IO
import java.util.UUID
import cats.effect.Resource
import java.time.LocalDateTime
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import cats.implicits.*

final case class Account(
    accountId: UUID,
    memberId: UUID,
    name: String,
    balance: Int,
    createdAt: LocalDateTime,
    closedAt: Option[LocalDateTime]
)

final case class AccountRepository private (
    pool: Resource[IO, Session[IO]]
) extends Repository[IO, UUID, Account] {

  import AccountRepository.*

  override def findMany(
      keys: List[UUID]
  ): IO[Map[UUID, Option[Account]]] =
    if keys.isEmpty then Map.empty.pure[IO]
    else
      pool.use(_.execute(findManyQuery(keys.size))(keys)).map { accounts =>
        val found = accounts.map(a => a.accountId -> a).toMap
        keys.map(k => k -> found.get(k)).toMap
      }

  override def upsertMany(states: Map[UUID, Account]): IO[Unit] =
    if states.isEmpty then IO.unit
    else
      pool.use { session =>
        session.transaction.use { _ =>
          states.values.toList
            .grouped(MaxUpsertChunkSize)
            .toList
            .traverse_(chunk =>
              session.execute(upsertManyCommand(chunk.size))(chunk).void
            )
        }
      }

  override def deleteMany(keys: List[UUID]): IO[Unit] =
    if keys.isEmpty then IO.unit
    else pool.use(_.execute(deleteManyCommand(keys.size))(keys)).void

  def find(key: UUID): IO[Option[Account]] =
    pool.use(_.option(findQuery)(key))

  def findByMemberId(memberId: UUID): IO[List[Account]] =
    pool.use(_.execute(findByMemberIdQuery)(memberId))
}

object AccountRepository:

  private val MaxUpsertChunkSize = 500

  private val accountCodec: Codec[Account] =
    (uuid *: uuid *: text *: int4 *: timestamp *: timestamp.opt).to[Account]

  private val findQuery: Query[UUID, Account] =
    sql"""
      SELECT account_id, member_id, name, balance, created_at, closed_at
      FROM accounts
      WHERE account_id = $uuid
    """.query(accountCodec)

  private def findByMemberIdQuery: Query[UUID, Account] =
    sql"""
      SELECT account_id, member_id, name, balance, created_at, closed_at
      FROM accounts
      WHERE member_id = $uuid
    """.query(accountCodec)

  private def findManyQuery(n: Int): Query[List[UUID], Account] =
    sql"""
      SELECT a.account_id, a.member_id, a.name, a.balance, a.created_at, a.closed_at
      FROM accounts a
      WHERE a.account_id = ANY(ARRAY[${uuid.list(n)}])
    """.query(accountCodec)

  private def upsertManyCommand(n: Int): Command[List[Account]] =
    sql"""
      INSERT INTO accounts (account_id, member_id, name, balance, created_at, closed_at)
      VALUES ${accountCodec.values.list(n)}
      ON CONFLICT (account_id) DO UPDATE SET
        member_id = EXCLUDED.member_id,
        name = EXCLUDED.name,
        balance = EXCLUDED.balance,
        created_at = EXCLUDED.created_at,
        closed_at = EXCLUDED.closed_at
    """.command

  private def deleteManyCommand(n: Int): Command[List[UUID]] =
    sql"DELETE FROM accounts WHERE account_id = ANY(ARRAY[${uuid.list(n)}])".command

  def make(pool: Resource[IO, Session[IO]]): AccountRepository =
    new AccountRepository(pool)
