# persistent4s Workshop — Banking Example

A guided workshop to discover the [persistent4s](https://github.com/antoniojimeneznieto/persistent4s) event sourcing library through a simple banking domain.

## Domain

The application models a bank with three concepts:

- **Member** — a bank customer (name, birth date)
- **Account** — a bank account owned by a member (balance, open/closed status)
- **Transaction** — a deposit, withdrawal, or transfer between accounts

## Prerequisites

- JDK 17+
- sbt 1.10+
- Docker (for the database)

## Setup

Start PostgreSQL:

```
docker-compose -f src/main/resources/docker-compose.yml up -d
```

Run the application (once you have completed the workshop steps):

```
sbt run
```

The Swagger UI will be available at: http://localhost:8080/docs

Run tests:

```
sbt test
```

## Workshop Steps

Work through the steps in order. Each `STEP-N.md` file describes what to implement and why.

| Step | File(s) | Concept |
|------|---------|---------|
| [Step 0](STEP-0.md) | `build.sbt` | Add persistent4s dependencies |
| [Step 1](STEP-1.md) | `domain/BankEvent.scala` | Define events |
| [Step 2](STEP-2.md) | `domain/**/commands/*.scala` | Implement command handlers |
| [Step 3](STEP-3.md) | `domain/**/Projection.scala` | Implement projections |
| [Step 4](STEP-4.md) | `infrastructure/BankModule.scala` | Wire the module |

If you get stuck, solution tags are available:
`step-0-solution`, `step-1-solution`, `step-2-solution`, `step-3-solution`, `step-4-solution`

```bash
git show step-1-solution:src/main/scala/domain/BankEvent.scala
```
