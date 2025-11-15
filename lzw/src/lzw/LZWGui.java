package lzw;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LZWGui extends JFrame {
    private final JTextField inputPath = new JTextField();
    private final JTextField outputPath = new JTextField();

    private final JRadioButton regular = new JRadioButton("Regular LZW", true);
    private final JRadioButton improved = new JRadioButton("Improved LZW");

    private final JButton btnCompress = new JButton("Compress");
    private final JButton btnDecompress = new JButton("Decompress");

    private final JTextArea outputArea = new JTextArea(12, 70);

    public LZWGui() {
        super("LZW Compression Tool"); // window title
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildUI(); // build the UI components
        wireEvents(); // wire the buttons
        pack();
        setLocationRelativeTo(null); // center on screen
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12)); // set padding around the window

        JPanel top = new JPanel(new GridLayout(3, 1, 8, 8)); // Top section: input, output, algorithm

        JPanel inputRow = new JPanel(new BorderLayout(6, 6));
        JButton inputFile = new JButton("Choose File");
        inputRow.add(new JLabel("Input file:"), BorderLayout.WEST);
        inputRow.add(inputPath, BorderLayout.CENTER);
        inputRow.add(inputFile, BorderLayout.EAST);

        JPanel outputRow = new JPanel(new BorderLayout(6, 6));
        JButton chooseOut = new JButton("Choose File");
        outputRow.add(new JLabel("Output file:"), BorderLayout.WEST);
        outputRow.add(outputPath, BorderLayout.CENTER);
        outputRow.add(chooseOut, BorderLayout.EAST);

        ButtonGroup groupOfButtons = new ButtonGroup(); // group for radio buttons
        groupOfButtons.add(regular);
        groupOfButtons.add(improved);
        JPanel algRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        algRow.add(new JLabel("Algorithm:"));
        algRow.add(regular);
        algRow.add(improved);

        top.add(inputRow);
        top.add(outputRow);
        top.add(algRow);

        JPanel middle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); // middle section: buttons
        middle.add(btnCompress);
        middle.add(btnDecompress);

        // bottom section: output area (read-only + scrollable)
        outputArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(outputArea);

        root.add(top, BorderLayout.NORTH);
        root.add(middle, BorderLayout.CENTER);
        root.add(scroll, BorderLayout.SOUTH);

        setContentPane(root); // set the content pane to the root panel

        // input chooser - open file dialog and set input path
        inputFile.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser(new File(".")); // starts in current directory
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                inputPath.setText(file.getAbsolutePath());// set input path
                fillOutputFromInput(file); // suggest output path from chosen file
            }
        });

        // output chooser - open save dialog and set output path
        chooseOut.addActionListener(event -> {
            File suggestedName = getSuggestedOutputFile();
            JFileChooser fileChooser = new JFileChooser(suggestedName.getParentFile()); // start in suggested directory
            fileChooser.setSelectedFile(suggestedName); // suggest default name
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                outputPath.setText(fileChooser.getSelectedFile().getAbsolutePath()); // set output path
            }
        });
    }

    // connects buttons to their actions (compress/decompress)
    private void wireEvents() {
        btnCompress.addActionListener(event -> onAction(true));
        btnDecompress.addActionListener(event -> onAction(false));
    }

    // get file extension in lower case, or "" if none
    private String extensionName(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0 && i < name.length() - 1) ? name.substring(i + 1).toLowerCase() : "";
    }

    // get file name without extension
    private String baseName(String name) {
        int i = name.lastIndexOf('.');
        return (i > 0) ? name.substring(0, i) : name;
    }

    // make sure output folder exists (compressed or decoded)
    private File createOutputFolder(boolean isCompress) {
        File outputsFolder = new File("Outputs");
        File subFolder = new File(outputsFolder, isCompress ? "Compressed" : "Decoded");
        subFolder.mkdirs();// create if not exists
        return subFolder;
    }

    // if file already exists, add _(<number>) until name is unique
    private File makeUnique(File file) {
        if (!file.exists())
            return file;

        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? name.substring(0, dotIndex) : name;
        String extension = (dotIndex > 0) ? name.substring(dotIndex) : "";
        File folder = file.getParentFile();
        int number = 1;
        File nextFile;
        do { // keep adding numbers until we find a unique name
            nextFile = new File(folder, baseName + "_(" + number + ")" + extension);
            number++;
        } while (nextFile.exists());
        return nextFile;
    }

    //suggest output file name from the input (handles both compress/decompress cases)
    private File getSuggestedOutputFile() {
        String inputText = inputPath.getText().trim();
        File compFolder = createOutputFolder(true);
        File decFolder = createOutputFolder(false);

        if (inputText.isEmpty())
            return new File(compFolder, "output.lzw");

        File inputFile = new File(inputText);
        String fileName = inputFile.getName();
        String base = baseName(fileName);
        String inputExt = extensionName(fileName);

        //if input is not .lzw, suggest compressed output
        if (!"lzw".equalsIgnoreCase(inputExt)) {
            return new File(compFolder, base + ".lzw");
        }

        //try to recover original extension from a simple LZWM header we wrote in compress
        try {
            byte[] data = Files.readAllBytes(inputFile.toPath());
            if (data.length >= 6 && data[0] == 'L' && data[1] == 'Z' && data[2] == 'W' && data[3] == 'M') {
                int nameLength = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
                if (data.length >= 6 + nameLength) {
                    String originalName = new String(data, 6, nameLength, StandardCharsets.ISO_8859_1);
                    String origExt = extensionName(originalName);
                    if (!origExt.isEmpty())
                        return new File(decFolder, base + "." + origExt);
                }
            }
        } catch (IOException e) { // ignore if we can't read the file
        }

        return new File(decFolder, base + ".decoded");
    }

    // set the output text field to a suggesten file path
    private void fillOutputFromInput(File inputFile) {
        if (inputFile == null)
            return;
        File suggestedFile = getSuggestedOutputFile();
        outputPath.setText(suggestedFile.getAbsolutePath());
    }

    // enable/disable buttons while processing
    private void setBusy(boolean busy) {
        btnCompress.setEnabled(!busy);
        btnDecompress.setEnabled(!busy);
    }

    // build header: magic + name length + original name
    private byte[] makeHeader(String fileName) {
        byte[] nameBytes = fileName.getBytes(StandardCharsets.ISO_8859_1);

        final int MAX_LEN = 65535;
        if (nameBytes.length > MAX_LEN) {
            nameBytes = java.util.Arrays.copyOf(nameBytes, MAX_LEN);
        }

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

        byte[] sign = new byte[] { 'L', 'Z', 'W', 'M' };
        byteOutStream.write(sign, 0, sign.length);

        int length = nameBytes.length;
        byteOutStream.write((length >>> 8) & 0xFF);
        byteOutStream.write(length & 0xFF);
        byteOutStream.write(nameBytes, 0, length);// write the file name bytes

        return byteOutStream.toByteArray();
    }

    // small class to hold LZW header information
    private static class LzwHeader {
        final String name;
        final int offset;

        LzwHeader(String n, int o) {
            name = n;
            offset = o;
        }
    }

    // read the LZW header or return a default one if not found
    private LzwHeader readHeaderOrDefault(byte[] data, File lzwFile) {
        if (data.length >= 6 && data[0] == 'L' && data[1] == 'Z' && data[2] == 'W' && data[3] == 'M') {
            int nameLength = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
            if (nameLength >= 0 && data.length >= 6 + nameLength) {
                String name = new String(data, 6, nameLength, StandardCharsets.ISO_8859_1);
                return new LzwHeader(name, 6 + nameLength);
            }
        }
        String defaultName = baseName(lzwFile.getName()) + ".decoded";
        return new LzwHeader(defaultName, 0);
    }

    private void onAction(boolean isCompress) {
        String input = inputPath.getText().trim();
        boolean isImproved = improved.isSelected(); //true for Improved LZW, false for Regular

        if (input.isEmpty()) { //check if input is empty
            JOptionPane.showMessageDialog(this, "Please select an input file.\n",
                    "No input file", JOptionPane.WARNING_MESSAGE);
            return; 
        }

        String fileExt = extensionName(new File(input).getName()); //get file extension
        if (isCompress && fileExt.equals("lzw")) { //already .lzw file
            JOptionPane.showMessageDialog(this, "This file is already compressed (.lzw). Please choose a file that is not compressed.\n ",
                    "Wrong file type", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        //if user tries to decompress, check if file is .lzw
        if (!isCompress && !fileExt.equals("lzw")) {
            JOptionPane.showMessageDialog(this,
                    "To decompress, choose a .lzw file.\n",
                    "Wrong file type", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //if the output path is empty, suggest a name
        if (outputPath.getText().trim().isEmpty()) {
            outputPath.setText(getSuggestedOutputFile().getAbsolutePath());
        }

        setBusy(true); //disable buttons while processing
        outputArea.setText(""); //clear output area
        final String selectedInput = input; //to make sure it won't change during processing

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    if (isCompress) { //COMPRESS
                        publish("Reading data from file .. \n");
                        File inputFile = new File(selectedInput);
                        byte[] data = Files.readAllBytes(inputFile.toPath()); //read all bytes from input file
                        String text = new String(data, StandardCharsets.ISO_8859_1); //convert bytes to string

                        publish("Compressing with " + (isImproved ? "Improved LZW" : "Regular LZW \n"));
                        ByteArrayOutputStream compressed = isImproved ? lzw.ImprovedLZW.Compress(text) : lzw.RegularLZW.Compress(text); //compress the text with the selected algorithm

                        //create header with original file name
                        String originalName = inputFile.getName();
                        byte[] header = makeHeader(originalName);


                        String base = baseName(originalName);
                        String algTag = isImproved ? "improved" : "regular";
                        File outFolder = createOutputFolder(true); //create output folder for compressed files
                        File outFile = new File(outFolder, base + "_" + algTag + ".lzw");
                        outFile = makeUnique(outFile); //make sure the file name is unique

                        publish("Writing .lzw file with metadata to:" + outFile.getAbsolutePath() + "\n");
                        try (FileOutputStream fileOutStream = new FileOutputStream(outFile)) {
                            fileOutStream.write(header); //write the header
                            fileOutStream.write(compressed.toByteArray()); //write the compressed data
                        }
                        outputPath.setText(outFile.getAbsolutePath());
                        publish("Done. \n Output: " + outFile.getAbsolutePath());

                    } else {
                        //DECOMPRESS
                        publish("Reading data from .lzw file .. \n");
                        File inFile = new File(selectedInput);
                        byte[] allBytes = Files.readAllBytes(inFile.toPath());

                        //reads the header or returns a default one
                        LzwHeader metaHeader = readHeaderOrDefault(allBytes, inFile);
                        String originalName = metaHeader.name;

                        //reads the data after the header
                        byte[] decodedData = new byte[allBytes.length - metaHeader.offset];
                        System.arraycopy(allBytes, metaHeader.offset, decodedData, 0, decodedData.length);
                        ByteArrayOutputStream inStream = new ByteArrayOutputStream(); //write the data to a stream
                        inStream.write(decodedData);

                        publish("Decompressing with " + (isImproved ? "Improved LZW" : "Regular LZW") + ".. \n");
                        String text = isImproved
                                ? lzw.ImprovedLZW.Decompress(inStream)
                                : lzw.RegularLZW.Decompress(inStream); //decompress the data with the selected algorithm


                        File outDir = createOutputFolder(false); //create output folder for decoded files
                        String baseLzw = baseName(inFile.getName());
                        String origExt = extensionName(originalName);
                        String algTag = isImproved ? "improved" : "regular";
                        String finalName = baseLzw + "_" + algTag + "." + (origExt.isEmpty() ? "decoded" : origExt);
                        File outFile = new File(outDir, finalName);
                        outFile = makeUnique(outFile); //make sure the file name is unique

                        publish("Writing decoded file to: " + outFile.getAbsolutePath() + "\n");
                        byte[] outBytes = text.getBytes(StandardCharsets.ISO_8859_1);
                        Files.write(outFile.toPath(), outBytes);
                        outputPath.setText(outFile.getAbsolutePath());
                        publish("Done. \n Output: " + outFile.getAbsolutePath());
                    }
                } catch (Throwable ex) {
                    publish("Error: " + ex);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(LZWGui.this, ex.toString(),
                            "Error", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks)
                    outputArea.append(s + "\n");
            }

            @Override
            protected void done() {
                setBusy(false);
            }
        };
        worker.execute();
    }
}
