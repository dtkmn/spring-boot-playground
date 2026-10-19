package tech.dtkmn.examples.aimcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class CustomerTools {
    private final CustomerService customers;

    public CustomerTools(CustomerService customers) {
        this.customers = customers;
    }

    @McpTool(name = "lookup_customer", description = "Look up one synthetic customer by positive numeric ID. Read-only; returns found=false when absent.",
        annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public LookupResult lookup(@McpToolParam(description = "Positive customer ID", required = true) long id) {
        return customers.findById(id)
            .map(customer -> new LookupResult(true, customer))
            .orElseGet(() -> new LookupResult(false, null));
    }

    public record LookupResult(boolean found, CustomerService.Customer customer) {}
}
