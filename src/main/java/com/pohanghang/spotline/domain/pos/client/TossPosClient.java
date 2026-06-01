package com.pohanghang.spotline.domain.pos.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Component
public class TossPosClient {

    private final WebClient tossPosWebClient;

    @Value("${toss-pos.merchant-id}")
    private String merchantId;

    @Value("${toss-pos.access-key}")
    private String accessKey;

    @Value("${toss-pos.secret-key}")
    private String secretKey;

    public TossPosClient(@Qualifier("tossPosWebClient") final WebClient tossPosWebClient) {
        this.tossPosWebClient = tossPosWebClient;
    }

    // 구간 내 완료 주문의 netAmount 합계 반환 (원)
    public int getTotalSales(final LocalDateTime startAt, final LocalDateTime endAt) {
        /*
        long from = startAt.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long to   = endAt.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        List<Map<String, Object>> orders = tossPosWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/merchants/{merchantId}/order/orders")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("orderStates", "COMPLETED")
                        .queryParam("size", 1000)
                        .build(merchantId))
                .header("x-access-key", accessKey)
                .header("x-secret-key", secretKey)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();

        return orders == null ? -1 : orders.stream()
                .mapToInt(o -> ((Number) o.get("netAmount")).intValue())
                .sum();
        */
        return 187500; // 목업
    }

    // 구간 내 가장 많이 팔린 메뉴명 반환
    public String getBestMenuName(final LocalDateTime startAt, final LocalDateTime endAt) {
        /*
        long from = startAt.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        long to   = endAt.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

        List<Map<String, Object>> orders = tossPosWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/merchants/{merchantId}/order/orders")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .queryParam("orderStates", "COMPLETED")
                        .queryParam("size", 1000)
                        .build(merchantId))
                .header("x-access-key", accessKey)
                .header("x-secret-key", secretKey)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList()
                .block();

        if (orders == null || orders.isEmpty()) return null;

        Map<String, long[]> itemStats = new HashMap<>(); // [quantity, totalPrice]
        Map<String, String> itemNames = new HashMap<>();
        for (Map<String, Object> order : orders) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            for (Map<String, Object> item : items) {
                String id = (String) item.get("itemId");
                itemNames.put(id, (String) item.get("itemName"));
                itemStats.computeIfAbsent(id, k -> new long[]{0, 0});
                itemStats.get(id)[0] += ((Number) item.get("quantity")).longValue();
            }
        }
        return itemStats.entrySet().stream()
                .max(Comparator.comparingLong(e -> e.getValue()[0]))
                .map(e -> itemNames.get(e.getKey()))
                .orElse(null);
        */
        return "아메리카노"; // 목업
    }
}
