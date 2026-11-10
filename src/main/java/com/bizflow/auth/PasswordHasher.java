package com.bizflow.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
    private static final int ITERATIONS=210_000;
    private static final int KEY_BITS=256;
    private static final SecureRandom RANDOM=new SecureRandom();
    private PasswordHasher(){}
    public static String hash(String password){
        byte[] salt=new byte[16];RANDOM.nextBytes(salt);byte[] key=derive(password.toCharArray(),salt,ITERATIONS);
        return "pbkdf2-sha256$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(key);
    }
    public static boolean matches(String password,String encoded){
        try{String[] parts=encoded.split("\\$",-1);if(parts.length!=4||!parts[0].equals("pbkdf2-sha256"))return false;int rounds=Integer.parseInt(parts[1]);if(rounds<100_000||rounds>1_000_000)return false;byte[] salt=Base64.getDecoder().decode(parts[2]);byte[] expected=Base64.getDecoder().decode(parts[3]);return MessageDigest.isEqual(expected,derive(password.toCharArray(),salt,rounds));}
        catch(RuntimeException ex){return false;}
    }
    private static byte[] derive(char[] password,byte[] salt,int rounds){try{PBEKeySpec spec=new PBEKeySpec(password,salt,rounds,KEY_BITS);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();java.util.Arrays.fill(password,'\0');}}catch(Exception e){throw new IllegalStateException("Password hashing is unavailable.",e);}}
}
