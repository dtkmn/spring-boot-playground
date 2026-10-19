package __PACKAGE_NAME__;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ai.chat.model.ChatModel;
import tools.jackson.databind.json.JsonMapper;
import __PACKAGE_NAME__.ai.OrderAssistant;
import __PACKAGE_NAME__.security.LocalBearerSecurityConfiguration;
import __PACKAGE_NAME__.web.OrderSupportController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verify fail-closed local identity configuration and the default model-free startup path. */
class LocalBearerStartupTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
            ServletWebSecurityAutoConfiguration.class))
        .withUserConfiguration(LocalBearerSecurityConfiguration.class,
            OrderSupportController.class, OrderAssistant.class)
        .withPropertyValues("spring.profiles.active=default",
            "pilot.auth.caller-a-token=" + OrderAssistantIntegrationTest.TOKEN_A,
            "pilot.auth.caller-b-token=" + OrderAssistantIntegrationTest.TOKEN_B);

    @Test
    void validDistinctTokensStartSecurityAndMapOnlyTheTwoFixturePrincipals() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(SecurityFilterChain.class)
                .hasSingleBean(OpaqueTokenIntrospector.class);
            var introspector = context.getBean(OpaqueTokenIntrospector.class);
            assertThat(introspector.introspect(OrderAssistantIntegrationTest.TOKEN_A).getName())
                .isEqualTo("caller-a");
            assertThat(introspector.introspect(OrderAssistantIntegrationTest.TOKEN_B).getName())
                .isEqualTo("caller-b");
            assertThatThrownBy(() -> introspector.introspect("an-unrecognized-token"))
                .isInstanceOf(BadOpaqueTokenException.class);
        });
    }

    @Test
    void missingTokenPreventsStartupEvenWithAiOff() {
        runner.withPropertyValues("pilot.auth.caller-a-token=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Each pilot Bearer token must contain at least 32 non-whitespace characters");
        });
    }

    @Test
    void shortTokenPreventsStartupEvenWithAiOff() {
        runner.withPropertyValues("pilot.auth.caller-b-token=too-short").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Each pilot Bearer token must contain at least 32 non-whitespace characters");
        });
    }

    @Test
    void equalTokensPreventStartupEvenWithAiOff() {
        runner.withPropertyValues("pilot.auth.caller-b-token=" + OrderAssistantIntegrationTest.TOKEN_A)
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("Pilot Bearer tokens must be distinct");
            });
    }

    @Test
    void defaultConfigurationHasNoModelAndTheAiEndpointIsUnavailable() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(ChatModel.class)
                .doesNotHaveBean(OrderAssistant.class);
            assertThat(context.getEnvironment().getProperty("spring.ai.model.chat")).isEqualTo("none");
            var request = JsonMapper.builder().build().createObjectNode()
                .put("question", "What is the order status?").put("orderId", 1001);
            assertThatThrownBy(() -> context.getBean(OrderSupportController.class)
                .support(() -> "caller-a", request)).isInstanceOfSatisfying(ResponseStatusException.class,
                    exception -> {
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                        assertThat(exception.getReason()).isEqualTo("Assistant unavailable");
                    });
        });
    }
}
