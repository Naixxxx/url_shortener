package org.com.url_shortener.services;

import org.com.url_shortener.core.repository.LinkRepository;

import java.security.SecureRandom;

public final class CodeGenerator {
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final SecureRandom rnd = new SecureRandom();
    private final LinkRepository repo;

    public CodeGenerator(LinkRepository repo) {
        this.repo = repo;
    }

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < 10_000; attempt++) {
            String code = randomBase62(8);
            if (!repo.existsByCode(code)) return code;
        }
        throw new IllegalStateException("Failed to generate unique code (too many collisions)");
    }

    private String randomBase62(int len) {
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            out[i] = ALPHABET[rnd.nextInt(ALPHABET.length)];
        }
        return new String(out);
    }
}