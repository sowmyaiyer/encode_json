JSON parser for ENCODE DCC data<br><br>
Compile like this <br>
javac -cp json-20140107.jar encode/json/EncodeDCCJsonParser.java<br>
(***Get java json library from http://central.maven.org/maven2/org/json/json/20140107/json-20140107.jar***)
<br><br>
Run like this<br>

java encode.json.EncodeDCCJsonParser "https://www.encodedcc.org/search/?type=experiment&replicates.library.biosample.biosample_type=tissue&organ_slims=small+intestine&assay_term_name=ChIP-seq&target.label=POLR2A" "/home/myhome/sampleOutput.tsv" "tsv" "/home/input/ENCODE2experiments.txt"
<br><br>
Sample input file from this repository(4th argument) - ENCODE2experiments.txt <br>

 * Input :
	  1. URL with query string built in. For example: https://www.encodedcc.org/search/?type=experiment&replicates.library.biosample.biosample_type=tissue&organ_slims=small+intestine&assay_term_name=ChIP-seq&target.label=POLR2A
	  2. Output file name (fully qualified)
	  3. Output file format (tsv or csv)
	  4. (optional) Fully qualified file that containts list of accession numbers in the following format. (See ENCODE2experiments.txt in this repo for a sample)<br>
	  ENCRXXXX1<br>
	  ENCRXXXX2<br>
	  .<br>
	  .<br>
	  .<br>
	  and so on<br> 
	  
* Output:
	 Program writes metadata to output file specified
