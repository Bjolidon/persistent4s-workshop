$version: "2"

namespace bank.api

use alloy#UUID
use alloy#simpleRestJson

@simpleRestJson
service AccountService {
    operations: [
        GetAccount
        GetAccountsByMember
        CreateAccount
        CloseAccount
    ]
}

@http(method: "GET", uri: "/accounts/{accountId}")
operation GetAccount {
    input: GetAccountInput
    output: GetAccountOutput
    errors: [AccountNotFound]
}

structure GetAccountInput {
    @required
    @httpLabel
    accountId: UUID
}

structure GetAccountOutput {
    @required
    account: Account
}

@http(method: "GET", uri: "/members/{memberId}/accounts")
operation GetAccountsByMember {
    input: GetAccountsByMemberInput
    output: GetAccountsByMemberOutput
}

structure GetAccountsByMemberInput {
    @required
    @httpLabel
    memberId: UUID
}

structure GetAccountsByMemberOutput {
    @required
    accounts: AccountList
}

@http(method: "POST", uri: "/accounts")
operation CreateAccount {
    input: CreateAccountInput
    output: CreateAccountOutput
    errors: [MemberNotFound, ValidationError]
}

structure CreateAccountInput {
    @required
    memberId: UUID

    @required
    name: String
}

structure CreateAccountOutput {
    @required
    accountId: UUID
}

@http(method: "DELETE", uri: "/accounts/{accountId}")
operation CloseAccount {
    input: CloseAccountInput
    output: CloseAccountOutput
    errors: [AccountNotFound]
}

structure CloseAccountInput {
    @required
    @httpLabel
    accountId: UUID
}

structure CloseAccountOutput {}

structure Account {
    @required
    accountId: UUID

    @required
    memberId: UUID

    @required
    name: String

    @required
    balance: Integer

    @required
    @timestampFormat("date-time")
    createdAt: Timestamp

    @timestampFormat("date-time")
    closedAt: Timestamp
}

list AccountList {
    member: Account
}

@error("client")
@httpError(404)
structure AccountNotFound {
    @required
    message: String
}

@error("client")
@httpError(404)
structure MemberNotFound {
    @required
    message: String
}

@error("client")
@httpError(400)
structure ValidationError {
    @required
    message: String
}
