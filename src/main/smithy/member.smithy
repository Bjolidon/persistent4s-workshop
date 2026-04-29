$version: "2"

namespace bank.api

use alloy#UUID
use alloy#dateFormat
use alloy#simpleRestJson

@simpleRestJson
service MemberService {
    operations: [
        GetMember
        CreateMember
    ]
}

@http(method: "GET", uri: "/members/{memberId}")
operation GetMember {
    input: GetMemberInput
    output: GetMemberOutput
    errors: [MemberNotFound]
}

structure GetMemberInput {
    @required
    @httpLabel
    memberId: UUID
}

structure GetMemberOutput {
    @required
    member: Member
}

@http(method: "POST", uri: "/members")
operation CreateMember {
    input: CreateMemberInput
    output: CreateMemberOutput
}

structure CreateMemberInput {
    @required
    name: String

    @required
    birthDate: Date
}

structure CreateMemberOutput {
    @required
    memberId: UUID
}

structure Member {
    @required
    memberId: UUID

    @required
    name: String

    @required
    birthDate: Date
}

@error("client")
@httpError(404)
structure MemberNotFound {
    @required
    message: String
}

@dateFormat
string Date
