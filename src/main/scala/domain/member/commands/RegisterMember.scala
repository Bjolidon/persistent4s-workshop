package domain.member.commands

import domain.BankEvent
import java.util.UUID
import persistent4s.CommandHandler
import persistent4s.Tag
import java.time.LocalDate
import domain.member.*
import domain.*

final case class RegisterMember(
    memberId: UUID,
    name: String,
    birthDate: LocalDate
)

object RegisterMemberHandler
    extends CommandHandler[RegisterMember, Unit, BankEvent] {

  override def tags(command: RegisterMember): Set[Tag] = Set.empty

  override def initial: Unit = ()

  override def evolve(
      command: RegisterMember,
      state: Unit,
      event: BankEvent
  ): Unit = ()

  override def validate(
      state: Unit,
      command: RegisterMember
  ): Either[Throwable, Unit] = Right(())

  override def decide(
      state: Unit,
      command: RegisterMember
  ): List[(Set[Tag], BankEvent)] =
    List(
      (
        Set(Tag.apply("member", command.memberId.toString)),
        MemberRegistered(command.memberId, command.name, command.birthDate)
      )
    )

}
