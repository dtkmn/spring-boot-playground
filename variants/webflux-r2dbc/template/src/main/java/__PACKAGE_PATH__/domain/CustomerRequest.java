package __PACKAGE_NAME__.domain;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
    @NotBlank String firstName,
    @NotBlank String lastName
) {
}
