package code;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Replica il comportamento del "Secure Properties Tool" ufficiale di MuleSoft
 * per algoritmo AES, modalita' CBC.
 *
 * Differenze chiave rispetto a un'implementazione AES "naive":
 * - La Key NON viene sottoposta ad hashing (niente SHA-256): i byte UTF-8
 *   della stringa Key sono usati direttamente come chiave AES. La lunghezza
 *   della stringa determina quindi la variante AES:
 *      16 caratteri -> AES-128
 *      24 caratteri -> AES-192
 *      32 caratteri -> AES-256
 * - Se non si usa un IV casuale, l'IV e' derivato dalla Key stessa
 *   (i primi 16 byte della chiave, pari alla dimensione del blocco AES),
 *   NON un blocco di zeri.
 * - E' supportata anche la modalita' "random IV": un IV casuale viene
 *   generato, anteposto (prepended) al ciphertext e poi tutto codificato
 *   in Base64. In decrypt, se richiesto, i primi 16 byte del payload
 *   decodificato vengono letti come IV.
 */
public class SecureAES {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int BLOCK_SIZE = 16; // AES block size in byte

    public static String encrypt(String key, String value, boolean useRandomIv) throws Exception {
        key = key.trim();
        value = value.trim();
        byte[] keyBytes = normalizeKey(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        byte[] ivBytes;
        if (useRandomIv) {
            ivBytes = new byte[BLOCK_SIZE];
            new SecureRandom().nextBytes(ivBytes);
        } else {
            // IV fisso derivato dalla chiave (comportamento default del tool ufficiale)
            ivBytes = Arrays.copyOf(keyBytes.length >= BLOCK_SIZE ? keyBytes
                    : key.getBytes(StandardCharsets.UTF_8), BLOCK_SIZE);
        }
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

        byte[] output = encrypted;
        if (useRandomIv) {
            // Prepend IV al ciphertext, come fa il tool ufficiale con --use-random-iv
            output = new byte[ivBytes.length + encrypted.length];
            System.arraycopy(ivBytes, 0, output, 0, ivBytes.length);
            System.arraycopy(encrypted, 0, output, ivBytes.length, encrypted.length);
        }
        return Base64.getEncoder().encodeToString(output);
    }

    public static String decrypt(String key, String encryptedValue, boolean useRandomIv) throws Exception {
        key = key.trim();
        encryptedValue = encryptedValue.trim();
        byte[] keyBytes = normalizeKey(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        byte[] decoded;
        try {
            decoded = Base64.getMimeDecoder().decode(encryptedValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Il valore inserito non e' un Base64 valido (probabile copia/incolla incompleta).", e);
        }

        byte[] ivBytes;
        byte[] cipherBytes;
        if (useRandomIv) {
            if (decoded.length <= BLOCK_SIZE) {
                throw new IllegalArgumentException(
                        "Il valore criptato e' troppo corto per contenere un IV casuale anteposto.");
            }
            ivBytes = Arrays.copyOfRange(decoded, 0, BLOCK_SIZE);
            cipherBytes = Arrays.copyOfRange(decoded, BLOCK_SIZE, decoded.length);
        } else {
            ivBytes = Arrays.copyOf(keyBytes.length >= BLOCK_SIZE ? keyBytes
                    : key.getBytes(StandardCharsets.UTF_8), BLOCK_SIZE);
            cipherBytes = decoded;
        }

        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

        try {
            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (BadPaddingException e) {
            throw new Exception(
                    "Decrittazione fallita: la Key (o l'opzione 'random IV') non corrisponde a quella "
                            + "usata per l'encrypt, oppure il Value e' incompleto/corrotto.", e);
        }
    }

    /**
     * I byte UTF-8 della chiave devono avere una lunghezza valida per AES: 16, 24 o 32 byte.
     * Se la chiave fornita ha una lunghezza diversa, viene segnalato un errore chiaro
     * invece di un generico "Invalid key length" da parte della JCE.
     */
    private static byte[] normalizeKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32) {
            return keyBytes;
        }
        throw new IllegalArgumentException(
                "La Key deve essere lunga esattamente 16, 24 o 32 caratteri (byte UTF-8) per AES-128/192/256. "
                        + "Lunghezza attuale: " + keyBytes.length + ".");
    }
}