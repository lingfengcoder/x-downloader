package com.lingfeng.rpc.util.dae.aes;


import com.lingfeng.rpc.util.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

public class AESUtil {

    private static final String CHARSET_NAME = "UTF-8";
    public static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String AES_NAME = "AES";
    private static final int keyLen = 32;
    private static final int ivLen = 16;

    //128bit = 16字节
    //192bit = 24字节
    //256bit = 32字节
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        String content = "ddff345234士大夫十分sdfsdf ";
        String key = "fKkHfVBSzPJIQhiNymPjeus1gtZv767z";
        String iv = "8cq2St2gyRmVxv0v";
        int x = 0;
        String encrypted = ""; //解密
        System.out.println("加密前：" + content);
        System.out.println("加密后：" + (encrypted = encrypt(content, key, iv)));
        //encrypted="2tqDEDAm3OZoBughwAoOqA==";
        System.out.println("解密后：" + decrypt(encrypted, key, iv));
    }

    /**
     * 加密
     *
     * @param content
     * @param key
     * @return
     */
    public static String encrypt(String content, String key, String iv) {
        byte[] result;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] bytes = key.getBytes(CHARSET_NAME);
            SecretKeySpec keySpec = new SecretKeySpec(bytes, AES_NAME);
            AlgorithmParameterSpec paramSpec = null;
            if (iv == null || iv.equals("")) {
                paramSpec = new IvParameterSpec(new byte[16]);
            } else {
                paramSpec = new IvParameterSpec(iv.getBytes(CHARSET_NAME));
            }
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);
            result = cipher.doFinal(content.getBytes(CHARSET_NAME));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(result);
    }

    /**
     * 解密
     *
     * @param content
     * @param key
     * @return
     */
    public static String decrypt(String content, String key, String iv) {
        try {
            if (StringUtils.isNotBlank(content)) {
                content = content.replaceAll(" ", "+");
            }
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(CHARSET_NAME), AES_NAME);

            AlgorithmParameterSpec paramSpec = null;
            if (iv == null || iv.equals("")) {
                paramSpec = new IvParameterSpec(new byte[16]);
            } else {
                paramSpec = new IvParameterSpec(iv.getBytes(CHARSET_NAME));
            }

            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            Base64.Decoder decoder = Base64.getDecoder();
            return new String(cipher.doFinal(decoder.decode(content)), CHARSET_NAME);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


}
