package com.planit.domain.place.model;

public enum DestinationCode {

    KAOHSIUNG_TW(new GeoRectangle(new GeoPoint(22.4656, 120.0581), new GeoPoint(22.7890, 120.5447))),
    GUAM_US(new GeoRectangle(new GeoPoint(13.3006, 144.5905), new GeoPoint(13.5880, 144.9969))),
    NAGOYA_JP(new GeoRectangle(new GeoPoint(35.0198, 136.6318), new GeoPoint(35.3432, 137.1814))),
    NHA_TRANG_VN(new GeoRectangle(new GeoPoint(12.1220, 109.0312), new GeoPoint(12.3556, 109.3622))),
    DA_NANG_VN(new GeoRectangle(new GeoPoint(15.9376, 108.0339), new GeoPoint(16.1712, 108.3705))),
    TOKYO_JP(new GeoRectangle(new GeoPoint(35.4516, 139.2612), new GeoPoint(35.9008, 140.0394))),
    LONDON_GB(new GeoRectangle(new GeoPoint(51.2828, -0.6327), new GeoPoint(51.7320, 0.3771))),
    ROME_IT(new GeoRectangle(new GeoPoint(41.7411, 12.1944), new GeoPoint(42.0645, 12.7984))),
    MANILA_PH(new GeoRectangle(new GeoPoint(14.4378, 120.7515), new GeoPoint(14.7612, 121.2169))),
    MACAU_CN(new GeoRectangle(new GeoPoint(22.1268, 113.4468), new GeoPoint(22.2706, 113.6410))),
    BARCELONA_ES(new GeoRectangle(new GeoPoint(41.2257, 1.8693), new GeoPoint(41.5491, 2.4679))),
    BANGKOK_TH(new GeoRectangle(new GeoPoint(13.5317, 100.1783), new GeoPoint(13.9809, 100.8253))),
    BORACAY_PH(new GeoRectangle(new GeoPoint(11.8955, 121.8339), new GeoPoint(12.0393, 122.0157))),
    BOHOL_PH(new GeoRectangle(new GeoPoint(9.7063, 123.9428), new GeoPoint(9.9937, 124.3442))),
    SAIPAN_US(new GeoRectangle(new GeoPoint(15.1059, 145.6561), new GeoPoint(15.2497, 145.8457))),
    SAPPORO_JP(new GeoRectangle(new GeoPoint(42.9001, 141.0514), new GeoPoint(43.2235, 141.6576))),
    SHANGHAI_CN(new GeoRectangle(new GeoPoint(31.0058, 121.1067), new GeoPoint(31.4550, 121.8407))),
    CEBU_PH(new GeoRectangle(new GeoPoint(10.1540, 123.7201), new GeoPoint(10.4774, 124.0507))),
    SINGAPORE_SG(new GeoRectangle(new GeoPoint(1.2084, 103.6222), new GeoPoint(1.4958, 104.0174))),
    OSAKA_JP(new GeoRectangle(new GeoPoint(34.5320, 135.2289), new GeoPoint(34.8554, 135.7757))),
    OKINAWA_JP(new GeoRectangle(new GeoPoint(26.0956, 127.5013), new GeoPoint(26.3292, 127.8605))),
    CHIANG_MAI_TH(new GeoRectangle(new GeoPoint(18.6715, 98.8152), new GeoPoint(18.9051, 99.1554))),
    KOTA_KINABALU_MY(new GeoRectangle(new GeoPoint(5.8636, 115.9111), new GeoPoint(6.0972, 116.2359))),
    KUALA_LUMPUR_MY(new GeoRectangle(new GeoPoint(2.9773, 101.4621), new GeoPoint(3.3007, 101.9117))),
    TAIPEI_TW(new GeoRectangle(new GeoPoint(24.8713, 121.3171), new GeoPoint(25.1947, 121.8137))),
    PARIS_FR(new GeoRectangle(new GeoPoint(48.6949, 2.1061), new GeoPoint(49.0183, 2.5983))),
    PHU_QUOC_VN(new GeoRectangle(new GeoPoint(10.1462, 103.7824), new GeoPoint(10.4336, 104.1856))),
    HANOI_VN(new GeoRectangle(new GeoPoint(20.8661, 105.5942), new GeoPoint(21.1895, 106.0742))),
    HONG_KONG_CN(new GeoRectangle(new GeoPoint(22.1756, 113.9314), new GeoPoint(22.4630, 114.4074))),
    FUKUOKA_JP(new GeoRectangle(new GeoPoint(33.4734, 130.2067), new GeoPoint(33.7070, 130.5967)));

    private final GeoRectangle rectangle;

    DestinationCode(GeoRectangle rectangle) {
        this.rectangle = rectangle;
    }

    public GeoRectangle getRectangle() {
        return rectangle;
    }
}
