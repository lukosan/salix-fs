package org.lukosan.salix.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
			logger.error(e);
		}
	}

	@Override
	public InputStream getInputStream(String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		try {
			return new FileInputStream(path);
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
		return null;
	}

	@Override
	public List<String> listFoldersInFolder(String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		return Arrays.asList(new File(path).list(DirectoryFileFilter.DIRECTORY));
	}

	@Override
	public List<String> listFilesInFolder(String... paths) {
		String path = rootPath + delimiter + StringUtils.arrayToDelimitedString(paths, delimiter);
		return Arrays.asList(new File(path).list(FileFileFilter.FILE));
	}
}
