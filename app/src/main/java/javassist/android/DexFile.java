package javassist.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import dx.dex.DexOptions;
import dx.dex.cf.CfOptions;
import dx.dex.cf.CfTranslator;
import dx.dex.file.ClassDefItem;
import dx.util.FileUtils;

public class DexFile {
	private final dx.dex.file.DexFile file;
	private final DexOptions dex_options = new DexOptions();
	
	public DexFile() {
		this.file = new dx.dex.file.DexFile(dex_options);
	}
	
	public void addClass(File classFile) {
		final CfOptions cf_options = new CfOptions();
		final ClassDefItem cdi = CfTranslator.translate(classFile.getName(), FileUtils.readFile(classFile), cf_options, dex_options);
		file.add(cdi);
	}
	
	public void writeFile(String filePath) throws IOException {
		final FileOutputStream fos = new FileOutputStream(filePath);
		Throwable error = null;
		try {
			file.writeTo(fos, null, false);
		} catch (IOException e) {
			error = e;
		} finally {
			fos.close();
		}
		if (null != error) {
			new File(filePath).delete();
		}
	}
}
