/*
 * Copyright 2017-2018 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Copyright 2017-2018 Goethe Center for Scientific Computing, University Frankfurt. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 */
package eu.mihosoft.vmftext.tests.miniclang;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Simple Cache Simulator with LRU replacement strategy inspired by
 * <a href="http://csapp.cs.cmu.edu/2e/home.html">Cache Lab</a>.
 *
 * @author Michael <info@michaelhoffer.de>
 */
public class CacheSim {

    private static class CacheParameters {
        private long s;                      // size of cache sets (bits)
        private long b;                      // block size of cache lines (bits)
        private int E;                       // number of cache lines per cache set
        private int S;                       // number of sets per cache, derived from S = 2^s
        // private int B;                       // cache line block size (bytes), derived from B = 2^b

        public CacheParameters(long s, int e, long b) {
            if(s < 1) {
                throw new IllegalArgumentException("Number of set bits cannot be < 1");
            }
            if(e < 1) {
                throw new IllegalArgumentException("Number of lines per set cannot be < 1");
            }
            if(b < 1) {
                throw new IllegalArgumentException("Number of block bits cannot be < 1");
            }

            this.s = s;
            E = e;
            this.b = b;

            // Compute S and B, 2^s and 2^b respectively
            this.S = (1 << this.s);
            // this.B = (1 << this.b);
        }
    }

    // structure for a line
    private static class CacheLine {
        private boolean valid;
        private long tag;
        private long timestamp;
    }

    // structure for a set; a pointer to an array of lines
    private static class CacheSet {
        private CacheLine[] lines;

        public CacheSet(int numLines) {
            this.lines = new CacheLine[numLines];

            for(int j = 0; j < lines.length;j++) {
                lines[j] = new CacheLine();
            }
        }
    }

    // structure for a cache; a pointer to an array of sets
    private static class Cache {
        private CacheSet[] sets;

        public Cache(int numSets, int numLinesPerSet) {
            this.sets = new CacheSet[numSets];

            for (int i = 0; i < numSets; i++) {
                this.sets[i] = new CacheSet(numLinesPerSet);
            }
        }
    }

    private boolean verbose;

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public CacheSim() {
        reset();
    }

    public enum Type {
        HIT,
        MISS,
        EVICTION
    }

    public static class Entry {
        private Type type;
        private String label;

        private int from;
        private int to;

        private int id;

        public Entry(Type type, String label) {
            this.type = type;
            this.label = label;

            parseId(label);
            parseLocation(label);

        }

        private void parseId(String label) {
            Pattern locationP = Pattern.compile("id=\\(.*?\\)");
            Matcher m = locationP.matcher(label);
            if(m.find()) {
                String idS = m.group();

                if(idS.length()<6) {
                    throw new RuntimeException("cannot parse entry label: required format is 'location=(from,to)'");
                }

                int first = Integer.parseInt(idS.substring(4, idS.length()-1).trim());
                id = first;

            } else {
                throw new RuntimeException("cannot parse entry label: required format is 'location=(from,to)'");
            }
        }
        private void parseLocation(String label) {
            Pattern locationP = Pattern.compile("location=\\(.*?\\)");
            Matcher m = locationP.matcher(label);
            if(m.find()) {
                String locationS = m.group();
                locationS = locationS.substring(10, locationS.length()-1);
                String[] coords = locationS.split(",");

                if(coords.length!=2) {
                    throw new RuntimeException("cannot parse entry label: required format is 'location=(from,to)'");
                }

                int first   = Integer.parseInt(coords[0].substring(0).trim());
                int seconds = Integer.parseInt(coords[1].trim());

                from = first;
                to = seconds;
            } else {
                throw new RuntimeException("cannot parse entry label: required format is 'location=(from,to)'");
            }
        }

        public String getLabel() {
            return label;
        }

        public Type getType() {
            return type;
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }

        public int getId() {
            return id;
        }
    }

    public static final class Hit extends Entry {
        public Hit(String label) {
            super(Type.HIT, label);
        }
    }

    public static final class Miss extends Entry {
        public Miss(String label) {
            super(Type.MISS, label);
        }
    }

    public static final class Eviction extends Entry {
        public Eviction(String label) {
            super(Type.EVICTION, label);
        }
    }

    /**
     * Simulation result containing hits, misses and evictions.
     */
    public static final class SimulationResult {
        private List<Hit> hits = new ArrayList<>();
        private List<Hit> unmodifiableHits = Collections.unmodifiableList(hits);
        private List<Miss> misses = new ArrayList<>();
        private List<Miss> unmodifiableMisses = Collections.unmodifiableList(misses);
        private List<Eviction> evictions = new ArrayList<>();
        private List<Eviction> unmodifiableEvictions = Collections.unmodifiableList(evictions);
        private List<Entry> trace = new ArrayList<>();
        private List<Entry> unmodifiableTrace = Collections.unmodifiableList(trace);

        private void addHit(Hit hit) {
            hits.add(hit);
            trace.add(hit);
        }

        private void addMiss(Miss miss) {
            misses.add(miss);
            trace.add(miss);
        }

        private void addEviction(Eviction eviction) {
            evictions.add(eviction);
            trace.add(eviction);
        }

        public List<Hit> getHits() {
            return unmodifiableHits;
        }

        public List<Miss> getMisses() {
            return unmodifiableMisses;
        }

        public List<Eviction> getEvictions() {
            return unmodifiableEvictions;
        }

        public List<Entry> getTrace() {
            return unmodifiableTrace;
        }

        @Override
        public String toString() {
            return "#hits: " + hits.size() + "  #misses: " + misses.size() + "  #evictions: " + evictions.size();
        }
    }

    private void reset() {
        timestamp = 0;
    }

    public static void main(String[] args) throws IOException {

        CacheSim sim = new CacheSim();

        SimulationResult result;

//        result = sim.execute(5,4,4,new File("traces/sort4k.trace"));
//        System.out.println(result);
//        result = sim.execute(5,8,6,new File("traces/sort.trace"));
//        System.out.println(result);


        int s = 5;
        int e = 1;
        int b = 5;

        result = sim.execute(s, e, b, new File("code-sample-64x64-no-blocks.trace"));
        System.out.println(result);
        result = sim.execute(s, e, b, new File("code-sample-64x64-blocks.trace"));
        System.out.println(result);
        result = sim.execute(s, e, b, new File("code-sample-64x64-blocks2.trace"));
        System.out.println(result);

        result = sim.execute(s, e, b, new File("code-sample-64x64-no-blocks-array.trace"));
        System.out.println(result);
        result = sim.execute(s, e, b, new File("code-sample-64x64-blocks-array.trace"));
        System.out.println(result);
        result = sim.execute(s, e, b, new File("code-sample-64x64-blocks2-array.trace"));
        System.out.println(result);

        result = sim.execute(s, e, b, new File("code-sample.trace"));
        System.out.println(result);
        result = sim.execute(s, e, b, new File("code-sample-tcc.trace"));
        System.out.println(result);

    }

    /**
     * Performs a simple cache simulation with the specified trace file using LRU replacement strategy.
     *
     * The file format (ebnf):
     * <pre>
     * ' '? operation=['I'|'L'|'S'|'M'] ' ' address=UNSIGNED_INT_64Bit ',' size=INT (',' label=STRING)?
     *
     * Example:
     *
     * I 0400d7d4,8,"[line:123, col:4],[line:123, col:12]"
     *  M 0421c7f0,4
     *  L 04f6b86,8
     *  S 7ff0005c8,8
     * </pre>
     *
     * Spaces in front of operations and labels are optional.
     *
     * @param s size of cache sets in bits, number of cache sets = 2^s
     * @param e number of cache lines per set
     * @param b block size in bits, block size in bytes = 2^b
     * @param traceFile trace file to simulate
     * @return simulation result containing hits, misses and evictions
     * @throws IOException if trace file can't be read
     */
    public SimulationResult execute(int s, int e, int b, File traceFile) throws IOException {
        SimulationResult simulationResult = new SimulationResult();

        CacheParameters par = new CacheParameters(s,e,b);

        Cache cache = new Cache(par.S, par.E);

        if (traceFile.exists()) {

            List<String> lines = Files.readAllLines(traceFile.toPath());

            simulateLines(simulationResult, par, cache, lines);
        }

        return simulationResult;
    }

    /**
     * Performs a simple cache simulation with the specified trace file using LRU replacement strategy.
     *
     * The file format (ebnf):
     * <pre>
     *    ('=='|'--') .*? '\n' // comment
     * ´|
     *    ' '? operation=['I'|'L'|'S'|'M'] ' ' address=UNSIGNED_INT_64Bit ',' size=INT (',' label=STRING)?
     *
     * Example:
     *
     * == a comment
     * -- another comment
     * I 0400d7d4,8,"[line:123, col:4],[line:123, col:12]"
     *  M 0421c7f0,4
     *  L 04f6b86,8
     *  S 7ff0005c8,8
     * </pre>
     *
     * Spaces in front of operations and labels are optional.
     *
     * @param s size of cache sets in bits, number of cache sets = 2^s
     * @param e number of cache lines per set
     * @param b block size in bits, block size in bytes = 2^b
     * @param memoryTrace trace to simulate
     * @return simulation result containing hits, misses and evictions
     */
    public SimulationResult execute(int s, int e, int b, String memoryTrace)  {
        SimulationResult simulationResult = new SimulationResult();

        CacheParameters par = new CacheParameters(s,e,b);

        Cache cache = new Cache(par.S, par.E);

        List<String> lines = Arrays.asList(memoryTrace.split("\\R"));

        simulateLines(simulationResult, par, cache, lines);

        return simulationResult;
    }

    private void simulateLines(SimulationResult simulationResult, CacheParameters par, Cache cache, List<String> lines) {
        for (String line : lines) {

            line = line.trim();

            // skip empty lines and comments
            if (line.isEmpty() || line.startsWith("==") || line.startsWith("--")) continue;

            String[] entries = line.split("\\s");

            char command = entries[0].trim().charAt(0);

            String[] addressEntries = entries[1].split(",");
            String addressString = addressEntries[0];

            int size = Integer.parseInt(addressEntries[1]);
            Long address = Long.parseUnsignedLong(addressString, 16);

            String label;

            if(addressEntries.length>2) {

                Matcher m = Pattern.compile("\"!.*!\"").matcher(line);

                if(m.find()) {
                    label = line.substring(m.start()+2,m.end()-2);
                } else {
                    label= "no label";
                }

            } else {
                label= "no label";
            }

            // for this simple simulation we do not distinguish between S & L
            switch (command) {
                //just ignore I
                case 'I':
                    break; // there's no instruction cache
                case 'L':
                    simulate(par, cache, simulationResult, command, address, size, label);
                    break;
                case 'S':
                    simulate(par, cache, simulationResult, command, address, size, label);
                    break;
                case 'M':
                    simulate(par, cache, simulationResult, command, address, size, label);
                    break;
                default:
                    break;
            }
        }
    }

    private int timestamp;                     // timestamp value for LRU

    private void simulate(CacheParameters par,
                          Cache cache,
                          SimulationResult simulationResult,
                          char act, long addr, int size, String label) {

        int empty = - 1;                       // index of empty space
        boolean H = false;                     // is there a hit

        int toEvict = 0;  // keeps track of what to evict

        // calculate address tag and set index
        int addr_tag = (int)(addr >>> (par.s + par.b));
        int tag_size = (int)(64 - (par.s + par.b));

        // the sign bit might actually be needed to hold the unsigned value
        // this is why we use a byte buffer to feed the long bits to a big integer
        // we perform the operation on the big integer until we can safely downcast again
        //
        // this was the equivalent c code using unsigned types
        //
        //            mem_addr_t addr_tag = addr >> ( par.s + par.b );
        //            int tag_size = ( 64 - ( par.s + par.b ) );
        //            unsigned long long temp = addr << ( tag_size );
        //            unsigned long long setid = temp >> ( tag_size + par.b );
        //            CacheSet set = cache.sets[setid];
        //
        // while java has an unsigned right shift operator there's no unsigned left-shift operator. while the operator
        // does not change between signed/unsigned we would need a way to tell long that the sign bit needs to be
        // interpreted as regular bit. with big integers we can do that by setting signum bit to 1 in the constructor.
        //
        long temp = addr << tag_size;
        byte[] tempBytes = new byte[8];
        ByteBuffer b = ByteBuffer.wrap(tempBytes);
        b.putLong(temp);
        BigInteger tempI = new BigInteger(1, tempBytes);
        int setid = tempI.shiftRight((int)(tag_size + par.b)).intValue();

        if(isVerbose()) {
            System.out.println("-> looking in set: " + setid);
        }

        CacheSet set = cache.sets[setid];

        long low = Long.MAX_VALUE;

        for (int e = 0; e < par.E; e++) {
            if (set.lines[e].valid) {
                // look for hit before eviction candidates
                if (set.lines[e].tag == addr_tag) {
                    simulationResult.addHit(new Hit(label));
                    H = true;
                    set.lines[e].timestamp = timestamp;
                    timestamp++;
                    if(isVerbose()) {
                        //System.out.println("    -> hit in line " + e + ", label: " + label);
                    }
                }
                //look for oldest line for eviction.
                else if (set.lines[e].timestamp < low) {
                    low = set.lines[e].timestamp;
                    toEvict = e;
                }
            }
            // if we haven't yet found an empty, mark one that we found.
            else if (empty == -1) {
                empty = e;
            }
        }

        //if we have a miss
        if (!H) {
            if(isVerbose()) {
                System.out.println("    -> miss, label: " + label);
            }
            simulationResult.addMiss(new Miss(label));
            // if we have an empty line
            if (empty > -1) {
                if(isVerbose()) {
                    System.out.println("    -> init empty line " + empty + ", label: " + label);
                }
                set.lines[empty].valid = true;
                set.lines[empty].tag = addr_tag;
                set.lines[empty].timestamp = timestamp;
            }
            // if the set is full we need to evict
            else {
                if(isVerbose()) {
                    System.out.println("    -> evicting line " + toEvict + ", label: " + label);
                }
                set.lines[toEvict].tag = addr_tag; // replace old tag with new tag
                set.lines[toEvict].timestamp = timestamp;
                simulationResult.addEviction(new Eviction(label));
            }
            timestamp++;
        }
        // if the instruction is M, we will always get a hit
        if (act == 'M') {
            simulationResult.addHit(new Hit(label));
        }
        /*
        // if the -v flag is set print out all debug information
        if (isVerbose()) {
            System.out.printf("%c ", act);
            System.out.printf("%s,%d,%s", Long.toString(addr,16), size, label);
            if (H) {
                System.out.printf(" Hit ");
            } else {
                System.out.printf(" Miss ");
            }
            if (E) {
                System.out.printf(" Eviction ");
            }
            System.out.printf("\n");
        }
        */
    }
}
