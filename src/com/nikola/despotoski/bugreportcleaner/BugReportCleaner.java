package com.nikola.despotoski.bugreportcleaner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BugReportCleaner {

	private static String PID_START_REGEX = "Start proc (%s+) for ([a-z]+ [^:]+): pid=(\\d+) uid=(\\d+) gids=(.*)";
	private static String PID_DIED_REGEX = "Process (%s+) \\(pid (\\d+)\\) has died.?";
	private static String PID_GC_REGEX = "(GC_(?:CONCURRENT|FOR_M?ALLOC|EXTERNAL_ALLOC|EXPLICIT) )(freed <?\\d+.)(, \\d+\\% free \\d+./\\d+., )(paused \\d+ms(?:\\+\\d+ms)?)";
	//Thanks to Maestro Jake Wharton for regexes https://github.com/JakeWharton/pidcat/blob/master/pidcat.py
	private static String PID_FIND_REGEX = "(\\d+-\\d+.\\d+:\\d+\\:\\d+.\\d+\\s+%d+\\s+%d+)(.*)";
	public static void printUsage(){
		System.out.println("Usage -p \"com.my.package\" -i \"path/to/bugreport.txt\" -o \"path/to/output/clean_bugreport.txt\" -gc [optional]");
		System.exit(6);
	}
	public static void main(String[] args) {
		String packageName = null;
		String filePath = null;
		String outPath = null;
		boolean keepGc = false;
		for(int i = 0 ; i < args.length; i++){
			if(args[i].equals("-p")){
				i++;
				packageName = args[i];
			}else if(args[i].equals("-i")){
				i++;
				filePath = args[i];
			}else if(args[i].equals("-o")){
				i++;
				outPath = args[i];
			}else if(args[i].equals("-gc")){
				keepGc = true;
			}
		}
		if(packageName == null){
			System.out.println("Package name is missing");
			printUsage();
		}else if(filePath == null){
			System.out.println("Input bugreport is missing");
			printUsage();
		}else if(outPath == null){
			System.out.println("Output file is missing");
			printUsage();
		}else{
			new Cleaner(filePath, new Predicate(packageName), outPath, keepGc).clean();
			System.out.println("Done!");
		}
	}
	
	private static class Predicate{
		private String mPackageName;
		private Pattern mStartPidPattern;
		private Pattern mEndPidPattern;
		private Pattern mGCPattern;
		public Predicate(String packageName){
			mPackageName = packageName;
			mStartPidPattern = Pattern.compile(String.format(PID_START_REGEX, mPackageName));
			mEndPidPattern = Pattern.compile(String.format(PID_DIED_REGEX, mPackageName));
			mGCPattern = Pattern.compile(PID_GC_REGEX);
			
		}
		
		public boolean findStart(String line){			
			return mStartPidPattern.matcher(line).find();
		}
		public boolean findDied(String line){
			return mEndPidPattern.matcher(line).find();
			
		}
		public boolean findGc(String line){
			return mGCPattern.matcher(line).find();
		}
		public boolean findPid(String line, int pidd){
			Matcher m = Pattern.compile(String.format(PID_FIND_REGEX, pidd,pidd)).matcher(line);			
			return m.matches() ;
		}
	}
	private static class Cleaner{
		private Predicate mPredicate;
		private File mOutFile;
		private String mInputFile;
		private boolean keepGC;

		public Cleaner(String inputFile, Predicate p, String outputFile, boolean gc){
			mInputFile = inputFile;
			mPredicate = p;
			mOutFile = new File(outputFile);
			keepGC = gc;
		}
		public void clean(){
			
			try {
				BufferedReader reader = new BufferedReader(new FileReader(mInputFile));
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(mOutFile, true)));
				String line = "";
				boolean pidStart = false;
				int pid = -1;
				while((line = reader.readLine()) != null){
					if(!pidStart && mPredicate.findStart(line)){
						pid = Integer.parseInt(line.substring(line.lastIndexOf("pid=")+"pid=".length(), line.indexOf("uid=")).trim());
						//use groups to find PID
						out.append(line);
						out.append('\n');
						pidStart = true;
					}else if(pidStart && !mPredicate.findDied(line) && mPredicate.findPid(line, pid)){
						if(!keepGC && mPredicate.findGc(line)) continue;
							out.append(line);
							out.append('\n');
					}else if(pidStart && mPredicate.findDied(line)){
						out.append(line);
						out.append('\n');
						out.append("------------------------------------------------------------------");
						out.append('\n');
						pidStart = false;
						pid=-1;
					}
					
				}
				reader.close();
			    out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   
		}
		
	}

}
