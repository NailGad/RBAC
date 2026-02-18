public record Permission(String name, String resource, String description) {

    public Permission {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name не может быть пустым");
        }
        name = name.trim().toUpperCase();
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Name не может содержать пробелы");
        }

        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("Resource не может быть пустым");
        }
        resource = resource.trim().toLowerCase();

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description не может быть пустым");
        }
        description = description.trim();
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