public class Instruction {
    boolean isReadInstruction;
    String validHexString;
    int index;
    String binary;
    
    public Instruction(String string, int index) {
        processString(string);
        this.index = index;
    }
    private void processString(String string) {
        
        string = string.trim();
        isReadInstruction = string.split(" ")[0].equals("r");

        validHexString = string.split(" ")[1];
        String zeros = "00000000";
        validHexString = zeros.substring(0, 8 - validHexString.length()) + validHexString;

        binary = convertHexToBinaryString(validHexString);
    }

    private String convertHexToBinaryString(String hexString){
        hexString = hexString.replaceAll("0", "0000");
        hexString = hexString.replaceAll("1", "0001");
        hexString = hexString.replaceAll("2", "0010");
        hexString = hexString.replaceAll("3", "0011");
        hexString = hexString.replaceAll("4", "0100");
        hexString = hexString.replaceAll("5", "0101");
        hexString = hexString.replaceAll("6", "0110");
        hexString = hexString.replaceAll("7", "0111");
        hexString = hexString.replaceAll("8", "1000");
        hexString = hexString.replaceAll("9", "1001");
        hexString = hexString.replaceAll("a", "1010");
        hexString = hexString.replaceAll("b", "1011");
        hexString = hexString.replaceAll("c", "1100");
        hexString = hexString.replaceAll("d", "1101");
        hexString = hexString.replaceAll("e", "1110");
        hexString = hexString.replaceAll("f", "1111");
        return hexString;
    }
    public boolean isReadInstruction() {
        return isReadInstruction;
    }
    public String getvalidHexString() {
        return validHexString;
    }
    public int getIndex() {
        return index;
    }
    public String getBinary() {
        return binary;
    }

    
    
}
