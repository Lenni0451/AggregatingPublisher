package net.lenni0451.aggregatingpublisher.utils;

import lombok.SneakyThrows;

import java.security.MessageDigest;
import java.util.HexFormat;

public class HashUtils {

    @SneakyThrows
    public static String md5(final byte[] input) {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(input);
        return HexFormat.of().formatHex(messageDigest.digest());
    }

    @SneakyThrows
    public static String sha1(final byte[] input) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(input);
        return HexFormat.of().formatHex(messageDigest.digest());
    }

    @SneakyThrows
    public static String sha256(final byte[] input) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(input);
        return HexFormat.of().formatHex(messageDigest.digest());
    }

    @SneakyThrows
    public static String sha512(final byte[] input) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(input);
        return HexFormat.of().formatHex(messageDigest.digest());
    }

}
