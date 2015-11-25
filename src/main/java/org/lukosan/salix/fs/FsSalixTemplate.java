package org.lukosan.salix.fs;

import org.lukosan.salix.SalixTemplate;

public class FsSalixTemplate implements SalixTemplate {
	
	private static final long serialVersionUID = 1L;
	
	private String scope;
	private String name;
	private String source;

	public FsSalixTemplate() {
		super();
	}
	
	public FsSalixTemplate(String scope, String name, String source) {
		this();
		setScope(scope);
		setName(name);
		setSource(source);
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}