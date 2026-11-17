# Integracije – mikroservisna Bookstore aplikacija

Projekat iz predmeta **Integracija razvoja i operative u informacionim tehnologijama (2026/27)**.
Jednostavna mikroservisna aplikacija za knjižaru na kojoj se primenjuju DevOps prakse:
Git workflow kroz feature grane i pull request-ove, unit i end-to-end testovi, CI pipeline,
logovi, metrike i trace-ovi.

## Arhitektura

```
                 Klijent (curl / Postman)
                           │  REST (HTTP/JSON)
                           ▼
                 ┌───────────────────┐
                 │   api-gateway     │  :8080   Spring Cloud Gateway
                 └───────────────────┘
        /api/books   /api/authors   /api/orders   /api/notifications
           │              │              │                 │
           ▼              ▼              ▼                 ▼
   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐
   │ book-service │ │author-service│ │order-service │ │ notification-service │
   │    :8081     │ │    :8082     │ │    :8083     │ │        :8084         │
   │ MVC + JPA    │ │ MVC          │ │ WebFlux      │ │ MVC + RabbitMQ       │
   └──────┬───────┘ └──────────────┘ └──┬────────┬──┘ └──────────▲───────────┘
          │                              │        │               │
          ▼            reaktivni poziv   │        │  OrderPlaced  │
     PostgreSQL  ◄───── (WebClient) ─────┘        └──► RabbitMQ ──┘

   Svi servisi šalju trace-ove u Zipkin (:9411) i izlažu metrike na /actuator/prometheus.
```

## Servisi

| Servis | Port | Tehnologija | Opis |
|---|---|---|---|
| `api-gateway` | 8080 | Spring Cloud Gateway (WebFlux) | Jedinstvena ulazna tačka, rutira zahteve ka servisima |
| `book-service` | 8081 | Spring MVC, JPA, PostgreSQL | CRUD i pretraga knjiga, broj knjiga po žanru |
| `author-service` | 8082 | Spring MVC | Lista autora (demonstracioni podaci) |
| `order-service` | 8083 | Spring WebFlux, WebClient, RabbitMQ | Kreiranje porudžbine, reaktivni poziv ka book-service, slanje događaja |
| `notification-service` | 8084 | Spring MVC, RabbitMQ | Prima događaje o porudžbinama i čuva notifikacije |

Infrastruktura (`infrastructure/docker-compose.yml`): **RabbitMQ** (5672, konzola 15672) i **Zipkin** (9411).
book-service koristi **PostgreSQL** (5432).

## Endpointi (preko gateway-a, `http://localhost:8080`)

| Metoda | Putanja | Servis | Opis |
|---|---|---|---|
| GET | `/api/books` | book | Sve knjige |
| GET | `/api/books/{id}` | book | Knjiga po ID-ju |
| POST | `/api/books` | book | Nova knjiga |
| PUT | `/api/books/{id}` | book | Izmena knjige |
| DELETE | `/api/books/{id}` | book | Brisanje knjige |
| GET | `/api/books/search?title=&author=&genre=` | book | Pretraga |
| GET | `/api/books/price?minPrice=&maxPrice=` | book | Knjige u rasponu cene |
| GET | `/api/books/count-by-genre` | book | Broj knjiga po žanru |
| GET | `/api/authors` | author | Svi autori |
| POST | `/api/orders` | order | Nova porudžbina (`{"bookId":1,"quantity":2}`) |
| GET | `/api/notifications` | notification | Primljene notifikacije o porudžbinama |

Svaki servis ima i `/actuator/health`, `/actuator/info` i `/actuator/prometheus` na svom portu.

## Komunikacija između servisa

| Tip | Gde | Kako |
|---|---|---|
| **REST API** | klijent → gateway → servisi | HTTP/JSON endpointi, gateway rutira po putanji |
| **Reaktivna komunikacija** | order-service → book-service | `WebClient` vraća `Mono<BookDto>`, obrada se nastavlja preko `flatMap` bez blokiranja niti (Project Reactor) |
| **Message Queue** | order-service → notification-service | order-service šalje događaj `OrderPlaced` na exchange `orders.exchange` (routing key `orders.placed`), notification-service ga čita iz reda `orders.notifications` |

Tok jedne porudžbine:

1. Klijent šalje `POST /api/orders` na gateway (8080).
2. Gateway prosleđuje zahtev order-service-u.
3. order-service reaktivno traži knjigu od book-service-a (`GET /api/books/{id}`); ako ne postoji, vraća 404.
4. order-service računa ukupnu cenu i šalje događaj `OrderPlaced` u RabbitMQ.
5. notification-service prima događaj i čuva notifikaciju (vidljivo na `GET /api/notifications`).
6. Klijent dobija `201 Created` sa podacima o porudžbini.

## Logovi, metrike i trace-ovi

- **Logovi**: svaki servis loguje u konzolu; svaka linija sadrži `[traceId-spanId]`, pa se logovi
  istog zahteva mogu povezati kroz sve servise.
- **Metrike**: Spring Boot Actuator + Micrometer, Prometheus format na `/actuator/prometheus`.
  Svaka metrika ima oznaku `application=<ime servisa>`. Iz `http_server_requests_seconds`
  dobijaju se throughput (`_count`), latency (`_sum / _count`, `_max`) i error rate (status 4xx/5xx).
- **Trace-ovi**: Micrometer Tracing (Brave) + Zipkin. Trace ID se prenosi kroz gateway,
  WebClient poziv i RabbitMQ poruku. Pregled na http://localhost:9411.

```bash
curl -s http://localhost:8083/actuator/prometheus | grep 'http_server_requests_seconds' | grep '/api/orders'
```

## Pokretanje lokalno

### Preduslovi

- JDK 17
- Docker i Docker Compose
- Maven nije potreban, svaki servis ima Maven Wrapper (`./mvnw`)

### 1. Infrastruktura

```bash
cd infrastructure
docker compose up -d                       # RabbitMQ i Zipkin
docker run -d --name postgres-dev \
  -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=integracije \
  -p 5432:5432 postgres:16                 # PostgreSQL za book-service
```

Ako kontejner `postgres-dev` već postoji: `docker start postgres-dev`.

### 2. Servisi

U pet terminala (ili iz IDE-a: Run As → Spring Boot App):

```bash
cd book-service && ./mvnw spring-boot:run
cd author-service && ./mvnw spring-boot:run
cd order-service && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd api-gateway && ./mvnw spring-boot:run
```

### 3. Primer

```bash
curl -i -X POST http://localhost:8080/api/books -H "Content-Type: application/json" \
  -d '{"bookName":"Clean Code","bookPrice":50.0,"authorName":"Robert C. Martin","bookGenre":"Technology","publishingYear":2008}'
curl http://localhost:8080/api/books
curl http://localhost:8080/api/authors
curl -i -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d '{"bookId":1,"quantity":2}'
curl http://localhost:8080/api/notifications
```

### Konfiguracija (environment promenljive)

| Promenljiva | Podrazumevano | Koristi |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/integracije`, `postgres`, `postgres` | book-service |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | `localhost`, `5672`, `guest`, `guest` | order, notification |
| `BOOK_SERVICE_URL` | `http://localhost:8081` | order-service, gateway |
| `AUTHOR_SERVICE_URL`, `ORDER_SERVICE_URL`, `NOTIFICATION_SERVICE_URL` | `http://localhost:8082/8083/8084` | gateway |
| `ZIPKIN_URL` | `http://localhost:9411/api/v2/spans` | svi servisi |

## Testovi

| Projekat | Testovi |
|---|---|
| `book-service` | unit (Mockito) za servis, `@WebMvcTest` za kontroler, `@DataJpaTest` za repozitorijum (H2 profil `test`) |
| `author-service` | `@WebMvcTest` za kontroler |
| `order-service` | unit za servis (Mockito + StepVerifier), test reaktivnog klijenta, `@WebFluxTest` za kontroler |
| `notification-service` | unit za listener, `@WebMvcTest` za kontroler |
| `api-gateway` | test rutiranja sa lažnim book-service-om |
| `e2e-tests` | end-to-end testovi kroz gateway nad pokrenutim sistemom |

Unit testovi ne zahtevaju bazu ni RabbitMQ (book-service u testovima koristi H2 u memoriji):

```bash
cd book-service && ./mvnw clean verify
```

Izveštaj o pokrivenosti koda (JaCoCo): `<servis>/target/site/jacoco/index.html`.

End-to-end testovi zahtevaju da ceo sistem radi (infrastruktura i svih 5 servisa):

```bash
cd e2e-tests && ./mvnw clean verify
```

Proveravaju: dostupnost autora, kreiranje i čitanje knjige, porudžbinu kroz book-service
i RabbitMQ do notification-service-a i 404 za nepostojeću knjigu.

## CI pipeline (GitHub Actions)

Workflow `.github/workflows/maven.yml` pokreće se na **svaki push** i **svaki pull request** ka `main`.

1. **Build and test** (matrica, 5 servisa paralelno): `./mvnw clean verify` – build, unit testovi i JaCoCo.
   Artefakti: JAR svakog servisa i izveštaji testova i pokrivenosti.
2. **End-to-end tests** (posle build-a): podiže PostgreSQL, RabbitMQ i Zipkin kao service kontejnere,
   pokreće svih 5 servisa iz JAR artefakata, čeka da svi budu `UP` i izvršava `e2e-tests`.
   Artefakt: izveštaj e2e testova.

## Git workflow

- Rad isključivo kroz feature grane (`feature/...`).
- Svaka izmena ide kroz pull request ka `main`; PR se spaja tek kada CI prođe.
- `main` sadrži samo stabilan kod.

## Struktura repozitorijuma

```
integracije/
├── api-gateway/
├── author-service/
├── book-service/
├── notification-service/
├── order-service/
├── e2e-tests/
├── infrastructure/
│   └── docker-compose.yml
└── .github/workflows/maven.yml
```
