package club.abelix.commons.util.http;

import lombok.Getter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class MultipartUtility {

    private final String boundary = "*****";
    private final String crlf = "\r\n";
    private final String twoHyphens = "--";
    @Getter
    private HttpURLConnection connection;
    private DataOutputStream request;

    public MultipartUtility(String requestURL) {
        try {
            URL url = new URL(requestURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setDoInput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + this.boundary);

            request = new DataOutputStream(connection.getOutputStream());
        } catch (IOException ignored) {
            System.out.println("Error when trying to connect to the url for uploading file in multipart/form-data, url: " + requestURL);
        }
    }

    public void addFilePart(String fieldName, File uploadFile) {
        try {
            String fileName = uploadFile.getName();
            request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    fieldName + "\";filename=\"" +
                    fileName + "\"" + this.crlf);
            request.writeBytes(this.crlf);

            byte[] bytes = Files.readAllBytes(uploadFile.toPath());
            request.write(bytes);
        } catch (IOException ignored) {
            System.out.println("Error when adding file as multipart/form-data field. Field name is " + fieldName + " and file path is " + uploadFile.getAbsolutePath());
        }
    }

    public void addBytesPart(String fieldName, String fileName, byte[] bytes) {
        try {
            request.writeBytes(this.twoHyphens + this.boundary + this.crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\";filename=\"" + fileName + "\"" + this.crlf);
            request.writeBytes(this.crlf);

            request.write(bytes);
        } catch (IOException ignored) {
            System.out.println("Error when adding bytes as multipart/form-data field. Field name is " + fieldName + " and file name is " + fileName);
        }
    }

    public void close() {
        connection.disconnect();
    }

    public void fastFinish() throws IOException {
        request.writeBytes(this.crlf);
        request.writeBytes(this.twoHyphens + this.boundary + this.twoHyphens + this.crlf);

        request.flush();
        request.close();
    }

    public String finish() throws IOException {
        String response = "error";

        fastFinish();

        int status = connection.getResponseCode();

        if (status == HttpURLConnection.HTTP_OK) {
            InputStream responseStream = new BufferedInputStream(connection.getInputStream());

            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

            String line;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            responseStreamReader.close();

            response = stringBuilder.toString();

            close();
        }

        return response;
    }
}