package Model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * אחסון פרטי אימות למנהל מחוץ לתיקיית הפרויקט.
 * נשמרים Salt וגיבוב PBKDF2 בלבד, ולא הסיסמה המקורית.
 */
public final class AdminCredentials {

    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 24;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private final Path file;

    public AdminCredentials() {
        this(Path.of(System.getProperty("user.home"), ".ludo",
                "admin-auth.properties"));
    }

    // מאפשר בדיקה מול קובץ זמני, ללא שינוי חשבון המנהל האמיתי.
    public AdminCredentials(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("Credential file is required");
        }
        this.file = file.toAbsolutePath().normalize();
    }

    public boolean isConfigured() {
        return Files.isRegularFile(file);
    }

    public void create(char[] password) throws IOException {
        if (password == null || password.length < 10 || password.length > 256) {
            throw new IllegalArgumentException(
                    "Admin password must contain 10-256 characters");
        }
        if (isConfigured()) {
            throw new IllegalStateException("Admin password is already configured");
        }

        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);

        Path directory = file.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
            restrictPermissions(directory, true);
        }

        String content = "version=1\n"
                + "iterations=" + ITERATIONS + "\n"
                + "salt=" + Base64.getEncoder().encodeToString(salt) + "\n"
                + "hash=" + Base64.getEncoder().encodeToString(hash) + "\n";

        // CREATE_NEW מונע החלפת סיסמה קיימת במקרה של שתי הפעלות במקביל.
        Files.writeString(file, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        restrictPermissions(file, false);
    }

    public boolean verify(char[] candidate) throws IOException {
        if (candidate == null || candidate.length == 0 || candidate.length > 256
                || !isConfigured()) {
            return false;
        }

        Properties saved = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            saved.load(input);
        }

        try {
            if (!"1".equals(saved.getProperty("version"))) {
                throw new IOException("Unsupported admin credential format");
            }
            int iterations = Integer.parseInt(saved.getProperty("iterations"));
            if (iterations < 100_000 || iterations > 2_000_000) {
                throw new IOException("Invalid credential parameters");
            }
            byte[] salt = Base64.getDecoder().decode(saved.getProperty("salt"));
            byte[] expected = Base64.getDecoder().decode(saved.getProperty("hash"));
            if (salt.length < 16 || expected.length != KEY_BITS / 8) {
                throw new IOException("Invalid stored credentials");
            }
            byte[] actual = derive(candidate, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IOException("Invalid stored admin credentials", exception);
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations)
            throws IOException {
        PBEKeySpec specification = new PBEKeySpec(password, salt,
                iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(specification).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IOException("Cannot derive admin password hash", exception);
        } finally {
            specification.clearPassword();
        }
    }

    // במערכות POSIX מוגדרות הרשאות לבעל החשבון בלבד.
    // ב-Windows משתמשים בהרשאות תיקיית פרופיל המשתמש.
    private static void restrictPermissions(Path target, boolean directory)
            throws IOException {
        if (Files.getFileAttributeView(target,
                java.nio.file.attribute.PosixFileAttributeView.class) == null) {
            return;
        }
        Set<PosixFilePermission> permissions = directory
                ? EnumSet.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE)
                : EnumSet.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(target, permissions);
    }
}
