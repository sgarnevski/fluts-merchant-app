# Trading of Fluts

A merchant buys fluts from piles ("schuurs") of boxes, always from the top down, and sells each flut
for 10 florins. For every scenario this project finds the **maximum profit** and **every number of
fluts** that reaches it (ascending, at most the 10 smallest), exactly as the specification asks.

This is the **platform** built on the Trading of Fluts core. The core — the algorithm, the parsers,
the console app and the REST API — lives in its own repository,
<https://github.com/sgarnevski/fluts-merchant>, and this project **builds on it instead of copying
it**: `flut-core`, `flut-common` and `flut-rest` come from there as Maven dependencies
(`1.0.0-SNAPSHOT`), installed by [`./install-core.sh`](#1-the-core). This repository adds the
database, login, merchants, UI and the SFTP/Kafka pipeline.

| Way | Where | Needs |
|---|---|---|
| Console app with the specification's input and output | [fluts-merchant](https://github.com/sgarnevski/fluts-merchant) (`flut-cli`) | Java 21 |
| REST API without a database, with Swagger UI | [fluts-merchant](https://github.com/sgarnevski/fluts-merchant) (`flut-rest`) | Java 21 |
| Full app: PostgreSQL, Keycloak login, scenario CRUD, import, React UI, several merchants | here: `flut-app` + `frontend` | Docker |
| Pipeline into the full app: signed files over SFTP → Kafka → database, per merchant | here: `flut-trader`, `flut-producer`, `flut-consumer` | Docker |

## Features at a glance

- **From the core** ([fluts-merchant](https://github.com/sgarnevski/fluts-merchant)): the algorithm,
  the strict text and JSON parsers with line numbers in every error, the evaluation REST endpoint
  with problem details and the `ScenarioRecorder` port this platform plugs its database into.
- **Full app**: scenarios stored in PostgreSQL (fully normalized, results computed on read), CRUD,
  paging, filters, file import; every scenario belongs to a **merchant**; Keycloak login
  (server-side session, CSRF) or bearer tokens; roles viewer / editor / admin.
- **UI**: scenario list with filters, create/edit form, evaluator; the scenario page shows the
  result, draws the piles five at a time (paged), outlines the boxes to buy in each pile with the
  per-pile profit, caps a pile at 20 drawn boxes, and offers downloads: the scenario and its result
  in the specification's formats, plus the result of each pile.
- **Pipeline**: per-merchant SFTP accounts, files signed with the merchant's secret, one signed
  Kafka message per scenario, bounded topic retention, dead-letter topic, no duplicates; two demo
  traders that keep delivering.
- **Platform additions to the formats** (`flut-platform-common`): scenario writers (text and JSON)
  and HMAC signing, used by the pipeline and the downloads.
- **Quality gates**: 100 % line and branch coverage in every module (backend and frontend), every
  Java variable `final`, `const`-only TypeScript, the API contract checked on both sides.

The `demo/` folder holds files to try everything with:

| File | What it is |
|---|---|
| `demo/input.txt` | the specification's example input (two scenarios) |
| `demo/output.txt` | the specification's expected output for it |
| `demo/input.json` | the same two scenarios in the normalized JSON format |
| `demo/large.txt` | one scenario with 7 piles of 60 boxes — shows the paging, the 20-box cap and the downloads in the UI |
| `demo/invalid.txt` | a pile that announces 3 boxes but lists 2 — shows the error with its line number |

## 1. The core

The algorithm, the parsers, the console app and the REST API without a database are in
<https://github.com/sgarnevski/fluts-merchant> — see its README for running them. This project uses
three of its modules (`flut-core`, `flut-common`, `flut-rest`) as dependencies, version
`1.0.0-SNAPSHOT`, from the local Maven repository:

```bash
./install-core.sh                                        # clone the core into .core/ (or update it) and install it
FLUTS_MERCHANT_DIR=../fluts-merchant ./install-core.sh   # or install from your own checkout, e.g. with local changes
FLUTS_MERCHANT_REF=<branch|tag|sha> ./install-core.sh    # or a specific revision (default: main)
```

Run it once before building with Maven, and again after the core changed. The Docker build does the
same by itself (build argument `FLUTS_MERCHANT_REF`), so `docker compose up --build` needs nothing
else.

## 2. Full app (Docker)

```bash
docker compose up --build
```

- UI: <http://localhost:3000> — log in as **`dutch` / `dutch`** or **`french` / `french`** (each
  the editor of their own merchant), **`viewer` / `viewer`** (Dutch, read only) or **`admin` / `admin`**
  (every merchant, read only). List, create, edit and delete scenarios, import a file, evaluate JSON
  or a file. Each scenario page draws its piles and marks the boxes to buy.
- Swagger UI of the full API: <http://localhost:3000/swagger-ui.html> (uses your login session)
- API directly: `localhost:8081` with a bearer token (see [Security](#security)),
  PostgreSQL: `localhost:5433` (user / password / db: `flut`),
  Keycloak admin console: <http://keycloak.localhost:8180> (`admin` / `admin`)

Stop it with `docker compose down` (keeps the data) or `docker compose down -v` (also wipes the
database and Keycloak).

The UI and the API are separate images and deploy independently:
`docker compose up --build -d frontend` rebuilds and restarts only the UI.

Every scenario that reaches the full app is stored — created in the UI, imported, or sent to
`/api/evaluations`. Files themselves are not stored: they are parsed, and only the normalized
scenarios are saved, with `inputType` `JSON` (JSON body), `FILE` (uploaded file) or `KAFKA`
(arrived through the [pipeline](#3-pipeline-sftp--kafka--database)).

**Merchants.** The system serves several clients: every scenario belongs to a merchant (`dutch`,
`french`, …). A user's merchant comes from their Keycloak account, never from the request; a merchant
user sees and changes only their own scenarios (another merchant's id answers 404), `admin` sees
everyone's. `GET /api/merchants` lists the merchants, `GET /api/me` tells the UI who you are.

| Method | Path | |
|---|---|---|
| POST | `/api/evaluations` | Solve a JSON body or a `.txt` / `.json` upload (the core's endpoint; here every evaluated scenario is also stored) |
| GET | `/api/scenarios` | List: `page`, `size`, `sort` (`name`, `createdAt`, `updatedAt`), filters `inputType`, `name`, `merchant` (admins) |
| POST | `/api/scenarios` | Create |
| GET / PUT / DELETE | `/api/scenarios/{id}` | Read (with the result and the best buy per pile) / replace / delete |
| GET | `/api/scenarios/{id}/result` | Only the result |
| GET | `/api/scenarios/{id}/file` | Download the scenario as a text file in the specification's input format |
| GET | `/api/scenarios/{id}/result/file` | Download the result in the specification's output format |
| GET | `/api/scenarios/{id}/schuurs/{n}/file`, `…/result/file` | One pile (1-based) as a scenario of its own, and its result |
| POST | `/api/scenarios/import` | Store every scenario of a file (201, results with ids) |
| GET | `/api/merchants` | The registered merchants |
| GET | `/api/me` | The logged-in user, roles and merchant |

Try the UI with `demo/large.txt`: import it on the scenario list ("Import file…"), open it, and you
get the piles five per page, each capped at 20 boxes with "… and 40 more (see the file)", the boxes
to buy outlined, "Download result" under every pile and the two downloads for the whole scenario.

## 3. Pipeline: SFTP → Kafka → database

```bash
docker compose --profile pipeline up --build
```

To drop the pipeline again:

```bash
# only the pipeline: the full app keeps running
docker compose --profile pipeline rm -sf kafka sftp flut-producer flut-consumer trader-dutch trader-french

# everything (pipeline and full app); add -v to also wipe the data and the SFTP folders
docker compose --profile pipeline down
```

Plain `docker compose down` (without `--profile pipeline`) does not stop the pipeline containers.

The pipeline adds six containers to the full app:

```
trader-dutch  ──SFTP (dutch)──▶ ┐
                                sftp ──shared volume──▶ flut-producer ──▶ kafka ──▶ flut-consumer ──▶ PostgreSQL
trader-french ──SFTP (french)─▶ ┘
```

- **`trader-dutch`, `trader-french`** simulate the merchants: every 2 minutes each uploads 1 to 5
  files with random scenarios (`.txt` in the original format or `.json`) to its own SFTP account,
  each file **signed** with the merchant's secret (`<name>.sig`, HMAC-SHA256 of the file).
- **`sftp`** is the common directory: an SFTP server (`localhost:2222`) with one account per
  merchant, `dutch` / `dutch` and `french` / `french`, whose home folders are a Docker volume.
  An account sees only its own folders.
- **`flut-producer`** reads that volume as a plain directory (it has no SFTP code). For every
  configured merchant it takes the files in `<merchant>/data/inbox`, **verifies the signature with
  that merchant's secret** — that is how the system knows who created a file — validates the content
  with the same parsers as everything else and sends **one signed JSON message per scenario** to the
  topic `flut.scenarios`. Sent files move to `data/processed`; rejected files (wrong or missing
  signature, invalid content) move to `data/failed` with a `<name>.error` file that says why.
- **`flut-consumer`** stores each scenario for its merchant with input type `KAFKA` — but only
  messages signed with the pipeline secret, so nobody else can drop records onto the shared topic.

Deliver a signed file yourself as the Dutch merchant and look at the result:

```bash
SIG=$(openssl dgst -sha256 -hmac dutch-secret demo/input.txt | awk '{print $NF}')
echo -n "$SIG" | curl -k -T - sftp://dutch:dutch@localhost:2222/data/inbox/mine.txt.sig   # signature first
curl -k -T demo/input.txt sftp://dutch:dutch@localhost:2222/data/inbox/mine.txt
curl -k -l sftp://dutch:dutch@localhost:2222/data/processed/            # mine.txt, a few seconds later

# The same file signed with the French secret is refused: signatures prove who made a file
SIG=$(openssl dgst -sha256 -hmac french-secret demo/input.txt | awk '{print $NF}')
echo -n "$SIG" | curl -k -T - sftp://dutch:dutch@localhost:2222/data/inbox/forged.txt.sig
curl -k -T demo/input.txt sftp://dutch:dutch@localhost:2222/data/inbox/forged.txt
curl -k sftp://dutch:dutch@localhost:2222/data/failed/forged.txt.error   # Signature does not match the dutch merchant's secret
docker compose logs -f trader-dutch trader-french flut-producer flut-consumer
```

Things worth knowing:

- **Origin is proven twice.** A file is accepted only with a signature made with the secret of
  the merchant whose account it arrived in; a message is stored only with a signature made with the
  pipeline secret. Neither the account, nor the message content, nor a guessed merchant name can
  make a scenario appear under another merchant.
- **Half-uploaded files are never read.** Traders upload the signature first, then the file as
  `<name>.part`, renamed when done; the producer also waits until a file has not changed for 2 seconds.
- **No duplicates.** Each message has an id derived from the file content and the scenario's
  position; the consumer stores it in a unique column and skips an id it already has. So a file
  that is sent again (producer restart, Kafka redelivery) is stored once — and so is the identical
  file uploaded twice.
- **Bad messages do not block the rest.** A message that is not a valid signed scenario of a
  registered merchant goes to the dead-letter topic `flut.scenarios.DLT`; other failures (database
  down) are retried three times first.
- **Retention is in Kafka only.** Kafka cannot keep "the last N messages", so the topic is bounded
  by age and size: messages are deleted after 6 hours (about 1000 scenarios at the trader's rate)
  and a partition never holds more than 10 MB (`flut.kafka.*`). PostgreSQL keeps every scenario.
  A consumer that is down for more than 6 hours misses the messages deleted in the meantime.
- **Settings:** `flut.trader.interval`, `flut.trader.min-files` / `max-files`,
  `flut.trader.invalid-file-ratio` (e.g. `FLUT_TRADER_INVALID_FILE_RATIO=0.2` makes every fifth file
  broken, to see `failed/` fill), `flut.producer.*`, `flut.consumer.*`. Secrets and passwords:
  `DUTCH_SECRET`, `FRENCH_SECRET`, `PIPELINE_SECRET`, `DUTCH_SFTP_PASSWORD`, `FRENCH_SFTP_PASSWORD`
  (demo defaults in `docker-compose.yml`).
- **Adding a merchant:** a row in `merchant`, a Keycloak user with the `merchant` attribute, an SFTP
  account, the secret in the producer's configuration, and — for the demo — a trader container.
- **Demo security:** Kafka is unauthenticated (the message signatures are what protect the topic),
  SFTP uses passwords, the traders do not check the server's host key, and all secrets are in
  `docker-compose.yml`. Fine on a laptop, not for production.

Kafka is reachable from the host at `localhost:9094`.

## 4. Docker cheat sheet

Everything runs from `docker-compose.yml`; the pipeline services are behind the profile `pipeline`.

| What | Command |
|---|---|
| Full app (PostgreSQL, Keycloak, API, UI) | `docker compose up --build` |
| Full app + pipeline (Kafka, SFTP, producer, consumer, both traders) | `docker compose --profile pipeline up --build` |
| In the background / follow the logs | `… up -d --build`, then `docker compose --profile pipeline logs -f` |
| Only the UI rebuilt and restarted | `docker compose up --build -d frontend` |
| Only the API rebuilt and restarted | `docker compose up --build -d flut-app` |
| Only one merchant's trader | `docker compose --profile pipeline up -d trader-french` (it needs `sftp`, started automatically) |
| Pause / resume the traders | `docker compose --profile pipeline stop trader-dutch trader-french` / `… start …` |
| One trader that sends a broken file now and then | `FLUT_TRADER_INVALID_FILE_RATIO=0.2 docker compose --profile pipeline up -d --force-recreate trader-dutch` |
| Different secrets or SFTP passwords | `DUTCH_SECRET=… FRENCH_SECRET=… PIPELINE_SECRET=… docker compose --profile pipeline up -d` (all services that use them must get the same value) |
| What is running | `docker compose --profile pipeline ps` |
| Drop only the pipeline, keep the full app running | `docker compose --profile pipeline rm -sf kafka sftp flut-producer flut-consumer trader-dutch trader-french` |
| Stop everything, keep the data | `docker compose --profile pipeline down` |
| Stop everything and wipe the data (database, Keycloak, SFTP folders) | `docker compose --profile pipeline down -v` |

Demo logins, all created by the Keycloak realm import:

| Login | Password | Merchant | May |
|---|---|---|---|
| `dutch` | `dutch` | Dutch merchant | read, create, change, delete, import, evaluate |
| `french` | `french` | French merchant | the same, for the French merchant |
| `viewer` | `viewer` | Dutch merchant | read only |
| `admin` | `admin` | all merchants | read only, with a merchant filter |

The same names are the SFTP accounts of the merchants (`dutch` / `dutch`, `french` / `french` on
port 2222), and `admin` / `admin` opens the Keycloak console at <http://keycloak.localhost:8180>.

Ports on the host: UI 3000, API 8081, Keycloak 8180,
PostgreSQL 5433, SFTP 2222, Kafka 9094.

## Security

The full app is secured; the core's console app and REST API (in fluts-merchant) are open.

- **Browser: server-side login.** The UI sends you to Keycloak; after login `flut-app` keeps the
  tokens in its HTTP session and the browser only holds a session cookie — no token ever reaches
  JavaScript. Requests that change data must carry the CSRF token (cookie `XSRF-TOKEN`, sent back
  as header `X-XSRF-TOKEN`). Logging out ends the Keycloak session too.
- **Interrupted logins recover by themselves.** If the app no longer has the session that started a
  login (it was restarted, or the Keycloak page stayed open past the session timeout), it simply
  starts the login again — Keycloak's session makes that invisible. Any other failure shows a
  "Sign-in could not be completed" page with a button to try again.
- **API clients: bearer tokens.** `Authorization: Bearer <access token>`, no CSRF token needed.
- **Roles** (Keycloak realm roles): `viewer` may read their merchant's scenarios; `editor` may also
  create, change, delete, import and evaluate (evaluations are stored); `admin` may read every
  merchant's scenarios and change nothing. The merchant is a Keycloak user attribute (`merchant`),
  sent as a token claim. The UI hides what you may not do.

```bash
TOKEN=$(curl -s -d grant_type=password -d client_id=flut-app -d client_secret=flut-app-secret \
  -d username=dutch -d password=dutch \
  http://keycloak.localhost:8180/realms/flut/protocol/openid-connect/token | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" localhost:8081/api/me
curl -H "Authorization: Bearer $TOKEN" -F file=@demo/input.txt localhost:8081/api/scenarios/import
```

Keycloak runs at `http://keycloak.localhost:8180` so that the browser and `flut-app` (inside Docker,
via a network alias) use the same issuer URL. Browsers and curl resolve `*.localhost` to your
machine; if yours does not, add `127.0.0.1 keycloak.localhost` to your hosts file. The realm with
its client, roles, the `merchant` attribute and the four users is imported from `keycloak/flut-realm.json`.

## Input formats and the algorithm

Both are the core's and are described in its README: the specification's text format with the
rules for input the specification leaves open, the JSON format, the limits, and how the algorithm
finds the maximum profit and the flut counts. In short, the JSON format is

```json
{ "scenarios": [ { "name": "Example 2",
                   "schuurs": [ { "boxPrices": [7, 3, 11, 9, 10] },
                                { "boxPrices": [1, 2, 3, 4, 10, 16, 10, 4, 16] } ] } ] }
```

and the platform uses exactly the core's parsers everywhere — uploads, the evaluator, the pipeline.

## Project structure

```
flut-platform-common  scenario writers (text, JSON) and HMAC signing — what the platform adds to the core's formats
flut-persistence      Flyway schema, JPA entities, repositories — shared by flut-app and flut-consumer
flut-app              Spring Boot app: the core's evaluation API + scenario CRUD, import, downloads, merchants, login
flut-kafka            what producer and consumer share: topic declarations (retention), signed message format
flut-trader           Spring Boot app: one merchant's demo client, uploads signed scenario files over SFTP on a schedule
flut-producer         Spring Boot app: merchant directories → Kafka, verifies file signatures, one signed message per scenario
flut-consumer         Spring Boot app: Kafka → PostgreSQL (input type KAFKA)
frontend              React + TypeScript (Vite), separate app, served by nginx
api/                  the committed OpenAPI contract of flut-app
demo/                 files to try things with: the specification's example (input.txt, output.txt, input.json), large.txt, invalid.txt
install-core.sh       builds and installs the core (flut-core, flut-common, flut-rest) from fluts-merchant
```

Dependencies point to the core and never back: `flut-app → flut-rest` (core) `+ flut-persistence +
flut-platform-common`; `flut-persistence → flut-common` (core); `flut-kafka → flut-common` (core) `+
flut-platform-common`; `flut-producer → flut-kafka`; `flut-consumer → flut-kafka + flut-persistence`;
`flut-trader → flut-platform-common`. The core knows nothing about this repository: `flut-app` plugs
its database into the core's `ScenarioRecorder` port, and stores the core's input types (JSON, file)
plus its own (`KAFKA`) as `ScenarioInputType`.

Design choices worth knowing:

- **Results are computed on read, never stored** — the algorithm takes microseconds, and a stored
  copy could go stale.
- **Fully normalized schema** — one row per scenario, schuur and box, ordered by `position`.
  A page of scenarios loads in a fixed three queries (batch fetching; tested).
- **API contract** — `api/flut-app.openapi.json` is compared with the live API in a test; the
  frontend generates its TypeScript types from it (`npm run api:check` fails when they are stale).

## Development

```bash
./install-core.sh  # once, and again after the core changed
./mvnw verify      # all backend modules; flut-persistence, flut-app and flut-consumer tests need Docker (Testcontainers)
./mvnw -pl flut-platform-common,flut-kafka,flut-producer,flut-trader verify   # everything that runs without Docker
```

`verify` fails when a module is below **100 % line and branch coverage** (JaCoCo) or when a
variable is not **`final`** (PMD, `config/pmd/ruleset.xml`). Coverage reports:
`<module>/target/site/jacoco/index.html`.

Run `flut-app` outside Docker, against the database and Keycloak of docker compose (the app reads
Keycloak's configuration at startup, so Keycloak must be running):

```bash
docker compose up -d postgres keycloak
./install-core.sh
./mvnw -q -pl flut-app -am package -DskipTests
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/flut java -jar flut-app/target/flut-app.jar
```

(Or against a local PostgreSQL 16 on 5432, the default: create the role and database once with
`sudo -u postgres createuser -P flut` (password `flut`) and `sudo -u postgres createdb -O flut flut`.)

Frontend (Node 22, see `frontend/.nvmrc`):

```bash
cd frontend
npm ci
npm run dev       # http://localhost:5173, proxies the API to flut-app on localhost:8081
npm run verify    # generated API types up to date + lint (const only) + tests with 100 % coverage
```

After an API change: run `./mvnw -pl flut-app verify -Dopenapi.update=true` to update the contract,
then `npm run api:generate` in `frontend/`, and commit both.
