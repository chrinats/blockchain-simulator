/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

/**
 *
 * @author AyinalaKaushik(UMKC-
 */
public class Sharables {

    static int Latency[][];
    static int NoOfNodes = 1024;
    static int BlockInterval = 10;
    static final int MaxBandwidth = 100 * 1024 * 1024; // bits per second
    static final int MinBandWidth = 1 * 1024 * 1024;
    static final int AvgBandWidth = (MaxBandwidth + MinBandWidth) / 2;
    static final int MaxLatencyDelay = 300;
    static final int MinLatencyDelay = 200;
    static final int ChainLength = 10;

    static final int TimeToVerify = 0;
    static int MaxDelay[] = new int[ChainLength];
    static long BlockSize = 1024 * 8; // in bits
    static int Tdelay = 0;
    public static long TotalPropagationTime = 0;

    static final long startTime = 0;
    static int ConnectionsCount[];
    static int Connections[][];
    static Node Nodes[];
    static int NodesBandwidth[];
    static int NodesLatency[];

    // PiChu settings
    static final int HeaderSize = 904; // in bits
    static final int ChunkSize = 1024;
    static final boolean EnablePiChu = false;
    static final int SignatureSize = 512;
    static int Cm = 11; // Default για correctness

    // RedFork settings
    static final boolean EnableRedFork = false;
    static final int RedForkPartASize = 1024 * 3; // in bits

    static long AvgPropagationTime = 0;

}
