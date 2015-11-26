package org.lukosan.salix.fs;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.lukosan.salix.SalixUrl;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FsSalixUrl implements SalixUrl {

	private static final long serialVersionUID = 1L;
	
	private String scope;
	private String url;
	private String view;
	private int status = 200;
	private Map<String, Object> map = new HashMap<String, Object>();
	
	public FsSalixUrl() {
		super();
	}
	public FsSalixUrl(String scope, String url, int status, String view, Map<String, Object> map) {
		this();
		setScope(scope);
		setUrl(url);
		setView(view);
		setMap(map);
		setStatus(status);
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	public String getScope() {
		return scope;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getView() {
		return view;
	}
	public void setView(String view) {
		this.view = view;
	}
	public Map<String, Object> getMap() {
		return map;
	}
	public void setMap(Map<String, Object> map) {
		this.map = map;
	}
	@JsonIgnore
	public LocalDateTime getPublished() {
		return LocalDateTime.now().minusYears(1L);
	}
	@JsonIgnore
	public LocalDateTime getRemoved() {
		return null;
	}
	public int getStatus() {
		return status > 0 ? status : 200;
	}
}
