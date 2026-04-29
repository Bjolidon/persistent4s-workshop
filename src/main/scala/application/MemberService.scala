package application

import domain.member.MemberRepository
import cats.effect.IO
import bank.api.*
import java.util.UUID
import smithy4s.time.LocalDate
import domain.member.commands.RegisterMemberHandler
import domain.member.commands.RegisterMember
import persistent4s.Event
import persistent4s.EventStore
import domain.BankEvent

class MemberServiceImpl(repository: MemberRepository)(using
    EventStore[IO, BankEvent]
) extends MemberService[IO] {

  override def getMember(memberId: UUID): IO[GetMemberOutput] = repository
    .find(memberId)
    .map {
      case Some(member) =>
        GetMemberOutput(
          Member(
            memberId = member.memberId,
            name = member.name,
            birthDate = Date(LocalDate.fromJava(member.birthDate))
          )
        )
      case None =>
        throw new NoSuchElementException(s"Member not found: $memberId")
    }

  override def createMember(
      name: String,
      birthDate: Date
  ): IO[CreateMemberOutput] = for {
    id <- IO(UUID.randomUUID())
    _ <- RegisterMemberHandler.run[IO](
      RegisterMember(id, name, birthDate.value.toJava)
    )
  } yield CreateMemberOutput(id)

}
