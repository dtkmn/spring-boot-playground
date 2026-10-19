package __PACKAGE_NAME__.web;

import java.security.Principal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import __PACKAGE_NAME__.ai.OrderAssistant;
import static org.springframework.http.HttpStatus.*;

@RestController
public class OrderSupportController {
    private final ObjectProvider<OrderAssistant> assistant;
    public OrderSupportController(ObjectProvider<OrderAssistant> assistant) { this.assistant = assistant; }

    @PostMapping("/api/ai/order-support")
    public OrderAssistant.Reply support(Principal principal, @RequestBody JsonNode request) {
        if (!request.isObject() || request.size() > 2 || !request.has("question")
                || !request.path("question").isString()) badRequest();
        String question = request.path("question").asString();
        if (question.isBlank() || question.length() > 1000) badRequest();
        Long orderId = null;
        if (request.has("orderId") && !request.path("orderId").isNull()) {
            JsonNode id = request.path("orderId");
            if (!id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0) badRequest();
            orderId = id.asLong();
        }
        // Reject unknown properties rather than silently treating them as trusted context.
        if (request.properties().stream().anyMatch(field ->
                !field.getKey().equals("question") && !field.getKey().equals("orderId"))) badRequest();
        var service = assistant.getIfAvailable();
        if (service == null) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Assistant unavailable");
        return service.ask(principal.getName(), orderId, question);
    }

    private static void badRequest() { throw new ResponseStatusException(BAD_REQUEST, "Invalid support request"); }
}
