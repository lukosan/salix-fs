package org.lukosan.salix.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

public class NixFsClient implements FsClient {

	private static final Log logger = LogFactory.getLog(NixFsClient.class);
	
	@Value("${salix.fs.path}")
	private String rootPath;
	
	private static final String delimiter = "/";
	
	@Override
	public void putInputStream(InputStream in, String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		try {
			FileOutputStream out = new FileOutputStream(path);
			IOUtils.copy(in, out);
		} catch (IOException e) {
			logger.error("Error writing " + path);
		}
	}

	@Override
	public InputStream getInputStream(String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		try {
			return new FileInputStream(path);
		} catch (FileNotFoundException e) {
			logger.error("Error reading " + path);
		}
		return null;
	}

	@Override
	public List<String> listFoldersInFolder(String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		if(new File(path).exists())
			return Arrays.asList(new File(path).list(DirectoryFileFilter.DIRECTORY));
		return Collections.emptyList();
	}

	@Override
	public List<String> listFilesInFolder(String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		if(new File(path).exists())
			return Arrays.asList(new File(path).list(FileFileFilter.FILE));
		return Collections.emptyList();
	}

	@Override
	public boolean exists(String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		return new File(path).exists();
	}
}
