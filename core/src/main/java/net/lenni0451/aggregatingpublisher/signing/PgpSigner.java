package net.lenni0451.aggregatingpublisher.signing;

import net.lenni0451.aggregatingpublisher.services.SigningService;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import javax.annotation.Nullable;
import java.io.*;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class PgpSigner implements SigningService {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }


    private final String name;
    private final PGPSecretKey secretKey;
    private final PGPPrivateKey privateKey;

    public PgpSigner(final File keyFile, @Nullable final String password, @Nullable final String keyId, @Nullable final String name) throws IOException, PGPException {
        this(new FileInputStream(keyFile), password, keyId, name);
    }

    public PgpSigner(final byte[] keyBytes, @Nullable final String password, @Nullable final String keyId, @Nullable final String name) throws IOException, PGPException {
        this(new ByteArrayInputStream(keyBytes), password, keyId, name);
    }

    public PgpSigner(final InputStream keyIn, @Nullable final String password, @Nullable final String keyId, @Nullable final String name) throws IOException, PGPException {
        try (InputStream in = keyIn) {
            InputStream decoderStream = PGPUtil.getDecoderStream(in);
            PGPObjectFactory factory = new PGPObjectFactory(decoderStream, new JcaKeyFingerprintCalculator());
            List<PGPSecretKeyRing> rings = new ArrayList<>();
            Object obj;
            while ((obj = factory.nextObject()) != null) {
                if (obj instanceof PGPSecretKeyRing ring) {
                    rings.add(ring);
                } else if (obj instanceof PGPSecretKeyRingCollection collection) {
                    collection.forEach(rings::add);
                }
            }

            PGPSecretKey matchedKey = this.findSecretKey(rings, keyId);
            if (matchedKey == null) {
                throw new IllegalArgumentException("No suitable PGP signing key found" + (keyId != null ? " for key ID: " + keyId : ""));
            }
            this.secretKey = matchedKey;

            PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder(
                    new JcaPGPDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build()
            ).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(password != null ? password.toCharArray() : new char[0]);
            this.privateKey = this.secretKey.extractPrivateKey(decryptor);

            String keyIdHex = Long.toHexString(this.secretKey.getKeyID()).toUpperCase();
            this.name = name != null ? name : "PGP (" + keyIdHex + ")";
        }
    }

    private PGPSecretKey findSecretKey(final List<PGPSecretKeyRing> rings, @Nullable final String keyId) {
        Long parsedKeyId = null;
        boolean isShort = false;
        if (keyId != null && !keyId.isEmpty()) {
            String clean = keyId.trim();
            if (clean.startsWith("0x") || clean.startsWith("0X")) {
                clean = clean.substring(2);
            }
            parsedKeyId = Long.parseUnsignedLong(clean, 16);
            isShort = clean.length() <= 8;
        }

        for (PGPSecretKeyRing ring : rings) {
            for (PGPSecretKey key : ring) {
                if (!key.isSigningKey()) continue;
                if (parsedKeyId != null) {
                    long currentId = key.getKeyID();
                    if (isShort) {
                        if ((currentId & 0xFFFFFFFFL) == (parsedKeyId & 0xFFFFFFFFL)) {
                            return key;
                        }
                    } else {
                        if (currentId == parsedKeyId) {
                            return key;
                        }
                    }
                } else {
                    return key;
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public byte[] sign(final String path, final byte[] data) throws Exception {
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
                new JcaPGPContentSignerBuilder(this.secretKey.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256).setProvider(BouncyCastleProvider.PROVIDER_NAME)
        );
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, this.privateKey);

        PGPSignatureSubpacketGenerator subpacketGen = new PGPSignatureSubpacketGenerator();
        subpacketGen.setIssuerKeyID(false, this.secretKey.getKeyID());
        signatureGenerator.setHashedSubpackets(subpacketGen.generate());

        signatureGenerator.update(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ArmoredOutputStream armoredOut = new ArmoredOutputStream(out)) {
            signatureGenerator.generate().encode(armoredOut);
        }
        return out.toByteArray();
    }

}
