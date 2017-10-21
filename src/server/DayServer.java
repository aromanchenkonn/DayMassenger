package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

public class DayServer {

	private static ArrayList streams;
	private static Statement st;
	private static Connection c;
	private static PrintWriter writer;

	public static void main(String[] args) {
		go();
	}

	private static void go() {
		streams = new ArrayList<PrintWriter>();
		setDB();
		try {
			ServerSocket ss = new ServerSocket(5001);
			while (true) {
				Socket sock = ss.accept();
				System.out.println("Connection has been accepted");
				writer = new PrintWriter(sock.getOutputStream());
				sendHistory();
				streams.add(writer);

				Thread t = new Thread(new Listner(sock));
				t.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendHistory() {
		String sql = "select msg from chart";
		ResultSet rs = null;
		try {
			rs = st.executeQuery(sql);
			while (rs.next()) {
				writer.println(rs.getString("msg"));
				writer.flush();
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static class Listner implements Runnable {

		BufferedReader reader;

		public Listner(Socket sock) {
			InputStreamReader is;
			try {
				is = new InputStreamReader(sock.getInputStream());
				reader = new BufferedReader(is);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public void run() {
			String msg;
			try {
				while ((msg = reader.readLine()) != null) {
					System.out.println(msg);
					tellEveryone(msg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private static void tellEveryone(String msg) {
		int x = msg.indexOf(":");
		String login = msg.substring(0, x);
		save(login, msg);
		Iterator it = streams.iterator();
		while (it.hasNext()) {
			try {
				writer = (PrintWriter) it.next();
				writer.println(msg);
				writer.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void save(String login, String msg) {
		String sql = "INSERT INTO chart (login,msg) VALUES('" + login + "','" + msg + "');";
		try {
			st.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void setDB() {
		String url = "jdbc:mysql://localhost:3306/messenger";
		String login = "root";
		String password = "12345";
		try {
			Class.forName("com.mysql.jdbc.Driver");
			c = DriverManager.getConnection(url, login, password);
			st = c.createStatement();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}

	}

}
