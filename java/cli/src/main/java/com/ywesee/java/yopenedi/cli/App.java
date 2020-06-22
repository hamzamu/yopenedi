/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.ywesee.java.yopenedi.cli;

import com.ywesee.java.yopenedi.OpenTrans.OpenTransReader;
import com.ywesee.java.yopenedi.OpenTrans.Order;
import com.ywesee.java.yopenedi.converter.Converter;
import com.ywesee.java.yopenedi.OpenTrans.OpenTransWriter;
import com.ywesee.java.yopenedi.Edifact.EdifactReader;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;

public class App {
    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option outputOption = new Option(
                "o",
                "out",
                true,
                "The path to output. When in multiple mode, it will be a folder. If the --input flag is used and output is not specified, the default output path is <input filename>.xml. If -m is not present and -i is not used, the output will be standard output."
        );
        outputOption.setType(String.class);
        options.addOption(outputOption);

        Option inputOption = new Option("i", "in", true, "The path to input file - Stdin is used if this is not provided");
        outputOption.setType(String.class);
        options.addOption(inputOption);

        Option multipleOption = new Option("m", "multiple", false, "Whether to generate multiple xml files. An Edifact file can contain multiple orders, but in our specific use case we only need one. Please use this option if you prefer generating a folder of multiple XML files.");
        options.addOption(multipleOption);

        Option mergeCdOption = new Option(null, "no-merge-contact-details", false, "Prevent merging multiple <CONTACT_DETAILS>");
        options.addOption(mergeCdOption);

        Option helpOption = new Option("h", "help", false, "Display help message");
        options.addOption(helpOption);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        boolean showHelp = false;
        if (cmd.hasOption("help")) {
            showHelp = true;
        }

        File outFile = null;
        boolean isMultiple = cmd.hasOption("m");
        if (cmd.hasOption("out")) {
            outFile = new File(cmd.getOptionValue("out"));
        } else {
            if (cmd.hasOption("in")) {
                if (isMultiple) {
                    outFile = new File(cmd.getOptionValue("in")+"_out");
                } else {
                    outFile = new File(cmd.getOptionValue("in") + ".xml");
                }
            } else if (isMultiple) {
                showHelp = true;
            }
        }
        if (showHelp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "openedi", options );
            return;
        }

        if (isMultiple) {
            if (!outFile.exists()) {
                outFile.mkdirs();
            }
        }

        try {
            InputStream s;
            if (cmd.hasOption("in")) {
                s = new FileInputStream(cmd.getOptionValue("in"));
            } else {
                s = System.in;
            }
            PushbackInputStream buffered = new PushbackInputStream(s, 8);
            switch (detectFileType(buffered)) {
                case OpenTrans:
                    openTransToEdifact(buffered, outFile, cmd);
                    break;
                case Edifact:
                    edifactToOpenTrans(buffered, outFile, cmd);
                    break;
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.toString());
        }
    }

    static void edifactToOpenTrans(InputStream in, File outFile, CommandLine cmd) throws Exception {
        boolean isMultiple = cmd.hasOption("m");
        EdifactReader edifactReader = new EdifactReader();
        ArrayList<com.ywesee.java.yopenedi.Edifact.Order> ediOrders = edifactReader.run(in);
        boolean useStdout = outFile == null;
        if (!useStdout) {
            System.out.println("Detected " + ediOrders.size() + " orders");
        }
        if (ediOrders.size() > 1 && !isMultiple) {
            System.out.println("Only the first order is used, if you want to export multiple orders, use the -m flag.");
        }
        Converter converter = new Converter();
        converter.shouldMergeContactDetails = !cmd.hasOption("no-merge-contact-details");
        for (com.ywesee.java.yopenedi.Edifact.Order edi : ediOrders) {
            Order otOrder = converter.orderToOpenTrans(edi);
            OutputStream out;
            if (outFile != null) {
                File targetFile;
                if (isMultiple) {
                    targetFile = new File(outFile, otOrder.id + ".xml");
                } else {
                    targetFile = outFile;
                }
                System.out.println("Outputting order(id=" + otOrder.id + ") to " + targetFile.getAbsolutePath());
                out = new FileOutputStream(targetFile);
            } else {
                out = System.out;
            }
            OpenTransWriter w = new OpenTransWriter();
            w.write(otOrder, out);
            out.close();
            if (!isMultiple) {
                break;
            }
        }
    }

    static void openTransToEdifact(InputStream in, File outFile, CommandLine cmd) throws Exception {
        OpenTransReader reader = new OpenTransReader();
        reader.run(in);
    }

    enum FileType {
        Edifact,
        OpenTrans,
    }

    static FileType detectFileType(PushbackInputStream s) throws Exception {
        final int bufferSize = 8;
        final byte[] buffer = new byte[bufferSize];
        s.read(buffer);
        String firstBitOfFile = new String(buffer).trim();
        s.unread(buffer);

        if (firstBitOfFile.startsWith("\uFEFF")) { // BOM
            firstBitOfFile = firstBitOfFile.substring(1);
        }

        if (firstBitOfFile.startsWith("<")) {
            return FileType.OpenTrans;
        } else if (firstBitOfFile.startsWith("U")) {
            return FileType.Edifact;
        }
        throw new Exception("Unrecognised file type");
    }
}
