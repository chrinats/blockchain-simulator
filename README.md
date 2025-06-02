
Blockchain Broadcasting Simulator

This project is a blockchain broadcasting simulator initially based on the open-source repository developed by Mobsya: [Mobsya/blockchain-simulator](https://github.com/Mobsya/blockchain-simulator). It simulates the behavior of block propagation mechanisms in decentralized networks.

## 🧩 Features

- **Kadcast Protocol Integration**: A structured broadcasting protocol based on the XOR-metric and DHT principles.
- **Hybrid Propagation Strategy**: Dynamically selects between monolithic and chunked broadcasting depending on block size (≤ 8KB for monolithic).
- **Performance Metrics**: Records total propagation time and chunk handling efficiency across different network scales.

## 📁 Project Structure

Only the relevant Java classes are maintained under: blockchain-simulator/build/classes/java/main/org/example/

## 🛠 How to Use

Clone the repository and run the simulation using IntelliJ IDEA or any Java IDE supporting Gradle.

## 📚 Reference

This project extends:
- [https://github.com/Mobsya/blockchain-simulator](https://github.com/Mobsya/blockchain-simulator)

Please refer to the original repository for the baseline architecture.
