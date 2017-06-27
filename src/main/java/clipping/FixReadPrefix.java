package clipping;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.stream.StreamSupport;

import org.biojava.nbio.sequencing.io.fastq.Fastq;
import org.biojava.nbio.sequencing.io.fastq.FastqReader;
import org.biojava.nbio.sequencing.io.fastq.FastqWriter;
import org.biojava.nbio.sequencing.io.fastq.SangerFastqReader;
import org.biojava.nbio.sequencing.io.fastq.SangerFastqWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FixReadPrefix {
    public static void main(String[] args) {

        FastqReader freader = new SangerFastqReader();
        FastqWriter fwriter = new SangerFastqWriter();

        if ( args.length == 0 ) {
            System.err.println("Please supply an input fastq");
            System.exit(1);
        } else if ( args.length == 1 ) {
            if ( args[0].equals("-") ) {
                try {
                    InputStream in = System.in;
                    OutputStream out = System.out;
                    fixPrefixes(freader, fwriter, in, out);
                } catch (IOException ioe) {
                    System.err.println("Failed process uncompressed fastq from stdin: "+ioe.getMessage());
                }
            } else {
                try {
                    InputStream in = new GZIPInputStream(new FileInputStream(args[0]));
                    OutputStream out = System.out;
                    fixPrefixes(freader, fwriter, in, out);
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
                    fixPrefixes(freader, fwriter, in, out);
                } catch (IOException ioe) {
                    System.err.println("Failed process uncompressed fastq from stdin: "+ioe.getMessage());
                }
            } else {
                try {
                    InputStream in = new GZIPInputStream(new FileInputStream(args[0]));
                    OutputStream out = new GZIPOutputStream(new FileOutputStream(args[1]));
                    fixPrefixes(freader, fwriter, in, out);
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

    private static Fastq fixPrefix(Fastq f) {
        if ( f.getDescription().startsWith("M_") ) {
            return f;
        } else if ( f.getDescription().startsWith("MT_") ) {
            return f.builder()
                    .withDescription(f.getDescription().replaceFirst("^MT","M"))
                    .withSequence(f.getSequence())
                    .withQuality(f.getQuality())
                    .withVariant(f.getVariant()).build();
        } else if ( f.getDescription().matches("^[^ ]+ 1:.*") ) {
            return f.builder()
                    .withDescription("F_"+f.getDescription())
                    .withSequence(f.getSequence())
                    .withQuality(f.getQuality())
                    .withVariant(f.getVariant()).build();
        } else if ( f.getDescription().matches("^[^ ]+ 2:.*") ) {
            return f.builder()
                    .withDescription("R_"+f.getDescription())
                    .withSequence(f.getSequence())
                    .withQuality(f.getQuality())
                    .withVariant(f.getVariant()).build();
        }

        return f;
    }

    private static void fixPrefixes(FastqReader freader, FastqWriter fwriter, InputStream in, OutputStream out) throws IOException {
        Iterable<Fastq> iterable = freader.read(in);
        StreamSupport.stream(iterable.spliterator(),false).map(f -> fixPrefix(f) ).forEach( r -> { try { fwriter.write(out, r); } catch (IOException ioe) { throw new RuntimeException("Failed to write to output");} } );
        out.flush();
        out.close();
    }
}


