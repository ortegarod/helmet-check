package com.oldschooldb;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class AuthService {
    private final OkHttpClient client;
    private final Gson gson;
    private final String serverUrl;
    private String apiToken;

    public AuthService(String serverUrl) {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.serverUrl = serverUrl;
    }

    public CompletableFuture<Boolean> authenticateToken(String apiToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/auth/test")
                    .get()
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        
                        if (jsonResponse.has("user")) {
                            JsonObject user = jsonResponse.getAsJsonObject("user");
                            JsonObject token = jsonResponse.getAsJsonObject("token");
                            log.info("Successfully authenticated with OldSchoolDB - User: {}, Token: {}", 
                                user.get("email").getAsString(), 
                                token.get("name").getAsString());
                            this.apiToken = apiToken; // Store the token for future requests
                            return true;
                        }
                    } else {
                        log.error("Token authentication failed with status: {}", response.code());
                        if (response.body() != null) {
                            log.error("Response: {}", response.body().string());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Token authentication request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(serverUrl + "/api/items/mappings")
                    .get()
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    boolean connected = response.isSuccessful();
                    log.info("Server connection test: {}", connected ? "SUCCESS" : "FAILED");
                    return connected;
                }
            } catch (IOException e) {
                log.error("Connection test failed", e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> sendBankData(Long accountHash, Item[] bankItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> items = new ArrayList<>();
                
                for (Item item : bankItems) {
                    if (item.getId() <= 0 || item.getQuantity() <= 0) {
                        continue; // Skip empty slots
                    }
                    
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("item_id", item.getId());
                    itemData.put("quantity", item.getQuantity());
                    items.add(itemData);
                }

                Map<String, Object> bankData = new HashMap<>();
                bankData.put("account_hash", accountHash);
                bankData.put("timestamp", System.currentTimeMillis());
                bankData.put("items", items);

                String jsonBody = gson.toJson(bankData);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/bank/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Bank data synced successfully for account: {}", accountHash);
                        return true;
                    } else {
                        log.error("Bank sync failed with status: {}", response.code());
                        if (response.body() != null) {
                            log.error("Response: {}", response.body().string());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Bank sync request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> sendInventoryData(Long accountHash, Item[] inventoryItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> items = new ArrayList<>();
                
                for (Item item : inventoryItems) {
                    if (item.getId() <= 0 || item.getQuantity() <= 0) {
                        continue; // Skip empty slots
                    }
                    
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("item_id", item.getId());
                    itemData.put("quantity", item.getQuantity());
                    items.add(itemData);
                }

                Map<String, Object> inventoryData = new HashMap<>();
                inventoryData.put("account_hash", accountHash);
                inventoryData.put("timestamp", System.currentTimeMillis());
                inventoryData.put("items", items);

                String jsonBody = gson.toJson(inventoryData);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/inventory/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Inventory data synced successfully for account: {}", accountHash);
                        return true;
                    } else {
                        log.error("Inventory sync failed with status: {}", response.code());
                        if (response.body() != null) {
                            log.error("Response: {}", response.body().string());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Inventory sync request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> sendEquipmentData(Long accountHash, Item[] equipmentItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> items = new ArrayList<>();
                
                for (Item item : equipmentItems) {
                    if (item.getId() <= 0 || item.getQuantity() <= 0) {
                        continue; // Skip empty equipment slots
                    }
                    
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("item_id", item.getId());
                    itemData.put("quantity", item.getQuantity());
                    items.add(itemData);
                }

                Map<String, Object> equipmentData = new HashMap<>();
                equipmentData.put("account_hash", accountHash);
                equipmentData.put("timestamp", System.currentTimeMillis());
                equipmentData.put("items", items);

                String jsonBody = gson.toJson(equipmentData);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/equipment/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Equipment data synced successfully for account: {}", accountHash);
                        return true;
                    } else {
                        log.error("Equipment sync failed with status: {}", response.code());
                        if (response.body() != null) {
                            log.error("Response: {}", response.body().string());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Equipment sync request failed", e);
            }
            return false;
        });
    }
}