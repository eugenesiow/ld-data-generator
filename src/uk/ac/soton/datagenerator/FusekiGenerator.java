package uk.ac.soton.datagenerator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

public class FusekiGenerator {

	public static void main(String[] args) {
		Properties prop = new Properties();
	 
		try {
			// load a properties file
			prop.load(new FileInputStream("config.properties"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		final String baseUri = prop.getProperty("baseUri");
		final int waitTime = Integer.parseInt(prop.getProperty("wait"));
		final String remoteRepo = prop.getProperty("remoteRepo");
		ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
		
		final AtomicInteger generatorRun = new AtomicInteger(1);
		final AtomicInteger readerRun = new AtomicInteger(1);
		final AtomicInteger monitorRun = new AtomicInteger(1);
		
		try {
			
			Thread readerThread = new Thread(new Runnable() {
				 public void run(){
					 ExecutorService executorService = Executors.newFixedThreadPool(100);
					 
					 while(readerRun.get()==1) {
						 final String dataStr = queue.poll();
						 if(dataStr!=null) {
							 executorService.execute(new Runnable() {

								@Override
								public void run() {
									UpdateRequest rq = new UpdateRequest();
									rq.add(dataStr);
									UpdateProcessor updater = UpdateExecutionFactory.createRemote(rq, remoteRepo);
									updater.execute();
//									System.out.println("id:"+Thread.currentThread().getId());
								}
								
							 });
							
						 }
					 }
					 executorService.shutdown();
					 System.out.println("End Reader Thread");
			     }
				 
			});
			
			readerThread.start();
			
			//data generator thread
			//generate data and put it in queue at intervals
			Thread generatorThread = new Thread(new Runnable() {
				
				Random randomGenerator = new Random(100);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
				
				@Override
				public void run() {
					long count = 0;
					while(generatorRun.get()==1) {
						try {
							Thread.sleep(waitTime);
							double temp = randomGenerator.nextFloat() * 10.0 + 20.0;
							String readingUri = baseUri + "temperature/" + UUID.randomUUID();
							String time = sdf.format(new Date());
							queue.add("INSERT DATA {<"+readingUri+"> <"+baseUri+"p/hasTemp> \""+temp+"\"; <"+baseUri+"p/time> \""+time+"\".}");
							count++;
							if(count%1000==0) {
								System.out.println(count);
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					System.out.println("End Generator Thread");
				}
				
			});
			generatorThread.start();
			
			//monitor thread
			Thread monitorThread = new Thread(new Runnable() {

				@Override
				public void run() {
					while(monitorRun.get()==1) {
						try {
							Thread.sleep(waitTime);
						
							if(queue.size()>5) {
								System.out.println(queue.size());
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					System.out.println("End Monitor Thread");
				}
				
			});
			monitorThread.start();
			
			//scan for input to break
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			while(true) {
				System.out.println("Enter 'exit' to end:");
		        String s = br.readLine();
		        if(s.equals("exit")) {
		        	generatorRun.set(0);
		        	readerRun.set(0);
		        	monitorRun.set(0);
		        	break;
		        }
			}
			
			System.out.println("Exiting...");
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
