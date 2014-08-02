package com.free.coreservices.directorycleaner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.free.jms.JmsAware;
import com.free.jms.JmsTemplate;
import com.jpmc.vpc.model.dart.event.DirectoryCleanerEvent;

public class FileCleanerJob implements BeanPostProcessor, JmsAware {
	private static final Log LOG = LogFactory.getLog(FileCleanerJob.class);

	private List<CleanFilesInDirectoryConfig> filesInDirecotryCleanupConfig = new ArrayList<CleanFilesInDirectoryConfig>();
	private List<CleanupDatedDirectoryConfig>  datedDirectoryCleanupConfig = new ArrayList<CleanupDatedDirectoryConfig>();
	private JmsTemplate jmsTemplate;
	private String fileInDirectoryCleanerQueue;
	private String datedDirectoryCleanerQueue;

	@Override
	public void setJmsTemplate(JmsTemplate arg0) {
		this.jmsTemplate=arg0;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {

		if (bean instanceof CleanFilesInDirectoryConfig){
			this.filesInDirecotryCleanupConfig.add((CleanFilesInDirectoryConfig)bean);
		} else if (bean instanceof CleanupDatedDirectoryConfig){
			this.datedDirectoryCleanupConfig.add((CleanupDatedDirectoryConfig)bean);
		}
		return bean;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public void publishMessages() {
		LOG.info("start directory cleaner jobs "+filesInDirecotryCleanupConfig.size()+" TO "+fileInDirectoryCleanerQueue+" "+datedDirectoryCleanupConfig.size()+" TO "+datedDirectoryCleanerQueue);

		for (CleanFilesInDirectoryConfig cleanFiles : filesInDirecotryCleanupConfig){
			File baseDir = new File(cleanFiles.getDirectory());
			if (!baseDir.exists()) {
				LOG.warn("Directory: " + cleanFiles.getDirectory() + " does not exist.");
				continue;
			}
			LOG.info("publish clean directory for " + baseDir.getAbsolutePath());

			DirectoryCleanerEvent event = new DirectoryCleanerEvent();
			event.setId(UUID.randomUUID());
			event.setDaysIdle(cleanFiles.getDaysIdle());
			event.setFileExcludePatterns(cleanFiles.getFilePatternExclude());
			event.setDirectory(baseDir.getAbsolutePath());
			jmsTemplate.publishMessage(fileInDirectoryCleanerQueue, event);
		}
		for (CleanupDatedDirectoryConfig cleanDirs : datedDirectoryCleanupConfig){
			File baseDir = new File(cleanDirs.getDirectory());
			if (baseDir.exists()) {
			for (File f : baseDir.listFiles()){
				LOG.info("publish clean directory by date "+f.getAbsolutePath());

				DirectoryCleanerEvent event = new DirectoryCleanerEvent();
				event.setId(UUID.randomUUID());
				event.setDaysIdle(cleanDirs.getDaysIdle());
				event.setFileExcludePatterns(cleanDirs.getFilePatternExclude());
				event.setDirectory(f.getAbsolutePath());
				jmsTemplate.publishMessage(datedDirectoryCleanerQueue, event);
			}
			} else {
				LOG.warn("Directory: " + cleanDirs.getDirectory() + " does not exist.");
			}
		}
	}

	public void setDatedDirectoryCleanerQueue(String datedDirectoryCleanerQueue) {
		this.datedDirectoryCleanerQueue = datedDirectoryCleanerQueue;
	}

	public void setFileInDirectoryCleanerQueue(
			String fileInDirectoryCleanerQueue) {
		this.fileInDirectoryCleanerQueue = fileInDirectoryCleanerQueue;
	}
}
