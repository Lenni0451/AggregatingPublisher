package net.lenni0451.aggregatingpublisher.signing;

import net.lenni0451.aggregatingpublisher.services.SigningService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GpgSigner implements SigningService {

    private final String name;
    private final String executable;
    private final String key;
    private final String passphrase;
    private final List<String> extraArgs;

    public GpgSigner(@Nullable final String key, @Nullable final String passphrase, @Nullable final String executable, @Nullable final List<String> extraArgs, @Nullable final String name) {
        this.key = key;
        this.passphrase = passphrase;
        this.executable = executable != null ? executable : "gpg";
        this.extraArgs = extraArgs != null ? List.copyOf(extraArgs) : List.of();
        this.name = name != null ? name : "GPG" + (key != null ? " (" + key + ")" : "");
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public byte[] sign(final String path, final byte[] data) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(this.executable);
        command.add("--batch");
        command.add("--yes");
        command.add("--detach-sign");
        command.add("--armor");
        if (this.key != null && !this.key.isEmpty()) {
            command.add("--local-user");
            command.add(this.key);
        }
        if (this.passphrase != null && !this.passphrase.isEmpty()) {
            command.add("--pinentry-mode");
            command.add("loopback");
            command.add("--passphrase");
            command.add(this.passphrase);
        }
        command.addAll(this.extraArgs);
        command.add("-o");
        command.add("-");
        command.add("-");

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(data);
        }
        byte[] signature = process.getInputStream().readAllBytes();
        byte[] error = process.getErrorStream().readAllBytes();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errorMsg = new String(error, StandardCharsets.UTF_8).trim();
            throw new IOException("GPG signing failed with exit code " + exitCode + ": " + errorMsg);
        }
        return signature;
    }

}
