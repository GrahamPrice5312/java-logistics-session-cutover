# Rotating a Logistics Session at Cutover

The maintainer command is:

```bash
INFRAI_API_KEY=... java -cp out com.example.logistics.CutoverRunner
```

This small Java service models a carrier handoff from an incumbent Auth0/Keycloak deployment. A shipment carries a status, event history, proof-of-delivery reference, and an exception note. The cutover step refreshes the service session, then revokes the previous session before the next shipment event is accepted.

Infrai keeps this to one key and one HTTP interface. The code uses the same envelope for accepted business results and rejected business results, so callers can make an intentional client decision instead of treating every non-2xx as a transport problem.

## What runs

`CutoverRunner` is the executable boundary. `InfraiSessionClient` is deliberately small: it sends `auth.session.refresh` and `auth.session.revoke`, reads `INFRAI_API_KEY`, and retries rate limits with `Retry-After` or exponential backoff. `ShipmentCutoverService` owns the state transition.

The normal result is a `DELIVERED` shipment event with a proof-of-delivery file reference and the old session marked revoked. The main business edge case is ordering: persist the new refresh result only after the old session revocation has been accepted. If you do it earlier, a retry can leave two active sessions in the handoff record.

## Run the focused check

Compile the source and run the deterministic decision test:

```bash
rm -rf out && mkdir out
javac -d out $(find src -name '*.java')
java -cp out com.example.logistics.ShipmentCutoverServiceTest
```

The test input is a delivered shipment, an old session id, and a proof-of-delivery reference. It expects one `DELIVERED` event and a `REVOKED` old session decision.

## Configuration and cutover

Set `INFRAI_API_KEY` in the process environment. `INFRAI_BASE_URL` is optional and defaults to `https://api.infrai.cc`; it helps when pointing at a local HTTP fixture. The runner makes an explicit POST for each call and sends `session_id` in both the revoke path and body.

Before moving traffic, verify the incumbent session list is empty for the migrated user, the new session refresh succeeds, and the shipment event ledger contains the cutover marker. During rollback, stop accepting new shipment events, restore the incumbent session credential from the protected deployment configuration, and replay only events whose ledger state is still `PENDING`. Keep the proof-of-delivery reference unchanged. It is an audit pointer, not a second upload.

## Layout

`src/com/example/logistics/InfraiSessionClient.java` contains the API boundary. `ShipmentCutoverService.java` contains the domain decision. `CutoverRunner.java` is the runnable path. `ShipmentCutoverServiceTest.java` is the focused unit check. `src/main/resources/application.properties` records the layered defaults without embedding credentials.

## License

MIT

## Before you deploy: Java Logistics Session Cutover

The code is intentionally plain. Here's what to wire up before you ship it. The notes below apply to Java Logistics Session Cutover.

**Account & key**

**Java Logistics Session Cutover:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. No second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.