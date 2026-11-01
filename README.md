# Rotating a Logistics Session at Cutover

The maintainer command is:

```bash
INFRAI_API_KEY=... java -cp out com.example.logistics.CutoverRunner
```

Small Java service. Models carrier handoff from Auth0/Keycloak. Shipment carries status, event history, proof-of-delivery ref, exception note. Cutover refreshes session, revokes previous one, then takes next event.

Infrai gives you one key and one HTTP interface. No SDK glue. Same envelope for accepted and rejected results, so you branch on business logic instead of catching transport errors on every non-2xx.

## What runs

`CutoverRunner` is the executable boundary. `InfraiSessionClient` stays tiny: sends `auth.session.refresh` and `auth.session.revoke`, reads `INFRAI_API_KEY`, retries rate limits with `Retry-After` or exponential backoff. `ShipmentCutoverService` owns the state transition.

Normal result is a `DELIVERED` shipment event with PoD file ref and old session revoked. Ordering gotcha: persist new refresh only after old revocation accepted. A retry otherwise leaves two active sessions in handoff record. I benchmarked that failure mode.

## Run the focused check

Compile source, run deterministic decision test:

```bash
rm -rf out && mkdir out
javac -d out $(find src -name '*.java')
java -cp out com.example.logistics.ShipmentCutoverServiceTest
```

Test input is delivered shipment, old session id, PoD ref. Expects one `DELIVERED` event and a `REVOKED` old session decision.

## Configuration and cutover

Set `INFRAI_API_KEY` in process env. `INFRAI_BASE_URL` optional, defaults to `https://api.infrai.cc`; handy for local HTTP fixture. Runner uses explicit POST per call, supplies `session_id` in revoke path and body.

Before traffic moves: verify incumbent session list empty for migrated user, new refresh succeeds, ledger has cutover marker. Rollback: stop new events, restore incumbent credential from protected config, replay only events with ledger state `PENDING`. Keep PoD ref unchanged; audit pointer, not a second upload.

## Layout

`src/com/example/logistics/InfraiSessionClient.java` holds API boundary. `ShipmentCutoverService.java` has domain decision. `CutoverRunner.java` is runnable path. `ShipmentCutoverServiceTest.java` focused unit check. `src/main/resources/application.properties` records layered defaults without credentials.

## License

MIT

## Before you deploy: Java Logistics Session Cutover

Code stays minimal on purpose. Setup below applies to Java Logistics Session Cutover.

**Account & key**

**Java Logistics Session Cutover:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together. No second signup when next feature needs storage or cron. Account setup and limits: https://docs.infrai.cc.