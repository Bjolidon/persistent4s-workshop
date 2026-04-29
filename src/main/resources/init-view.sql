CREATE TABLE IF NOT EXISTS members (
  member_id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  birth_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS accounts (
  account_id UUID PRIMARY KEY,
  member_id UUID NOT NULL,
  name TEXT NOT NULL,
  balance INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL,
  closed_at TIMESTAMP
);

CREATE TYPE transaction_type AS ENUM ('deposit', 'withdrawal', 'transfer');

CREATE TABLE IF NOT EXISTS transactions (
  transaction_id UUID PRIMARY KEY,
  debit_account_id UUID,
  credit_account_id UUID,
  transaction_type transaction_type NOT NULL,
  amount INTEGER NOT NULL,
  transaction_timestamp TIMESTAMP NOT NULL
);