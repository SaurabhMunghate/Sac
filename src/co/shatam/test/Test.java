package co.shatam.test;

import java.io.IOException;

import org.json.JSONArray;

import com.shatam.util.U;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String data = "[[\"18334 W. Purdue Ave.\",\"   \",\"Waddell\",\"AZ\",\"85355\"]]";
/*        byte[] bytesData = data.getBytes();
        URL url = new URL("http://fixaddress.com/API/bulk");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("data", data);
        OutputStream out = conn.getOutputStream();
        out.write(bytesData);
        out.flush();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String output;
        while ((output = br.readLine()) != null) {
        System.out.println(output+"jjjj");

	}*/
		
		//Soundex sound = new Soundex();
		//Metaphone phone = new Metaphone();
		
	    //U.log(sound.encode("rakesh"));
	    //U.log(phone.encode("rakesh"));
		
		
		org.json.JSONArray outputObj = new JSONArray();
		outputObj.put(data);
		U.log(outputObj.toString());
		org.json.JSONArray outputObj1 = new JSONArray();
		outputObj1.put(outputObj);
		U.log(outputObj1.toString());
	}
}
