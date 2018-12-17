package com.ma.monitoringlibrary;



import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by abeer on 20-Nov-18.
 */

class Encryption {
    private String encryptedSecretKey = "not generate yet ";

    Encryption() {

    }

    private final String publicKeyString = " MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDHb5Z6UX5YOKlN0/qAni1DLUl8IcnjbLqEi6GJ" +
            "PkUj9zpAGGLHtd5fRk4cBkd988MROwf7mdzBmjS6MZrGa3DY/WeNxnIillNnKd1UBfAzem5f/X6s" +
            "Pk54dEhE0W49yqzxfm4o2Ko5DQ2BAoqlW6dOf/6S3RpubIVPq1zvMXoV5QIDAQAB";


    String Encrypt(String text) {
        try {
            // 1. generate secret key using AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128); // AES is currently available in three key sizes: 128, 192 and 256 bits.The design and strength of all key lengths of the AES algorithm are sufficient to protect classified information up to the SECRET level
            SecretKey secretKey = keyGenerator.generateKey();

            String a= Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
            Log.e("SecretKey String",a);

            // 2. get string which needs to be encrypted
            //  String text = "<your_string_which_needs_to_be_encrypted_here>";

            // 3. encrypt string using secret key
            // encrypt compressed package using AES secret key
            byte[] raw = secretKey.getEncoded();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[16]));
            String cipherTextString = Base64.encodeToString(cipher.doFinal(text.getBytes(Charset.forName("UTF-8"))), Base64.DEFAULT);


            // 4. get public key
            // RSA public key
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.decode(publicKeyString, Base64.DEFAULT));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicSpec);

            // 6. encrypt secret key using public key
            // encrypt AES key using RSA public key
            Cipher cipher2 = Cipher.getInstance("RSA/ECB/PKCS1Padding");//"RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            cipher2.init(Cipher.ENCRYPT_MODE, publicKey);
            encryptedSecretKey = Base64.encodeToString(cipher2.doFinal(secretKey.getEncoded()), Base64.DEFAULT);
            //encryptedSecretKey = RSAEncrypt(a);
            // 7. pass cipherTextString (encypted sensitive data) and encryptedSecretKey to your server via your preferred way.
            // Tips:
            // You may use JSON to combine both the strings under 1 object.
            // You may use a volley call to send this data to your server.

            return cipherTextString;
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        return text;

    }

    String getEncryptedSecretKey() {
        return encryptedSecretKey;
    }

    String RSAEncrypt(String input)
    {
        String strEncrypted = null;
        KeyFactory factory = null;
        try {

            byte[] expBytes = Base64.decode(String.valueOf("AQAB".getBytes("UTF-8")), Base64.DEFAULT);
            byte[] modBytes = Base64.decode(String.valueOf("rFsMn+idg8jmVMk249DzJc7AFft3+/jcnYDTh9wHee3tgFu1gBRh7e+ao+MWq7NEN0N7kUHa7O4c/ND2Ahcx/h4mXD5KDoixFRBUsxYqCJVA68qYJ7vozVPMjNr4jeOo1xt+oevO5+mUWtcaib5Iw51u1Jq/6qCqLsm8Eq3cnsE=".getBytes("UTF-8")), Base64.DEFAULT);
            byte[] dBytes = Base64.decode(String.valueOf("Gs8mzZDPP3p2aWXLBfCwgYcBVeoBpc318wHg5VcSSqL5uGeLedqxyOLmOOvP0PFXgQkcJWIK/aOkGqcePQECo3TNiK+uLSwc97V3spZah70FFJVyh23Y+o0wlRGHAm5Nj9QieHlVwhgJPkNUJYgH9qkwB9aCpl+rdAG3da2fQ2E=".getBytes("UTF-8")), Base64.DEFAULT);

            BigInteger modules = new BigInteger(1, modBytes);
            BigInteger exponent = new BigInteger(1, expBytes);
            BigInteger d = new BigInteger(1, dBytes);

            //String input = "test";

            RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(modules, exponent); // PKCS#1 padding is used. true is for OAEP padding.
            factory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = factory.generatePublic(pubSpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encrypted = cipher.doFinal(input.getBytes("UTF-8"));

            strEncrypted = Base64.encodeToString(encrypted, Base64.DEFAULT);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return strEncrypted;
    }

}
