package domain.member

import persistent4s.Projection
import cats.effect.IO
import domain.BankEvent
import java.util.UUID
import persistent4s.EventEnvelope
import persistent4s.EventFilter
import persistent4s.EventTypeName
import domain.*

final class MemberProjection private (
    protected val repository: MemberRepository
) extends Projection[IO, BankEvent, UUID, Member] {

  override def name: String = "MemberProjection"

  override def filter: Set[EventTypeName] = Set(
    EventTypeName.of[MemberRegistered]
  )

  override def resolveKeys(event: EventEnvelope[BankEvent]): List[UUID] =
    event.payload match
      case MemberRegistered(memberId, _, _) => List(memberId)
      case _                                => Nil

  override def handle(
      state: Option[Member],
      event: EventEnvelope[BankEvent]
  ): IO[Option[Member]] = (state, event.payload) match
    case (None, MemberRegistered(memberId, name, birthDate)) =>
      IO.pure(Some(Member(memberId, name, birthDate)))
    case _ =>
      IO.raiseError(
        new RuntimeException(
          s"Failed to apply event: ${event.payload} to state: $state"
        )
      )
}

object MemberProjection {
  def make(repository: MemberRepository): MemberProjection =
    new MemberProjection(repository)
}
