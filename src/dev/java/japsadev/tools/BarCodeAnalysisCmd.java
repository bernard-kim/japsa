package japsadev.tools;

import java.io.IOException;

import jaligner.matrix.MatrixLoaderException;
import japsa.util.CommandLine;
import japsa.util.deploy.Deployable;
//import japsadev.bio.hts.barcode.BarCode;
import japsadev.bio.hts.barcode.BarCodeAnalysis;
import japsadev.bio.hts.barcode.BarCodeAnalysisVerbose;

@Deployable(
		scriptName = "jsa.dev.barcode", 
		scriptDesc = "Clustering nanopore sequences based on barcode"
		)
public class BarCodeAnalysisCmd extends CommandLine{
	public BarCodeAnalysisCmd(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc()); 

		addString("bcFile", null, "Barcode file",true);		
		addString("seqFile", null, "Nanopore sequences file",true);
		addString("scriptRun", null, "Invoke command script to run npScarf",true);
		addBoolean("verbose", false, "Verbose mode to show alignments (blossom62)");
		addBoolean("print", false, "Print out demultiplexed reads to corresponding FASTA file or not.");
		addStdHelp();
	}
	public static void main(String[] args) throws IOException, InterruptedException, MatrixLoaderException{
		CommandLine cmdLine = new BarCodeAnalysisCmd ();
		args = cmdLine.stdParseLine(args);

		String bcFile = cmdLine.getStringVal("bcFile");
		String script = cmdLine.getStringVal("scriptRun");
		String seqFile = cmdLine.getStringVal("seqFile");
		Boolean v = cmdLine.getBooleanVal("verbose"),
				p = cmdLine.getBooleanVal("print");
		BarCodeAnalysis.toPrint = BarCodeAnalysisVerbose.toPrint = p;

		if(v){
			BarCodeAnalysisVerbose bc = new BarCodeAnalysisVerbose(bcFile,script);
			bc.clustering(seqFile);
		}else{
			BarCodeAnalysis bc = new BarCodeAnalysis(bcFile,script);
			bc.clustering(seqFile);
		}
		
	}
}
/*RST*
---------------------------------------------------------------------------------------
 *barcode*: real-time de-multiplexing Nanopore reads from barcode sequencing
---------------------------------------------------------------------------------------

 *barcode* (jsa.np.barcode) is a program that demultiplex the nanopore reads from 
 Nanopore barcode sequencing. Downstream analysis can be invoked concurrently by an input script.

*barcode* is included in the `Japsa package <http://mdcao.github.io/japsa/>`_.

<usage>

~~~~~~~~~~~~~~
Usage examples
~~~~~~~~~~~~~~

A summary of *barcode* usage can be obtained by invoking the --help option::

    jsa.np.barcode --help

Input
=====
 *barcode* takes three files as required input::

	jsa.np.barcode -seq <*nanopore reads*> -bc <*barcode.fasta*> -script <*analysis script*>

<*nanopore reads*> is either the long reads in FASTA/FASTQ file (after MinION sequencing is 
finished) or standard input ( specified by "-", for real-time analysis). 
	
<*barcode.fasta*> is the FASTA file of barcode sequences (given by ONT) with name correspond to the assigned sample id.

<*analysis script*> is the script for further action on the de-multiplexed reads. It must be
executable by invoking:

	<*analysis script*> <*id*>
	
in which <*id*> is the identifier of a sample as given in the <*barcode.fasta*>. The script can be set to do 
nothing if users only interest in de-multiplexing reads alone.
	
	Missing any file would break down the whole pipeline.

Output
=======
 *barcode* output depends on the <*analysis script*> because the de-multiplexed reads are streamed directly to its dedicated process.
If ones only interest in de-multiplexing alone, then the script should be as simple as to write stream to file. For example:


Real-time scaffolding for barcode sequencing
=====================
One use-case for barcode sequencing is to run *npscarf* on the resulted de-multiplexed reads. This could be done by calling a script 
that can take an output folder of long reads from a sample to scaffold its corresponding short-reads (e.g. SPAdes) assembly.

 *RST*/