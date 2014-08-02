package com.free.coreservices.archiver;

import java.io.File;
import java.util.List;

import jsr166y.ForkJoinPool;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * recusively crawls a large directory using threads & visitor pattern.
 * @author E001668
 *
 */
public class DirectoryCrawler {
	private static final Log LOG = LogFactory.getLog(DirectoryCrawler.class);

//	private static class QueueConsumer extends Thread{
//		private volatile boolean go=true;
//		private BlockingQueue<File> watchQueue;
//		private DirectoryCrawlerVisitor vizitors[];
//		public QueueConsumer(BlockingQueue<File> queue,DirectoryCrawlerVisitor ...visitors){
//			this.watchQueue=queue;
//			this.vizitors=visitors;
//		}
//
//		@Override
//		public void run() {
//			while (this.go){
//				try {
//					File file = watchQueue.poll(1, TimeUnit.SECONDS);
//					if (file!=null){
//						for (DirectoryCrawlerVisitor v : vizitors){
//							v.visit(file);
//						}
//					}
//				} catch (InterruptedException e){
//					Thread.currentThread().interrupt();
//					this.go=false;
//
//					// drain what you can
//					List<File> fil = new ArrayList<File>();
//					watchQueue.drainTo(fil);
//					for (File f : fil){
//						for (DirectoryCrawlerVisitor v : vizitors){
//							v.visit(f);
//						}
//					}
//					break;
//				}
//			}
//		}
//
//		void stopMe() {
//			this.go=false;
//		}
	ForkJoinPool pool;


	public DirectoryCrawler(int crawlerThreadCount) {
		pool = new ForkJoinPool(crawlerThreadCount);
	}


	public List<File> visit(File baseDir, long maxAge,String archiveNamePrefix){


//		QueueConsumer sonsumer = new QueueConsumer(notificationQueue,visitors);
//		sonsumer.start();

		StopWatch sw = new StopWatch();
		sw.start();
		LOG.info("crawl directory "+baseDir);

		// crawl &  block until done
		List<File> stuff=pool.invoke(new DirectoryCrawlerWorkers(baseDir, archiveNamePrefix,maxAge));

//		while (!pool.isQuiescent()){
//			try {
//				TimeUnit.SECONDS.sleep(5);
//			} catch (InterruptedException e){
//				LOG.info("Interrupted!");
//			}
//		}

//		sonsumer.stopMe();
//		sonsumer.interrupt();
//		// wait 10 seconds for thread to stop
//		try {
//			sonsumer.join(10000);
//		} catch (InterruptedException e){
//			Thread.currentThread().interrupt();
//		}
//
		sw.stop();
		LOG.info("done crawling directory "+baseDir+" took "+sw.getTime()+" msec");
		return stuff;
	}

	public void shutdown() {
		pool.shutdown();
	}
}