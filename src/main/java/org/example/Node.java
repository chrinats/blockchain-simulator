/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.example.Scheduler.*;

/**
 *
 * @author AyinalaKaushik(UMKC-
 */
public class Node {

    int Id;
    OptimizedChain chain;
    Block Last;
    long NextEventTime = Long.MAX_VALUE;
    long MiningTime = 0;
    int LastBlockDelay = 0;

    private List<List<Node>> buckets; // Kadcast-style routing buckets
    private Block genesisBlock;
    private boolean isKadcastEnabled;

    // Max neighbors = 1% of the total nodes
    public static final int MAX_NEIGHBORS = Sharables.NoOfNodes / 100;

    // Node constructor – initializes chain, sets up Kadcast buckets if needed
    Node(Block Genesis, int Id, boolean isKadcast) {
        this.Id = Id;
        this.isKadcastEnabled = isKadcast;
        this.Last = Genesis;
        this.chain = new OptimizedChain(Genesis);

        // Kadcast routing table: 160 K-buckets (common with 160-bit IDs)
        this.buckets = new ArrayList<>();
        for (int i = 0; i < 160; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    // Simulate mining time based on exponential distribution
    long getMiningTime() {
        int NoNodes = Sharables.NoOfNodes;
        int Interval = Sharables.BlockInterval;
        double P = (double) 1 / (Interval * NoNodes);

        long Out = 0;
        while (Out == 0) {
            Out = ((long) ((Math.log(Math.random()) / Math.log(1 - P))));
        }

        return Out * 1000 > 0 ? Out * 1000 : Long.MAX_VALUE;
    }

    // Start mining event from this node
    public void init() {
        CreateMiningEvent(1, Sharables.startTime);
    }

    // Called when it’s this node’s turn to mine a block
    public void CallBlack(long EventTime, int BlockNumber) {
        if (Last.NodeNumber < BlockNumber) {
            int timeTakentoMine = (int) ((EventTime - Last.TimeStamp));
            Block temp = new Block(Last.TimeStamp, Last.NodeNumber + 1, EventTime, Id);
            Last = temp;
            if (chain != null) chain.add(temp);

            CreateMiningEvent(BlockNumber + 1, EventTime);

            if (isKadcastEnabled) {
                Set<String> visited = Collections.synchronizedSet(new HashSet<>()); // not yet used
            } else {
                Broadcast(temp);  // traditional broadcast
            }
        }
    }

    // Kadcast: place neighbor in appropriate bucket by XOR distance
    public void addNeighbor(Node neighbor) {
        int distance = calculateXORDistance(this.Id, neighbor.getId());
        int bucketIndex = (int) (Math.log(distance) / Math.log(2));
        if (bucketIndex >= 0 && bucketIndex < buckets.size()) {
            buckets.get(bucketIndex).add(neighbor);
        }
    }

    // Collect all neighbors across all buckets
    public List<Node> getNeighbors() {
        List<Node> allNeighbors = new ArrayList<>();
        for (List<Node> bucket : buckets) {
            allNeighbors.addAll(bucket);
        }
        return allNeighbors;
    }

    public int getBandwidth() { return Sharables.NodesBandwidth[Id]; }

    public int getLatency() { return Sharables.NodesLatency[Id]; }

    int calculateXORDistance(int id1, int id2) {
        int xor = id1 ^ id2;
        return Integer.highestOneBit(xor);  // represents distance in Kadcast
    }

    public int getId() { return Id; }

    public OptimizedChain getChain() { return chain; }

    public Block getLastBlock() { return Last; }

    public int maxBucketLevel() { return buckets.size(); }

    // Creates a mining event scheduled in the future
    void CreateMiningEvent(int BlockNumber, long CurrentTime) {
        if (BlockNumber <= Sharables.ChainLength) {
            MiningTime = getMiningTime();
            NextEventTime = CurrentTime + MiningTime;
            if (NextEventTime < 0) NextEventTime = Long.MAX_VALUE;
            Scheduler.addEvent(Id, NextEventTime, BlockNumber, this);
        } else {
            Scheduler.init(); // Stop mining if chain length reached
        }
    }

    // Called when this node receives a new block
    public void NewBlock(Block temp, int MyDelay, int ReceivedFrom) {
        if (!chain.exists(temp)) {
            if (isKadcastEnabled) {
                MyDelay += calculateKadcastDelay(temp);
            } else if (Sharables.EnablePiChu) {
                int chunks = (int) Math.ceil((double)(Sharables.BlockSize - Sharables.HeaderSize) / Sharables.ChunkSize);
                int chunkDelay = (int) (((double)(Sharables.ChunkSize + Sharables.SignatureSize) * 1000) / Sharables.NodesBandwidth[Id]);
                MyDelay += chunks * chunkDelay * Sharables.Cm;
            }

            if (temp.NodeNumber > Last.NodeNumber && (temp.TimeStamp + MyDelay) < NextEventTime) {
                Last = temp;
                LastBlockDelay = MyDelay;
                CreateMiningEvent(temp.NodeNumber + 1, temp.TimeStamp + MyDelay);
            }

            if (temp.NodeNumber == Last.NodeNumber && LastBlockDelay > MyDelay) {
                Last = temp;
                LastBlockDelay = MyDelay;
            }

            chain.add(temp);

            if (MyDelay > Sharables.MaxDelay[temp.NodeNumber - 1]) {
                Sharables.MaxDelay[temp.NodeNumber - 1] = MyDelay;
            }
        }
    }

    // Estimates Kadcast delay from sender to receiver
    private int calculateKadcastDelay(Block block) {
        long size = Sharables.BlockSize;
        int txDelay = (int)((size * 1000.0) / Sharables.NodesBandwidth[Id]);
        int valDelay = (int)((size / (2 * 8 * 1024)) / 4);
        return Sharables.NodesLatency[Id] + valDelay + txDelay;
    }

    // Kadcast broadcast method using chunk-based transmission and K-buckets
    void KadcastBroadcast(Node sender1) {
        int chunkCount = (int)Math.ceil((double)Sharables.BlockSize / Sharables.ChunkSize);
        long totalPropagationTime = 0;
        Set<String> sentChunks = new HashSet<>();
        Queue<ChunkTransmission> queue = new LinkedList<>();

        for (int chunkId = 0; chunkId < chunkCount; chunkId++) {
            queue.add(new ChunkTransmission(chunkId, sender1, 0, chunkId * 10));
        }

        while (!queue.isEmpty()) {
            ChunkTransmission tx = queue.poll();
            int chunkId = tx.chunkId;
            Node sender = tx.sender;
            int bucketLevel = tx.bucketLevel;
            long txTime = tx.transmissionTime;

            List<Node> bucket = getNeighborsInBucket(bucketLevel);
            if (bucket.isEmpty()) continue;

            long maxDelay = 0;
            for (Node neighbor : bucket) {
                String key = neighbor.getId() + ":" + chunkId;
                if (sentChunks.contains(key)) continue;

                long delay = Functions.calculateDelay(sender, neighbor);
                maxDelay = Math.max(maxDelay, delay);

                queue.add(new ChunkTransmission(chunkId, neighbor, bucketLevel + 1, txTime + delay));
                sentChunks.add(key);
            }

            totalPropagationTime = txTime + maxDelay;
        }

        Sharables.TotalPropagationTime = totalPropagationTime;
    }
    // Represents a transmission event of a block in the network
    class BlockTransmission {
        Node sender; // The node sending the block
        int bucketLevel; // The XOR-distance-based level of the routing bucket
        long transmissionTime; // The time at which this transmission occurs

        BlockTransmission(Node sender, int bucketLevel, long transmissionTime) {
            this.sender = sender;
            this.bucketLevel = bucketLevel;
            this.transmissionTime = transmissionTime;
        }
    }
    // Implements the Kadcast broadcast algorithm without chunking.
    // The block is transmitted as a single unit, following Kademlia's bucket-based routing.
    void KadcastBroadcastWithoutChunks(Node sender) {
        long totalPropagationTime = 0;  // Tracks the maximum time taken for full propagation
        Set<String> sentBlocks = new HashSet<>();   // Keeps track of nodes that have already received the block
        Queue<BlockTransmission> queue = new LinkedList<>(); // Queue for BFS traversal of the network

        queue.add(new BlockTransmission(sender, 0, 0));

        // BFS-like traversal to simulate propagation to neighbors per XOR-distance level
        while (!queue.isEmpty()) {
            BlockTransmission tx = queue.poll();
            Node currentSender = tx.sender;
            int bucketLevel = tx.bucketLevel;
            long txTime = tx.transmissionTime;

            List<Node> bucket = getNeighborsInBucket(bucketLevel);
            if (bucket.isEmpty()) continue;

            long maxDelay = 0;
            for (Node neighbor : bucket) {
                String key = String.valueOf(neighbor.getId());
                if (sentBlocks.contains(key)) continue;

                long delay = Functions.calculateBlockDelay(currentSender, neighbor);
                maxDelay = Math.max(maxDelay, delay);

                queue.add(new BlockTransmission(neighbor, bucketLevel + 1, txTime + delay));
                sentBlocks.add(key);
            }

            totalPropagationTime = txTime + maxDelay;
        }

        Sharables.TotalPropagationTime = totalPropagationTime;
    }

    public List<Node> getNeighborsInBucket(int level) {
        if (level >= 0 && level < buckets.size()) {
            return new ArrayList<>(buckets.get(level));
        }
        return Collections.emptyList();
    }

    static class ChunkTransmission {
        int chunkId;
        Node sender;
        int bucketLevel;
        long transmissionTime;

        public ChunkTransmission(int chunkId, Node sender, int bucketLevel, long transmissionTime) {
            this.chunkId = chunkId;
            this.sender = sender;
            this.bucketLevel = bucketLevel;
            this.transmissionTime = transmissionTime;
        }
    }

    // Basic BFS-style propagation used in traditional simulation (not Kadcast)
    void Broadcast(Block temp) {
        class Ntemp {
            int Parent;
            int NodeNumber;
            int HopCount;

            public Ntemp(int NodeNumber, int HopCount, int Parent) {
                this.Parent = Parent;
                this.NodeNumber = NodeNumber;
                this.HopCount = HopCount;
            }
        }

        Queue<Ntemp> queue = new LinkedList<>();
        HashSet<Integer> InQueue = new HashSet<>();
        InQueue.add(Id);

        int connCount = Sharables.ConnectionsCount[Id];
        if (connCount == 0) {
            System.out.println("This node does not have connection: " + Id);
            return;
        }

        long size = Sharables.EnablePiChu ? Sharables.HeaderSize : Sharables.BlockSize;
        if (Sharables.EnableRedFork) size = Sharables.RedForkPartASize;

        int tdelay = (int)((size * 1000.0) / Sharables.NodesBandwidth[Id]);
        int valDelay = (int)((size / (2 * 8 * 1024)) / 4);
        int delay = (connCount - 1) * tdelay + Sharables.NodesLatency[Id] + valDelay;

        for (int i = 0; i < connCount; i++) {
            int nbr = Sharables.Connections[Id][i];
            queue.add(new Ntemp(nbr, delay, Id));
            InQueue.add(nbr);
        }

        while (!queue.isEmpty()) {
            Ntemp current = queue.remove();
            int nodeNum = current.NodeNumber;
            int hop = current.HopCount;

            Sharables.Nodes[nodeNum].NewBlock(temp, hop, current.Parent);

            for (int i = 0; i < Sharables.ConnectionsCount[nodeNum]; i++) {
                int nbr = Sharables.Connections[nodeNum][i];
                if (!InQueue.contains(nbr)) {
                    int tdelay1 = (int)((size * 1000.0) / Sharables.NodesBandwidth[nodeNum]);
                    int valDelay1 = (int)((size / (2 * 8 * 1024)) / 4);
                    queue.add(new Ntemp(nbr, hop + (Sharables.ConnectionsCount[nodeNum] - 1) * tdelay1 + Sharables.NodesLatency[nodeNum] + valDelay1, nodeNum));
                    InQueue.add(nbr);
                }
            }
        }
    }
}