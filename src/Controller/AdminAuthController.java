package Controller;

import java.io.IOException;
import java.util.Arrays;

import Model.AdminCredentialsStore;

/** מתאם בין מסך כניסת המנהל לבין שמירת פרטי האימות ב-Model. */
public final class AdminAuthController {

    private final AdminCredentialsStore store;

    public AdminAuthController() {
        this(new AdminCredentialsStore());
    }

    public AdminAuthController(AdminCredentialsStore store) {
        if (store == null) {
            throw new IllegalArgumentException("Credential store is required");
        }
        this.store = store;
    }

    public boolean requiresSetup() {
        return !store.isConfigured();
    }

    // הגדרה ראשונית מתאפשרת רק כאשר עדיין לא קיימת סיסמה.
    public void setupPassword(char[] password, char[] confirmation)
            throws IOException {
        if (password == null || confirmation == null
                || !Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        store.create(password);
    }

    public boolean authenticate(char[] password) throws IOException {
        return store.verify(password);
    }
}
