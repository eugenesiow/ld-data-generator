package uk.ac.soton.datagenerator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class AarhusGenerator {

	public static void main(String[] args) {
		Properties prop = new Properties();
	 
		try {
			// load a properties file
			prop.load(new FileInputStream("config.properties"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		String path = "data/aarhus_parking.csv";
		try {
			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
			DateTime dt = fmt.parseDateTime(prop.getProperty("startDate"));
			int speed = Integer.parseInt(prop.getProperty("speed"));
			
			BufferedReader br = new BufferedReader(new FileReader(path));
			
			br.readLine(); //header
			
			String line="";
			while((line=br.readLine())!=null) {
				String[] parts = line.split(",");
				DateTime dtNext = fmt.parseDateTime(parts[1]);
				long timeDiff = dtNext.getMillis()-dt.getMillis();
				if(timeDiff<0)
					timeDiff = 0;
				timeDiff /= speed;
				Thread.sleep(timeDiff);
				
				dt = dtNext;
				
//				UpdateRequest rq = new UpdateRequest();
//				String baseUri = "http://localhost/";
//				rq.add("INSERT DATA {<"+baseUri+"events/"+UUID.randomUUID()+"> <"+baseUri+"carpark> \""+parts[4]+"\"; <"+baseUri+"time> \""+parts[1]+"\"; <"+baseUri+"vehicles> \""+parts[0]+"\"}");
////				System.out.println(rq.toString());
//				UpdateProcessor updater = UpdateExecutionFactory.createRemote(rq, "http://192.168.72.2:3030/test/update");
//				updater.execute();
				System.out.println(line);
			}
			
			br.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
	}

}
