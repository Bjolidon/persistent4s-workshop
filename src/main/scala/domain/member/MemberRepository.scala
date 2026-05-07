package domain.member

import java.time.LocalDate

import persistent4s.Repository
import cats.effect.IO
import java.util.UUID
import cats.effect.Resource
import java.time.LocalDateTime
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import cats.implicits.*

final case class Member(
    memberId: UUID,
    name: String,
    birthDate: LocalDate
)

final case class MemberRepository private (
    pool: Resource[IO, Session[IO]]
) extends Repository[IO, UUID, Member] {

  import MemberRepository.*

  override def findMany(keys: List[UUID]): IO[Map[UUID, Option[Member]]] =
    if keys.isEmpty then Map.empty.pure[IO]
    else
      pool.use(_.execute(findManyQuery(keys.size))(keys)).map { members =>
        val found = members.map(m => m.memberId -> m).toMap
        keys.map(k => k -> found.get(k)).toMap
      }

  override def upsertMany(states: Map[UUID, Member]): IO[Unit] =
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

  def find(key: UUID): IO[Option[Member]] =
    pool.use(_.option(findQuery)(key))

}

object MemberRepository {

  private val MaxUpsertChunkSize = 500

  private val memberCodec: Codec[Member] =
    (uuid *: text *: date).to[Member]

  private val findQuery: Query[UUID, Member] =
    sql"""
      SELECT member_id, name, birth_date
      FROM members
      WHERE member_id = $uuid
    """.query(memberCodec)

  private def findManyQuery(n: Int): Query[List[UUID], Member] =
    sql"""
      SELECT member_id, name, birth_date
      FROM members
      WHERE member_id = ANY(ARRAY[${uuid.list(n)}])
    """.query(memberCodec)

  private def upsertManyCommand(n: Int): Command[List[Member]] =
    sql"""
      INSERT INTO members (member_id, name, birth_date)
      Values ${memberCodec.values.list(n)}
      ON CONFLICT (member_id) do UPDATE SET
        name = EXCLUDED.name,
        birth_date = EXCLUDED.birth_date
    """.command

  private def deleteManyCommand(n: Int): Command[List[UUID]] =
    sql"DELETE FROM members WHERE member_id = ANY(ARRAY[${uuid.list(n)}])".command

  def make(pool: Resource[IO, Session[IO]]): MemberRepository =
    new MemberRepository(pool)
}
