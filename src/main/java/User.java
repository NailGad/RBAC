public record User(String username, String fullName, String email) {

    public User {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username не может быть пустым");
        }
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException(
                    "Username должен содержать только латинские буквы, цифры и символ подчеркивания, " +
                            "длиной от 3 до 20 символов"
            );
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name не может быть пустым");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        if (!email.contains("@") || !email.substring(email.indexOf("@") + 1).contains(".")) {
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