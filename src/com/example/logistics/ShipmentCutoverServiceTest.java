package com.example.logistics;

public final class ShipmentCutoverServiceTest {
    public static void main(String[] args) {
        ShipmentCutoverService.Shipment shipment = new ShipmentCutoverService.Shipment("SHP-2048", "pod/SHP-2048.pdf", "customs review");
        InfraiSessionClient.SessionResult accepted = new InfraiSessionClient.SessionResult(true, "", "", 200);
        ShipmentCutoverService.CutoverResult result = new ShipmentCutoverService().decide(shipment, accepted, accepted);
        require(result.decision() == ShipmentCutoverService.Decision.REVOKED, "old session must be revoked");
        require(result.event().equals("DELIVERED_WITH_EXCEPTION"), "exception must remain visible");
        require(result.proofOfDelivery().equals("pod/SHP-2048.pdf"), "proof reference must be retained");
        System.out.println("shipment cutover decision: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
