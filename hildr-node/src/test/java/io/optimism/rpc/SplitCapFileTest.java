package io.optimism.rpc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.*;
import java.nio.file.Files;
import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.web3j.utils.Numeric;

public class SplitCapFileTest {

    private static ObjectMapper mapper = new ObjectMapper();

    private static final Key key = Keys.hmacShaKeyFor(
            Numeric.hexStringToByteArray("bf549f5188556ce0951048ef467ec93067bc4ea21acebe46ef675cd4e8e015ff"));

    public static void main(String[] args) throws IOException, InterruptedException {
        // bedrockL1
        //        System.out.println((byte) 0x8e);
        testSendJsonToGeth();
        //        testSplitCapFile();
    }

    static void testSendJsonToGeth() throws IOException, InterruptedException {
        String outputDirPath = "/Users/xiaqingchuan/WorkSpace/github/rollup/rollup_request";
        File jsonsDir = new File(outputDirPath);
        if (!jsonsDir.exists() || jsonsDir.isFile()) {
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.MINUTES)
                .writeTimeout(30, TimeUnit.MINUTES)
                .build();
        File[] files = jsonsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });
        if (files == null || files.length == 0) {
            return;
        }
        List<File> sortedFiles = Arrays.stream(files)
                .sorted(new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        var f1NoStr = f1.getName().replace("_request.debug.json", "");
                        var f2NoStr = f2.getName().replace("_request.debug.json", "");
                        return Integer.compare(Integer.parseInt(f1NoStr), Integer.parseInt(f2NoStr));
                    }
                })
                .toList();

        // initial
        //            var startIndex = 0;
        //            var id = "";
        //            var url = "http://127.0.0.1:8551";
        // op-geth record
        //    var startIndex = 0;
        //    var id = ",\"id\":421849";
        //    var url = "http://127.0.0.1:8552";
        // v24 besu record
        var startIndex = 0;
        var id = ",\"id\":880954}";
        var url = "http://127.0.0.1:8551";

        var hasFound = false;
        int fileSize = sortedFiles.size();

        for (int i = startIndex; i < fileSize; i++) {
            var jsonFile = sortedFiles.get(i);
            try (BufferedReader br = Files.newBufferedReader(jsonFile.toPath(), UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    var sendData = mapper.readValue(line, new TypeReference<>() {});
                    line = mapper.writeValueAsString(sendData);
                    if (!StringUtils.isEmpty(id) && !hasFound) {
                        if (!line.contains(id)) {
                            System.out.println(String.format("will skip json: %s", line));
                            continue;
                        } else {
                            hasFound = true;
                        }
                    }
                    System.out.println(String.format("read from: %d, will send json: %s", i, line));
                    RequestBody requestBody = RequestBody.create(line, MediaType.get("application/json"));
                    Request postReq = new Request.Builder()
                            .addHeader("authorization", String.format("Bearer %1$s", jwt(key)))
                            .post(requestBody)
                            .url(url)
                            .build();
                    Response execute = client.newCall(postReq).execute();
                    Map<String, Object> map = mapper.readValue(execute.body().byteStream(), new TypeReference<>() {});

                    if (map.get("result") == null && map.get("error") != null) {
                        System.out.println("throw error when send json: " + line);
                        System.out.println(map);
                        return;
                    }
                    System.out.println(map);
                    Thread.sleep(20);
                }
            }
        }
    }

    static void testSplitCapFile() {
        String filePath = "/Users/xiaqingchuan/WorkSpace/github/rollup/op-geth.cap";
        String outputDirPath = "/Users/xiaqingchuan/WorkSpace/github/rollup/request_rollop_new";
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File dataFile = new File(filePath);
        if (!dataFile.exists()) {
            return;
        }
        // read file in line
        var fileId = 0;
        var outputFilePath = String.format("[%d]Request.debug.json", fileId);
        var outputFile = new File(outputDirPath, outputFilePath);
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line
                if (StringUtils.isEmpty(line)) {
                    continue;
                }
                if (!line.contains("\"jsonrpc\"") || line.contains("eth_chainId")) {
                    continue;
                }
                Map<String, Object> data = mapper.readValue(line, new TypeReference<>() {});
                var json = mapper.writeValueAsString(data);
                if (!outputFile.exists()) {
                    if (!outputFile.createNewFile()) {
                        continue;
                    }
                }
                if (outputFile.length() > 1024 * 1024 * 500) {
                    fileId++;
                    outputFilePath = String.format("[%d]Request.debug.json", fileId);
                    outputFile = new File(outputDirPath, outputFilePath);
                }
                try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), UTF_8, CREATE, APPEND)) {
                    writer.write(json);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String jwt(final Key key) {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        Date expirationDate = Date.from(now.plusSeconds(60));
        return Jwts.builder()
                .setIssuedAt(nowDate)
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
