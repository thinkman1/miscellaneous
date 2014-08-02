package com.jpmc.cto.dart.extract.edw.xml.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * given a FileOutputStream 
 * @author e001668
 *
 */
public class FastTarOutputStream extends TarArchiveOutputStream {
	private static final Log LOG = LogFactory.getLog(FastTarOutputStream.class);
	FileOutputStream fout;
	long currentPosition;
	private int localRecordsWritten;
	private byte padding[];
	private Field haveUnclosedEntryField;
	
	public FastTarOutputStream(FileOutputStream outputFile) throws Exception {
		super(outputFile);
		this.fout=outputFile;
		
		// close the entry    		
		haveUnclosedEntryField = TarArchiveOutputStream.class.getDeclaredField("haveUnclosedEntry");	
		haveUnclosedEntryField.setAccessible(true);
	}
	

	
	public void putArchiveEntryDirect(File file,String name) throws Exception{
		TarArchiveEntry entry = new TarArchiveEntry(name);
		
		long sourceFileLength=file.length();
		
		entry.setSize(sourceFileLength);
		this.putArchiveEntry(entry);
		//fout.flush();
		
		if (this.padding==null){
			this.padding=new byte[getRecordSize()];
			Arrays.fill(this.padding, (byte)0);
		}

		long oldPos = fout.getChannel().position();
		
		// write a filler space of nulls 
		int writeCount=(int)sourceFileLength/getRecordSize();
		if (sourceFileLength%getRecordSize()!=0){
			writeCount++;
		}
		
		// write this filler
		for (int i = 0 ; i < writeCount;i++){
			fout.write(this.padding);
		}
		fout.flush();
		
		// now overlay what you just wrote with the file data using the zero copy IO call
		FileInputStream fin = new FileInputStream(file);
		fout.getChannel().transferFrom(fin.getChannel(), oldPos, sourceFileLength);
		
		IOUtils.closeQuietly(fin);
		
		localRecordsWritten+=writeCount;
	 	
		haveUnclosedEntryField.set(this, Boolean.FALSE);
	}

	
	/**
	 * we have to override the next 3 since the class uses them to write other things.  we need to keep where the channel is pointing 
	 */
	@Override
	public void write(byte[] b) throws IOException {
		fout.getChannel().write(ByteBuffer.wrap(b));
	}
	
	  /**
     * 
     *
     * @param buf The buffer containing the record data to write.
     * @param offset The offset of the record data within buf.
     * @throws IOException on error
     */
    private void writeRecord(byte[] buf, int offset) throws IOException {
 
        if ((offset + getRecordSize()) > buf.length) {
            throw new IOException("record has length '" + buf.length
                                  + "' with offset '" + offset
                                  + "' which is less than the record size of '"
                                  + getRecordSize() + "'");
        }

        fout.getChannel().write(ByteBuffer.wrap(buf, offset, getRecordSize()));
        localRecordsWritten++;
    }

	
	  /**
     * Write an archive record to the archive.
     *
     * @param record The record data to write to the archive.
     * @throws IOException on error
     */
    private void writeRecord(byte[] record) throws IOException {
        if (record.length != getRecordSize()) {
            throw new IOException("record to write has length '"
                                  + record.length
                                  + "' which is not the record size of '"
                                  + getRecordSize() + "'");
        }

        fout.write(record);
      
        localRecordsWritten++;
    }
    
    @Override
    public void finish() throws IOException {

    	fout.flush();
    	
    	// set the record count
    	try {
    		Field f = TarArchiveOutputStream.class.getDeclaredField("recordsWritten");	
    		f.setAccessible(true);
    		f.set(this, localRecordsWritten);
    	} catch (Exception e){
    		throw new IOException("couldn't set recofd count", e);
    	}
    	
    	super.finish();
    }
    
}
