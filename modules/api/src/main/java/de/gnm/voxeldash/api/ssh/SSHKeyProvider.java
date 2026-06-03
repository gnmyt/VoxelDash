package de.gnm.voxeldash.api.ssh;

import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.session.SessionContext;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;

public class SSHKeyProvider extends AbstractKeyPairProvider {

    private final String publicKeyString;
    private final String privateKeyString;

    public SSHKeyProvider(String publicKeyString, String privateKeyString) {
        this.publicKeyString = publicKeyString;
        this.privateKeyString = privateKeyString;
    }

    @Override
    public Iterable<KeyPair> loadKeys(SessionContext session) {
        try {
            System.out.println("Loading keys from strings");
            byte[] publicKeyBytes = java.util.Base64.getDecoder().decode(publicKeyString);
            byte[] privateKeyBytes = java.util.Base64.getDecoder().decode(privateKeyString);

            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
            PrivateKey privateKey = keyFactory.generatePrivate(privKeySpec);

            return Collections.singletonList(new KeyPair(publicKey, privateKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load keys from strings", e);
        }
    }
}