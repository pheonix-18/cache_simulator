import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

// Cache Data Structure
public class CacheDS {
    
    String cacheName;

    int blockSize, cacheSize, cacheAssoc, rows, cols;
    int indexBitsLength, tagBitsLength, blockOffsetBitsLength;
    int replacementPolicy, inclusionPolicy;

    int reads, readMiss, readHits;
    int writes, writeMiss, writeHits;
    int writeBacks, invalidationWriteBacks;
    // although present for both caches, invalidationWriteBacks used only for L1 Cache;


    List<LinkedList<CBlock>> cache = new ArrayList<>();
    
    public CacheDS(String cacheName, int blockSize, int cacheSize, int cacheAssoc, int replacementPolicy, int inclusionPolicy) {
        initializeCacheMembers();
        this.cacheName = cacheName;
        this.blockSize = blockSize;
        this.cacheSize = cacheSize;
        this.cacheAssoc = cacheAssoc;
        this.replacementPolicy = replacementPolicy;
        this.inclusionPolicy = inclusionPolicy;
        calculateRowsAndCols();
        calculateBitLengths();
        initializeCache();
    }

    public void initializeCacheMembers(){
        this.blockSize = 0;
        this.cacheSize = 0;
        this.cacheAssoc = 0;
        this.rows = 0;
        this.cols = 0;
        this.indexBitsLength = 0;
        this.tagBitsLength = 0;
        this.blockOffsetBitsLength = 0;
        this.replacementPolicy = 0;
        this.inclusionPolicy = 0;
        this.reads = 0;
        this.readMiss = 0;
        this.readHits = 0;
        this.writes = 0;
        this.writeMiss = 0;
        this.writeHits = 0;
        this.writeBacks = 0;
        this.invalidationWriteBacks = 0;
    }
    
    private void initializeCache() {
        for(int i = 0; i < rows; i++){
            cache.add(new LinkedList<CBlock>());
        }
    }

    private void calculateBitLengths() {
        indexBitsLength = (int)(Math.log(rows)/Math.log(2.0));
        blockOffsetBitsLength = (int)(Math.log(blockSize)/Math.log(2.0));
        tagBitsLength = 32 - indexBitsLength - blockOffsetBitsLength;
    }

    private void calculateRowsAndCols() {
        cacheAssoc = cacheAssoc != 0? cacheAssoc : 1;
        rows = (cacheSize)/(blockSize * cacheAssoc);
        cols = cacheAssoc;
    }

    public boolean isCacheHit(int index, String tag){
        LinkedList<CBlock> row = cache.get(index);
        for(CBlock block: row){
            if(block.getTag().equals(tag))
                return true;
        }
        return false;
    }

    public boolean updateLRU(int index, String tag){
        LinkedList<CBlock> row = cache.get(index);
        //System.out.println("Row Size" + row.size());
        if(replacementPolicy == 0){
            //LRU
            int lruValue = 0;
            for(CBlock block: row){
                if(block.getTag().equals(tag)){
                    lruValue = block.getLRUIndex();
                    break;
                }
            }
            for(CBlock block: row){
                if(block.getTag().equals(tag)){
                    block.setLRUIndex(row.size() - 1);
                }
                else if(block.getLRUIndex()>=lruValue)
                    block.setLRUIndex(block.getLRUIndex() - 1);
            }    
        }
        else if(replacementPolicy == 2){
            // Optimal Read or Write Hit
            // int reuseDistance = reuse
        }
        return true;
    }
    
    void evictBlock(CBlock toRemove){
        //System.out.println("Evict" + " " + toRemove.getTag());
        LinkedList<CBlock> row = cache.get(toRemove.getIndex());
        int lruValue = toRemove.getLRUIndex();
        row.removeFirstOccurrence(toRemove);
        if(replacementPolicy == 0){
            for(CBlock block: row){

                if(block.getLRUIndex() > lruValue)
                    block.setLRUIndex(block.getLRUIndex()-1);
            }
        }
    }

    
    void printRow(int index){
        LinkedList<CBlock> list = cache.get(index);
        for(CBlock c: list){
            System.out.println(c.getHexString() + " " + c.getIndex() + " " + c.getTag() + " " + c.getLRUIndex());
        }
    }


    void incrementReads(){
        reads++;
    }
    void incrementReadHits(){
        readHits++;
    }
    void incrementReadMisses(){
        readMiss++;
    }
    void incrementWrites(){
        writes++;    
    }
    void incrementWriteHits(){
        writeHits++;
    }
    void incrementWriteMisses(){
        writeMiss++;
    }
    void incrementWriteBacks(){
        writeBacks++;
    }
    void incrementInvalidationWriteBacks(){
        invalidationWriteBacks++;
    }
    double getMissRate(String cacheType){
        try{
            if(cacheType.equals("L1")){
                return (readMiss + writeMiss * 1.0)/(reads + writes);
            }
        else
            if(reads == 0)
                return 0;
            return (readMiss * 1.0)/reads;  
        }
        catch(Exception e){
            return 0;
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getIndexBitsLength() {
        return indexBitsLength;
    }

    public int getTagBitsLength() {
        return tagBitsLength;
    }

    public int getReplacementPolicy() {
        return replacementPolicy;
    }

    public int getInclusionPolicy() {
        return inclusionPolicy;
    }

    public int getReads() {
        return reads;
    }

    public int getReadMiss() {
        return readMiss;
    }

    public int getReadHits() {
        return readHits;
    }

    public int getWrites() {
        return writes;
    }

    public int getWriteMiss() {
        return writeMiss;
    }

    public int getWriteHits() {
        return writeHits;
    }

    public int getWriteBacks() {
        return writeBacks;
    }

    public int getInvalidationWriteBacks() {
        return invalidationWriteBacks;
    }

    public void printCacheContents(){
        if(!isValidCache())
            return;
        System.out.println("===== " + cacheName +  " contents =====");
        for(int i = 0; i < cache.size(); i++){
            System.out.print("Set \t " + i + ":\t ");    
            for(CBlock block: cache.get(i)){
                System.out.print(block.getTag() + " ");
                if(block.isDirtyBit())
                    System.out.print("D\t");
                else    
                    System.out.print("\t");
            }
            System.out.println();
        }
    }

    boolean isValidCache(){
        return cacheSize != 0;
    }

    public int getIndex(String binary) {
        // Index bits are from tag, tag + index
        // For Fully Associative Case
        if(indexBitsLength <= 0)
            return 0;
        String index = binary.substring(tagBitsLength, tagBitsLength + indexBitsLength);
        return Integer.parseInt(index, 2);
    }

    public String getTag(String binary) {
        String binaryTag = binary.substring(0, tagBitsLength);
        return Integer.toHexString(Integer.parseInt(binaryTag, 2));
    }

    public boolean isReadHit() {
        return false;
    }

    public int getRowSize(int index) {
        LinkedList<CBlock> row = cache.get(index);
        return row == null? 0: row.size();
    }
    
    public CBlock getVictim(int index, String tag) {
        LinkedList<CBlock> list = cache.get(index);
        if(replacementPolicy == 0){
            //LRU
            for(CBlock block: list){
                if(block.getLRUIndex() == 0)
                    return block;
            }
        }
        else if(replacementPolicy == 1){
            // FIFO
            int leastFIFO = Integer.MAX_VALUE;
            CBlock FIFO = null;
            for(CBlock block: list){
                if(block.getFIFOIndex() < leastFIFO){
                    leastFIFO = block.getFIFOIndex();
                    FIFO = block;
                }
            }
            return FIFO;
        }
        else{
            // Optimal Replacement
            int maxValue = Integer.MIN_VALUE;
            CBlock maxBlock = null;
            for(CBlock block: list){
                if(block.getOptIndex() > maxValue){
                    maxValue = block.getOptIndex();
                    maxBlock = block;
                }
            }
            return maxBlock;
        }
        return null;
    }

    public void addBlock(CBlock cBlock) {
        cache.get(cBlock.getIndex()).add(cBlock);
    }

    // public void addBlock(int position, CBlock cBlock){
    //     if(position == -1)
    //         addBlock(cBlock);
    //     else
    //         cache.get(cBlock.getIndex()).add(position, cBlock);
    // }

    public void removeBlockToMaintainInclusiveProperty(Instruction victimInstruction) {
        int index = getIndex(victimInstruction.getBinary());
        String tag = getTag(victimInstruction.getBinary());
        // Get victim block, which is equal to this tag
        CBlock victimBlock = getVictimBlock(index, tag);
        if(victimBlock == null){
            return;
        }
        int vBlockLRUIndex = victimBlock.getLRUIndex();
        if(victimBlock.isDirtyBit()){
            this.incrementInvalidationWriteBacks();
        }
        LinkedList<CBlock> list = cache.get(index);
        for(CBlock block: list){
            if(block.getLRUIndex() > vBlockLRUIndex){
                block.setLRUIndex(block.getLRUIndex() - 1);
            }
        }
        this.evictBlockToMaintainInclusiveProperty(victimBlock);
    }

    private CBlock getVictimBlock(int index, String tag) {
        LinkedList<CBlock> list = cache.get(index);
        for(CBlock block: list)
            if(block.getTag().equals(tag))
                return block;
        return null;
    }

    private void evictBlockToMaintainInclusiveProperty(CBlock victimBlock) {
        LinkedList<CBlock> row = cache.get(victimBlock.getIndex());
        row.removeFirstOccurrence(victimBlock);
    }

    public void setDirtyBit(int index, String tag) {
        LinkedList<CBlock> list = cache.get(index);
        for(CBlock block: list){
            if(block.getTag().equals(tag)){
                block.setDirtyBit(true);
                return;
            }
        }
    }

    public void updateOptimal(int index, String tag, int reuseDistance) {
        LinkedList<CBlock> list = cache.get(index);
        for(CBlock block: list){
            if(block.getTag().equals(tag)){
                block.setOptIndex(reuseDistance);
                break;
            }
        }
    }

    public void replaceBlock(CBlock victim, int index, String tag, String getvalidHexString, boolean isDirtyBit, int lruIndex, int fifoIndex,
            int optIndex) 
    {
        victim.setIndex(index);
        victim.setTag(tag);
        victim.setHexString(getvalidHexString);
        victim.setDirtyBit(isDirtyBit);
        victim.setLRUIndex(lruIndex);
        victim.setFIFOIndex(fifoIndex);
        victim.setOptIndex(optIndex);
    }

    public void updateReplacementIndex(CBlock victim) {
        LinkedList<CBlock> list = cache.get(victim.getIndex());
        if(replacementPolicy == 0){
            // LRU
            int victimLRUIndex = 0;
            for(CBlock block: list){
                if(block.getTag().equals(victim.getTag())){
                    victimLRUIndex = block.getLRUIndex();
                    break;
                }
            }
            for(CBlock block: list){
                if(block.getTag().equals(victim.getTag())){
                    block.setLRUIndex(list.size() - 1);
                }
                else if(block.getLRUIndex() > victimLRUIndex)
                    block.setLRUIndex(block.getLRUIndex() - 1);
            }
        }
    }

    public void setFIFOIndex(int index, String tag) {
        LinkedList<CBlock> list = cache.get(index);
        if(replacementPolicy == 1){
            for(CBlock block: list){
                if(block.getTag().equals(tag)){
                    block.setFIFOIndex(list.size() - 1);
                    break;
                }
            }
        }
    }

    public boolean isRowFull(int index) {
        if(cache.get(index)==null)
            return false;
        return cache.get(index).size() == cacheAssoc;
    }
}
