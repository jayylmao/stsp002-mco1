package com.fintech.mco1;

import com.fintech.mco1.security.JwtUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of register / login / JWT-protected access.
 * Runs against in-memory H2 with its own throwaway signing key, so it never touches the real MySQL database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authflow;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=" + AuthFlowTest.SECRET,
        "jwt.expiration-ms=3600000"
})
@AutoConfigureMockMvc
class AuthFlowTest {
    static final String SECRET = "test-only-secret-test-only-secret-1234567890";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    // ---- register ----

    @Test
    void registerCreatesAnApplicantAndNeverReturnsThePassword() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Test User", email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("APPLICANT"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void registerIgnoresARoleSentByTheClient() throws Exception {
        String body = """
                {"fullName":"Sneaky","email":"%s","password":"%s","role":"ADMIN"}
                """.formatted(uniqueEmail(), PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("APPLICANT"));
    }

    @Test
    void registerRejectsADuplicateEmailEvenWithDifferentCapitalisation() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Someone Else", email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorType").value("Conflict"));
    }

    @Test
    void registerReportsEveryValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("", "not-an-email", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("Validation Failed"))
                .andExpect(jsonPath("$.message", containsString("Full name is required")))
                .andExpect(jsonPath("$.message", containsString("valid email")))
                .andExpect(jsonPath("$.message", containsString("Password must be between")));
    }

    @Test
    void registerRejectsAMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    // ---- login ----

    @Test
    void loginReturnsATokenThatOpensProtectedEndpoints() throws Exception {
        String email = uniqueEmail();
        register(email);

        String token = loginAndGetToken(email, PASSWORD);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("APPLICANT"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void loginResponseDescribesTheToken() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.data.user.email").value(email));
    }

    @Test
    void loginTreatsEmailCaseInsensitively() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("  " + email.toUpperCase() + " ", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailAreIndistinguishable() throws Exception {
        String email = uniqueEmail();
        register(email);

        String wrongPassword = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "not-the-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(uniqueEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(
                JsonPath.<String>read(wrongPassword, "$.message"),
                JsonPath.<String>read(unknownEmail, "$.message"));
        org.junit.jupiter.api.Assertions.assertEquals("Invalid email or password",
                JsonPath.<String>read(wrongPassword, "$.message"));
    }

    // ---- protected access ----

    @Test
    void protectedEndpointRejectsRequestsWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorType").value("Unauthorized"));
    }

    @Test
    void protectedEndpointRejectsAGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsAnExpiredToken() throws Exception {
        String email = uniqueEmail();
        register(email);
        String expired = new JwtUtil(SECRET, -60_000).generateToken(email);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsATokenSignedWithAnotherKey() throws Exception {
        String email = uniqueEmail();
        register(email);
        String forged = new JwtUtil("a-completely-different-secret-0987654321", 3_600_000).generateToken(email);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsATamperedToken() throws Exception {
        String email = uniqueEmail();
        register(email);
        String[] parts = loginAndGetToken(email, PASSWORD).split("\\.");
        parts[1] = (parts[1].charAt(0) == 'A' ? 'B' : 'A') + parts[1].substring(1);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenForAnUnknownUserIsRejected() throws Exception {
        String ghost = new JwtUtil(SECRET, 3_600_000).generateToken(uniqueEmail());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + ghost))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", not(containsString("User not found"))));
    }

    // ---- helpers ----

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Test User", email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private static String registerJson(String fullName, String email, String password) {
        return """
                {"fullName":"%s","email":"%s","password":"%s"}
                """.formatted(fullName, email, password);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
