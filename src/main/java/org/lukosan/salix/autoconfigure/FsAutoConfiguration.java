package org.lukosan.salix.autoconfigure;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lukosan.salix.SalixService;
import org.lukosan.salix.fs.FsClient;
import org.lukosan.salix.fs.FsSalixService;
import org.lukosan.salix.fs.NixFsClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FsAutoConfiguration {

	@Configuration
	public static class SalixFsConfiguration {
		
		private static final Log logger = LogFactory.getLog(SalixFsConfiguration.class);
		
		@PostConstruct
		public void postConstruct() {
			if(logger.isInfoEnabled())
				logger.info("PostConstruct " + getClass().getSimpleName());
		}
		
		@Bean
		public SalixService fsSalixService() {
			return new FsSalixService();
		}
		
		@Bean
		@ConditionalOnMissingBean(FsClient.class)
		public FsClient nixFsClient() {
			return new NixFsClient();
		}
		
	}
	
}