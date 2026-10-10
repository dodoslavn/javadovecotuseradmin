package eu.fordo.mailadmin.service;

import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Two distinct hashing paths that must never be cross-used:
 *  - web login passwords -> bcrypt, verified entirely within this JVM
 *  - Dovecot mail account passwords -> shells out to `doveadm pw`, so the
 *    resulting hash is guaranteed to be whatever Dovecot on the mail host
 *    itself accepts.
 */
public class PasswordService {

    private static final String DOVEADM_SCHEME = "SHA512-CRYPT";

    public String hashWebPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(12));
    }

    public boolean verifyWebPassword(String plaintext, String hash) {
        return BCrypt.checkpw(plaintext, hash);
    }

    /**
     * Shells out to `doveadm pw -s SCHEME`, feeding the plaintext via stdin (twice, for its
     * "repeat password" prompt) rather than as a CLI argument — argv is visible to other
     * local users via /proc/<pid>/cmdline, stdin is not.
     */
    public String hashDovecotPassword(String plaintext) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("doveadm", "pw", "-s", DOVEADM_SCHEME);
        pb.redirectErrorStream(false);
        Process proc = pb.start();
        try (OutputStream stdin = proc.getOutputStream()) {
            String line = plaintext + "\n";
            stdin.write(line.getBytes(StandardCharsets.UTF_8));
            stdin.write(line.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.readLine();
        }
        int exit = proc.waitFor();
        if (exit != 0 || output == null) {
            throw new IOException("doveadm pw failed with exit code " + exit);
        }
        return output.trim();
    }
}
