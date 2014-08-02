package com.free.coreservices.archiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.free.jms.JmsTemplate;
import com.jpmc.vpc.model.dart.event.FileArchiveActionEvent;

public class ArchiveKickoffJob implements BeanPostProcessor {
	private static final Log LOG = LogFactory.getLog(ArchiveKickoffJob.class);
	private JmsTemplate ctoJmsTemplate;
	private String localDir;
	private List<ArchiveConfig> fileArchiveConfiguration = new ArrayList<ArchiveConfig>();

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {

		if (bean instanceof ArchiveConfig){
			fileArchiveConfiguration.add((ArchiveConfig)bean);
		}

		return bean;
	}

	public List<ArchiveConfig> getFileArchiveConfiguration() {
		return fileArchiveConfiguration;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}


	public void publishArchiveMessages(String destination) {

		for (ArchiveConfig conf : fileArchiveConfiguration){
			// we'll get one message for each subdir under the monitored directory.  This will allow us to do all the operations in parallel.
			// make sure you strip off the prod/dr prefix since the service can deal with it, plus each region (prod/dr) will get it's own
			// queue to work on since we want to be completely active/active
			File monitorDir = new File(localDir,conf.getBaseDirectory());

			//Validate.isTrue(monitorDir.exists(),"directory "+monitorDir.getAbsolutePath()+" doesn't seem to want to exist");

			if (monitorDir.exists()){
				String files[] = null;
				File archiveFiles[];
				if (conf.isChildDirectoriesNeedArchived()){
					files=monitorDir.list();
					archiveFiles=new File[files.length];
					for (int i=0; i < files.length;i++){
						archiveFiles[i]=new File(monitorDir,files[i]);
					}
				} else {
					archiveFiles= new File[]{monitorDir};
				}

				if (!conf.isChildDirectoriesNeedArchived() && StringUtils.isEmpty(conf.getCustomArchiveFilter())){
					LOG.warn("you are creating a mega archive under the base directory but you haven't specified a filter to age entries, I will not publish this");
				} else {
					for (File fn : archiveFiles){
						LOG.info("Publish message for dir "+fn.getAbsolutePath());

						FileArchiveActionEvent action =new FileArchiveActionEvent();
						action.setAgeByName(conf.isAgeIsDirectoryName());
						action.setConcatArchives(conf.isConcatArchives());
						action.setDirectory(fn.getAbsolutePath());
						action.setIdleHours(conf.getLastModifiedHours());
						action.setLiveDays(conf.getMaxLifeDays());
						action.setUseDirNameInArchive(conf.isUseDirNameInArchiveKey());
						action.setAchiveFilePrefix(conf.getArchiveFilePrefix());
						action.setCustomArchiveFilter(conf.getCustomArchiveFilter());
						ctoJmsTemplate.publishMessage(destination, action);
					}
				}
			} else {
				LOG.info("Directory "+monitorDir.getAbsolutePath()+" isn't there...");
			}
		}
	}

	public void setFileArchiveConfiguration(
			List<ArchiveConfig> fileArchiveConfiguration) {
		this.fileArchiveConfiguration = fileArchiveConfiguration;
	}

	public void setCtoJmsTemplate(JmsTemplate ctoJmsTemplate) {
		this.ctoJmsTemplate = ctoJmsTemplate;
	}

	public void setLocalDir(String localDir) {
		this.localDir = localDir;
	}
}
