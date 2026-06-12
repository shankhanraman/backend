package com.arogya.cafe;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arogya.cafe.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies the per-role endpoint gates from the business rules. */
@SpringBootTest
@AutoConfigureMockMvc
class RoleAuthorizationTest extends AbstractIntegrationTest {

    private static final String VALID_ORDER_JSON =
            "{\"customerId\":1,\"lines\":[{\"menuItemId\":1,\"sizeVariant\":\"Regular\",\"quantity\":1}]}";

    @Autowired
    private MockMvc mvc;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(
            username = "chef",
            roles = {"CHEF"})
    void chefCannotCreateAnOrder() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "server",
            roles = {"SERVER"})
    void serverCannotPrepareAKot() throws Exception {
        mvc.perform(post("/api/kots/1/prepare")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "chef",
            roles = {"CHEF"})
    void chefCannotServeAnOrder() throws Exception {
        mvc.perform(post("/api/orders/1/serve")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "chef",
            roles = {"CHEF"})
    void chefCannotPayABill() throws Exception {
        mvc.perform(post("/api/bills/1/pay")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "server",
            roles = {"SERVER"})
    void authenticatedStaffCanReadReferenceData() throws Exception {
        mvc.perform(get("/api/menu-items")).andExpect(status().isOk());
    }

    // ---- Positive gates: an authorized role must reach the handler (404 for missing data, NOT 403).
    // Guards against an authorization bug that denies everyone (which the deny-only tests above would miss).

    @Test
    @WithMockUser(
            username = "chef",
            roles = {"CHEF"})
    void chefIsAllowedToReachPrepare() throws Exception {
        mvc.perform(post("/api/kots/999999/prepare")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(
            username = "cashier",
            roles = {"CASHIER"})
    void cashierIsAllowedToReachPay() throws Exception {
        mvc.perform(post("/api/bills/999999/pay")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(
            username = "server",
            roles = {"SERVER"})
    void serverIsAllowedToReachServe() throws Exception {
        mvc.perform(post("/api/orders/999999/serve")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(
            username = "manager",
            roles = {"MANAGER"})
    void managerIsAllowedToReachEveryWorkflowTransition() throws Exception {
        mvc.perform(post("/api/kots/999999/prepare")).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/999999/serve")).andExpect(status().isNotFound());
        mvc.perform(post("/api/bills/999999/pay")).andExpect(status().isNotFound());
    }
}
