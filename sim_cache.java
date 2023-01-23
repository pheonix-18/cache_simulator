class sim_cache {
	public static void main(String[] args) {
		//new CacheManager(32, 1048576, 32786, 0, 0, 0, 0, "gcc_trace.txt");
		//new CacheManager(16, 1024, 2, 0, 0, 2, 0, "vortex_trace.txt");
		//new CacheManager(16, 1024, 2, 8192, 4, 0, 0, "gcc_trace.txt");
		int blockSize = Integer.parseInt(args[0]);
		int l1Size = Integer.parseInt(args[1]);
		int l1Assoc = Integer.parseInt(args[2]);
		int l2Size = Integer.parseInt(args[3]);
		int l2Assoc = Integer.parseInt(args[4]);
		int replacementPolicy =Integer.parseInt(args[5]);
		int inclusionPolicy = Integer.parseInt(args[6]);
		String tracefile = args[7];
		new CacheManager(blockSize, l1Size, l1Assoc, l2Size, l2Assoc, replacementPolicy, inclusionPolicy, tracefile);
		
	}
}
