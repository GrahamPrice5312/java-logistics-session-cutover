package com.example.logistics;

public final class ShipmentCutoverService {
    public enum Decision { REVOKED, REJECTED }
    public record Shipment(String trackingNumber, String proofOfDelivery, String exceptionNote) {}
    public record CutoverResult(Decision decision, String event, String proofOfDelivery) {}

    public CutoverResult decide(Shipment shipment, InfraiSessionClient.SessionResult refresh,
                                InfraiSessionClient.SessionResult revoke) {
        if (shipment.trackingNumber().isBlank() || shipment.proofOfDelivery().isBlank()
                || !refresh.ok() || !revoke.ok()) {
            return new CutoverResult(Decision.REJECTED, "PENDING", shipment.proofOfDelivery());
        }
        String event = shipment.exceptionNote().isBlank() ? "DELIVERED" : "DELIVERED_WITH_EXCEPTION";
        return new CutoverResult(Decision.REVOKED, event, shipment.proofOfDelivery());
    }
}
