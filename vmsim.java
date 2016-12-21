import java.io.*;
import java.util.*;

/**
 *     Overall running: ./vmsim –n <numframes> -a <opt|clock|aging|work> [-r <refresh>] [-t <tau>] <tracefile>
 *     Simulates virtual memory algorithms Optimal, Clock, Aging, and Working Set
 */
public class vmsim {

    public static int memAccess = 0;
    public static int pageFaults = 0;
    public static int writes = 0;
    public static String traceFile = "Initializing traceFile";
    public static BufferedReader buff = null;
    public static ArrayList<String> hex;
    public static ArrayList<String> accessOpt;
    public static  ArrayList<Page> pageTable;
    public static  int[] frameArray;

    public static void main(String[] args) throws IOException {

        int numFrames = 0;
        int refresh = 0;
        int tau = 0;
        String algChoice="Initializing algChoice";

        //Looks at arguments at sets values appropriately.
        for (int i =0; i<args.length; ++i){
            if(args[i].equals("-n")){
                numFrames = Integer.parseInt(args[i+1]);
                //System.out.println("numFrames is: " + numFrames + "\n");
            }
            else if (args[i].equals("-a")){
                algChoice = args[i+1];
                //System.out.println("algChoice is: " + algChoice + "\n");
            }
            else if (args[i].equals("-r")){
                refresh = Integer.parseInt(args[i+1]);
                //System.out.println("refresh is: " + refresh + "\n");
            }
            else if (args[i].equals("-t")) {
                tau = Integer.parseInt(args[i+1]);
                //System.out.println("tau is: " + tau + "\n");
            }
            else if (i==args.length-1){
                traceFile = args[i];
            }
        }

		//Creates Page Table
        pageTable = new ArrayList<>();
        int maxPages = 1024*1024;
        for(int i=0;i<maxPages;i++){
            Page entry = new Page();
            pageTable.add(i, entry);
        }

        String [] readLine = null;
        hex = new ArrayList<>();
        accessOpt = new ArrayList<>();

        buff = new BufferedReader(new FileReader(traceFile));
        while(buff.ready()){
            readLine = buff.readLine().split(" ");
            hex.add(readLine[0]);
            accessOpt.add(readLine[1]);
            memAccess++;
        }
        frameArray = new int[numFrames];
        for(int i=0;i<numFrames;i++) {
            frameArray[i] = -1;
        }

        //Jumps to algorithm methods needed
        if(algChoice.equals("opt")){
            opt(numFrames);
        }
        else if (algChoice.equals("clock")){
            clock(numFrames);
        }
        else if(algChoice.equals("aging")){
            aging(numFrames, refresh);
        }
        else if(algChoice.equals("work")){
            work(numFrames, refresh, tau);
        }


        System.out.println("Algorithm: " + algChoice);
        System.out.println("Number of frames: \t\t" + numFrames);
        System.out.println("Total memory accesses: \t" + memAccess);
        System.out.println("Total Page faults: \t\t" + pageFaults);
        System.out.println("Total writes to disk: \t" + writes);


    }

	
	/*
	*	Optimal Page Replacement algorithm. Not realistic in terms of 
	*/
    public static void opt(int numFrames){

        //Converts hex to decimal
        for(int i=0;i<hex.size();i++) {
            int pageNum = Integer.decode("0x" + hex.get(i).substring(0, 5));
            int currFrame = 0;

			//Checks if the correct algorithm was chosen
            Page entry = pageTable.get(pageNum);
            entry.index = pageNum;
            entry.ref = true;
            entry.age = memAccess;
            if(accessOpt.get(i).equals("W"))
                entry.dirty = true;
            if(!entry.valid){
                pageFaults++;

                if(currFrame < numFrames){
                    frameArray[currFrame] = pageNum;
                    entry.frame = currFrame;
                    entry.valid = true;
                    currFrame++;
                }
                else{
                    int evictFrame= 0;
                    int oldest=0;
                    for(int j=0; j<frameArray.length; j++){
                        if(frameArray[j]==-1){
                            evictFrame = frameArray[j];
                            break;
                        }
                        else{
                            if(oldest < pageTable.get(frameArray[j]).age) {
                                oldest = pageTable.get(frameArray[j]).age;
                                evictFrame = frameArray[j];
                            }
                        }
                    }
                    Page evictPage = pageTable.get(evictFrame);
                    if(evictPage.dirty){
                        writes++;
                    }
                    frameArray[evictPage.frame] = entry.index;
                    entry.frame = evictPage.frame;
                    entry.valid = true;
                    evictPage.dirty = false;
                    evictPage.ref = false;
                    evictPage.frame = -1;
                    evictPage.valid = false;
                    pageTable.set(evictFrame, evictPage);
                }
            }

            pageTable.set(pageNum, entry);
            memAccess++;
        }
    }
	
	/*
	*	Clock algorithm
	*/
    public static void clock(int numFrames){

        int clockLoc =0;
        //Converts hex to decimal
        for(int i=0;i<hex.size();i++) {
            int pageNum = Integer.decode("0x" + hex.get(i).substring(0, 5));
            int currFrame = 0;

            Page entry = pageTable.get(pageNum);
            entry.index = pageNum;
            entry.ref = true;
            if (accessOpt.get(i).equals("W"))
                entry.dirty = true;
            if (!entry.valid) {
                pageFaults++;
                if (currFrame < numFrames) {
                    frameArray[currFrame] = pageNum;
                    entry.frame = currFrame;
                    entry.valid = true;
                    currFrame++;
                } else {
                    int evictFrame = 0;
                    boolean check = true;
                    while (check) {
                        if (!pageTable.get(frameArray[clockLoc]).ref) {
                            evictFrame = frameArray[clockLoc];
                            check = false;
                        } else {
                            pageTable.get(frameArray[clockLoc]).ref = false;
                        }
                        clockLoc++;
                    }

                    Page evictPage = pageTable.get(evictFrame);
                    if (evictPage.dirty) {
                        writes++;
                    }

                    frameArray[evictPage.frame] = entry.index;
                    entry.frame = evictPage.frame;
                    entry.valid = true;
                    evictPage.dirty = false;
                    evictPage.ref = false;
                    evictPage.frame = -1;
                    evictPage.valid = false;
                    pageTable.set(evictFrame, evictPage);
                }
            }
            pageTable.set(pageNum, entry);
            memAccess++;
        }
    }
	
	/*
	*	Aging algorithm for page replacement
	*/
    public static void aging(int numFrames, int refresh){
        int currFrame = 0;

        //Converts hex to decimal
        for(int i=0;i<hex.size();i++) {
            int pageNum = Integer.decode("0x" + hex.get(i).substring(0, 5));


            if (memAccess % refresh == 0) {
                for (int k = 0; k < currFrame; k++) {
                    Page entry = pageTable.get(frameArray[k]);
                    entry.ref = false;
                    pageTable.set(entry.index, entry);
                }
            }

            Page entry = pageTable.get(pageNum);
            entry.index = pageNum;
            entry.ref = true;

            if (accessOpt.get(i).equals("W"))
                entry.dirty = true;
            if (!entry.valid) {
                pageFaults++;
                if (currFrame < numFrames) {
                    frameArray[currFrame] = pageNum;
                    entry.frame = currFrame;
                    entry.valid = true;
                    currFrame++;
                } else {
                    int evictFrame = 0;
                    int maxBit = 0;
                    int maxBitLoc = 0;
                    for (int j = 0; j < frameArray.length; j++) {
                        if (!pageTable.get(frameArray[j]).ref) {
                            if (maxBit < pageTable.get(frameArray[j]).bitAge) {
                                maxBit = pageTable.get(frameArray[j]).bitAge;
                                maxBitLoc = j;
                            }
                        } else {
                            pageTable.get(frameArray[j]).ref = false;
                            int bitShift = pageTable.get(frameArray[j]).bitAge;
                            pageTable.get(frameArray[j]).bitAge = bitShift << 1;
                        }
                        if (j == frameArray.length - 1) {
                            evictFrame = frameArray[maxBitLoc];
                        }

                    }

                    Page evictPage = pageTable.get(evictFrame);
                    if (evictPage.dirty) {
                        writes++;
                    }

                    frameArray[evictPage.frame] = entry.index;
                    entry.frame = evictPage.frame;
                    entry.valid = true;
                    evictPage.dirty = false;
                    evictPage.ref = false;
                    evictPage.frame = -1;
                    evictPage.valid = false;
                    pageTable.set(evictFrame, evictPage);
                }
            }

            pageTable.set(pageNum, entry);
            memAccess++;
        }
    }
	
	/*
	*	WOrking Set algorithm for page replacement
	*/
    public static void work(int numFrames, int refresh, int tau){
        int currFrame = 0;

        //Converts hex to decimal
        for(int i=0;i<hex.size();i++) {
            int pageNum = Integer.decode("0x" + hex.get(i).substring(0, 5));

            if (memAccess % refresh == 0) {
                for (int k = 0; k < currFrame; k++) {
                    Page entry = pageTable.get(frameArray[k]);
                    entry.ref = false;
                    entry.age = memAccess;
                    pageTable.set(entry.index, entry);
                }
            }

            Page entry = pageTable.get(pageNum);
            entry.index = pageNum;
            entry.ref = true;

            if (accessOpt.get(i).equals("W"))
                entry.dirty = true;
            if (!entry.valid) {
                pageFaults++;
                if (currFrame < numFrames) {
                    frameArray[currFrame] = pageNum;
                    entry.frame = currFrame;
                    entry.valid = true;
                    currFrame++;
                } else {
                    int evictFrame = 0;
                    int oldest = 0;

                    for (int j = 0; j < numFrames; j++) {
                        if (!pageTable.get(frameArray[j]).ref) {
                            if (!pageTable.get(frameArray[j]).dirty) {
                                evictFrame = frameArray[j];
                            } else {
                                if (pageTable.get(frameArray[j]).age > tau) {
                                    if (pageTable.get(frameArray[j]).dirty) {
                                        writes++;
                                        pageTable.get(frameArray[j]).dirty = false;
                                    }
                                }
                                if (oldest < pageTable.get(frameArray[j]).age) {
                                    oldest = pageTable.get(frameArray[j]).age;
                                    evictFrame = frameArray[j];
                                }
                            }
                        }
                    }


                    Page evictPage = pageTable.get(evictFrame);
                    if(evictPage.frame==-1){
                        evictPage.frame =0;
                    }

                    frameArray[evictPage.frame] = entry.index;
                    entry.frame = evictPage.frame;
                    entry.valid = true;
                    evictPage.dirty = false;
                    evictPage.ref = false;
                    evictPage.frame = -1;
                    evictPage.valid = false;
                    pageTable.set(evictFrame, evictPage);
                }
            }
            pageTable.set(pageNum, entry);
            memAccess++;
        }
    }
}
