
package encode.json;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.*;

public class EncodeDCCJsonParser {

	/*
	 * Input:
	 * 1. URL with query strings built in. For example: https://www.encodedcc.org/search/?type=experiment&replicates.library.biosample.biosample_type=tissue&organ_slims=small+intestine&assay_term_name=ChIP-seq&target.label=POLR2A
	 * 2. Output file (fully qualified)
	 * 3. Output file format (tsv or csv)
	 * 4 (optional) Fully qualified file that containts list of accession numbers in this format
	 * ENCRXXXX1
	 * ENCRXXXX2
	 * .
	 * .
	 * .
	 * and so on 
	 * 
	 * Output:
	 * Program writes metadata to output file specified
	 * 
	 */
	public static void main(String[] args) throws Exception {

		// build a JSON-formatted URL
		
		String urlString = args[0] + "&format=json&limit=all";
		String outputFile = args[1];
		String outputFileFormat = args[2];
		//Load up external list if available
		List<String> allowedAccessions = new ArrayList<String>();
		if (args.length == 4)
		{
			String accessionList = args[3];
			if (accessionList != null && accessionList.length() > 0)
			{
				
			    Scanner scan = new Scanner(new File(accessionList));
			    while (scan.hasNext())
			    {
			    	String line = scan.nextLine();
			    	if (line.trim().split("\\s+").length > 1)
			    	{
			    		System.err.println("Accession list not in correct format. Only one accession number in each line allowed");
			    		throw new Exception("Accession list not in correct format. Only one accession number in each line allowed");
			    	}
			    	allowedAccessions.add(line);
			    }
			    scan.close();
			}
		}
		String output_delimiter = null;
		
		if (outputFileFormat.equals("tsv"))
			output_delimiter = "\t";
		else if (outputFileFormat.equals("csv"))
			output_delimiter = ",";
		else
			throw new Exception("output format can only be tsv or csv");
		
		FileWriter outFile = new FileWriter(outputFile);
		// Create title string for output file. When you add/remove columns, remember to consolidate with last line that adds actual metadata.
		StringBuffer title = new StringBuffer().append("accession").append(output_delimiter)
												 .append("in approved list").append(output_delimiter)
												 .append("organism").append(output_delimiter)
												 .append("life stage").append(output_delimiter)
												 .append("age").append(output_delimiter)
												 .append("assay").append(output_delimiter)
												 .append("tissue/cell line").append(output_delimiter)
												 .append("target").append(output_delimiter)
												 .append("status").append(output_delimiter)
												 .append("description").append(output_delimiter)
												 .append("fileFormat").append(output_delimiter)
												 .append("fileSize").append(output_delimiter)
												 .append("URL").append(output_delimiter)
												 .append("md5sum").append(output_delimiter)
												 .append("lab").append(output_delimiter)
												 .append("date created").append(output_delimiter)
												 .append("biologicalReplicate").append(output_delimiter)
												 .append("technicalReplicate").append(output_delimiter)
												 .append("run type").append(output_delimiter)
												 .append("read length").append(output_delimiter)
												 .append("control details(@Accession;URL;md5Sum)")
												 .append("\n");
		//write title out to file
		outFile.write(title.toString());
		
		
	    URL url = new URL(urlString);
	    String host = url.getHost();
	    String protocol = url.getProtocol();
	 
	    // read from the URL, load into local StringBuffer object
	    Scanner scan = new Scanner(url.openStream());
	    StringBuffer strBuffer = new StringBuffer();
	    while (scan.hasNext())
	        strBuffer.append(scan.nextLine());
	    scan.close();
	 
	    // build a JSON object out of String
	    JSONObject obj = new JSONObject(strBuffer.toString());
	    if (! obj.getString("notification").equals("Success"))
	    {
	       throw new Exception("Failed on notification field != \"Success\"");
	    }
	 
	    // Start parsing down the JSON objects. 
	    JSONArray searchResults = obj.getJSONArray("@graph");
	    int n = searchResults.length();
	    
	    //For each experiment in results (from input URL)
	    for (int i = 0; i < n; i ++)
	    {
	    	JSONObject experiment = searchResults.getJSONObject(i);
	    	String relativeURL = "NA";
	    	String accession = "NA";
	    	String assay = "NA";
	    	String tissue = "NA";
	    	String target = "NA";
	    	String status = "NA";
	    	JSONObject details = null;
	    	String organism = "NA";
	    	String age = "NA";
	    	String lifeStage = "NA";
	    	String description = "NA";
	    	String inAccesionList = "NA";
	    	
	    	
	    	//experiment-specific details
	    	try {
	    	 relativeURL = experiment.getString("@id");
	    	 accession = experiment.getString("accession");
	    	 assay = experiment.getString("assay_term_name");
	    	 tissue = experiment.getString("biosample_term_name");
	    	 target = experiment.getString("target.label"); //antibody
	    	 status = experiment.getString("status"); //released 
	    	 details = experiment.getJSONArray("replicates").getJSONObject(0).getJSONObject("library").getJSONObject("biosample");
	    	 organism = details.getJSONObject("organism").getString("scientific_name");
	    	 age = details.getString("age") + details.getString("age_units"); //11.5 days, 8 weeks, etc
	    	 lifeStage = details.getString("life_stage"); //embryonic, adult etc
	    	 description = experiment.getString("description");
	    	 
	    	
	    	} catch (JSONException jsonException)
	    	{
	    		if (jsonException.getMessage().indexOf("not found") > 0)
	    		{
	    			if (jsonException.getMessage().indexOf("accession") > 0)
	    				throw jsonException;
	    			else
	    				System.err.println("Warning: "+jsonException.getMessage() +  "for accession number " + accession);
	    		}
	    		else
	    			throw jsonException;
	    	}
	    	//check if this is in the external list (if provided)
	    	if (allowedAccessions.size() > 0)
	    	{
		    	if (allowedAccessions.contains(accession))
		    	{
		    		inAccesionList = "yes";
		    	} else {
		    		inAccesionList = "no";
		    	}
		    }
	    	URL experimentPage = new URL(new StringBuffer().append(protocol).append("://").append(host).append(relativeURL).append("?format=json").toString());
	    	
	    	 // drill down into experiment page to get to details of downloadable files
		    Scanner scanExperimentPage = new Scanner(experimentPage.openStream());
		    StringBuffer strBufferExperimentPage = new StringBuffer();
		    while (scanExperimentPage.hasNext())
		    	strBufferExperimentPage.append(scanExperimentPage.nextLine());
		    scanExperimentPage.close();
		    
		    // build a JSON object for experiment page
		    JSONObject objExperiment = new JSONObject(strBufferExperimentPage.toString());
		    StringBuffer controlDetails = new StringBuffer().append("NA");
		    String runType = "NA";
		    
		    try {
		    	runType = objExperiment.getString("run_type"); //Single-ended vs paired end
		    	JSONArray controls = objExperiment.getJSONArray("possible_controls");
		    	int l = controls.length();
		    	//For each control file listed
		    	for (int k = 0; k < l; k ++)
		    	{
		    		JSONObject control = controls.getJSONObject(k);
		    		String control_accession = control.getString("accession");
		    		JSONArray controlFiles = control.getJSONArray("files");
		    		controlDetails = new StringBuffer();
		    		for (int a = 0; a < controlFiles.length(); a ++)
		    		{
		    			URL controlURL = new URL(new StringBuffer().append(protocol).append("://").append(host).append(controlFiles.getString(a)).append("?format=json").toString());
		    			Scanner scanControlPage;
		    			try {
		    			 scanControlPage = new Scanner(controlURL.openStream());
		    			} catch (IOException ioe)
		    			{
		    				System.err.println("Warning: "+"Error fetching page " + controlURL + " for accession " + accession);
		    				ioe.printStackTrace();
		    				continue;
		    			}
		    			 StringBuffer strBufferControlPage = new StringBuffer();
		    			 while (scanControlPage.hasNext())
		    				 strBufferControlPage.append(scanControlPage.nextLine());
		    			 scanControlPage.close();
		    			 
		    			 // build a JSON object for control file page
		    			 JSONObject objControl = new JSONObject(strBufferControlPage.toString());
		    			 String controlHref = objControl.getString("href");
		    			 String controlMd5 = objControl.getString("md5sum");
		    			 
		    			 //build one string for "control details" column, merging multiple control file metadata
		    			 controlDetails.append("@").append(control_accession).append(";").append(protocol).append("://").append(host).append(controlHref).append(";").append(controlMd5);
		    		}
		    	}
		    } catch(JSONException jsonException)
	    	{
	    		if (jsonException.getMessage().indexOf("not found") > 0)
	    		{
	    			System.err.println("Warning: "+jsonException.getMessage()  +  "for accession number " + accession);
	    		}
	    		else
	    			throw jsonException;
	    	}
		    
		    //Now get to actual files for experiment
		    JSONArray files = null;
		    try {
		    	files = objExperiment.getJSONArray("files");
		    } catch (JSONException jsex)
		    {
		    	System.err.println("Warning: "+"No files found for accession number " + accession);
		    	throw jsex;
		    }
		    int c = files.length();
		    // For each file
		    for (int j = 0; j < c; j ++ )
		    {
		    	JSONObject file = files.getJSONObject(j);
		    	String fileFormat = "NA";
		    	long fileSize = 0;
		    	String href = "NA";
		    	String md5sum = "NA";
		    	String submittedBy = "NA";
		    	String dateCreated = "NA";
		    	int biologicalReplicate = 0;
		    	int technicalReplicate = 0;
		    	String readLength = "NA";
		    	
		    	// file-specific details
		    	try {
		    		fileFormat = file.getString("file_format");
		    		fileSize = file.getLong("file_size");
		    		href = protocol + "://" + host + file.getString("href");
		    		md5sum = file.getString("md5sum");
		    		submittedBy = file.getJSONObject("submitted_by").getString("lab").replaceFirst("/labs/", "").replace("/","");
		    		dateCreated = file.getString("date_created");
		    		biologicalReplicate = file.getJSONObject("replicate").getInt("biological_replicate_number");
		    		technicalReplicate = file.getJSONObject("replicate").getInt("technical_replicate_number");
		    		readLength = file.getJSONObject("replicate").getInt("read_length") + file.getJSONObject("replicate").getString("read_length_units");
		    	} catch (JSONException jsonException)
		    	{
		    		if (jsonException.getMessage().indexOf("not found") > 0)
		    		{
		    			System.err.println("Warning: "+jsonException.getMessage()  +  "for accession number " + accession);
		    		}
		    		else
		    			throw jsonException;
		    	}
		    	// Build metadata row. If you add/delete columns, remember to consolidate with title row in the beginning of method
		    	StringBuffer row = new StringBuffer().append(accession).append(output_delimiter)
		    										 .append(inAccesionList).append(output_delimiter)
	    											 .append(organism).append(output_delimiter)
	    											 .append(lifeStage).append(output_delimiter)
	    											 .append(age).append(output_delimiter)
	    											 .append(assay).append(output_delimiter)
	    											 .append(tissue).append(output_delimiter)
	    											 .append(target).append(output_delimiter)
	    											 .append(status).append(output_delimiter)
	    											 .append(description).append(output_delimiter)
	    											 .append(fileFormat).append(output_delimiter)
	    											 .append(fileSize).append(output_delimiter)
	    											 .append(href).append(output_delimiter)
	    											 .append(md5sum).append(output_delimiter)
	    											 .append(submittedBy).append(output_delimiter)
	    											 .append(dateCreated).append(output_delimiter)
	    											 .append(biologicalReplicate).append(output_delimiter)
	    											 .append(technicalReplicate).append(output_delimiter)
	    											 .append(runType).append(output_delimiter)
	    											 .append(readLength).append(output_delimiter)
	    											 .append(controlDetails)
	    											 .append("\n");
		    	//write row to output file
		    	outFile.write(row.toString());
		    	
		    	}
		    	
		    }
	    	outFile.close();
		    
	    }
      	
}
