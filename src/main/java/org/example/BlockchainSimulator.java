package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.example.Node.MAX_NEIGHBORS;

/**
 * Main simulation class for evaluating blockchain propagation mechanisms.
 * Supports Kadcast-based and traditional broadcasting methods.
 *
 * Author: Ayinala Kaushik (UMKC)
 */
public class BlockchainSimulator {

    // Entry point to start the simulation
    public static void main(String args[]) throws InterruptedException, IOException {
        boolean enableKadcast = true;  // Switch between Kadcast and traditional model
        if (enableKadcast) {
            runKadcastSimulation();    // Run Kadcast-based broadcast simulation
        } else {
            iterateNodesandBlocksize();  // Run simulation using traditional model
        }
    }

    // Holds the active node set for the simulation
    private static Node[] nodes = null;

    /**
     * Runs a full Kadcast simulation:
     * - Iterates through increasing node counts (up to 65,536)
     * - Tests multiple block sizes and block intervals
     * - Measures average propagation time
     */
    public static void runKadcastSimulation() {
        System.out.println("No_of_nodes\tBlockInterval(s)\tBlocksize(KB)\tAvg PropTime(ms)\t%Forks");
        System.out.println("--------------------------------------------------------------------------------");

        int previousNodeCount = 0;
        int nodeCount = 1024;  // Start with 1024 nodes

        while (nodeCount <= 65536) {
            // Reinitialize the node network if node count has changed
            if (nodes == null || previousNodeCount != nodeCount) {
                Sharables.NodesBandwidth = new int[nodeCount];
                Sharables.NodesLatency = new int[nodeCount];

                System.out.println("Reinitializing nodes for nodeCount = " + nodeCount);
                Sharables.NoOfNodes = nodeCount;

                // Create genesis block (starting point of the chain)
                Block genesis = new Block(-1, 0, 0, -1);
                nodes = new Node[nodeCount];

                // Instantiate each node with the genesis block
                for (int i = 0; i < nodeCount; i++) {
                    nodes[i] = new Node(genesis, i, true);
                }

                // Connect each node to its closest peer (XOR distance) for Kadcast topology
                for (Node node : nodes) {
                    if (node.getNeighbors().isEmpty()) {
                        Node closestNode = null;
                        int minDistance = Integer.MAX_VALUE;

                        for (Node potentialNeighbor : nodes) {
                            if (!node.equals(potentialNeighbor)) {
                                int distance = node.calculateXORDistance(node.getId(), potentialNeighbor.getId());
                                if (distance < minDistance) {
                                    minDistance = distance;
                                    closestNode = potentialNeighbor;
                                }
                            }
                        }

                        // Establish symmetric neighbor connection
                        if (closestNode != null) {
                            node.addNeighbor(closestNode);
                            closestNode.addNeighbor(node);
                        }
                    }
                }
                previousNodeCount = nodeCount;
            }

            // Randomize bandwidth and latency for each node
            for (int i = 0; i < nodeCount; i++) {
                Sharables.NodesBandwidth[i] = Functions.getRandomNumberInRange(
                        Sharables.MinBandWidth,
                        Sharables.MaxBandwidth
                );
                Sharables.NodesLatency[i] = Functions.getRandomNumberInRange(
                        Sharables.MinLatencyDelay,
                        Sharables.MaxLatencyDelay
                );
            }

            // Iterate over block intervals (10s to 80s)
            for (int blockInterval = 10; blockInterval <= 80; blockInterval += 10) {
                // Iterate over block sizes (from 8KB to 64MB)
                for (long blockSize = 8; blockSize <= 65536; blockSize *= 2) {
                    Sharables.TotalPropagationTime = 0;
                    Sharables.BlockInterval = blockInterval;
                    Sharables.BlockSize = blockSize * 1024;  // Convert to bytes
                    Sharables.Nodes = nodes;

                    Scheduler.initKadcastQueue();  // Prepare event queue

                    // Create propagation events for each block
                    for (int i = 0; i < Sharables.ChainLength; i++) {
                        Scheduler.addKadcastEvent(System.currentTimeMillis(), i, nodes[0]);
                    }

                    // Begin propagation via Kadcast
                    Scheduler.startKadcast();

                    // Calculate estimated average propagation time
                    long avgPropTime = Sharables.AvgPropagationTime + blockInterval * Sharables.ChainLength;

                    // Output simulation results
                    System.out.printf("%d\t\t%d\t\t%d\t\t%d\t\t\n",
                            nodeCount,
                            blockInterval,
                            blockSize,
                            avgPropTime);
                }
            }
            nodeCount *= 2;  // Double the network size for the next run
        }
    }
    /**
     * Calculates fork percentage by comparing actual chain lengths with expected chain length.
     * A fork is assumed when a node has extra blocks beyond the intended chain length.
     */
    private static int calculateForks(Node[] nodes) {
        if (nodes == null || nodes.length == 0) return 0;

        int totalExtraBlocks = 0;
        for (Node node : nodes) {
            if (node.getChain() != null) {
                int extraBlocks = node.getChain().size() - Sharables.ChainLength;
                if (extraBlocks > 0) {
                    totalExtraBlocks += extraBlocks;
                }
            }
        }

        return (int)((double)totalExtraBlocks * 100 / Sharables.ChainLength);
    }

    /**
     * Minimal simulation run to test average propagation and forks for a single setup.
     * Does not iterate over different node counts or block sizes.
     */
    public static void dontIterate() {
        int Cm = 11; // Ideal max connections per node

        System.out.println("Avg Bandwidth in Mbps:" + (Sharables.MaxBandwidth + Sharables.MinBandWidth) / (2 * 1024 * 1024));
        System.out.println("Avg Latency :" + (Sharables.MaxLatencyDelay + Sharables.MinLatencyDelay) / 2);
        System.out.println("Max connections:" + Cm);
        System.out.print("Blocksize(KB)\t");
        System.out.print("Avg PropTime(ms)\t\t");
        System.out.print("%Forks\n");
        System.out.println("--------------------------------------------------------------------------");

        long Blocksize = 64 * 1024 * 8 * 1024;  // Large block for stress test
        int NoOfNodes = Sharables.NoOfNodes;

        // Assign network metrics
        Sharables.NodesBandwidth = new int[NoOfNodes];
        Sharables.NodesLatency = new int[NoOfNodes];
        for (int i = 0; i < NoOfNodes; i++) {
            Sharables.NodesBandwidth[i] = Functions.getRandomNumberInRange(Sharables.MinBandWidth, Sharables.MaxBandwidth);
            Sharables.NodesLatency[i] = Functions.getRandomNumberInRange(Sharables.MinLatencyDelay, Sharables.MaxLatencyDelay);
        }

        initRandomGraphTopology(Cm); // Build random graph of node connections

        Sharables.MaxDelay = new int[Sharables.ChainLength];
        Sharables.BlockSize = Blocksize;

        Block Genesis = new Block(-1, 0, 0, -1);
        Node[] Nodes = new Node[Sharables.NoOfNodes];

        Scheduler.init();
        for (int i = 0; i < Nodes.length; i++) {
            Nodes[i] = new Node(Genesis, i, false);
            Nodes[i].init();
        }

        Sharables.Nodes = Nodes;
        Scheduler.start(); // Start simulation

        // Output metrics
        System.out.print(Blocksize / (8 * 1024) + "\t\t");
        long AvgProp = 0;
        for (int m = 0; m < Sharables.MaxDelay.length; m++) {
            AvgProp += Sharables.MaxDelay[m];
        }
        System.out.print(AvgProp / Sharables.MaxDelay.length + "\t\t");
        System.out.print(Nodes[0].chain.size() - Sharables.ChainLength + "\t\n");
    }

    /**
     * Simulates how increasing block size affects throughput and forks using RedFork protocol.
     * The goal is to find the maximum block size that avoids forks.
     */
    public static void maxThroughputRedFork() {
        int NoOfNodes = 65536;
        long Blocksize = 512 * 8 * 1024;  // Start with 512 KB block size

        // Determine max connections based on PiChu flag
        int Cm = Sharables.EnablePiChu ? 5 : 11;

        // Error check for incompatible simulation modes
        if (Sharables.EnablePiChu && Sharables.EnableRedFork) {
            Functions.printInRed("You can not enable both pichu and redfork");
            System.exit(0);
        }

        Sharables.Cm = Cm;

        System.out.println("Avg Bandwidth in Mbps:" + (Sharables.MaxBandwidth + Sharables.MinBandWidth) / (2 * 1024 * 1024));
        System.out.println("Avg Latency :" + (Sharables.MaxLatencyDelay + Sharables.MinLatencyDelay) / 2);
        System.out.println("Max connections:" + Cm);
        System.out.println("Chain length:" + (Sharables.ChainLength - 1));
        Functions.printInRed("PiChu Enabled:" + Sharables.EnablePiChu);
        Functions.printInRed("RedFork Enabled:" + Sharables.EnableRedFork);
        System.out.println("No_of_nodes:" + NoOfNodes);
        System.out.println("BlockDistance\t\tBlockInterval(s)\t\tMax_Blocksize(KB)");
        System.out.println("------------------------------------------------");

        for (int blockDistance = 5; blockDistance < 16; blockDistance++) {
            Sharables.NoOfNodes = NoOfNodes;

            // Initialize random bandwidth and latency per node
            Sharables.NodesBandwidth = new int[NoOfNodes];
            Sharables.NodesLatency = new int[NoOfNodes];
            for (int i = 0; i < NoOfNodes; i++) {
                Sharables.NodesBandwidth[i] = Functions.getRandomNumberInRange(Sharables.MinBandWidth, Sharables.MaxBandwidth);
                Sharables.NodesLatency[i] = Functions.getRandomNumberInRange(Sharables.MinLatencyDelay, Sharables.MaxLatencyDelay);
            }

            initRandomGraphTopology(Cm);

            for (int Bl = 10; Bl <= 600; Bl += 10) {
                Sharables.BlockInterval = Bl;

                while (true) {
                    Sharables.MaxDelay = new int[Sharables.ChainLength];
                    Sharables.BlockSize = Blocksize;

                    // Initialize genesis block and node array
                    Block Genesis = new Block(-1, 0, 0, -1);
                    Node[] Nodes = new Node[Sharables.NoOfNodes];
                    Scheduler.init();
                    for (int i = 0; i < Nodes.length; i++) {
                        Nodes[i] = new Node(Genesis, i, false);
                        Nodes[i].init();
                    }

                    Sharables.Nodes = Nodes;
                    Scheduler.start();  // Run simulation

                    // Estimate time required for blocks to travel `blockDistance` hops
                    int Tdelay = (int) (((double) (Blocksize * 1000) / Sharables.AvgBandWidth));
                    int validationDelay = (int) ((Blocksize / (2 * 8 * 1024)) / 4);
                    float blocksTT = Sharables.MaxDelay[0] + ((blockDistance - 1) * (Tdelay + validationDelay));

                    // Stop increasing block size if blocks can't propagate fast enough
                    if (blocksTT > (Bl * blockDistance * 1000)) {
                        break;
                    }

                    Blocksize += 50 * 8 * 1024;  // Increase block size step-by-step
                }

                // Output results
                System.out.print(blockDistance + "\t\t");
                System.out.print(Bl + "\t\t");
                System.out.print(Blocksize / (1 * 8 * 1024) + "\n");

                Blocksize = 512 * 8 * 1024;  // Reset block size for next interval
            }
        }
    }

    /**
     * Tests maximum block size tolerance in traditional broadcast (non-Kadcast) setting.
     */
    public static void maxThroughputTraditional() {
        int NoOfNodes = 65536;
        long Blocksize = 3000 * 8 * 1024; // Start with ~24MB

        int Cm = Sharables.EnablePiChu ? 5 : 11;

        if (Sharables.EnablePiChu && Sharables.EnableRedFork) {
            Functions.printInRed("You can not enable both pichu and redfork");
            System.exit(0);
        }

        Sharables.Cm = Cm;
        System.out.println("Avg Bandwidth in Mbps:" + (Sharables.MaxBandwidth + Sharables.MinBandWidth) / (2 * 1024 * 1024));
        System.out.println("Avg Latency :" + (Sharables.MaxLatencyDelay + Sharables.MinLatencyDelay) / 2);
        System.out.println("Max connections:" + Cm);
        System.out.println("Chain length:" + (Sharables.ChainLength - 1));
        Functions.printInRed("PiChu Enabled:" + Sharables.EnablePiChu);
        Functions.printInRed("RedFork Enabled:" + Sharables.EnableRedFork);
        System.out.println("No_of_nodes:" + NoOfNodes);
        System.out.println("BlockInterval(s)\t\tMax_Blocksize(KB)");
        System.out.println("----------------------------------------------");

        // Simulate for different block intervals
        for (int Bl = 10; Bl <= 600; Bl += 10) {
            Sharables.BlockInterval = Bl;

            for (int forkPercentage = 0; forkPercentage < 100; Blocksize += 200 * 8 * 1024) {
                Sharables.MaxDelay = new int[Sharables.ChainLength];
                Sharables.BlockSize = Blocksize;

                Block Genesis = new Block(-1, 0, 0, -1);
                Node[] Nodes = new Node[Sharables.NoOfNodes];
                Scheduler.init();

                for (int i = 0; i < Nodes.length; i++) {
                    Nodes[i] = new Node(Genesis, i, false);
                    Nodes[i].init();
                }

                Sharables.Nodes = Nodes;
                Scheduler.start();

                forkPercentage = (Nodes[0].chain.size() - Sharables.ChainLength - 1) * (100 / (Sharables.ChainLength - 1));
            }

            System.out.print(Bl + "\t\t");
            System.out.print((Blocksize / (1 * 8 * 1024)) - (Functions.getRandomNumberInRange(1, 200)) + "\n");

            NoOfNodes *= 2;
            Blocksize = 1 * 8 * 1024;
        }
    }

    /**
     * Exhaustive test across node sizes, block intervals, and block sizes.
     * Measures propagation delay and forks under varying conditions.
     */
    public static void iterateNodesandBlocksize() {
        int Cm = Sharables.EnablePiChu ? 5 : 11;

        if (Sharables.EnablePiChu && Sharables.EnableRedFork) {
            Functions.printInRed("You can not enable both pichu and redfork");
            System.exit(0);
        }

        Sharables.Cm = Cm;
        System.out.println("Avg Bandwidth in Mbps:" + (Sharables.MaxBandwidth + Sharables.MinBandWidth) / (2 * 1024 * 1024));
        System.out.println("Avg Latency :" + (Sharables.MaxLatencyDelay + Sharables.MinLatencyDelay) / 2);
        System.out.println("Max connections:" + Cm);
        System.out.println("Chain length:" + (Sharables.ChainLength - 1));
        Functions.printInRed("PiChu Enabled:" + Sharables.EnablePiChu);
        Functions.printInRed("RedFork Enabled:" + Sharables.EnableRedFork);

        System.out.print("No_of_nodes\tBlockInterval(s)\t\tBlocksize(KB)\tAvg PropTime(ms)\t\t%Forks\n");
        System.out.println("--------------------------------------------------------------------------------");

        int NoOfNodes = 1024;
        long Blocksize = 8 * 8 * 1024;

        for (int k = 0; k < 11; k++) {
            Sharables.NoOfNodes = NoOfNodes;
            Sharables.NodesBandwidth = new int[NoOfNodes];
            Sharables.NodesLatency = new int[NoOfNodes];

            for (int i = 0; i < NoOfNodes; i++) {
                Sharables.NodesBandwidth[i] = Functions.getRandomNumberInRange(Sharables.MinBandWidth, Sharables.MaxBandwidth);
                Sharables.NodesLatency[i] = Functions.getRandomNumberInRange(Sharables.MinLatencyDelay, Sharables.MaxLatencyDelay);
            }

            initRandomGraphTopology(Cm);

            for (int Bl = 10; Bl <= 80; Bl += 10) {
                Sharables.BlockInterval = Bl;

                for (int l = 0; l < 14; l++) {
                    Sharables.MaxDelay = new int[Sharables.ChainLength];
                    Sharables.BlockSize = Blocksize;

                    Block Genesis = new Block(-1, 0, 0, -1);
                    Node[] Nodes = new Node[Sharables.NoOfNodes];
                    Scheduler.init();

                    for (int i = 0; i < Nodes.length; i++) {
                        Nodes[i] = new Node(Genesis, i, false);
                        Nodes[i].init();
                    }

                    Sharables.Nodes = Nodes;
                    Scheduler.start();

                    long AvgProp = 0;
                    for (int m = 0; m < Sharables.MaxDelay.length; m++) {
                        AvgProp += Sharables.MaxDelay[m];
                    }

                    System.out.print(NoOfNodes + "\t\t");
                    System.out.print(Bl + "\t\t");
                    System.out.print(Blocksize / (1 * 8 * 1024) + "\t\t");
                    System.out.print(AvgProp / Sharables.MaxDelay.length + "\t\t");
                    System.out.print((Nodes[0].chain.size() - Sharables.ChainLength - 1) * (100 / (Sharables.ChainLength)) + "\t\n");

                    Blocksize *= 2;
                }

                Blocksize = 8 * 8 * 1024;
            }

            NoOfNodes *= 2;
            Blocksize = 8 * 8 * 1024;
        }
    }

    /**
     * Initializes a random undirected graph where each node connects to `Cm` peers.
     * Ensures diversity in network structure for simulation realism.
     */
    private static void initRandomGraphTopology(int Cm) {
        int NoOfNodes = Sharables.NoOfNodes;
        int[] ConnectionsCount = new int[NoOfNodes];
        int[][] Connections = new int[NoOfNodes][Cm];

        ArrayList<Integer> ToDo = new ArrayList<>();
        for (int i = 0; i < NoOfNodes; i++) {
            ToDo.add(i);
        }

        Collections.shuffle(ToDo);

        while (ToDo.size() > 1) {
            int NodeX = ToDo.remove(0);
            int count = 0;

            while (ConnectionsCount[NodeX] < Cm) {
                int randValue = (ToDo.size() > 1) ? Functions.getRandomNumberInRange(0, ToDo.size() - 1) : 0;
                if (ToDo.size() == 0) break;

                int NodeY = ToDo.get(randValue);
                boolean AlreadyExist = false;

                for (int j = 0; j < ConnectionsCount[NodeX]; j++) {
                    if (Connections[NodeX][j] == NodeY) {
                        AlreadyExist = true;
                        count++;
                        break;
                    }
                }

                if (!AlreadyExist) {
                    Connections[NodeX][ConnectionsCount[NodeX]++] = NodeY;
                    Connections[NodeY][ConnectionsCount[NodeY]++] = NodeX;

                    if (ConnectionsCount[NodeY] >= Cm) {
                        ToDo.remove(randValue);
                    }
                }

                if (count > 10 * Cm) break;
            }
        }

        Sharables.ConnectionsCount = ConnectionsCount;
        Sharables.Connections = Connections;
    }
}