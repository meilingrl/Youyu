package com.campusmarket.backend.user;

import com.campusmarket.backend.BackendTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileTest extends BackendTestBase {

    private static final String USER = "mock-1001-USER";

    @Test
    void getProfileReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.nickname").exists())
                .andExpect(jsonPath("$.data.privilege").exists());
    }

    @Test
    void getProfileUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getVerificationStatus() throws Exception {
        mockMvc.perform(get("/api/users/verification")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("approved"));
    }

    @Test
    void getAddressesReturnsList() throws Exception {
        String response = mockMvc.perform(get("/api/users/addresses")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        java.util.List<Object> addresses = JsonPath.read(response, "$.data");
        org.junit.jupiter.api.Assertions.assertFalse(addresses.isEmpty());
    }

    @Test
    void createAddressSuccess() throws Exception {
        String response = mockMvc.perform(post("/api/users/addresses")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("""
                                {
                                  "receiverName": "Test User",
                                  "receiverPhone": "13900000001",
                                  "addressType": "campus",
                                  "campusArea": "NEU Hunnan Campus Dorm 5",
                                  "detailAddress": "Building 5 Room 101",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiverName").value("Test User"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void setDefaultAddress() throws Exception {
        String createResponse = mockMvc.perform(post("/api/users/addresses")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("""
                                {
                                  "receiverName": "Default Me",
                                  "receiverPhone": "13900000002",
                                  "addressType": "campus",
                                  "campusArea": "NEU Hunnan Campus Dorm 8",
                                  "detailAddress": "Building 8 Room 202",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number addressId = JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(put("/api/users/addresses/{addressId}/default", addressId)
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void authLogoutReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void authMeReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.role").exists());
    }
}
