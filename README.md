# LZW Compression (Java)

This project implements both standard LZW compression and an improved version in Java.  
It includes modules for compression, decompression, bit-level management, and a simple GUI.

## Project Structure

```
LZW-Compression/
│
├── inputs/           # Example input files (user-provided)
│   └── example.txt
│
├── lzw/              # Java source files
│   ├── Program.java
│   ├── BitManager.java
│   ├── RegularLZW.java
│   ├── ImprovedLZW.java
│   └── LZWGui.java
│
└── Outputs/          # Generated automatically during runtime
```

## Features

- Regular LZW compression  
- Improved LZW version  
- Compression and decompression support  
- Bit-level read/write manager  
- Simple GUI for running compression and decompression  
- Clean folder separation (inputs / outputs / source)

## How to Run

### Run with GUI
```
java LZWGui
```

### Run Regular LZW (CLI)
```
java Program regular <inputFile> <outputFile>
```

### Run Improved LZW (CLI)
```
java Program improved <inputFile> <outputFile>
```

Outputs are created automatically inside the `Outputs/` folder.

## Author
**Maya Sofer**  
Computer Science Student, Sapir College
