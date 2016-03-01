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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The suggested layout is:
 * 
 * Given a path, e.g. "/home/username/salix-files", there exists a folder structure for each "scope" as:
 * /home/username/salix-files/scopename/urls/            -- urls go in here, serialized using yaml or json, "index" denotes a folder's url
 * /home/username/salix-files/scopename/templates/       -- templates go in here, stored as text but using whatever template format you like
 * /home/username/salix-files/scopename/configuration/   -- configurations go in here, serialized using json
 * /home/username/salix-files/scopename/resource/        -- resources go in here, uploaded as whatever files you like and the resourceType
 *                                                           and contentType are guessed from each file's extension
 */
public class FsSalixService implements SalixService {

	private static final Log logger = LogFactory.getLog(FsSalixService.class);
	
	@Value("${salix.fs.url.path:urls}")
	private String urlPath;
	@Value("${salix.fs.url.suffix:.yml}")
	private String urlSuffix;
	
	@Value("${salix.fs.template.path:templates}")
	private String templatePath;
	@Value("${salix.fs.template.suffix:.html}")
	private String templateSuffix;
	
	@Value("${salix.fs.resource.path:resources}")
	private String resourcePath;
	@Value("${salix.fs.resource.suffix:}")
	private String resourceSuffix;
	
	@Value("${salix.fs.configuration.path:configurations}")
	private String configurationPath;
	@Value("${salix.fs.configuration.suffix:.json}")
	private String configurationSuffix;
	
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
			for(String key : client.listFilesInFolder(scope, configurationPath)) {
				if(target.equalsIgnoreCase(strip(key, configurationSuffix)) && matches(key, configurationSuffix, templatePath, urlPath))
					configs.add(configuration(scope, strip(key, configurationSuffix)));
			}
		}
		return configs.stream().filter(c -> null != c).collect(Collectors.toList());
	}

	@Override
	public SalixConfiguration configuration(String scope, String target) {
		if(StringUtils.isEmpty(target))
			return null;
		InputStream stream = client.getInputStream(scope, configurationPath, target + configurationSuffix);
		try {
			return stream == null ? null : mapper.readValue(stream, FsSalixConfiguration.class);
		} catch (Exception e) {
			logger.error(e);
			return null;
		} finally {
			close(stream);
		}
	}

	private void close(InputStream stream) {
		if(null == stream)
			return;
		try {
			stream.close();
		} catch (IOException e) {
			logger.error(e);
		}
	}

	@Override
	public SalixConfiguration save(String scope, String target, Map<String, Object> map) {
		try {
			FsSalixConfiguration configuration = new FsSalixConfiguration(scope, target, map);
			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream in = new PipedInputStream(out);
			new Thread(new Runnable() {
				    public void run(){
				    	try {
							mapper.writeValue(out, configuration);
						} catch (IOException e) {
							logger.error(e);
						}
				    }
				}).start();
			client.putInputStream(in, scope, configurationPath, target + configurationSuffix);
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
		url = toFsUrl(url);
		if(StringUtils.isEmpty(url))
			return null;
		InputStream stream = client.getInputStream(scope, urlPath, url + urlSuffix);
		try {
			if(urlSuffix.endsWith("json"))
				return stream == null ? null : mapper.readValue(stream, FsSalixUrl.class);
			return new Yaml().loadAs(stream, FsSalixUrl.class);
		} catch (Exception e) {
			logger.error("Problem reading SalixUrl", e);
			return null;
		} finally {
			close(stream);
		}
	}

	@Override
	public SalixUrl save(SalixUrl salixUrl) {
		try {
			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream in = new PipedInputStream(out);
			new Thread(new Runnable() {
				    public void run(){
				    	try {
							mapper.writeValue(out, salixUrl);
						} catch (IOException e) {
							logger.error(e);
						}
				    }
				}).start();
			client.putInputStream(in, salixUrl.getScope(), urlPath, toFsUrl(salixUrl.getUrl()) + urlSuffix);
			return salixUrl;
		} catch (IOException e) {
			logger.error(e);
			return salixUrl;
		}
	}
	
	private String toFsUrl(String url) {
		if(url.length() == 0) url += "/";
		if(url.endsWith("/")) url += "index";
		return url;
	}

	@Override
	public SalixUrl save(String scope, String url, int status, String view, LocalDateTime published, LocalDateTime removed, Map<String, Object> map) {
		FsSalixUrl s3url = new FsSalixUrl(scope, url, status, view, map);
		return save(s3url);
	}

	@Override
	public SalixTemplate template(String name) {
		if(StringUtils.isEmpty(name))
			return null;
		InputStream stream = null;
		try {
			stream = client.getInputStream(templatePath, name + templateSuffix);
			return stream == null ? null : mapper.readValue(stream, FsSalixTemplate.class);
		} catch (Exception e) {
			logger.error(e);
			return null;
		} finally {
			close(stream);
		}
	}

	@Override
	public SalixTemplate template(String name, String scope) {
		if(StringUtils.isEmpty(name))
			return null;
		InputStream stream = null;
		try {
			stream = client.getInputStream(scope, templatePath, name + templateSuffix);
			return stream == null ? null : new FsSalixTemplate(scope, name, IOUtils.toString(stream));
		} catch (Exception e) {
			logger.error(e);
			return null;
		} finally {
			close(stream);
		}
	}

	@Override
	public SalixTemplate save(String scope, String name, String source) {
		try {
			FsSalixTemplate template = new FsSalixTemplate(scope, name, source);
			InputStream in = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
			client.putInputStream(in, scope, templatePath, name + templateSuffix);
			return template;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public SalixResource resource(String sourceId, String scope) {
		return new FsSalixResource(scope, resourcePath, sourceId + resourceSuffix, client);
	}

	@Override
	public SalixResource save(String scope, String sourceId, String sourceUri, Map<String, Object> map) {
		InputStream in = new ByteArrayInputStream(MapUtils.asString(map).getBytes(StandardCharsets.UTF_8));
		client.putInputStream(in, scope, resourcePath, sourceId + resourceSuffix);
		return new FsSalixResource(scope, resourcePath, sourceId + resourceSuffix, client);
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
		for(String key : client.listFilesInFolder(scope, templatePath)) {
			if(matches(key, templateSuffix, configurationPath, resourcePath))
				templates.add(template(strip(key, templateSuffix), scope));
		}
		return templates.stream().filter(c -> null != c).collect(Collectors.toList());
	}

	@Override
	public List<SalixResource> resourcesIn(String scope) {
		List<SalixResource> resources = new ArrayList<SalixResource>();
		for(String key : client.listFilesInFolder(scope, resourcePath)) {
			if(matches(key, resourceSuffix, templatePath, urlPath))
				resources.add(resource(strip(key, resourceSuffix), scope));
		}
		return resources.stream().filter(c -> null != c).collect(Collectors.toList());
	}
	
	@Override
	public List<SalixUrl> urlsIn(String scope) {
		List<SalixUrl> urls = new ArrayList<SalixUrl>();
		for(String key : client.listFilesInSubFolders(scope, urlPath)) {
			if(matches(key, urlSuffix, configurationPath, resourcePath))
				urls.add(url(strip(key, urlSuffix), scope));
		}
		return urls.stream().filter(c -> null != c).collect(Collectors.toList());
	}
	
	@Override
	public List<SalixConfiguration> configurationsIn(String scope) {
		List<SalixConfiguration> configs = new ArrayList<SalixConfiguration>();
		for(String key : client.listFilesInFolder(scope, configurationPath)) {
			if(matches(key, configurationSuffix, urlPath, resourcePath, templatePath))
				configs.add(configuration(scope, strip(key, configurationSuffix)));
		}
		return configs.stream().filter(c -> null != c).collect(Collectors.toList());
	}

	private String strip(String key, String suffix) {
		return key.substring(0, key.length() - suffix.length());
	}

	private boolean matches(String key, String suffix, String... exclusions) {
		for(String exclusion : exclusions)
			if(StringUtils.hasText(exclusion) && key.startsWith(exclusion))
				return true;
		return (StringUtils.hasText(suffix) ? key.endsWith(suffix) : true);
	}
	
	public static String arrayToDelimitedString(String[] arr, String delim) {
		if (ObjectUtils.isEmpty(arr)) {
			return "";
		}
		if (arr.length == 1) {
			return ObjectUtils.nullSafeToString(arr[0]);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if(StringUtils.hasText(arr[i])) {
				if (sb.length() > 0) {
					sb.append(delim);
				}
				sb.append(trim(arr[i], delim));
			}
		}
		return sb.toString();
	}

	private static String trim(String raw, String delim) {
		if(raw.startsWith(delim))
			raw = raw.substring(1);
		if(raw.endsWith(delim))
			raw = raw.substring(0, raw.length() - 1);
		return raw;
	}

}
