public record Permission(String name, String resource, String description) {

    public Permission {

        ValidationUtils.requireNonEmpty(name, "Name");
        name = ValidationUtils.normalizeString(name).toUpperCase();
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Name не может содержать пробелы");
        }

        ValidationUtils.requireNonEmpty(resource, "Resource");
        resource = ValidationUtils.normalizeString(resource).toLowerCase();

        ValidationUtils.requireNonEmpty(description, "Description");
        description = ValidationUtils.normalizeString(description);
    }


    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches;
        if (namePattern == null || namePattern.isBlank()) {
            nameMatches = true;
        } else {
            nameMatches = this.name.contains(namePattern.toUpperCase());
        }

        boolean resourceMatches;
        if (resourcePattern == null || resourcePattern.isBlank()) {
            resourceMatches = true;
        } else {
            resourceMatches = this.resource.contains(resourcePattern.toLowerCase());
        }

        return nameMatches && resourceMatches;
    }
}