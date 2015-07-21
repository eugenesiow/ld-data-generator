package uk.ac.soton.datagenerator;

import java.io.File;

import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class CreateSqlite {
	public static void main(String[] args) {
		File dbFile = new File("temp.db");
		dbFile.delete();
		
		try {
			SqlJetDb db = SqlJetDb.open(dbFile, true);
			db.beginTransaction(SqlJetTransactionMode.WRITE);
			try {
				db.createTable("CREATE TABLE triples (s VARCHAR(255) , p VARCHAR(255), \n" + 
						"             o VARCHAR(255))");
			} finally {
				db.commit();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
