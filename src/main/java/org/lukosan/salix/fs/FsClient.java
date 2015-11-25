package org.lukosan.salix.fs;

import java.io.InputStream;
import java.util.List;

public interface FsClient {

	void putInputStream(InputStream in, String... paths);

	InputStream getInputStream(String... paths);

	List<String> listFoldersInFolder(String... paths);

	List<String> listFilesInFolder(String... paths);
}