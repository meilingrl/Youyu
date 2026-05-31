package com.youyu.backend.user;

import com.youyu.backend.BackendTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    void updateProfileChangesNicknameOnly() throws Exception {
        mockMvc.perform(patch("/api/users/profile")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"nickname\":\"Nickname From Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.nickname").value("Nickname From Test"))
                .andExpect(jsonPath("$.data.user.username").value("zhangsan"));
    }

    @Test
    void updateProfileRejectsLoginIdChange() throws Exception {
        mockMvc.perform(patch("/api/users/profile")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"nickname\":\"Still Test\",\"username\":\"new-login\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void uploadAvatarSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.avatar").value(org.hamcrest.Matchers.startsWith("/uploads/avatars/user-1001-")))
                .andExpect(jsonPath("$.data.avatarUrl").value(org.hamcrest.Matchers.startsWith("/uploads/avatars/user-1001-")));
    }

    @Test
    void uploadAvatarRejectsUnsupportedType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                "not image".getBytes()
        );

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void uploadAvatarRejectsOversizedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[(10 * 1024 * 1024) + 1]
        );

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void bindEmailPlaceholderValidatesFormatAndUniqueness() throws Exception {
        mockMvc.perform(put("/api/users/me/email")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(put("/api/users/me/email")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"email\":\"lisi@campus.edu.cn\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void bindEmailPlaceholderReturnsPendingStatus() throws Exception {
        mockMvc.perform(put("/api/users/me/email")
                        .header("Authorization", "Bearer " + USER)
                        .contentType("application/json")
                        .content("{\"email\":\"nickname-test@campus.edu.cn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("nickname-test@campus.edu.cn"))
                .andExpect(jsonPath("$.data.bindingStatus").value("pending_verification"))
                .andExpect(jsonPath("$.data.verificationEnabled").value(false))
                .andExpect(jsonPath("$.data.emailLoginEnabled").value(false));
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
        Assertions.assertFalse(addresses.isEmpty());
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
