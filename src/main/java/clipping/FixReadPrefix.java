package clipping;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.stream.StreamSupport;
import java.io.PrintStream;

import org.biojava.nbio.sequencing.io.fastq.Fastq;
import org.biojava.nbio.sequencing.io.fastq.FastqReader;
import org.biojava.nbio.sequencing.io.fastq.FastqWriter;
import org.biojava.nbio.sequencing.io.fastq.SangerFastqReader;
import org.biojava.nbio.sequencing.io.fastq.SangerFastqWriter;
import org.biojava.nbio.sequencing.io.fastq.StreamListener;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FixReadPrefix {
    public static void main(String[] args) {

        FastqReader freader = new SangerFastqReader();

        if ( args.length == 0 ) {
            System.err.println("Please supply an input fastq");
            System.exit(1);
        } else if ( args.length == 1 ) {
            if ( args[0].equals("-") ) {
                try {
                    InputStream in = System.in;
                    OutputStream out = System.out;
                    fixPrefixes(freader, in, out);
                } catch (IOException ioe) {
                    System.err.println("Failed process uncompressed fastq from stdin: "+ioe.getMessage());
                }
            } else {
                try {
                    InputStream in = new GZIPInputStream(new FileInputStream(args[0]));
                    OutputStream out = System.out;
                    fixPrefixes(freader, in, out);
                } catch (IOException ioe) {
                    System.err.println("Failed to process fastq from compressed input file: "+ioe.getMessage());
                    System.exit(1);
                }
            }
        } else if ( args.length == 2 ) {
            if ( args[0].equals("-") ) {
                try {
                    InputStream in = System.in;
                    OutputStream out = new GZIPOutputStream(new FileOutputStream(args[1]));
                    fixPrefixes(freader, in, out);
                } catch (IOException ioe) {
                    System.err.println("Failed process uncompressed fastq from stdin: "+ioe.getMessage());
                }
            } else {
                try {
                    InputStream in = new GZIPInputStream(new FileInputStream(args[0]));
                    OutputStream out = new GZIPOutputStream(new FileOutputStream(args[1]));
                    fixPrefixes(freader, in, out);
                } catch (IOException ioe) {
                    System.err.println("Failed to process fastq from compressed input file: "+ioe.getMessage());
                    System.exit(1);
                }
            }
        } else {
            System.err.println("Too many arguments");
            System.exit(1);
        }
    }

    private static String fixPrefix(String description) {
        if ( description.startsWith("M_") ) {
            return description;
        } else if ( description.startsWith("MT_") ) {
            return description.replaceFirst("^MT","M");
        } else if ( description.matches("^[^ ]+ 1:.*") ) {
            return "F_"+description;
        } else if ( description.matches("^[^ ]+ 2:.*") ) {
            return "R_"+description;
        }

        return description;
    }

    private static void fixPrefixes(FastqReader freader, InputStream in, OutputStream out) throws IOException {
        PrintStream pout = new PrintStream(out);
        
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        freader.stream(br, new StreamListener() {
            @Override
            public void fastq (final Fastq f) {
                pout.print("@");
                pout.println(fixPrefix(f.getDescription()));
                pout.println(f.getSequence());
                pout.println("+");
                pout.println(f.getQuality());
            }
        });
        if ( pout.checkError() ) {
            throw new RuntimeException("Failed to write to output");
	}
        pout.close();
    }
}


