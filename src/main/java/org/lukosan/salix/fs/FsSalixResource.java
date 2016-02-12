package org.lukosan.salix.fs;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.lukosan.salix.ResourceWriter;
import org.lukosan.salix.SalixResource;
import org.lukosan.salix.SalixResourceType;

public class FsSalixResource implements SalixResource {

	private static final long serialVersionUID = 1L;

	private String scope;
	private String sourceId;
	private String path;
	private FsClient client;

	public FsSalixResource() {
		super();
	}
	
	public FsSalixResource(String scope, String path, String sourceId, FsClient client) {
		this();
		this.scope = scope;
		this.sourceId = sourceId;
		this.client = client;
		this.path = path;
	}
	
	public String getScope() {
		return scope;
	}

	public String getSourceId() {
		return sourceId;
	}

	public String getSourceUri() {
		return scope + "/resources/" + sourceId;
	}

	public String getResourceId() {
		return getSourceId();
	}

	public String getResourceUri() {
		return "/salix/resource/" + getResourceId();
	}

	private static final String[] txts = { "txt", "css" };
	private static final String[] jsons = { "json", "yml" };
	
	public SalixResourceType getResourceType() {
		for(String suffix : txts)
			if(getResourceId().endsWith("." + suffix))
				return SalixResourceType.TEXT;
		for(String suffix : jsons)
			if(getResourceId().endsWith("." + suffix))
				return SalixResourceType.JSON;
		return SalixResourceType.BINARY;
	}

	public String getContentType() {
		return null;
	}

	@Override
	public void writeTo(ResourceWriter writer) throws IOException {
		InputStream stream = client.getInputStream(scope, path, sourceId);
		try {
			IOUtils.copy(stream, writer.getOutputStream());
		} finally {
			stream.close();
		}
	}

	@Override
	public boolean exists() {		
		return client.exists(scope, path, sourceId);
	}
}