package com.jpmc.dart.filesync.http;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * read the file from the local file system and dump it to the http input stream
 * with NIO methods.
 * @author E001668
 *
 */
public class NioFileRequestEntity implements RequestEntity {
	private static final Log LOG = LogFactory.getLog(NioFileRequestEntity.class);
    private final String contentType;
    private final long length;
    private ByteBuffer buff;
    public static int SEND_BUFFER=40000;
    private RandomAccessFile file;

    public NioFileRequestEntity(final RandomAccessFile file,final String contentType,ByteBuffer sendBuffer) {
        super();
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }
    	try {
    		length= file.length();
    	} catch (Exception e){
    		throw new RuntimeException(e);
    	}
        this.contentType = contentType;
		this.buff=sendBuffer;
		this.file=file;
    }
    public long getContentLength() {
    	return length;
    }

    public String getContentType() {
        return this.contentType;
    }

    public boolean isRepeatable() {
        return false;
    }

    public void writeRequest(final OutputStream out) throws IOException {
    	StopWatch sw = new StopWatch();
    	sw.start();
    	try {
    		if (buff!=null){
    			file.seek(0);
    			// using NIO & reusable buffers  to limit the amount of GC ops we are doing.
    			int read=0;
    			while (read > -1) {
    				buff.clear();
    				read=file.getChannel().read(buff);
    				if (read > -1){
        				out.write(buff.array(), 0,read);
    				}
    			}
    			out.flush();
    		} else {
    			BufferedInputStream fin = null;
    			try {
    				fin = new BufferedInputStream(new FileInputStream(file.getFD()));
    				IOUtils.copyLarge(fin, out);
    			}
    			finally {
    				IOUtils.closeQuietly(fin);
    			}
    		}
    	} catch (Exception e) {
    		file.seek(0);
    		LOG.warn("exception on send:",e);
    		throw new IOException(e);
    	}    finally {
    		sw.stop();
    	}
    }
}
