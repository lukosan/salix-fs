package org.lukosan.salix.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lukosan.salix.MapUtils;
import org.lukosan.salix.SalixConfiguration;
import org.lukosan.salix.SalixResource;
import org.lukosan.salix.SalixService;
import org.lukosan.salix.SalixTemplate;
import org.lukosan.salix.SalixUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This relies on convention over configuration:
 * 
 * Given a path, e.g. "/home/username/salix-files", it assumes there is a folder structure for each "scope" as:
 * /home/username/salix-files/scopename/urls/            <-- url go in here, serialized using json, / in filenames are replaced with _
 * /home/username/salix-files/scopename/templates/       <-- templates go in here, stored as text
 * /home/username/salix-files/scopename/configurations/  <-- configurations go in here, serialized using json
 * /home/username/salix-files/scopename/resources/       <-- resources go in here, uploaded as whatever files you like and the resourceType
 *                                                           and contentType are guessed from each file's extension
 */
public class FsSalixService implements SalixService {

	private static final Log logger = LogFactory.getLog(FsSalixService.class);
	
	@Autowired
	private FsClient client;
	
	private ObjectMapper mapper;
	
	public FsSalixService() {
		mapper = new ObjectMapper();
	}
	
	@Override
	public Set<String> scopes() {
		return new HashSet<String>(client.listFoldersInFolder(""));
	}

	@Override
	public List<SalixConfiguration> configurationsFor(String target) {
		List<SalixConfiguration> configs = new ArrayList<SalixConfiguration>();
		for(String scope : scopes()) {
			for(String key : client.listFilesInFolder(scope, "configurations")) {
				if(key.equalsIgnoreCase(target))
					configs.add(configuration(scope, key));
			}
		}
		return configs.stream().filter(c -> null != c).collect(Collectors.toList());
	}

	@Override
	public SalixConfiguration configuration(String scope, String target) {
		if(StringUtils.isEmpty(target))
			return null;
		try {
			return mapper.readValue(client.getInputStream(scope, "configurations", target), FsSalixConfiguration.class);
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public SalixConfiguration save(String scope, String target, Map<String, Object> map) {
		try {
			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream in = new PipedInputStream(out);
			FsSalixConfiguration configuration = new FsSalixConfiguration(scope, target, map);
			mapper.writeValue(out, configuration);
			client.putInputStream(in, scope, "configurations", target);
			return configuration;
		} catch (IOException e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public List<SalixUrl> activeUrls() {
		// all are active
		return allUrls();
	}

	@Override
	public SalixUrl url(String url, String scope) {
		if(StringUtils.isEmpty(url))
			return null;
		try {
			return mapper.readValue(client.getInputStream(scope, "urls", url.replace('/', '_')), FsSalixUrl.class);
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public SalixUrl save(SalixUrl salixUrl) {
		try {
			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream in = new PipedInputStream(out);
			mapper.writeValue(out, salixUrl);
			client.putInputStream(in, salixUrl.getScope(), "urls", salixUrl.getUrl().replace('/',  '_'));
			return salixUrl;
		} catch (IOException e) {
			logger.error(e);
			return salixUrl;
		}
	}

	@Override
	public SalixUrl save(String scope, String url, int status, String view, LocalDateTime published, LocalDateTime removed, Map<String, Object> map) {
		FsSalixUrl s3url = new FsSalixUrl(scope, url, view, map);
		return save(s3url);
	}

	@Override
	public SalixTemplate template(String name) {
		if(StringUtils.isEmpty(name))
			return null;
		try {
			return mapper.readValue(client.getInputStream("templates", name), FsSalixTemplate.class);
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public SalixTemplate template(String name, String scope) {
		if(StringUtils.isEmpty(name))
			return null;
		try {
			return new FsSalixTemplate(scope, name, IOUtils.toString(client.getInputStream(scope, "templates", name)));
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public SalixTemplate save(String scope, String name, String source) {
		try {
			FsSalixTemplate template = new FsSalixTemplate(scope, name, source);
			InputStream in = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
			client.putInputStream(in, scope, "templates", name);
			return template;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public SalixResource resource(String sourceId, String scope) {
		return new FsSalixResource(scope, sourceId, client);
	}

	@Override
	public SalixResource save(String scope, String sourceId, String sourceUri, Map<String, Object> map) {
		InputStream in = new ByteArrayInputStream(MapUtils.asString(map).getBytes(StandardCharsets.UTF_8));
		client.putInputStream(in, scope, "templates", sourceId);
		return new FsSalixResource(scope, sourceId, client);
	}

	@Override
	public SalixResource save(String scope, String sourceId, String sourceUri, String contentType, String text) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SalixResource save(String scope, String sourceId, String sourceUri, String contentType, byte[] bytes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SalixTemplate> templatesIn(String scope) {
		List<SalixTemplate> templates = new ArrayList<SalixTemplate>();
		for(String key : client.listFilesInFolder(scope, "templates")) {
			templates.add(template(key, scope));
		}
		return templates.stream().filter(c -> null != c).collect(Collectors.toList());
	}

	@Override
	public List<SalixResource> resourcesIn(String scope) {
		List<SalixResource> resources = new ArrayList<SalixResource>();
		for(String key : client.listFilesInFolder(scope, "resources")) {
			resources.add(resource(key, scope));
		}
		return resources.stream().filter(c -> null != c).collect(Collectors.toList());
	}
	
	@Override
	public List<SalixUrl> urlsIn(String scope) {
		List<SalixUrl> urls = new ArrayList<SalixUrl>();
		for(String key : client.listFilesInFolder(scope, "urls")) {
			urls.add(url(key.replace('_', '/'), scope));
		}
		return urls.stream().filter(c -> null != c).collect(Collectors.toList());
	}
	
	@Override
	public List<SalixConfiguration> configurationsIn(String scope) {
		List<SalixConfiguration> configs = new ArrayList<SalixConfiguration>();
		for(String key : client.listFilesInFolder(scope, "configurations")) {
			configs.add(configuration(scope, key));
		}
		return configs.stream().filter(c -> null != c).collect(Collectors.toList());
	}

}
