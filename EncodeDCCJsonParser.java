package encode.json;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;
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
	 * 5. File with HTTP Authentication credentials in this format
	 * <User>:<Password>
	 * 
	 * Output:
	 * Program writes metadata to output file specified
	 * 
	 */
	public static void main(String[] args) throws JSONException,IOException {

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
			    		throw new IOException("Accession list not in correct format. Only one accession number in each line allowed");
			    	}
			    	allowedAccessions.add(line);
			    }
			    scan.close();
			}
		}
		String auth = null;
		if (args.length == 5)
		{
			String authKeyPairFileName = args[4];
			Scanner sc = new Scanner(new File(authKeyPairFileName));
			auth = sc.next();
		    sc.close();
		}
		
		
		String output_delimiter = null;
		
		if (outputFileFormat.equals("tsv"))
			output_delimiter = "\t";
		else if (outputFileFormat.equals("csv"))
			output_delimiter = ",";
		else
			throw new IOException("output format can only be tsv or csv");
		
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
												 .append("treatments").append(output_delimiter)
												 .append("experimentStatus").append(output_delimiter)
												 .append("description").append(output_delimiter)
												 .append("fileStatus").append(output_delimiter)
												 .append("fileFormat").append(output_delimiter)
												 .append("output type").append(output_delimiter)
												 .append("fileSize").append(output_delimiter)
												 .append("URL").append(output_delimiter)
												 .append("md5sum").append(output_delimiter)
												 .append("assembly").append(output_delimiter)
												 .append("lab").append(output_delimiter)
												 .append("date created").append(output_delimiter)
												 .append("biologicalReplicate").append(output_delimiter)
												 .append("technicalReplicate").append(output_delimiter)
												 .append("run type").append(output_delimiter)
												 .append("read length").append(output_delimiter)
												 .append("control details(@Accession;URL;md5Sum)").append(output_delimiter)
												 .append("friendly name")
												 .append("\n");
		//write title out to file
		outFile.write(title.toString());
		
	    URL url = new URL(urlString);
	    
	    URLConnection urlConn = url.openConnection();
	    if (auth != null)
	    {
	    	String basicAuth = "Basic " + new String(new Base64().encode(auth.getBytes()));
	    	urlConn.setRequestProperty ("Authorization", basicAuth);
	    }
	    	
	    String host = url.getHost();
	    String protocol = url.getProtocol();
	   
	 
	    // read from the URL, load into local StringBuffer object
	    Scanner scan = new Scanner(urlConn.getInputStream());
	    StringBuffer strBuffer = new StringBuffer();
	    while (scan.hasNext())
	        strBuffer.append(scan.nextLine());
	    scan.close();
	 
	    // build a JSON object out of String
	    JSONObject obj = new JSONObject(strBuffer.toString());
	    if (! obj.optString("notification").equals("Success"))
	    {
	       throw new JSONException("Failed on notification field != \"Success\"");
	    }
	 
	    // Start parsing down the JSON objects. 
	    JSONArray searchResults = obj.getJSONArray("@graph");
	    int n = searchResults.length();
	    
	    //For each experiment in results (from input URL)
	    for (int i = 0; i < n; i ++)
	    {
	    	 System.out.println("Starting experiment " + (i+1) + " of " + n);
	    	//experiment-specific details
	    	JSONObject experiment = searchResults.getJSONObject(i);
	    	String relativeURL = experiment.getString("@id");
	    	String accession = experiment.getString("accession");
	    	String assay = experiment.optString("assay_term_name","NA");
	    	String tissue =  experiment.optString("biosample_term_name","NA");
	    	String target = experiment.optString("target.label","NA"); //antibody
	    	String status = experiment.optString("status","NA"); //released
	    	String age = "NA";
	    	String lifeStage = "NA";
	    	String organism = "NA";
	    	JSONObject details = new JSONObject();
	    	StringBuffer treatments = new StringBuffer();
	    	try{
	    		details = experiment.optJSONArray("replicates").optJSONObject(0).optJSONObject("library").optJSONObject("biosample");
	    		age = details.optString("age","NA") + details.optString("age_units","NA"); //11.5 days, 8 weeks, etc
		    	lifeStage = details.optString("life_stage","NA"); //embryonic, adult etc
		    	String organism_full = details.optJSONObject("organism") != null?details.optJSONObject("organism").optString("scientific_name","NA"):"NA";
		    	String[] s = organism_full.split("\\s+");
		    	organism = s[0].substring(0,1).toLowerCase() + s[1].substring(0,1).toLowerCase();
		    	JSONArray treatments_arr = details.optJSONArray("treatments");
		    	
		    	if (treatments_arr != null)
		    	{
		    		int treatments_num = treatments_arr.length();
		    		for (int t = 0; t < treatments_num; t ++)
		    		{
		    			if (t > 0)
		    				treatments.append(";");
		    			treatments.append(treatments_arr.getJSONObject(t).optString("treatment_term_name"));
		    		}
		    	}
	    	}catch (NullPointerException ne) { 
	    		//do nothing
	    	}
	    	
	    	
	    	String description =  experiment.optString("description","NA");
	    	String inAccesionList = "NA";
	    	
	    	

	    	
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
		    String runType = objExperiment.optString("run_type","NA");
	    	String assembly = objExperiment.optString("assembly","NA");

	    	JSONArray controls = objExperiment.optJSONArray("possible_controls");
	    	int l = controls == null?0:controls.length();
	    	//For each control listed
	    	for (int k = 0; k < l; k ++)
	    	{
	    		JSONObject control = controls.optJSONObject(k);
	    		if (control == null)
	    			continue;
	    		String control_url = control.optString("@id");
	    		String control_accession = control.optString("accession");
	    		
	    		controlDetails = new StringBuffer();

    			URL controlURL = new URL(new StringBuffer().append(protocol).append("://").append(host).append(control_url).append("?format=json").toString());
    			Scanner scanControlPage = new Scanner(controlURL.openStream());
    			StringBuffer strBufferControlPage = new StringBuffer();
    			while (scanControlPage.hasNext())
    			{
    				strBufferControlPage.append(scanControlPage.nextLine());
    			}
    			scanControlPage.close();
    			 
    			 // build a JSON object for control experiment 
    			 JSONObject objControl = new JSONObject(strBufferControlPage.toString());
    			 JSONArray controlFiles = objControl.optJSONArray("files");
    			 int numControlFiles = controlFiles == null ? 0:controlFiles.length();
    			 for (int a = 0; a < numControlFiles; a ++)
    			 {
    				 JSONObject controlFile = controlFiles.optJSONObject(a);
    				 if (controlFile == null)
    					 continue;
    				 String controlFilestatus = controlFile.optString("status","NA");
    				 if (controlFilestatus.equals("released"))
    				 {
    					 String controlHref = controlFile.optString("href","NA");
	    				 String controlMd5 = controlFile.optString("md5sum","NA");
    					 //build one string for "control details" column, merging multiple control file metadata
    					 controlDetails.append("@").append(control_accession).append(";").append(protocol).append("://").append(host).append(controlHref).append(";").append(controlMd5);
    				 }
    			 }

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
		    	JSONObject file = files.optJSONObject(j);
		    	if (file == null)
		    		continue;
		    	String fileStatus = file.optString("status","NA");
		    	String fileFormat = file.optString("file_format","NA");
		    	long fileSize = file.optLong("file_size",0);
		    	String href = protocol + "://" + host + file.optString("href","NA");
		    	String md5sum = file.optString("md5sum","NA");
		    	String submittedBy = file.optJSONObject("submitted_by") == null? "NA":file.optJSONObject("submitted_by").optString("lab","NA").replaceFirst("/labs/", "").replace("/","");
		    	String dateCreated = file.optString("date_created","NA");
		    	
		    	int biologicalReplicate = 0;
		    	int technicalReplicate = 0;
		    	String readLength = "NA";
		    	String outputType = "NA";
		    	JSONObject replicate = file.optJSONObject("replicate");
		    	if (replicate != null && replicate.optInt("read_length") != 0)
		    	{
		    		readLength = replicate.optInt("read_length") + replicate.optString("read_length_units","NA");
		    		biologicalReplicate = replicate.optInt("biological_replicate_number");
		    		technicalReplicate = replicate.optInt("technical_replicate_number");
		    	}
		    	assembly = file.optString("assembly","NA");
		    	
		    	// Build metadata row. If you add/delete columns, remember to consolidate with title row in the beginning of method
		    	final String underscore = "_";
		    	String friendly_name = new StringBuffer().append(accession).append(underscore)
		    										     .append(organism).append(underscore)
		    										     .append(lifeStage).append(underscore)
		    										     .append(age).append(underscore)
		    										     .append(tissue).append(underscore)
		    										     .append(assay).append(underscore)
		    										     .append(target).append(underscore)
		    										     .append(biologicalReplicate).append(underscore)
		    										     .append(technicalReplicate).toString();
		    	
		    	StringBuffer row = new StringBuffer().append(accession).append(output_delimiter)
		    										 .append(inAccesionList).append(output_delimiter)
	    											 .append(organism).append(output_delimiter)
	    											 .append(lifeStage).append(output_delimiter)
	    											 .append(age).append(output_delimiter)
	    											 .append(assay).append(output_delimiter)
	    											 .append(tissue).append(output_delimiter)
	    											 .append(target).append(output_delimiter)
	    											 .append(treatments.toString()).append(output_delimiter)
	    											 .append(status).append(output_delimiter)
	    											 .append(description).append(output_delimiter)
	    											 .append(fileStatus).append(output_delimiter)
	    											 .append(fileFormat).append(output_delimiter)
	    											 .append(outputType).append(output_delimiter)
	    											 .append(fileSize).append(output_delimiter)
	    											 .append(href).append(output_delimiter)
	    											 .append(md5sum).append(output_delimiter)
	    											 .append(assembly).append(output_delimiter)
	    											 .append(submittedBy).append(output_delimiter)
	    											 .append(dateCreated).append(output_delimiter)
	    											 .append(biologicalReplicate).append(output_delimiter)
	    											 .append(technicalReplicate).append(output_delimiter)
	    											 .append(runType).append(output_delimiter)
	    											 .append(readLength).append(output_delimiter)
	    											 .append(controlDetails).append(output_delimiter)
	    											 .append(friendly_name)
	    											 .append("\n");
		    	//write row to output file
		    	outFile.write(row.toString());
		    	
		    	}
		    System.out.println("Done writing details for experiment " + (i+1) + " of " + n);	
		    }
	    	outFile.close();
		    
	    }
      	
}