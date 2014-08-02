package com.free.coreservices.archiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jsr166y.RecursiveTask;


/**
 * since walking a huge directory tree takes *forever* treat it as a divide &amp; conquer problem.
 * @author E001668
 *
 */
public class DirectoryCrawlerWorkers extends RecursiveTask<List<File>> {
	private static final long serialVersionUID = 1L;

	//private static final Log LOG = LogFactory.getLog(DirectoryCrawlerWorkers.class);

	File baseDir;
	File files[];
	long maxAge;
	String archiveNamePrefix;

	public DirectoryCrawlerWorkers(File baseDir,String archiveNamePrefix,long maxAge){
		this.baseDir=baseDir;
		this.files=null;
		this.maxAge=maxAge;
		this.archiveNamePrefix=archiveNamePrefix;
	}

	/**
	 * @param start No idea what this is for
	 * @param end No idea what this is for
	 */
	public DirectoryCrawlerWorkers(long maxAge,String archiveNamePrefix,int start, int end,File ...files){
		this.files=files;
		this.maxAge=maxAge;
		this.archiveNamePrefix=archiveNamePrefix;
	}


	@Override
	protected List<File> compute() {
		if (baseDir!=null){
			if (baseDir.isDirectory()){
				this.files = baseDir.listFiles();
			}
		}

		List<DirectoryCrawlerWorkers> forks = new ArrayList<DirectoryCrawlerWorkers>();

		List<File> results = new ArrayList<File>();

		long now=System.currentTimeMillis();

		for (File work : files){
			if ((!work.getName().contains(archiveNamePrefix))&&
					(!work.getName().contains("-part-"))&&
					(!work.getAbsolutePath().contains(archiveNamePrefix))){
				if (work.isDirectory()){
					// see if it has files under it
					DirectoryCrawlerWorkers forker = new DirectoryCrawlerWorkers( work,archiveNamePrefix,maxAge);

					File foo[]=work.listFiles();
					if (foo.length>0){
						// if this directory has files, fork
						if (foo[0].isFile()){
							forker.fork();
							forks.add(forker);
						} else {
							results.addAll(forker.compute());
						}
					}

				} else {
					long diff = (now-work.lastModified());
					if (diff>maxAge){
						results.add(work);
					}
				}
			}
		}
		// gather the results from the forked tasks
		for (DirectoryCrawlerWorkers f : forks){
			results.addAll(f.join());
		}

		return results;
	}

}
