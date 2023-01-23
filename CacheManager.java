import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class CacheManager {
    private static final boolean DEBUG = false;
    public static int idxe = 0;
    public final int replacementPolicy;
    List<Instruction> traceFileCmds;
    // A Binary string with key has list of integers specificing its occurence in
    // the trace_file
    HashMap<String, LinkedList<Integer>> futureAccessTimeMap;

    CacheDS l1Cache;
    CacheDS l2Cache;

    CacheManager(int blockSize, int l1Size, int l1Assoc, int l2Size, int l2Assoc, int replacementPolicy,
            int inclusionPolicy, String tracefile) {
        printSimulationConfiguration(blockSize, l1Size, l1Assoc, l2Size, l2Assoc, replacementPolicy, inclusionPolicy,
                tracefile);
        this.replacementPolicy = replacementPolicy;

        l1Cache = new CacheDS("L1", blockSize, l1Size, l1Assoc, replacementPolicy, inclusionPolicy);
        l2Cache = new CacheDS("L2", blockSize, l2Size, l2Assoc, replacementPolicy, inclusionPolicy);

        traceFileCmds = processFile(tracefile);

        futureAccessTimeMap = new HashMap<>();
        if (replacementPolicy == 2)
            createHashMapForOptimalAlgorithm(traceFileCmds); // 2 - Optimal Replacement Algorithm
        handleTraceFileCmds();

        if (l1Cache.isValidCache())
            l1Cache.printCacheContents();

        if (l2Cache.isValidCache())
            l2Cache.printCacheContents();

        printSimulationResults();
    }

    private void createHashMapForOptimalAlgorithm(List<Instruction> traceFileCmds2) {
        int indexCounter = 1;
        for (Instruction istr : traceFileCmds2) {
            String hex = istr.getvalidHexString();
            String hex0 = hex.substring(0, 7) + "0";
            if (!futureAccessTimeMap.containsKey(hex0))
                futureAccessTimeMap.put(hex0, new LinkedList<Integer>());
            else
                futureAccessTimeMap.get(hex0).addLast(indexCounter);
            indexCounter++;
        }
    }

    
    int cnt = 1;
    private void handleTraceFileCmds() {
        int x = 1;
        for (Instruction cmd : traceFileCmds) {
            log("----------------------------------------");
            if(x == 95){
                if (cmd.isReadInstruction) {
                    readCacheL1(cmd);
                } else {
                    writeCacheL1(cmd);
                }
            }
            else{    
            if (cmd.isReadInstruction) {
                readCacheL1(cmd);
            } else {
                writeCacheL1(cmd);
            }
        }
            cnt++;
            x++;
        }
    }

    private static void log(String string) {
        if (DEBUG)
            System.out.println(string);
    }

    private void readCacheL1(Instruction cmd) {
        l1Cache.incrementReads();
        String s = cmd.getvalidHexString().substring(0, 7) + "0";
        int index = l1Cache.getIndex(cmd.getBinary());
        idxe = index;
        String tag = l1Cache.getTag(cmd.getBinary());
        log("# " + cnt + " : read " + cmd.getvalidHexString());
        log("L1 read : " + s + " (tag " + tag + ", index " + index + ")");
        if (l1Cache.isCacheHit(index, tag)) {
            l1Cache.incrementReadHits();
            log("L1 hit");
            handleCacheHit(cmd, l1Cache, index, tag, "r");
        } else {
            log("L1 miss");
            l1Cache.incrementReadMisses();
            if (l1Cache.getRowSize(index) == l1Cache.getCols()) {
                // Set Full -> Time to evict
                CBlock victim = l1Cache.getVictim(index, tag);
                if (victim.isDirtyBit()) {
                    log("L1 victim: " + victim.getHexString() + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", dirty)");
                    l1Cache.incrementWriteBacks();
                    if (l2Cache.isValidCache()) {
                        Instruction victimInstruction = new Instruction("w " + victim.getHexString(), -1);
                        writeCacheL2(victimInstruction);
                    }
                } else {
                    log("L1 victim: " + victim.getHexString() + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", clean)");
                }
                // Works for LRU Only
                l1Cache.updateReplacementIndex(victim);
                // Update Values
                // When updating for LRU, we change >current and set current to rowsize - 1
                // When updating FIFO, we use current index of the command
                // When updating optimal, we replace it with reuse distance
                l1Cache.replaceBlock(victim, index, tag, cmd.getvalidHexString(), false, l1Cache.getRowSize(index) - 1,
                        cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex()));
            } else {
                log("L1 victim: none");
                l1Cache.addBlock(new CBlock(index, tag, cmd.getvalidHexString(), false, l1Cache.getRowSize(index),
                        cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex())));
            }
            if (l2Cache.isValidCache()) {
                readCacheL2(cmd);
            }
        }
        if(replacementPolicy == 0)
            log("L1 update LRU");
        else
            log("L1 update optimal");
    }

    private void writeCacheL1(Instruction cmd) {
        l1Cache.incrementWrites();
        String s = cmd.getvalidHexString().substring(0, 7) + "0";
        int index = l1Cache.getIndex(cmd.getBinary());
        idxe = index;
        String tag = l1Cache.getTag(cmd.getBinary());
        log("# " + cnt + " : write " + cmd.getvalidHexString());
        log("L1 write : " + s + " (tag " + tag + ", index " + index + ")");
        if (l1Cache.isCacheHit(index, tag)) {
            log("L1 hit");
            l1Cache.incrementWriteHits();
            // Updates LRU, FIFO, OPT Counters
            handleCacheHit(cmd, l1Cache, index, tag, "w");
        } else {
            log("L1 miss");
            l1Cache.incrementWriteMisses();
            if (l1Cache.getRowSize(index) == l1Cache.getCols()) {
                // Set Full -> Time to evict
                // Gets Victim according to policy
                CBlock victim = l1Cache.getVictim(index, tag);
                if (victim.isDirtyBit()) {
                    log("L1 victim: " + victim.getHexString() + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", dirty)");
                    l1Cache.incrementWriteBacks();
                    if (l2Cache.isValidCache()) {
                        Instruction victimInstruction = new Instruction("w " + victim.getHexString(), -1);
                        writeCacheL2(victimInstruction);
                    }
                } else {
                    log("L1 victim: " + victim.getHexString() + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", clean)");
                }
                // Replaces the victim by first updating LRU of higher than victim
                l1Cache.updateReplacementIndex(victim);
                l1Cache.replaceBlock(victim, index, tag, cmd.getvalidHexString(), true, l1Cache.getRowSize(index) - 1,
                        cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex()));
            } else {
                log("L1 victim: none");
                l1Cache.addBlock(new CBlock(index, tag, cmd.getvalidHexString(), true, l1Cache.getRowSize(index),
                        cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex())));
            }
            if (l2Cache.isValidCache()) {
                readCacheL2(cmd);
            }
        }
        if(replacementPolicy == 0)
            log("L1 update LRU");
        else
            log("L1 update optimal");
        l1Cache.setDirtyBit(index, tag);
        log("L1 set dirty");
    }

    private void readCacheL2(Instruction cmd) {
        l2Cache.incrementReads();
        String s = cmd.getvalidHexString().substring(0, 7) + "0";
        int index = l2Cache.getIndex(cmd.getBinary());
        String tag = l2Cache.getTag(cmd.getBinary());
        log("L2 read : " + s + " (tag " + tag + ", index " + index + ")");
        if (l2Cache.isCacheHit(index, tag)) {
            log("L2 hit");
            l2Cache.incrementReadHits();
            log("L2 update LRU");
            handleCacheHit(cmd, l2Cache, index, tag, "r");
        } else {
            log("L2 miss");
            l2Cache.incrementReadMisses();
            if (l2Cache.getRowSize(index) == l2Cache.getCols()) {
                // Set Full -> Time to evict
                // Get victim according to replacement policy
                CBlock victim = l2Cache.getVictim(index, tag);
                if (l2Cache.inclusionPolicy == 1) {
                    // Inclusive
                    Instruction victimInstruction = new Instruction("r " + victim.getHexString(), -1);
                    l1Cache.removeBlockToMaintainInclusiveProperty(victimInstruction);
                    // log("L2 Inclusive Evict To L1");
                }
                if (victim.isDirtyBit()) {
                    log("L2 victim: " + s + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", dirty)");
                    l2Cache.incrementWriteBacks();
                } else {
                    log("L2 victim: " + s + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", clean)");
                }
                l2Cache.updateReplacementIndex(victim);
                l2Cache.replaceBlock(victim, index, tag, cmd.getvalidHexString(), false, l2Cache.getRowSize(index) - 1,
                        cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex()));
            } else {
                log("L2 victim: none");
                l2Cache.addBlock(new CBlock(index, tag, cmd.getvalidHexString(), false, l2Cache.getRowSize(index),
                        cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex())));
            }
            log("L2 update LRU");
        }
    }

    private void writeCacheL2(Instruction cmd) {
        l2Cache.incrementWrites();
        String s = cmd.getvalidHexString().substring(0, 7) + "0";
        int index = l2Cache.getIndex(cmd.getBinary());
        String tag = l2Cache.getTag(cmd.getBinary());
        log("L2 write : " + s + " (tag " + tag + ", index " + index + ")");
        if (l2Cache.isCacheHit(index, tag)) {
            log("L2 hit");
            l2Cache.incrementWriteHits();
            handleCacheHit(cmd, l2Cache, index, tag, "w");
        } else {
            log("L2 miss");
            l2Cache.incrementWriteMisses();
            if (l2Cache.getRowSize(index) == l2Cache.getCols()) {
                CBlock victim = l2Cache.getVictim(index, tag);
                if (l2Cache.inclusionPolicy == 1) {
                    Instruction victimInstruction = new Instruction("r " + victim.getHexString(), -1);
                    l1Cache.removeBlockToMaintainInclusiveProperty(victimInstruction);
                    // log("L2 Inclusive Evict To L1");
                }
                if (victim.isDirtyBit()) {
                    log("L2 victim: " + s + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", dirty)");
                    l2Cache.incrementWriteBacks();
                } else {
                    log("L2 victim: " + s + " (tag " + victim.getTag() + ", index " + victim.getIndex() + ", clean)");
                }
                l2Cache.updateReplacementIndex(victim);
                l2Cache.replaceBlock(victim, index, tag, cmd.getvalidHexString(), true, l2Cache.getRowSize(index) - 1,
                        cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex()));
            } else {
                log("L2 victim: none");
            
            l2Cache.addBlock(new CBlock(index, tag, cmd.getvalidHexString(), true, l2Cache.getRowSize(index),
                    cmd.getIndex(), getReuseDistance(cmd.getvalidHexString(), cmd.getIndex())));
            }
                    // l2Cache.addBlock(new CBlock(index, tag,cmd.getvalidHexString(), true, -1,
            // -1));
        }
        log("L2 update LRU");
        l2Cache.setDirtyBit(index, tag);
        log("L2 set dirty");
    }

    private void handleCacheHit(Instruction cmd, CacheDS Cache, int index, String tag, String hitType) {
        LinkedList<CBlock> list = Cache.cache.get(index);
        if (replacementPolicy == 0) {
            // LRU
            int lruValue = 0;
            for (CBlock block : list) {
                if (block.getTag().equals(tag)) {
                    lruValue = block.getLRUIndex();
                    break;
                }
            }
            for (CBlock block : list) {
                if (block.getTag().equals(tag)) {
                    block.setLRUIndex(list.size() - 1);
                } else if (block.getLRUIndex() > lruValue)
                    block.setLRUIndex(block.getLRUIndex() - 1);
            }
        } else if (replacementPolicy == 2) {
            // Optimal
            // Replace Hit block with its future value;
            int reuseDistance = getReuseDistance(cmd.getvalidHexString(), cmd.getIndex());
            for (CBlock block : list) {
                if (block.getTag().equals(tag)) {
                    block.setOptIndex(reuseDistance);
                }
            }
        }

    }

    private int getReuseDistance(String hex, int index) {
        if (replacementPolicy != 2)
            return -1;
        String hex0 = hex.substring(0, 7) + "0";
        LinkedList<Integer> mapValue = futureAccessTimeMap.get(hex0);
        
        if (mapValue.size() == 0) {
            // log(index + " " + "Next occurence of " + hexString + " is " + Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        } else {
            int v = mapValue.pollFirst();
            // log(index + " " + "Next occurence of " + hexString + " is " + v);
            return v;
        }
    }

    List<Instruction> processFile(String fileName) {
        String path = "./traces/" + fileName;
        File file = new File(path);
        List<Instruction> IST = new ArrayList<>();
        int i = 0;
        try {
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                Instruction ISTR = new Instruction(line, i);
                IST.add(ISTR);
                i++;
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return IST;
    }

    private void printSimulationResults() {
        System.out.println("===== Simulation results (raw) =====");
        System.out.println("a. number of L1 reads:        " + l1Cache.getReads());
        System.out.println("b. number of L1 read misses:  " + l1Cache.getReadMiss());
        System.out.println("c. number of L1 writes:       " + l1Cache.getWrites());
        System.out.println("d. number of L1 write misses: " + l1Cache.getWriteMiss());
        System.out.println("e. L1 miss rate:              " + String.format("%.6f", l1Cache.getMissRate("L1")));
        System.out.println("f. number of L1 writebacks:   " + l1Cache.getWriteBacks());
        System.out.println("g. number of L2 reads:        " + l2Cache.getReads());
        System.out.println("h. number of L2 read misses:  " + l2Cache.getReadMiss());
        System.out.println("i. number of L2 writes:       " + l2Cache.getWrites());
        System.out.println("j. number of L2 write misses: " + l2Cache.getWriteMiss());
        if (l2Cache.getReads() == 0)
            System.out.println("k. L2 miss rate:              " + "0");
        else
            System.out.println("k. L2 miss rate:              " + String.format("%.6f", l2Cache.getMissRate("L2")));
        System.out.println("l. number of L2 writebacks:   " + l2Cache.getWriteBacks());
        int memoryTraffic = 0;
        if (l2Cache.isValidCache()) {
            // (with L2, should match h+j+l for non-inclusive cache: all L2 read misses + L2
            // write misses + writebacks from L2
            memoryTraffic = l2Cache.getReadMiss() + l2Cache.getWriteMiss() + l2Cache.getWriteBacks();
            if (l1Cache.getInclusionPolicy() == 1) {
                // for inclusive cache, writebacks directly from L1 to memory due to
                // invalidation should
                // also be counted)
                memoryTraffic += l1Cache.getInvalidationWriteBacks();
            }
        } else {
            // (without L2, should match b+d+f: L1 read misses + L1 write misses +
            // writebacks from L1)
            memoryTraffic = l1Cache.getReadMiss() + l1Cache.getWriteMiss() + l1Cache.getWriteBacks();
        }

        System.out.println("m. total memory traffic:      " + memoryTraffic);
    }

    private void printSimulationConfiguration(int blockSize, int l1Size, int l1Assoc, int l2Size, int l2Assoc,
            int replacementPolicy, int inclusionPolicy, String tracefile) {
        System.out.println("===== Simulator configuration =====");
        System.out.println("BLOCKSIZE:             " + blockSize);
        System.out.println("L1_SIZE:               " + l1Size);
        System.out.println("L1_ASSOC:              " + l1Assoc);
        System.out.println("L2_SIZE:               " + l2Size);
        System.out.println("L2_ASSOC:              " + l2Assoc);
        String replacementPolicyString;
        if (replacementPolicy == 0)
            replacementPolicyString = "LRU";
        else if (replacementPolicy == 1)
            replacementPolicyString = "FIFO";
        else
            replacementPolicyString = "optimal";
        System.out.println("REPLACEMENT POLICY:    " + replacementPolicyString);
        String inclusionPolicyString;
        if (inclusionPolicy == 0)
            inclusionPolicyString = "non-inclusive";
        else
            inclusionPolicyString = "inclusive";
        System.out.println("INCLUSION PROPERTY:    " + inclusionPolicyString);
        System.out.println("trace_file:            " + tracefile);
    }

}
