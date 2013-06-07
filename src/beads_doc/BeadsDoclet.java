import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

/**
 * Creates the beads quick reference and feature set document.
 * 
 * @author ben
 */
public class BeadsDoclet {
	
	public static int NUM_COLS = 3;
	
	
	public static boolean start(RootDoc root) {
		writeContents(root);
		return true;
	}

	private static void writeContents(RootDoc root) {
		try {
			String outputDir = getDir(root.options());
			ClassDoc[] classes = root.classes();
			
			// for each category, store a list of classes from that category
			Map<String,LinkedList<ClassDoc>> categoryMap = new HashMap<String,LinkedList<ClassDoc>>();
			int totalEntries = 0; // total number of entries that we will print
			for (int i = 0; i < classes.length; i++) {
//				PackageDoc pd = classes[i].containingPackage();
//				System.out.printf("class %s in package %s\n", classes[i].name(), pd.name());
				
				// categories
				Tag[] tags = classes[i].tags("beads.category");
				if (tags.length > 0) {
					for (Tag t : tags) {
						String category = t.text();
						if (!categoryMap.containsKey(category))
							categoryMap.put(category, new LinkedList<ClassDoc>());
							
						categoryMap.get(category).add(classes[i]);
						totalEntries++;
					}
				}
			}
			
			// Write the output to a quickref html page
			String filename = outputDir + "/index.html";
			System.out.println("Writing overview to \"" + filename + "\"");			
			PrintWriter file = new PrintWriter(filename);
			// write header
			BufferedReader header = new BufferedReader(new FileReader(outputDir + "/quickref.header"));
			String line = null;
			while ((line = header.readLine())!=null)
			{
				file.println(line);
			}
			
			// For each category, write out the classes which fall under that category
			int numEntriesPrintedThisCol = 0;			
			file.println("<div class=\"column\">");
			for (String category: categoryMap.keySet())
			{	
				file.printf("<div class=\"category\"><h5>%s</h5>\n",category);				
				for (ClassDoc cd: categoryMap.get(category))
				{
					// construct the hyperlink to the main API
					String href = "../doc/" + cd.containingPackage().toString().replaceAll("\\.", "/") + "/" + cd.name() + ".html";
					file.printf("<a href=\"%s\">%s</a><br />", href, cd.name());
					numEntriesPrintedThisCol++;
				}
				file.println("</div>");
				
				if (numEntriesPrintedThisCol > totalEntries/NUM_COLS)
				{
					file.println("</div>\n<div class=\"column\">");
					numEntriesPrintedThisCol = 0;
				}
			}
			if (numEntriesPrintedThisCol > totalEntries/NUM_COLS)
			{
				file.println("</div>");
			}			
			
			// write footer
			BufferedReader footer = new BufferedReader(new FileReader(outputDir + "/quickref.footer"));
			while ((line = footer.readLine())!=null)
			{
				file.println(line);
			}
			
			// close it
			file.close();
			
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getDir(String[][] options) {
		String dir = null;
		for (int i = 0; i < options.length; i++) {
			String[] opt = options[i];
			if (opt[0].equals("-d")) {
				dir = opt[1];
			}
		}
		return dir;
	}

	public static int optionLength(String option) {
		if (option.equals("-d")) {
			return 2;
		}
		return 0;
	}

	public static boolean validOptions(String options[][],
			DocErrorReporter reporter) {
		boolean foundDirOption = false;
		for (int i = 0; i < options.length; i++) {
			String[] opt = options[i];
			if (opt[0].equals("-d")) {
				if (foundDirOption) {
					reporter.printError("Only one -dg option allowed.");
					return false;
				} else {
					foundDirOption = true;
				}
			}
		}
		if (!foundDirOption) {
			reporter
					.printError("Usage: javadoc -d output dir -doclet ListTags ...");
		}
		return foundDirOption;
	}

}
