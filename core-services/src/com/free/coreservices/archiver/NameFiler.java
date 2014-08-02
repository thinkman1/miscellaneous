package com.free.coreservices.archiver;

import java.io.File;
import java.io.FilenameFilter;

public class NameFiler implements FilenameFilter {
		private String prefix;
		private boolean ignoreLockFile=false;
		public NameFiler(String prefix){
			this.prefix=prefix;
		}
		public NameFiler(String prefix,boolean ignoreLockFile){
			this.prefix=prefix;
			this.ignoreLockFile=ignoreLockFile;
		}
		@Override
		public boolean accept(File dir, String name) {
			if (name.startsWith(prefix)){
				if (!name.endsWith(".tmp")){
					if (ignoreLockFile){
						return true;
					}
					File lockFileExists = new File(dir,name+".tmp");
					if (!lockFileExists.exists()){
						return true;
					}
				}
			}
			return false;
		}
	}