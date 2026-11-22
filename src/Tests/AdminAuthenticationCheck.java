package Tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import Controller.AdminAuthController;
import Model.AdminCredentialsStore;

/** בדיקות כניסת מנהל על קובץ זמני בלבד, ללא שינוי פרטי האימות האמיתיים. */
public final class AdminAuthenticationCheck {

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable operation, String message) {
        try {
            operation.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("ludo-admin-auth-");
        Path credentials = folder.resolve("admin-auth.properties");
        char[] goodPassword = "LocalAdmin!2026".toCharArray();
        char[] wrongPassword = "WrongPassword123".toCharArray();
        try {
            AdminAuthController auth = new AdminAuthController(
                    new AdminCredentialsStore(credentials));
            check(auth.requiresSetup(), "Expected initial setup");
            check(!auth.authenticate(goodPassword),
                    "Authentication succeeded before setup");
            System.out.println("PASS - initial setup required");

            expectInvalid(() -> {
                try {
                    auth.setupPassword(goodPassword, wrongPassword);
                } catch (java.io.IOException e) {
                    throw new AssertionError(e);
                }
            }, "Mismatched passwords accepted");
            check(!Files.exists(credentials),
                    "Credentials created after mismatch");
            System.out.println("PASS - mismatched password confirmation rejected");

            auth.setupPassword(goodPassword, goodPassword);
            check(Files.isRegularFile(credentials), "Credential file missing");
            check(!auth.requiresSetup(), "Setup was not completed");
            String saved = Files.readString(credentials, StandardCharsets.UTF_8);
            check(!saved.contains("LocalAdmin!2026"),
                    "Plaintext password was stored");
            System.out.println("PASS - hashed credentials saved outside project");

            // טעינה מחדש מאותו קובץ מדמה כניסה לאחר סגירת המשחק.
            AdminAuthController restarted = new AdminAuthController(
                    new AdminCredentialsStore(credentials));
            check(restarted.authenticate(goodPassword),
                    "Correct password failed after restart");
            check(!restarted.authenticate(wrongPassword),
                    "Wrong password accepted");
            System.out.println("PASS - correct login and wrong login after restart");

            expectInvalid(() -> {
                try {
                    restarted.setupPassword(goodPassword, goodPassword);
                } catch (java.io.IOException e) {
                    throw new AssertionError(e);
                }
            }, "Second password setup accepted");
            System.out.println("PASS - duplicate setup blocked");
            System.out.println("All admin authentication checks passed.");
        } finally {
            Arrays.fill(goodPassword, '\0');
            Arrays.fill(wrongPassword, '\0');
            Files.deleteIfExists(credentials);
            Files.deleteIfExists(folder);
        }
    }
}
