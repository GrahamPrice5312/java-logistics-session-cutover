package com.example.logistics;

public final class CutoverRunner {
    public static void main(String[] args) throws Exception {
        String refreshToken = System.getenv("INFRAI_REFRESH_TOKEN");
        String oldSession = System.getenv("INFRAI_OLD_SESSION_ID");
        if (refreshToken == null || oldSession == null) {
            throw new IllegalStateException("INFRAI_REFRESH_TOKEN and INFRAI_OLD_SESSION_ID are required");
        }
        InfraiSessionClient client = new InfraiSessionClient();
        InfraiSessionClient.SessionResult refreshed = client.refresh(refreshToken);
        InfraiSessionClient.SessionResult revoked = client.revoke(oldSession);
        ShipmentCutoverService.CutoverResult result = new ShipmentCutoverService().decide(
                new ShipmentCutoverService.Shipment("SHP-2048", "pod/SHP-2048.pdf", ""), refreshed, revoked);
        System.out.println(result.event() + " " + result.proofOfDelivery() + " " + result.decision());
    }
}
