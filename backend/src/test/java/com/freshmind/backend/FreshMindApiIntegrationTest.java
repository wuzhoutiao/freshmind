package com.freshmind.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.freshmind.backend.inventory.InventoryItemRepository;
import com.freshmind.backend.notification.ExpiryNotificationJob;
import com.freshmind.backend.notification.ExpiryNotificationRepository;
import com.freshmind.backend.support.FixedClockTestConfig;
import com.freshmind.backend.user.UserAccountRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class FreshMindApiIntegrationTest {

    private static final String TOKEN_HEADER = "X-Token";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserAccountRepository users;

    @Autowired
    InventoryItemRepository items;

    @Autowired
    ExpiryNotificationRepository notifications;

    @Autowired
    ExpiryNotificationJob notificationJob;

    @BeforeEach
    void cleanUserData() {
        notifications.deleteAll();
        items.deleteAll();
        users.deleteAll();
    }

    @Test
    void protectedEndpointRequiresLoginSession() throws Exception {
        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/inventory"));
    }

    @Test
    void createInventoryEstimatesExpiryAndCanBeListed() throws Exception {
        String token = register("owner@example.com");

        MvcResult created = create(token,
                inventoryJson("Milk", "OTHERS", "2.000", "bottle", "2026-01-10", null));
        JsonNode item = objectMapper.readTree(created.getResponse().getContentAsString());

        mockMvc.perform(get("/api/v1/inventory")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(item.get("id").stringValue()))
                .andExpect(jsonPath("$.content[0].expiresOn").value("2026-01-17"))
                .andExpect(jsonPath("$.content[0].expiryEstimated").value(true))
                .andExpect(jsonPath("$.content[0].status").value("NEAR_EXPIRY"));
    }

    @Test
    void inventoryCanBeUpdatedAndPartiallyConsumed() throws Exception {
        String token = register("update@example.com");
        MvcResult created = create(token,
                inventoryJson("Tomato", "VEGETABLES", "3", "pcs", "2026-01-10", "2026-01-20"));
        String itemId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").stringValue();

        mockMvc.perform(patch("/api/v1/inventory/{id}", itemId)
                        .header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryJson(
                                "Tomato", "VEGETABLES", "4", "pcs", "2026-01-10", "2026-01-20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4));

        mockMvc.perform(post("/api/v1/inventory/{id}/consume", itemId)
                        .header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(3))
                .andExpect(jsonPath("$.depleted").value(false));
    }

    @Test
    void inventoryIsIsolatedBetweenUsers() throws Exception {
        String ownerToken = register("first@example.com");
        String otherToken = register("second@example.com");
        MvcResult created = create(ownerToken,
                inventoryJson("Apple", "FRUITS", "2", "pcs", "2026-01-10", "2026-02-01"));
        String itemId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").stringValue();

        mockMvc.perform(get("/api/v1/inventory/{id}", itemId)
                        .header(TOKEN_HEADER, otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void recommendationExplainsMatchedAndMissingIngredients() throws Exception {
        String token = register("chef@example.com");
        create(token, inventoryJson("Rice", "OTHERS", "1", "bowl", "2026-01-10", "2026-04-01"));
        create(token, inventoryJson("Egg", "OTHERS", "6", "pcs", "2026-01-10", "2026-01-17"));
        create(token, inventoryJson("Tomato", "VEGETABLES", "2", "pcs", "2026-01-10", "2026-01-17"));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipe.name").value("Tomato Egg Fried Rice"))
                .andExpect(jsonPath("$[0].matchRate").value(0.6))
                .andExpect(jsonPath("$[0].nearExpiryIngredientsUsed.length()").value(2))
                .andExpect(jsonPath("$[0].missingIngredients.length()").value(2));
    }

    @Test
    void recommendationIgnoresCustomIngredientsOutsideTheCatalog() throws Exception {
        String token = register("custom@example.com");
        create(token, inventoryJson(
                "Mystery Powder", "OTHERS", "1", "bag", "2026-01-10", "2026-02-01"));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void dashboardDoesNotWaitForOrEmbedAiRecommendations() throws Exception {
        String token = register("dashboard@example.com");
        create(token, inventoryJson(
                "Tomato", "VEGETABLES", "2", "pcs", "2026-01-10", "2026-01-17"));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.topRecommendations").doesNotExist());
    }

    @Test
    void dailyJucJobCreatesOneExpiryNotificationPerItem() throws Exception {
        String token = register("notice@example.com");
        create(token, inventoryJson("Milk", "OTHERS", "1", "bottle", "2026-01-10", "2026-01-17"));

        notificationJob.generateDailyNotifications();
        notificationJob.generateDailyNotifications();

        mockMvc.perform(get("/api/v1/notifications")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ingredientName").value("Milk"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        String token = register("logout@example.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/inventory")
                        .header(TOKEN_HEADER, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidDatesReturnSimpleApiError() throws Exception {
        String token = register("validation@example.com");

        mockMvc.perform(post("/api/v1/inventory")
                        .header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryJson(
                                "Milk", "OTHERS", "1", "bottle", "2026-01-16", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/inventory"));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "correct-horse-battery-staple",
                                "displayName", "Test User"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").stringValue();
    }

    private MvcResult create(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory")
                        .header(TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
    }

    private String inventoryJson(
            String name,
            String category,
            String quantity,
            String unit,
            String purchasedOn,
            String expiresOn) throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ingredientName", name);
        values.put("category", category);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("purchasedOn", purchasedOn);
        if (expiresOn != null) {
            values.put("expiresOn", expiresOn);
        }
        return objectMapper.writeValueAsString(values);
    }
}
