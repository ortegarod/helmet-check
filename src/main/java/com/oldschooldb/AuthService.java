package com.oldschooldb;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
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

    public AuthService(String serverUrl, OkHttpClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
        this.serverUrl = serverUrl;
    }

    public CompletableFuture<String> authenticateToken(String apiToken) {
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
                            log.info("Authenticated successfully");
                            this.apiToken = apiToken;
                            return null; // null = success
                        }
                        return "Auth response missing user data";
                    } else if (response.code() == 401) {
                        return "Token invalid or expired (401) - generate a new token";
                    } else if (response.code() == 403) {
                        return "Token forbidden (403) - check token permissions";
                    } else {
                        return "Server error (" + response.code() + ")";
                    }
                }
            } catch (IOException e) {
                return "Cannot reach server at " + serverUrl + " - is it running?";
            }
        });
    }

    public CompletableFuture<String> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(serverUrl + "/api/items/mappings")
                    .get()
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.info("Server connection test: SUCCESS");
                        return null; // null = success
                    }
                    return "Server returned " + response.code();
                }
            } catch (IOException e) {
                return "Cannot reach server at " + serverUrl + " - check Server URL in plugin settings";
            }
        });
    }

    public CompletableFuture<Boolean> sendBankData(Long accountHash, String playerName, List<Map<String, Object>> bankItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> bankData = new HashMap<>();
                bankData.put("account_hash", accountHash);
                bankData.put("player_name", playerName);
                bankData.put("timestamp", System.currentTimeMillis());
                bankData.put("items", bankItems);

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

    public CompletableFuture<Boolean> sendInventoryData(Long accountHash, String playerName, List<Map<String, Object>> inventoryItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> inventoryData = new HashMap<>();
                inventoryData.put("account_hash", accountHash);
                inventoryData.put("player_name", playerName);
                inventoryData.put("timestamp", System.currentTimeMillis());
                inventoryData.put("items", inventoryItems);

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

    public CompletableFuture<Boolean> sendSkillData(Long accountHash, Skill[] skills, int[] levels, int[] xp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> skillList = new ArrayList<>();
                for (int i = 0; i < skills.length; i++) {
                    Map<String, Object> s = new HashMap<>();
                    s.put("skill", skills[i].getName());
                    s.put("level", levels[i]);
                    s.put("xp", xp[i]);
                    skillList.add(s);
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("account_hash", accountHash);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("skills", skillList);

                String jsonBody = gson.toJson(payload);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/skills/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (IOException e) {
                log.error("Skill sync request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> sendQuestData(Long accountHash, String[] names, String[] states) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> questList = new ArrayList<>();
                for (int i = 0; i < names.length; i++) {
                    Map<String, Object> q = new HashMap<>();
                    q.put("name", names[i]);
                    q.put("state", states[i]);
                    questList.add(q);
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("account_hash", accountHash);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("quests", questList);

                String jsonBody = gson.toJson(payload);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/quests/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (IOException e) {
                log.error("Quest sync request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> sendPrayerUnlockData(Long accountHash, List<Map<String, Object>> unlocks) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("account_hash", accountHash);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("unlocks", unlocks);

                String jsonBody = gson.toJson(payload);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/prayer-unlocks/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Prayer unlocks synced successfully for account: {}", accountHash);
                        return true;
                    } else {
                        log.error("Prayer unlock sync failed with status: {}", response.code());
                        if (response.body() != null) {
                            log.error("Response: {}", response.body().string());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Prayer unlock sync request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> sendTimeTrackingData(Long accountHash, String profileKey, Map<String, String> timeTrackingEntries) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> entries = new ArrayList<>();
                for (Map.Entry<String, String> entry : timeTrackingEntries.entrySet()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("key", entry.getKey());
                    data.put("value", entry.getValue());
                    entries.add(data);
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("account_hash", accountHash);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("profile_key", profileKey);
                payload.put("entries", entries);

                String jsonBody = gson.toJson(payload);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/timetracking/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Time tracking data synced successfully for account: {}", accountHash);
                        return true;
                    } else {
                        log.error("Time tracking sync failed with status: {}", response.code());
                        if (response.body() != null) {
                            log.error("Response: {}", response.body().string());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Time tracking sync request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> sendSlayerTaskData(Long accountHash, String taskName, String taskLocation, Integer initialAmount, Integer remainingAmount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("account_hash", accountHash);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("task_name", taskName);
                payload.put("task_location", taskLocation);
                payload.put("initial_amount", initialAmount);
                payload.put("remaining_amount", remainingAmount);

                String jsonBody = gson.toJson(payload);
                RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

                Request request = new Request.Builder()
                    .url(serverUrl + "/api/plugin/slayer-task/sync")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("User-Agent", "OldSchoolDB-Plugin/1.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.debug("Slayer task synced successfully for account: {}", accountHash);
                        return true;
                    } else {
                        log.error("Slayer task sync failed with status: {}", response.code());
                        if (response.body() != null) {
                            log.error("Response: {}", response.body().string());
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Slayer task sync request failed", e);
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> sendEquipmentData(Long accountHash, String playerName, List<Map<String, Object>> equipmentItems) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> equipmentData = new HashMap<>();
                equipmentData.put("account_hash", accountHash);
                equipmentData.put("player_name", playerName);
                equipmentData.put("timestamp", System.currentTimeMillis());
                equipmentData.put("items", equipmentItems);

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
