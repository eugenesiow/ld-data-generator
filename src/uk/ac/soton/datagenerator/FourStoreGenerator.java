package uk.ac.soton.datagenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
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

public class FourStoreGenerator {

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
			final BufferedWriter bw = new BufferedWriter(new FileWriter("50k_50ms.log"));
			final URL updateURL = new URL(prop.getProperty("fourStoreUpdate"));
			
			Thread readerThread = new Thread(new Runnable() {
				 public void run(){
					 ExecutorService executorService = Executors.newFixedThreadPool(100);
					 
					 while(readerRun.get()==1) {
						 String queueStrRaw = queue.poll();
						 if(queueStrRaw!=null) {
							 String[] queueStr = queueStrRaw.split("\t");
							 final String dataStr = queueStr[1];
							 final String countStr = queueStr[0];

							 executorService.execute(new Runnable() {

								@Override
								public void run() {
									long startTime = System.currentTimeMillis();
									
									try {
										HttpURLConnection connection = (HttpURLConnection) updateURL
												.openConnection();
	
										connection.setDoOutput(true);
										connection.setDoInput(true);
										connection.setRequestMethod("POST");
										connection.setConnectTimeout(1000000);
	
										DataOutputStream ps = new DataOutputStream(connection.getOutputStream());
										ps.writeBytes(dataStr);
										ps.flush();
										ps.close();
										
										BufferedReader in = new BufferedReader(new InputStreamReader(connection
												.getInputStream()));
										StringBuilder responseBuilder = new StringBuilder();
										String str;
										while (null != ((str = in.readLine()))) {
											responseBuilder.append(str + System.getProperty("line.separator")); 
										}
										in.close();
//										System.out.println(str);
									} catch(Exception e) {
										try {
											bw.append("[conerror]");
											bw.newLine();
										} catch (IOException e1) {
											// TODO Auto-generated catch block
											e1.printStackTrace();
										}
//										e.printStackTrace();
									}
									
									long timeTaken = System.currentTimeMillis() - startTime;
									try {
										bw.append(countStr + "\t" + Long.toString(timeTaken));
										bw.newLine();
									} catch (IOException e) {
										e.printStackTrace();
									}
									
								}
								
							 });
							
						 }
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
							queue.add(count+"\tupdate=INSERT+DATA+{<"+readingUri+">+<"+baseUri+"p/hasTemp>+\""+temp+"\";+<"+baseUri+"p/time>+\""+time+"\".}");
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
