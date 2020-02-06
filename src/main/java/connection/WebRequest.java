package connection;

import javafx.scene.control.TextInputDialog;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;

public class WebRequest {

    public static String requestPOST(String URLStr, String parameterString){
        HttpURLConnection connection = null;
        InputStream in = null;
        BufferedReader reader = null;
        OutputStream out = null;

        try {
            //接続するURLを指定する
            URL url = new URL(URLStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            PrintWriter printWriter = new PrintWriter(connection.getOutputStream());
            printWriter.print(parameterString);
            printWriter.close();

            connection.connect();

            // POSTデータ送信処理

            out = connection.getOutputStream();
            out.write("POST DATA".getBytes("UTF-8"));
            out.flush();

            int status = connection.getResponseCode();
            System.out.println("HTTP status:" + status);

            if (status == HttpURLConnection.HTTP_OK) {

                in = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(in));

                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                return output.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static String requestGET(String URLStr, HashMap<String,String> headers){
        HttpURLConnection connection = null;
        InputStream in = null;
        BufferedReader reader = null;
        OutputStream out = null;

        try {
            //接続するURLを指定する
            URL url = new URL(URLStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            for(var entry: headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.connect();

            int status = connection.getResponseCode();
            System.out.println("HTTP status:" + status);

            if (status == HttpURLConnection.HTTP_OK) {

                in = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                System.out.println(output.toString());
                return output.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}
