$version: "2"

namespace bank.api

use alloy#UUID
use alloy#simpleRestJson

@simpleRestJson
service TransactionService {
    operations: [
        GetTransaction
        Deposit
        Withdraw
        Transfer
    ]
}

@http(method: "GET", uri: "/transactions/{transactionId}")
operation GetTransaction {
    input: GetTransactionInput
    output: GetTransactionOutput
    errors: [
        TransactionNotFound
    ]
}

structure GetTransactionInput {
    @required
    @httpLabel
    transactionId: UUID
}

structure GetTransactionOutput {
    @required
    transaction: Transaction
}

@http(method: "POST", uri: "/transactions/deposit")
operation Deposit {
    input: DepositInput
    output: DepositOutput
    errors: [
        AccountNotFound
        ValidationError
    ]
}

structure DepositInput {
    @required
    accountId: UUID

    @required
    amount: Integer
}

structure DepositOutput {
    @required
    transactionId: UUID
}

@http(method: "POST", uri: "/transactions/withdraw")
operation Withdraw {
    input: WithdrawInput
    output: WithdrawOutput
    errors: [
        AccountNotFound
        ValidationError
    ]
}

structure WithdrawInput {
    @required
    accountId: UUID

    @required
    amount: Integer
}

structure WithdrawOutput {
    @required
    transactionId: UUID
}

@http(method: "POST", uri: "/transactions/transfer")
operation Transfer {
    input: TransferInput
    output: TransferOutput
    errors: [
        AccountNotFound
        ValidationError
    ]
}

structure TransferInput {
    @required
    debitAccountId: UUID

    @required
    creditAccountId: UUID

    @required
    amount: Integer
}

structure TransferOutput {
    @required
    transactionId: UUID
}

structure Transaction {
    @required
    transactionId: UUID

    debitAccountId: UUID

    creditAccountId: UUID

    @required
    transactionType: TransactionType

    @required
    amount: Integer

    @required
    @timestampFormat("date-time")
    timestamp: Timestamp
}

enum TransactionType {
    DEPOSIT
    WITHDRAWAL
    TRANSFER
}

@error("client")
@httpError(404)
structure TransactionNotFound {
    @required
    message: String
}

@error("client")
@httpError(404)
structure AccountNotFound {
    @required
    message: String
}

@error("client")
@httpError(400)
structure ValidationError {
    @required
    message: String
}
