package com.portfolio.notificationapi;

import com.portfolio.notificationapi.config.ApiSecurityConfig;
import com.portfolio.notificationapi.controller.*;
import com.portfolio.notificationapi.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@WebMvcTest(controllers={ChannelController.class, NotificationController.class}, properties={
    "notification.security.admin-username=admin-test", "notification.security.admin-password=admin-test-password-2026",
    "notification.security.integration-username=artpizza-test", "notification.security.integration-password=integration-test-password-2026"})
@Import(ApiSecurityConfig.class)
class ApiSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean NotificationService notifications;
    @MockBean ChannelService channels;
    @Test void publicChannelsRemainAvailable() throws Exception {mvc.perform(get("/api/v1/channels")).andExpect(status().isOk());}
    @Test void anonymousCannotReadRecipients() throws Exception {mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());}
    @Test void anonymousCannotCreate() throws Exception {mvc.perform(post("/api/v1/notifications").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());}
    @Test void wrongPasswordIsRejected() throws Exception {mvc.perform(get("/api/v1/notifications").with(httpBasic("admin-test","wrong"))).andExpect(status().isUnauthorized());}
    @Test void adminCanList() throws Exception {mvc.perform(get("/api/v1/notifications").with(httpBasic("admin-test","admin-test-password-2026"))).andExpect(status().isOk());}
    @Test void integrationCannotListRecipients() throws Exception {mvc.perform(get("/api/v1/notifications").with(httpBasic("artpizza-test","integration-test-password-2026"))).andExpect(status().isForbidden());}
    @Test void integrationCannotDelete() throws Exception {mvc.perform(delete("/api/v1/notifications/1").with(httpBasic("artpizza-test","integration-test-password-2026"))).andExpect(status().isForbidden());}
    @Test void integrationCannotManageChannels() throws Exception {mvc.perform(post("/api/v1/channels").with(httpBasic("artpizza-test","integration-test-password-2026")).contentType("application/json").content("{}")).andExpect(status().isForbidden());}
    @Test void integrationCanReachCreateValidation() throws Exception {mvc.perform(post("/api/v1/notifications").with(httpBasic("artpizza-test","integration-test-password-2026")).contentType("application/json").content("{}")).andExpect(status().isBadRequest());}
}
