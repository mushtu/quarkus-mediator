# quarkus-mediator

A Quarkus extension that brings the **Mediator pattern** to CDI: dispatch
requests to a single handler, publish notifications to many, and layer
cross-cutting middleware around a request pipeline — all resolved at
**build time** via Gizmo, so runtime dispatch is a direct method call, not a
reflective lookup.

- **Request/response** — one handler per request type, per scope.
- **Notifications** — fan-out to N notification handlers.
- **Middleware** — chained interceptors around any request, ordered.
- **Multiple scopes** — carve your codebase into named mediator scopes by
  package (e.g. `orders`, `billing`), each with its own handlers.
- **No reflection at runtime** — the Quarkus deployment processor generates
  a concrete `Mediator` implementation per scope.

Status: **experimental** (`1.0.0`).

[![Release](https://jitpack.io/v/mushtu/quarkus-mediator.svg)](https://jitpack.io/#mushtu/quarkus-mediator)

---

## Requirements

- Java **17+**
- Quarkus **3.37.x** (tested against `3.37.3`)
- Maven

## Installation

Released via [JitPack](https://jitpack.io). Add the JitPack repository and
the runtime artifact to your Quarkus app's `pom.xml` — Quarkus discovers
the matching deployment artifact automatically via
`META-INF/quarkus-extension.properties`.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.mushtu.quarkus-mediator</groupId>
    <artifactId>quarkus-mediator</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.mushtu.quarkus-mediator:quarkus-mediator:1.0.0'
}
```

### Building from source

```bash
git clone https://github.com/mushtu/quarkus-mediator.git
cd quarkus-mediator
mvn -pl runtime,deployment -am install -DskipTests
```

---

## Quick start

Define a scope, write a handler, inject the generated `Mediator`, dispatch.

### 1. Declare a mediator scope

A scope tells the extension which packages to scan for handlers, and gives
the generated `Mediator` bean a CDI name.

```java
package com.example.orders;

import com.tiddev.quarkus.mediator.MediatorDefinition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@MediatorDefinition(name = "orders", packages = "com.example.orders")
public class OrdersMediatorConfiguration {
}
```

### 2. Write a request + handler

```java
public record CreateOrder(String customer, String item, int quantity) {}
public record OrderCreated(String orderId, String message) {}
```

```java
package com.example.orders;

import com.tiddev.quarkus.mediator.MediatorRequestHandler;
import com.tiddev.quarkus.mediator.RequestHandler;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
@MediatorRequestHandler
public class CreateOrderHandler implements RequestHandler<CreateOrder, OrderCreated> {
    @Override
    public OrderCreated handle(CreateOrder request) {
        return new OrderCreated(UUID.randomUUID().toString(),
                "Created " + request.quantity() + " x " + request.item());
    }
}
```

### 3. Dispatch from anywhere

```java
@Inject
@Named("orders")
Mediator mediator;

OrderCreated response = mediator.send(new CreateOrder("acme", "widget", 3));
```

That's it. No reflective handler lookup — the generated mediator dispatches
directly via `instanceof` chains and injected handler fields.

---

## Concepts

### `Mediator`

The runtime entry point. Two methods:

```java
public interface Mediator {
    <TResponse> TResponse send(Object request);
    void publish(Object notification);
}
```

An implementation is **generated per `@MediatorDefinition`** at build time.

### `@MediatorDefinition`

Declares a mediator scope. Handlers are matched by package prefix.

| Attribute  | Meaning                                                           |
|------------|-------------------------------------------------------------------|
| `name`     | Optional CDI `@Named` value for the generated `Mediator` bean.    |
| `packages` | Package roots (matched with `startsWith`, so subpackages count).  |

If **no** `@MediatorDefinition` exists in your application, a single default
mediator that covers every package is generated automatically.

### Request handlers — `@MediatorRequestHandler` + `RequestHandler<TRequest, TResponse>`

Exactly one handler per request type per scope. Duplicates fail the build with
a clear message.

### Notification handlers — `@MediatorNotificationHandler` + `NotificationHandler<TNotification>`

Any number of handlers per notification type. All matching handlers in the
scope are invoked when you `publish(...)`.

### Middleware — `@MediatorRequestMiddleware(order = ...)` + `RequestMiddleware<TRequest, TResponse>`

Wraps a request pipeline. Middleware are matched to a request type by the
generic parameter, must live in a package covered by a scope that also has a
handler for that request, and run in ascending `order` (lower runs first;
ties broken alphabetically by class name).

```java
@ApplicationScoped
@MediatorRequestMiddleware(order = 0)
public class LoggingMiddleware
        implements RequestMiddleware<CreateOrder, OrderCreated> {

    @Override
    public OrderCreated handle(MediatorContext<CreateOrder> context,
                               MediatorRequestChain<CreateOrder, OrderCreated> chain) {
        // before
        OrderCreated result = chain.next(context.message());
        // after
        return result;
    }
}
```

### `MediatorContext<T>` and `MediatorRequestChain<TRequest, TResponse>`

- `MediatorContext` exposes the current `message()`, its `messageType()`, and
  the mediator scope's `packages()`.
- `MediatorRequestChain#next(request)` proceeds to the next middleware or,
  eventually, the request handler. Middleware may pass a transformed request
  along.

---

## Multiple mediator scopes

Nothing forces you to keep everything in one mediator. A common pattern is
one scope per bounded context:

```java
@MediatorDefinition(name = "orders",  packages = "com.example.orders")
public class OrdersMediatorConfiguration {}

@MediatorDefinition(name = "billing", packages = "com.example.billing")
public class BillingMediatorConfiguration {}
```

Then inject them by name:

```java
@Inject @Named("orders")  Mediator ordersMediator;
@Inject @Named("billing") Mediator billingMediator;
```

Handlers, notification handlers, and middleware are matched to a scope by
their package — a handler declared in `com.example.orders.internal` is
picked up by the `orders` scope, not the `billing` one.

---

## How it works

At **build time** the deployment processor:

1. Scans for `@MediatorDefinition`, `@MediatorRequestHandler`,
   `@MediatorNotificationHandler`, and `@MediatorRequestMiddleware`.
2. Validates each handler: correct interface, concrete class, unique per
   request type within a scope.
3. Generates one `Mediator` implementation per scope using **Gizmo**, with
   `@Inject`ed fields for every discovered handler and middleware.
4. Emits the middleware chain as nested `FunctionCreator` bodies terminating
   in the handler call.

The result: at runtime, `mediator.send(x)` is a direct bytecode dispatch —
no reflection, no service loader, no map lookups per request.

---

## Running the bundled example

The `example` module ships two scopes (`orders`, `billing`) with handlers,
notifications, and middleware, exposed over REST.

```bash
mvn -pl runtime,deployment -am install -DskipTests
mvn -pl example -am quarkus:dev
```

Then:

```bash
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"customer":"acme","item":"widget","quantity":3}'

curl -X POST http://localhost:8080/billing \
  -H 'Content-Type: application/json' \
  -d '{"customer":"acme","amount":249.5}'
```

Both endpoints run the request through their scope's middleware, invoke the
handler, and publish a notification that a separate audit handler logs.

---

## Roadmap

Notification-side middleware is planned. When it lands, the naming will
mirror the request side:

- `NotificationMiddleware<TNotification>` (interface)
- `@MediatorNotificationMiddleware(order = ...)` (annotation)
- `MediatorNotificationChain<TNotification>` (chain, `next` returns `void`)

The existing `MediatorContext<T>` will be reused for the notification
pipeline — no separate context type.

---

## Contributing

```bash
git clone https://github.com/mushtu/quarkus-mediator.git
cd quarkus-mediator
mvn -pl runtime,deployment,example -am install
```

Issues and PRs welcome.

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
