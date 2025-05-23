package org.example;

/**
 * Represents a block in the blockchain simulation.
 * Each block holds basic metadata necessary for validation and network propagation.
 * This class is instantiated by nodes to build and maintain their local blockchain.
 *
 * Author: Ayinala Kaushik (UMKC)
 */
public class Block {

    // Timestamp of the previous block (used to maintain chain continuity)
    long PrevTimeStamp;

    // ID of the node that received or is storing this block
    int NodeNumber;

    // Timestamp when this block was created or received
    long TimeStamp;

    // ID of the node that mined this block
    int MinerId;

    // Number of hops this block has taken through the network (useful in Kadcast)
    int HopCount;

    /**
     * Constructs a new Block object.
     *
     * @param PrevTimeStamp Timestamp of the block that comes before this one
     * @param NodeNumber    Identifier of the node handling this block
     * @param TimeStamp     Time when this block was created or processed
     * @param MinerId       ID of the miner (node) that produced the block
     */
    public Block(long PrevTimeStamp, int NodeNumber, long TimeStamp, int MinerId) {
        this.PrevTimeStamp = PrevTimeStamp;
        this.NodeNumber = NodeNumber;
        this.TimeStamp = TimeStamp;
        this.MinerId = MinerId;
    }

    /**
     * Returns a human-readable representation of this block's key fields.
     *
     * @return A formatted string with block metadata
     */
    public String toString() {
        String temp = " NodeNumber=" + NodeNumber +
                " PrevTimeStamp=" + PrevTimeStamp +
                " TimeStamp=" + TimeStamp +
                " MinerId=" + MinerId;
        return temp;
    }
}