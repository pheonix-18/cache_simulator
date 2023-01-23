public class CBlock {
    int index;
    String tag;
    String hexString;
    boolean isDirtyBit;
    int LRUIndex;
    int OptIndex;
    int FIFOIndex;
    
    public CBlock(int index, String tag, String hexString, boolean isDirtyBit, int lRUIndex, int FIFOIndex, int optIndex) {
        this.index = index;
        this.tag = tag;
        this.hexString = hexString;
        this.isDirtyBit = isDirtyBit;
        this.FIFOIndex = FIFOIndex;
        this.LRUIndex = lRUIndex;
        this.OptIndex = optIndex;
    }
    public int getFIFOIndex() {
        return FIFOIndex;
    }
    public void setFIFOIndex(int fIFOIndex) {
        FIFOIndex = fIFOIndex;
    }
    public String getHexString() {
        return hexString;
    }
    public void setHexString(String hexString) {
        this.hexString = hexString;
    }
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public boolean isDirtyBit() {
        return isDirtyBit;
    }
    public void setDirtyBit(boolean isDirtyBit) {
        this.isDirtyBit = isDirtyBit;
    }
    public int getLRUIndex() {
        return LRUIndex;
    }
    public void setLRUIndex(int lRUIndex) {
        LRUIndex = lRUIndex;
    }
    public int getOptIndex() {
        return OptIndex;
    }
    public void setOptIndex(int optIndex) {
        OptIndex = optIndex;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CBlock other = (CBlock) obj;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        return true;
    }
    
}
