package uk.ac.soton.datagenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class SqliteGenerator {
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
		ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
		
		final AtomicInteger generatorRun = new AtomicInteger(1);
		final AtomicInteger readerRun = new AtomicInteger(1);
		
		try {
			final BufferedWriter bw = new BufferedWriter(new FileWriter(prop.getProperty("logfile")));
			
			Thread readerThread = new Thread(new Runnable() {
				 public void run(){
					 ExecutorService executorService = Executors.newFixedThreadPool(100);
					 
					 final File dbFile = new File("temp.db");	
					try {
						final SqlJetDb db = SqlJetDb.open(dbFile, true);						
						
						while(readerRun.get()==1) {
							 final String dataStr = queue.poll();
							 if(dataStr!=null) {
								 executorService.execute(new Runnable() {

									@Override
									public void run() {
										long startTime = System.currentTimeMillis();
										
										try {        
											 db.beginTransaction(SqlJetTransactionMode.WRITE);
											 
											 Random randomGenerator = new Random(100);
											 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
											 
											 try {
											    ISqlJetTable table = db.getTable("triples");
											    String readingUri = baseUri + "temperature/" + UUID.randomUUID();
											    String time = sdf.format(new Date());
											    double temp = randomGenerator.nextFloat() * 10.0 + 20.0;
											    table.insert(readingUri, baseUri+"p/hasTemp", temp);
											    table.insert(readingUri, baseUri+"p/time", time);
											  } finally {
											    db.commit();
											  }
										 } catch(Exception e) {
											 e.printStackTrace();
										 }
										
										long timeTaken = System.currentTimeMillis() - startTime;
										try {
											bw.append(Long.toString(timeTaken));
											bw.newLine();
										} catch (IOException e) {
											e.printStackTrace();
										}
										
									}
									
								 });
								
							 }
						}
					} catch (SqlJetException e1) {
						e1.printStackTrace();
					}
					
					 
					 try {
						 bw.flush();
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					 executorService.shutdown();
					 System.out.println("End Reader Thread");
			     }
				 
			});
			
			readerThread.start();
			
			//data generator thread
			//generate data and put it in queue at intervals
			Thread generatorThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					long count = 0;
					while(generatorRun.get()==1) {
						try {
							Thread.sleep(waitTime);
							queue.add("1");
							count++;
							if(count%1000==0) {
								System.out.println(count);
								try {
									bw.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						} catch (InterruptedException e) {
							try {
								bw.append("[error]");
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							e.printStackTrace();
						}
					}
					System.out.println("End Generator Thread");
				}
				
			});
			generatorThread.start();
			
			//scan for input to break
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			while(true) {
				System.out.println("Enter 'exit' to end:");
		        String s = br.readLine();
		        if(s.equals("exit")) {
		        	generatorRun.set(0);
		        	readerRun.set(0);
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
