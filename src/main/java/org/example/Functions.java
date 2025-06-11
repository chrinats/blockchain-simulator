/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author AyinalaKaushik(UMKC-
 */
public class Functions {

    static Random r = new Random();
    public static byte[] getHash(byte[] Input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(Input);
            return messageDigest;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Functions.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    static int compare(byte[] one, byte[] two) {
        if (one.length != two.length) {
            return -2;
        }
        for (int i = 0; i < one.length; i++) {
            int left = (int) one[i] & 0xFF;

            int right = (int) two[i] & 0xFF;
            if (left > right) {
                return 1;
            }
            if (left < right) {
                return -1;
            }
        }

        return 0;
    }

    public static long calculateDelay(Node sender, Node receiver) {
        int bandwidth = sender.getBandwidth();
        int latency = sender.getLatency();

        long transmissionDelay = (Sharables.ChunkSize * 1000) / bandwidth;
        long processingDelay = (Sharables.ChunkSize / (2 * 8 * 1024)) / 4;
        long networkDelay = latency;

        return transmissionDelay + processingDelay + networkDelay;
    }


    public static long calculateBlockDelay(Node sender, Node receiver) {
        int bandwidth = sender.getBandwidth();
        int latency = sender.getLatency();

        long blockSizeKB = Sharables.BlockSize;

        long transmissionDelay = (blockSizeKB * 1000) / bandwidth;

        long processingDelay = blockSizeKB/16;

        long networkDelay = latency;

        return transmissionDelay + processingDelay + networkDelay;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        int lenA = a.length;
        int lenB = b.length;
        byte[] c = Arrays.copyOf(a, lenA + lenB);
        System.arraycopy(b, 0, c, lenA, lenB);
        return c;
    }
    
    static int getRandomNumberInRange(int min, int max) {

		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}
		return r.nextInt((max - min) + 1) + min;
	}
    
    static void printInRed(String temp){
        System.out.println("\u001B[31m"+temp+"\u001B[0m");
    }
    

}
