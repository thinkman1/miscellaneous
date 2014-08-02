package com.jpmc.dart.util.staxutils.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jpmc.dart.util.staxutils.SimpleDataSelector;
import com.jpmc.dart.util.staxutils.StreamingXmlReader;
import com.jpmc.dart.util.staxutils.XmlEventRecorder;

public class XMLGrep {
	private static class Eqs extends SimpleDataSelector{
		String value;
		boolean matched;
		public Eqs(String path, String value) throws Exception{
			super(path);
			this.value=value;
		}

		@Override
		public void match(XmlEventRecorder recorder) throws Exception {
			super.match(recorder);
			matched= (this.value.equals(getData().get(0)));
		}

		public boolean isMatched() {
			return matched;
		}
	}


	public static void main(String[] args) {
		List<Eqs> selects = new ArrayList<Eqs>();

		try{
			// fist arg is file
			for (int i = 1; i < args.length ;i++){
				Eqs sel = new Eqs(args[i].split("=")[0],args[i].split("=")[1]);
				selects.add(sel);
			}

			Eqs sel[] = new Eqs[selects.size()];
			selects.toArray(sel);

			// recurisvely climb though files
			File start = new File(args[0]);
			search(start, sel);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void search (File f, Eqs selects[]) throws Exception{
		if(f.isDirectory()){
			for (String fileName : f.list()){
				search(new File(f,fileName), selects);
			}
		} else {
			ZipInputStream zin = new ZipInputStream(new FileInputStream(f));
			ZipEntry zen=zin.getNextEntry();
			while (zen !=null){
				if (zen.getName().endsWith(".xml")){
					break;
				}
			}

			StreamingXmlReader reader = new StreamingXmlReader();
			reader.vistXml(zin, selects);

			boolean found=true;

			for (Eqs sel : selects){
				if (!sel.matched){
					found=false;
					break;
				}
			}

			zin.close();

			if (found){
				System.out.println("Found in "+f.getAbsolutePath());
			}

		}
	}

}
