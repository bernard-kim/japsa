/*****************************************************************************
 * Copyright (c) Minh Duc Cao, Monash Uni & UQ, All rights reserved.         *
 *                                                                           *
 * Redistribution and use in source and binary forms, with or without        *
 * modification, are permitted provided that the following conditions        *
 * are met:                                                                  * 
 *                                                                           *
 * 1. Redistributions of source code must retain the above copyright notice, *
 *    this list of conditions and the following disclaimer.                  *
 * 2. Redistributions in binary form must reproduce the above copyright      *
 *    notice, this list of conditions and the following disclaimer in the    *
 *    documentation and/or other materials provided with the distribution.   *
 * 3. Neither the names of the institutions nor the names of the contributors*
 *    may be used to endorse or promote products derived from this software  *
 *    without specific prior written permission.                             *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS   *
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, *
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR    *
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR         *
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,     *
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,       *
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR        *
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING      *
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS        *
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.              *
 ****************************************************************************/

/**************************     REVISION HISTORY    **************************
 * 21/12/2012 - Minh Duc Cao: Created                                        
 *  
 ****************************************************************************/
package japsa.tools.seq;


import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import japsa.util.CommandLine;
import japsa.util.deploy.Deployable;

import java.io.IOException;



/**
 * @author Minh Duc Cao (http://www.caominhduc.org/)
 *
 */
@Deployable(scriptName = "jsa.seq.split",
scriptDesc = "Break a multiple sequence files to each sequence per file")
public class SplitSequenceFileCmd extends CommandLine{	
	public SplitSequenceFileCmd(){
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);		
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());


		addStdInputFile();
		addStdAlphabet();

		addString("output", "out_", "Prefix of the output files");
		addString("format", "fasta", "Format of output files. Options : japsa or fasta");

		addStdHelp();		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{				
		CommandLine cmdLine = new SplitSequenceFileCmd();					
		args = cmdLine.stdParseLine(args);
		/**********************************************************************/

		//Get dna 		
		String alphabetOption = cmdLine.getStringVal("alphabet");		
		Alphabet alphabet = Alphabet.getAlphabet(alphabetOption);
		if (alphabet == null)
			alphabet = Alphabet.DNA5();

		String output = cmdLine.getStringVal("output");
		String format = cmdLine.getStringVal("format");
		String input = cmdLine.getStringVal("input");


		SequenceReader reader = SequenceReader.getReader(input);

		Sequence seq;
		if (format.equals("fasta")){
			while ((seq = reader.nextSequence(alphabet)) != null){
				seq.writeFasta(output+seq.getName()+".fasta");
			}
		}else {//if (outType.equals("jsa")){
			while ((seq = reader.nextSequence(alphabet)) != null){
				seq.writeJSA(output+seq.getName()+".jsa");
			}
		}		
		reader.close();
	}
}

/*RST*
---------------------------------------------
*jsa.seq.split*: Split multiple sequence file
---------------------------------------------

*jsa.seq.split* splits a file containing multiple sequences to each file
containing a sequence. It is included in the 
`Japsa package <http://mdcao.github.io/japsa/>`_. 
Please see check the installation_ page for instructions.  

.. _installation: ../install.html

<usage> 

*RST*/