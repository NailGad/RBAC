public record User(String username, String fullName, String email) {

    public User {

        ValidationUtils.requireNonEmpty(username, "Username");
        username = ValidationUtils.normalizeString(username);
        if (!ValidationUtils.isValidUsername(username)) {
            throw new IllegalArgumentException(
                    "Username должен содержать только латинские буквы, цифры и символ подчеркивания, " +
                            "длиной от 3 до 20 символов"
            );
        }

        ValidationUtils.requireNonEmpty(fullName, "Full name");
        fullName = ValidationUtils.normalizeString(fullName);

        ValidationUtils.requireNonEmpty(email, "Email");
        email = ValidationUtils.normalizeString(email);
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Email должен содержать символ @ и точку после @"
            );
        }
    }

    public static User create(String username, String fullName, String email) {
        return new User(username, fullName, email);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}