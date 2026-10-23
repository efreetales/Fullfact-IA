package br.com.debateemcontexto;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
final class AnalysisClient {
    static JSONObject request(String endpoint, String token, String route, JSONObject input) throws Exception {
        URL url = new URL(endpoint.replaceAll("/+$", "") + route);
        if (!url.getProtocol().equals("https") || !url.getHost().endsWith(".supabase.co") || !url.getPath().startsWith("/functions/v1/fullfact")) throw new Exception("Use o endereço HTTPS do serviço Fullfact-IA no Supabase.");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(15000); connection.setReadTimeout(95000); connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST"); connection.setDoOutput(true); connection.setRequestProperty("Content-Type", "application/json; charset=utf-8"); connection.setRequestProperty("x-fullfact-token", token);
            byte[] data = input.toString().getBytes(StandardCharsets.UTF_8); connection.setFixedLengthStreamingMode(data.length);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(data); }
            int status = connection.getResponseCode(); InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) throw new Exception("O serviço não respondeu. Confira sua conexão.");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[8192];
            try (InputStream inputStream = stream) { int count; while ((count = inputStream.read(buffer)) != -1) { if (bytes.size()+count>2000000) throw new Exception("Resposta maior do que o esperado."); bytes.write(buffer,0,count); } }
            JSONObject result; try { result = new JSONObject(bytes.toString("UTF-8")); } catch (Exception error) { throw new Exception("O serviço retornou uma resposta inválida."); }
            if (status >= 300) throw new Exception(result.optString("error", "Não foi possível concluir. Tente novamente.")); return result;
        } catch (java.net.SocketTimeoutException error) { throw new Exception("A análise demorou mais que o esperado. Tente um trecho menor."); }
        catch (java.io.IOException error) { throw new Exception("Não foi possível acessar a análise. Confira a internet."); }
        finally { connection.disconnect(); }
    }
}
