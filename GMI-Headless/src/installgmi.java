import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.google.gson.Gson;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

public class installgmi {
	
	public static int PropertiesVersion = 1;
	public static int ThisGMIHeadless = 2;
	public static String ThisGMIVersionReadable = "2.0.0"; //Different for Headless and GMI but same description in file.
	
	public static String SheetID = "";
	public static String APIKey = "";
	public static String PropertiesRange = "ModsProperties!A1:B16"; //must be changed every config version
	public static String ModListRange = "ModsList!D1:G"; //need last item row number to end of string
		
	public static boolean NeedGMIUpdates = false;
	public static boolean GMIHasUpdates = false;
	public static boolean NeedModUpdates = false;
	public static boolean NeedFabricLoader = false;

	private static int LOCAL_MODPACK_VERSION;
	private static String LOCAL_FABRICLOADER_VERSION;
	private static long UNIX_lAST_OPEN;
	
	private static int LAST_ITEM_ROW_NUMBER;
	private static String SP_FABRICLOADER_VERSION; //SP = Server Properties
	private static String SP_FABRICLOADER_LINK;
	private static String SP_HEADLESS_LINK;
	private static String JSON_RESPONSE;
	
	private static ArrayList<String> modlist = new ArrayList<String>();
	
	public static void main(String[] args) {
				
		System.out.println("Setting Up");
		ParseLocalConfig();
		GetOnlineProperties();
		GetOnlineModList();
		CheckForFabric();
		System.out.println("Done!");
			
		System.out.println("GMI-" + ThisGMIVersionReadable + " for HighskyMC Modpack running in Headless mode");
		
		DoChecks();	
		
		if (!NeedGMIUpdates) {
			StepOne();
		}

		
	}

	//Types:
	//	FULL - All the Mods
	//	NECESSARY - Only the Mods needed to join the server
	private static void ParseModsList(String type) {
        Gson g = new Gson();
        SheetsModListHandler model = g.fromJson(JSON_RESPONSE, SheetsModListHandler.class);
        List<List<String>> values = model.getValues();
        
        try { //Checks if the modlist is longer than the set end row. Usually this means that the modlist has changed. Doesnt cover removing mods.
            if (values.get(values.size()).toString() != "") {
            	System.out.println("Something went wrong. Restart GMI. If this happens again, contact your server owner");
            	System.exit(404);
            }
        }
        catch (IndexOutOfBoundsException e)
        {
        	
        }
            
        for (int i = 1; i <= values.size()-1; i++) {
        	
        	List<String> stray = values.get(i);
        	
        	try {
        		
        		if (!stray.get(0).equals("")) {
		      		
            		if (type == "FULL") {
            			if (!stray.get(2).equals("Server")) {
            				modlist.add(stray.get(0).trim().replaceAll("\"", ""));
            			}	
            		}
            		else if (type == "NECESSARY") {
            			if (!stray.get(2).equals("Server")) {
                			if (stray.get(3).equals("Yes")) {
                				modlist.add(stray.get(0).trim().replaceAll("\"", ""));       				
                			}
            			}
            		}
            		else {
            			System.out.println("???");
            		}
            	}
        		
        	}
        	catch (IndexOutOfBoundsException e) {
        		
        	}
        	
        }
        
        return;
	}
	
	private static void GetOnlineProperties() {
		HttpRequest http = new HttpRequest();
		try {
			String response = http.Request(SheetID, PropertiesRange, APIKey);	
	        Gson g = new Gson();
	        SheetsConfigHandler model = g.fromJson(response, SheetsConfigHandler.class);
	        Object[] arrayed = model.getValues().toArray();    
	        findvalue(arrayed, "");
	        
		} catch (IOException | NullPointerException e) {
			e.printStackTrace();
			System.out.println("You either have no internet OR you need to update a new version of GMI");
			System.exit(0);
		}
		
	}
	
	private static void GetOnlineModList() {
		HttpRequest http = new HttpRequest();
		try {
			JSON_RESPONSE = http.Request(SheetID, ModListRange + (LAST_ITEM_ROW_NUMBER+1), APIKey);	        
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
	}
	
	private static String findvalue(Object[] arrayed, String find) {
		
        for (int i = 0; i<arrayed.length; i++) {
        	String stray = arrayed[i].toString();
        	stray = stray.substring(1,stray.length()-1);
        	String[] astray = stray.split(",");
        	
        	if (find != "") {
        		
            	if (astray[0].equals(find)) {
            		return astray[1];
            	}
        		
        	} else {
        		
            	if (astray[0].equals("Properties Version")) {
            		if (Integer.parseInt(astray[1].trim()) != PropertiesVersion) {
            			NeedGMIUpdates = true;
            		}
            	}
            	
               	if (astray[0].equals("Latest GMI Headless")) {
               		if (Integer.parseInt(astray[1].trim()) != ThisGMIHeadless) {
            			GMIHasUpdates = true;
            		}
               	}
               	if (astray[0].equals("Latest Client Modpack Version")) {
               		if (Integer.parseInt(astray[1].trim()) != LOCAL_MODPACK_VERSION) {
            			NeedModUpdates = true;
            		}
               	}
               	if (astray[0].equals("Last Item Row Number")) {
               		LAST_ITEM_ROW_NUMBER = Integer.parseInt(astray[1].trim());
               	}
               	if (astray[0].equals("Fabric Loader Version")) {
               		
               		SP_FABRICLOADER_VERSION = astray[1];
               	}
               	if (astray[0].equals("Fabric Loader Link")) {
               		SP_FABRICLOADER_LINK = astray[1];
               	}
               	if (astray[0].equals("DL Link GMI Headless")) {
               		SP_HEADLESS_LINK = astray[1];
               	}
           	} 	
        }		
        
		return "";
	}
	
	private static void ParseLocalConfig() {

		AppDirs appDirs = AppDirsFactory.getInstance();
		Path path = Paths.get(appDirs.getUserDataDir(".gmi", null, null, true));
		
        File file = new File(path.toAbsolutePath()+ "/gmi.properties");
        if (file.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(file);
                Properties props = new Properties();
                props.load(inputStream);
                if (props.containsKey("local_modpack_version")) {
                	LOCAL_MODPACK_VERSION = Integer.parseInt(props.getProperty("local_modpack_version"));
                }
                if (props.containsKey("unix_last_open")) {
                	UNIX_lAST_OPEN = Integer.parseInt(props.getProperty("unix_last_open"));
                }
                inputStream.close();
            } catch (IOException e) {
                // File should exist because we checked before
                e.printStackTrace();
            }
        }
	}

	private static void DoChecks() {
		
		if (NeedGMIUpdates) {
			System.out.println("There is a new version of GMI that is needed to go further. Downloading it now.");
			HandleGMIDL();
		}
		
		if (GMIHasUpdates && !NeedGMIUpdates) {
			System.out.println("There is a new version of GMI that you should update to for a better experience, however it is not necessary. Go to the discord server, #server-updates to get the new version.");
		}
		
		if (NeedModUpdates && !NeedGMIUpdates) {
			System.out.println("There is a new version of the modpack!");
		}
	}
	
	public static void StepOne() {
		try (Scanner a = new Scanner(System.in)) {
			String b = "";
			while (b.equals("")) {
				System.out.println("Do you want to install this modpack? (Y/N)");
			  b = a.next();
			}
			
			if (b.startsWith("Y")) {
				StepTwo();
			} else if (b.startsWith("N")) {
				System.exit(0);
			} else {
				System.out.println(b);
				System.out.println("Incorrect Parameter");
				StepOne();
			}
		}
	}
	
	public static void StepTwo() {
		String c = GetClientModPath("mods").toString();
		
		if (new File(c).exists()) {
			System.out.println("Your current modpack path is: " + c);
			
			try (Scanner a = new Scanner(System.in)) {
				String b = "";
				while (b.equals("")) {
					System.out.println("Is this Correct? (Y/N)");
				  b = a.next();
				}
				
				if (b.startsWith("Y")) {
					intermediate2(c);
				} else if (b.startsWith("N")) {
					StepTwoNo();
				} else {
					System.out.println("Incorrect Parameter");
					StepTwo();
				}
			}
		} else {
			System.out.println("Mods folder is not where I expect it to be. If you've opened fabric-minecraft at least once before, try finding your mods folder manually and putting it here.");
			StepTwoNo();
		}
		

	}
	
	public static void StepTwoNo() {
		
		String b = "";
		try (Scanner a = new Scanner(System.in)) {
			while (b.equals("")) {
				System.out.println("Enter a valid directory:");
			  b = a.next();
			}
			
			if (new File(b).exists()) {	
				
				intermediate(b);

			} else {
				System.out.println("Folder does not exist.");
				StepTwoNo();
			}	
		}
		
		

	}
	
	public static void intermediate(String b) {
		try (Scanner k = new Scanner(System.in)) {
			String l = "";
			while (l.equals("")) {
				System.out.println("Confirm? (Y/N)");
			  l = k.next();
			}
			
			if (l.startsWith("Y")) {
				intermediate2(b);
			} else if (l.startsWith("N")) {
				StepTwoNo();
			} else {
				System.out.println("Incorrect Parameter");
				StepTwoNo();
			}
			
		}
	}
	
	public static void intermediate2(String b) {
		try (Scanner k = new Scanner(System.in)) {
			String l = "";
			while (l.equals("")) {
				System.out.println("Do you want to install all the mods? FULL allows for a better experience and is recommended. However, NECESSARY may run better if you're not using a powerful computer. (FULL/NECESSARY)");
			  l = k.next();
			}
			
			if (l.startsWith("FULL")) {
				ParseModsList("FULL");
				StepThree(b);
			} else if (l.startsWith("NECESSARY")) {
				ParseModsList("NECESSARY");
				StepThree(b);
			} else {
				System.out.println("Incorrect Parameter");
				intermediate2(b);
			}
			
		}
	}
	
	public static void StepThree(String path) {
		
		System.out.println("Checking for previous mods...");
		PathCleanclass pcc = new PathCleanclass();
		pcc.path = path;
		Thread threads = new Thread(pcc);
		threads.start();
		
		try {
			threads.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StepFour(path);
	}
	
	public static void StepFour(String path) {
		download(path);
	}
	
	
	//////////////////////////////////////
   
	public static void CheckForFabric() {
		
		File file = new File(GetClientModPath(".mc").toString(), "versions");
		ArrayList<String> UnsupportedFabricVersions = new ArrayList<String>();
		boolean foundit = false;
		
		if (file.isDirectory()) {
			File[] directories = file.listFiles(File::isDirectory);
			
			for (File dir : directories) {
				
				if (dir.getName().contains("fabric")) {
					if (dir.getName().contains(SP_FABRICLOADER_VERSION.trim())) {
						foundit = true;
						return;
					}
					else
					{
						UnsupportedFabricVersions.add(dir.getName());
					}
				}
			}
		}
		
		if (foundit) {
			return;
		}
		else {
			if (UnsupportedFabricVersions.size() != 0) {
				System.out.println("You have fabric versions: " + String.join("; ", UnsupportedFabricVersions));
				System.out.println("However you don't have the required fabric version: " + SP_FABRICLOADER_VERSION);
				System.out.println("Proceeding to Fabric Installation");
				
			}
			else {
				System.out.println("You do not have fabric installed. Proceeding to Fabric Installation");			
			}
			
			HandleFabricLoaderDL();
			return;
		}
		
	}
	
	public static void HandleFabricLoaderDL() {
		try {
			downloadUsingNIO(SP_FABRICLOADER_LINK, new File(System.getProperty("java.io.tmpdir"), "fabric.jar").getPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		executeJAR(new File(System.getProperty("java.io.tmpdir"), "fabric.jar").getPath());
		CheckForFabric();
	}
	
	public static void HandleGMIDL() {
		
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
		
		try {
			downloadUsingNIO(SP_HEADLESS_LINK, new File(System.getProperty("java.io.tmpdir"), "gmi.jar").getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			if (isWindows) {
				Runtime.getRuntime().exec("cmd.exe /C start java" + " -jar " + new File(System.getProperty("java.io.tmpdir"), "gmi.jar").getPath());
			}else {
				Runtime.getRuntime().exec("sh -c open java" + " -jar " + new File(System.getProperty("java.io.tmpdir"), "gmi.jar").getPath());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
		
	}
	
    public static void executeJAR(String Jar) {

        Process pb;
		try {
			pb = Runtime.getRuntime().exec("java" + " -jar " + Jar);
			pb.waitFor();   
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Something went wrong. Error: EXECUTEJAR_FAILED");
			System.exit(404);
		}

    }

	
	public static Path GetClientModPath(String what) {
		
		AppDirs appDirs = AppDirsFactory.getInstance();
		
		if (what == "mods") {
			Path path = Paths.get(appDirs.getUserDataDir(".minecraft", "mods", null, true));
			return path;
		} else if (what == ".mc") {
			Path path = Paths.get(appDirs.getUserDataDir(".minecraft", null, null, true));
			return path;
		} else {
			return null;
		}

	}
	
	public static void download(String b) {
	   	  
	   String os = System.getProperty("os.name");
	   
	   if (os.startsWith("Windows")) {
		   
		   try (ProgressBar pb = new ProgressBarBuilder()
				   .setStyle(ProgressBarStyle.ASCII)
				   .setTaskName("Downloading")
				   .setInitialMax(modlist.size())
				   .build()) { 
			   for (int i = 0; i < modlist.size(); i++) {
			    	 
			    	 String url = modlist.get(i);
			    	 
			    	 try {
				            downloadUsingNIO(url, b + "/mods_01_" + i + "-" + url.split("/")[url.split("/").length-1] + ".jar");
				            pb.step();
					        
				        } catch (IOException e) {
				        	System.out.println("Download failed: " + e);
				        	System.exit(404);
				        }
			     }
			   } 
	   } else {
		   try (ProgressBar pb = new ProgressBar("Downloading", modlist.size())) {
				   for (int i = 0; i < modlist.size(); i++) {
			    	 
			    	 String url = modlist.get(i);
			    	 
			    	 try {
				            downloadUsingNIO(url, b + "/mods_01_" + i + "-" + url.split("/")[url.split("/").length-1] + ".jar");
				            pb.step();
					        
				        } catch (IOException e) {
				        	System.out.println("Download failed: " + e);
				        	System.exit(404);
				        }
			     }
			   } 
	   }
	   	   
	   System.out.println("Finished Downloading");
	   System.exit(0);
	  
   }
   	  
	private static void downloadUsingNIO(String Strurl, String path) throws IOException {
       final URLConnection connection = new URL(Strurl).openConnection();
       connection.addRequestProperty("User-Agent",
               "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:102.0) Gecko/20100101 Firefox/102.0");

       final int contentLength = connection.getContentLength();
       final File end = new File(path);

       if (end.exists()) {
           final URLConnection savedFileConnection = end.toURI().toURL().openConnection();
           if (savedFileConnection.getContentLength() == contentLength) {
               return;
           }
       } else {
           final File dir = end.getParentFile();
           if (!dir.exists())
               dir.mkdirs();
       }

       final ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
       final FileOutputStream fos = new FileOutputStream(end);
       fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
       fos.close();
		
       return;
   }

}
