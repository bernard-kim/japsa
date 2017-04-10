/*****************************************************************************
 * Copyright (c) 2017 Son Hoang Nguyen (s.hoangnguyen@imb.uq.edu.au),
 *               2017 Minh Duc Cao (minhduc.cao@gmail.com).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the names of the institutions nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package japsadev.bio.np.phage;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Date;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import japsa.seq.SequenceOutputStream;
import japsa.util.HTSUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceExtractor {
	private static final Logger LOG = LoggerFactory.getLogger(SequenceExtractor.class);

	static final int FLANKING=200;

	boolean trim=false;
	//Sequence plasmid;
	String plasmidFile; //plasmid fasta/q file that are already indexed by bwa
	int s5, e5,
		s3, e3;
	String bwaExe = "bwa";
	
	public SequenceExtractor(String seqFile, String bwaExe, boolean trim, int e5, int s3) throws IOException{
		plasmidFile=seqFile;
		this.e5=e5;
		this.s5=this.e5-FLANKING;
		this.s3=s3;
		this.e3=this.s3+FLANKING;
		this.bwaExe = bwaExe;
		this.trim = trim;
	}
	
	public void extractInsertSequence(String inFile, int qual, String format, int bwaThread, String output) throws IOException, InterruptedException{
		SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);

		SamReader reader = null;
		Process bwaProcess = null;

		if (format.endsWith("am")){//bam or sam
			if ("-".equals(inFile))
				reader = SamReaderFactory.makeDefault().open(SamInputResource.of(System.in));
			else
				reader = SamReaderFactory.makeDefault().open(new File(inFile));	
		}else{
			LOG.info("Starting bwa  at " + new Date());

			ProcessBuilder pb = null;
			if ("-".equals(inFile)){
				pb = new ProcessBuilder(bwaExe, 
						"mem",
						"-t",
						"" + bwaThread,
						"-k11",
						"-W20",
						"-r10",
						"-A1",
						"-B1",
						"-O1",
						"-E1",
						"-L0",
						"-a",
						"-Y",
//						"-K",
//						"20000",
						plasmidFile,
						"-"
						).
						redirectInput(Redirect.INHERIT);
			}else{
				pb = new ProcessBuilder(bwaExe, 
						"mem",
						"-t",
						"" + bwaThread,
						"-k11",
						"-W20",
						"-r10",
						"-A1",
						"-B1",
						"-O1",
						"-E1",
						"-L0",
						"-a",
						"-Y",
//						"-K",
//						"20000",
						plasmidFile,
						inFile
						);
			}
			bwaProcess  = pb.redirectError(ProcessBuilder.Redirect.to(new File("/dev/null"))).start();

			//Logging.info("bwa started x");	
			reader = SamReaderFactory.makeDefault().open(SamInputResource.of(bwaProcess.getInputStream()));
		}

		SAMRecordIterator iter = reader.iterator();

		String currentReadName = "";
		int start = 0, end = 0,
				longestLeftFlankAlignment = 0, longestRightFlankAlignment = 0;
		SequenceOutputStream outFile = SequenceOutputStream.makeOutputStream(output);
		
		int count=0;
		SAMRecord currentRecord= null, prevRecord = null;
		while (iter.hasNext()) {
			currentRecord = iter.next();
			
			if (currentRecord.getReadUnmappedFlag())
				continue;
			if (currentRecord.getMappingQuality() <= qual)
				continue;
			count++;
			//not the first occurrance				
			if (!currentReadName.equals("") && !currentReadName.equals(currentRecord.getReadName())) {	
				if(longestLeftFlankAlignment == 0 || longestRightFlankAlignment == 0 || start>=end){
					//System.out.println("Not applicable! Smt shjtty on read " + prevRecord.getReadName() + " length=
					// " +	 "" + prevRecord.getReadLength());
				}else{
					String readSub = trim?prevRecord.getReadString().substring(start+FLANKING,end-FLANKING)
							:prevRecord.getReadString().substring(start,end);
					LOG.info("Detect insert sequence of length " + readSub.length());
					Sequence rs = new Sequence(Alphabet.DNA16(), readSub, prevRecord.getReadName());
					rs.writeFasta(outFile);	
				}
				
				//Reset all values
				start = end = 0;
				longestLeftFlankAlignment = longestRightFlankAlignment = 0;
			} 
			currentReadName = currentRecord.getReadName();
			int alignmentLength = currentRecord.getAlignmentEnd()-currentRecord.getAlignmentStart();
			//System.out.printf("Reading: %5d %s length=%d %d %d %b\n", count, currentReadName, currentRecord
			//		.getReadLength(), currentRecord.getAlignmentStart(), currentRecord.getAlignmentEnd(),
			//		currentRecord.getReadNegativeStrandFlag());

			//FIXME: this can be improved
			if (currentRecord.getAlignmentStart() <= s5 && currentRecord.getAlignmentEnd() >= e5){
				
				if(alignmentLength > longestLeftFlankAlignment){
					int [] refPositions = {s5,e5}; 
					int [] pos = HTSUtilities.positionsInRead(currentRecord, refPositions);
					//System.out.printf("...considering %5d %s %s %d %d %b\n", count, currentReadName, "FRONT",
					//		pos[0], pos[1], currentRecord.getReadNegativeStrandFlag());
					
					if(pos[0] > s5*0.8 && pos[0] < s5*1.2){
						start = pos[0];
						longestLeftFlankAlignment = alignmentLength;
					}else{
					//	System.out.println("...ignored due to wrong left flank: " + pos[0] + " compared to expected " +
					//			"value of " + s5);
					}
				}


					
			}
			if (currentRecord.getAlignmentStart() <= s3 && currentRecord.getAlignmentEnd() >= e3){
				
				if(alignmentLength > longestRightFlankAlignment){
					int [] refPositions = {s3,e3}; 
					int [] pos = HTSUtilities.positionsInRead(currentRecord, refPositions);
					System.out.printf("...considering %5d %s %s %d %d %b\n", count, currentReadName, "BACK", pos[0], pos[1], currentRecord.getReadNegativeStrandFlag());

					end = pos[1];
					longestRightFlankAlignment = alignmentLength;
				}
							
			}	
			prevRecord=currentRecord;

		}// while
		//last one
		if(longestLeftFlankAlignment == 0 || longestRightFlankAlignment == 0 || start>=end){
			System.out.println("Not applicable! Smt shjtty on read " + prevRecord.getReadName() + " length= " + prevRecord.getReadLength());
		}else{
			String readSub = prevRecord.getReadString().substring(start,end);
			System.out.println("Detect insert sequence of length " + readSub.length());
			Sequence rs = new Sequence(Alphabet.DNA16(), readSub, prevRecord.getReadName());
			rs.writeFasta(outFile);	
		}
		
		iter.close();
		outFile.close();
		reader.close();		
		if (bwaProcess != null){
			bwaProcess.waitFor();
		}

	}
	
	public static void main(String[] args){
		try {
			SequenceExtractor vector = new SequenceExtractor("/home/s.hoangnguyen/Projects/Phage/plasmid.fasta",
					 "bwa", false, 1658, 2735);
			vector.extractInsertSequence("/home/s.hoangnguyen/Projects/Phage/2d_1.fasta", 0, "fasta", 2,
					 "/home/s.hoangnguyen/Projects/Phage/insert-200.fasta");
			
//			ArrayList<Sequence> list = SequenceReader.readAll("/home/s.hoangnguyen/Projects/Phage/test.fasta",Alphabet.DNA());
//			for(Sequence e:list)
//				vector.extractInsertSequence(e);
//				
				
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	
 }
